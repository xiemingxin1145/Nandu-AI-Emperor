package com.xiemingxin.nandu.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** OpenAI 官方通道。默认用小模型，玩家可以在设置页改成任意可用模型名。 */
class OpenAiProvider(
    private val apiKey: String,
    private val model: String = "gpt-4o-mini"
) : AiProvider, WorldPlanningProvider {
    override val providerType = AiProviderType.OPENAI
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> =
        OpenAiCompatibleEngine.parseEdict(
            apiKey = apiKey,
            baseUrl = "https://api.openai.com/v1",
            model = model.ifBlank { "gpt-4o-mini" },
            edictText = edictText,
            gameContext = gameContext,
            errorPrefix = "OpenAI"
        )

    override suspend fun planWorldTurn(context: WorldTurnContext): Result<WorldTurnPlan> =
        OpenAiCompatibleEngine.planWorldTurn(
            apiKey = apiKey,
            baseUrl = "https://api.openai.com/v1",
            model = model.ifBlank { "gpt-4o-mini" },
            context = context,
            errorPrefix = "OpenAI"
        )
}

class GeminiProvider(private val apiKey: String) : AiProvider {
    override val providerType = AiProviderType.GEMINI
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        return Result.failure(NotImplementedError("Gemini官方接口下一版接入；当前请先用自定义 OpenAI-compatible 中转。"))
    }
}

/** OpenRouter 默认走便宜通用模型，不要求 Sonnet 级模型才能玩。 */
class OpenRouterProvider(
    private val apiKey: String,
    private val model: String = "deepseek/deepseek-chat"
) : AiProvider, WorldPlanningProvider {
    override val providerType = AiProviderType.OPENROUTER
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> =
        OpenAiCompatibleEngine.parseEdict(
            apiKey = apiKey,
            baseUrl = "https://openrouter.ai/api/v1",
            model = model.ifBlank { "deepseek/deepseek-chat" },
            edictText = edictText,
            gameContext = gameContext,
            errorPrefix = "OpenRouter"
        )

    override suspend fun planWorldTurn(context: WorldTurnContext): Result<WorldTurnPlan> =
        OpenAiCompatibleEngine.planWorldTurn(
            apiKey = apiKey,
            baseUrl = "https://openrouter.ai/api/v1",
            model = model.ifBlank { "deepseek/deepseek-chat" },
            context = context,
            errorPrefix = "OpenRouter"
        )
}

/**
 * 任意 OpenAI-compatible 中转站。
 * apiKey 可以为空；baseUrl 可以填到 /v1，也可以直接填完整 /chat/completions。
 */
class CustomApiProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : AiProvider, WorldPlanningProvider {
    override val providerType = AiProviderType.CUSTOM
    override val isConfigured get() = baseUrl.isNotBlank() && model.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> =
        OpenAiCompatibleEngine.parseEdict(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            edictText = edictText,
            gameContext = gameContext,
            errorPrefix = "自定义中转站"
        )

    override suspend fun planWorldTurn(context: WorldTurnContext): Result<WorldTurnPlan> =
        OpenAiCompatibleEngine.planWorldTurn(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            context = context,
            errorPrefix = "自定义中转站"
        )
}

/**
 * OpenAI-compatible 轻量客户端。
 * 模型负责理解语言，本地规则负责世界真实性；模型输出不再要求百分百干净，
 * 前后解释、Markdown、think 块会由本地恢复层处理。
 */
private object OpenAiCompatibleEngine {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(55, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private data class PendingClarification(
        val providerKey: String,
        val accumulatedEdict: String
    )

    private data class CourtConversationMemory(
        val providerKey: String,
        val transcript: String
    )

    @Volatile
    private var pendingClarification: PendingClarification? = null

    /** 御前闲聊/问策的短期连续上下文。只保留最近少量文本，不写存档、不改变 GameState。 */
    @Volatile
    private var courtConversationMemory: CourtConversationMemory? = null

    suspend fun parseEdict(
        apiKey: String,
        baseUrl: String,
        model: String,
        edictText: String,
        gameContext: GameContext,
        errorPrefix: String
    ): Result<EdictResult> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.isNotBlank()) { "$errorPrefix Base URL未配置" }
            require(model.isNotBlank()) { "$errorPrefix 模型名未配置" }

            val providerKey = "${baseUrl.trim()}|${model.trim()}"
            // UI 已把“问政 / 闲聊 / 下旨”拆开。这个一次性路由只决定本轮类型，不写入玩家正文。
            val forcedInteractionType = CourtInteractionRoute.consume()
            val pending = pendingClarification?.takeIf { it.providerKey == providerKey }
            val rememberedConversation = courtConversationMemory
                ?.takeIf { it.providerKey == providerKey }
                ?.transcript
                .orEmpty()

            val modelUserPrompt = buildString {
                if (!forcedInteractionType.isNullOrBlank()) {
                    appendLine("本轮交互类型已由玩家在界面明确选择：$forcedInteractionType。禁止改判为其他类型。")
                }
                if (rememberedConversation.isNotBlank()) {
                    appendLine("御前此前连续对话（只作语境，不是新圣旨）：")
                    appendLine(rememberedConversation)
                }
                if (pending != null) {
                    appendLine("上一道未完圣意（仅作后台上下文，禁止原样复述给玩家）：${pending.accumulatedEdict}")
                    appendLine("本次玩家原话：$edictText")
                    if (forcedInteractionType == null) {
                        append("请先判断本句是否确实是在补充上一道圣意；若明显是新话题、闲谈、问策或一条新的完整命令，就按新话语分类，不要强行续接旧旨意。")
                    }
                } else {
                    append("玩家原话：$edictText")
                }
            }

            val rawText = requestText(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                systemPrompt = buildEdictSystemPrompt(gameContext),
                userPrompt = modelUserPrompt,
                // 推理型中转常把一部分 token 用在 reasoning；900 很容易只剩“思考”没有最终正文。
                maxTokens = 1500,
                temperature = 0.08,
                errorPrefix = errorPrefix
            )

            val firstPayload = AiJsonRecovery.firstJsonObject(rawText)
            val firstDecoded = firstPayload?.let { payload ->
                runCatching { json.decodeFromString(EdictResult.serializer(), payload) }.getOrNull()
            }

            val decoded = firstDecoded ?: run {
                val repaired = requestText(
                    apiKey = apiKey,
                    baseUrl = baseUrl,
                    model = model,
                    systemPrompt = repairEdictSystemPrompt(),
                    userPrompt = "$modelUserPrompt\n上一轮模型输出：${AiJsonRecovery.compactDiagnostic(rawText, 900)}",
                    maxTokens = 1100,
                    temperature = 0.0,
                    errorPrefix = errorPrefix
                )
                val repairedPayload = AiJsonRecovery.firstJsonObject(repaired)
                    ?: throw IllegalStateException("$errorPrefix 已连接，但模型没有返回可用的游戏结构；请重试或换一个更听指令的模型。")
                runCatching { json.decodeFromString(EdictResult.serializer(), repairedPayload) }
                    .getOrElse {
                        throw IllegalStateException("$errorPrefix 已连接，但返回格式仍无法用于游戏；请重试。")
                    }
            }

            val normalized = normalizeEdictResult(decoded, edictText, gameContext, forcedInteractionType)
            updateClarificationMemory(providerKey, pending, edictText, normalized)
            updateConversationMemory(providerKey, edictText, normalized)
            normalized
        }
    }

    private fun updateClarificationMemory(
        providerKey: String,
        previous: PendingClarification?,
        currentUserText: String,
        result: EdictResult
    ) {
        val type = result.interactionType.uppercase()
        if (result.clarificationNeeded && type in setOf("ORDER", "CLARIFICATION")) {
            val accumulated = if (previous != null) {
                "${previous.accumulatedEdict}\n补充：$currentUserText"
            } else {
                currentUserText
            }
            pendingClarification = PendingClarification(providerKey, accumulated.takeLast(1800))
        } else {
            pendingClarification = null
        }
    }

    private fun updateConversationMemory(
        providerKey: String,
        currentUserText: String,
        result: EdictResult
    ) {
        val type = result.interactionType.uppercase()
        if (type !in setOf("CHAT", "CONSULT")) {
            if (!result.clarificationNeeded) courtConversationMemory = null
            return
        }
        val previous = courtConversationMemory
            ?.takeIf { it.providerKey == providerKey }
            ?.transcript
            .orEmpty()
        val replies = result.npcResponses.joinToString("\n") { "${it.officerId}：${it.text}" }
        val next = buildString {
            if (previous.isNotBlank()) append(previous).append('\n')
            append("陛下：").append(currentUserText)
            if (replies.isNotBlank()) append('\n').append(replies)
        }.takeLast(2600)
        courtConversationMemory = CourtConversationMemory(providerKey, next)
    }

    suspend fun planWorldTurn(
        apiKey: String,
        baseUrl: String,
        model: String,
        context: WorldTurnContext,
        errorPrefix: String
    ): Result<WorldTurnPlan> = withContext(Dispatchers.IO) {
        runCatching {
            require(baseUrl.isNotBlank()) { "$errorPrefix Base URL未配置" }
            require(model.isNotBlank()) { "$errorPrefix 模型名未配置" }

            val rawText = requestText(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                systemPrompt = buildWorldSystemPrompt(context),
                userPrompt = "推演第${context.turn}旬。只返回规定JSON。",
                maxTokens = 900,
                temperature = 0.12,
                errorPrefix = errorPrefix
            )
            val payload = AiJsonRecovery.firstJsonObject(rawText)
                ?: throw IllegalStateException("$errorPrefix 世界推演返回中没有可用JSON")
            val result = json.decodeFromString(WorldTurnPlan.serializer(), payload)
            result.copy(
                actions = result.actions.filter { WorldAction.isValid(it.type) }.take(4),
                npcInitiatives = result.npcInitiatives.filter { it.text.isNotBlank() }.take(3)
            )
        }
    }

    private fun normalizeEdictResult(
        result: EdictResult,
        userText: String,
        context: GameContext,
        forcedInteractionType: String?
    ): EdictResult {
        val validCommands = result.commands
            .filter { EdictCommand.isValid(it.type) }
            .take(8)

        val forced = forcedInteractionType?.uppercase()?.takeIf { it in setOf("CHAT", "CONSULT", "ORDER") }
        val normalizedType = forced ?: result.interactionType.uppercase().let {
            if (it in setOf("CHAT", "CONSULT", "ORDER", "CLARIFICATION")) it
            else when {
                validCommands.isNotEmpty() -> "ORDER"
                result.clarificationNeeded -> "CLARIFICATION"
                userText.contains("？") || userText.contains("?") || userText.contains("如何") ||
                    userText.contains("怎么看") || userText.contains("多少") || userText.contains("多久") -> "CONSULT"
                else -> "CHAT"
            }
        }

        val knownOfficerIds = context.availableOfficers.map { it.id }.toSet()
        val inCourtIds = context.availableOfficers.filter { it.status == "IN_COURT" }.map { it.id }.toSet()
        val allowedResponseIds = if (normalizedType in setOf("CHAT", "CONSULT")) inCourtIds else knownOfficerIds
        val validResponses = result.npcResponses
            .filter { it.officerId in allowedResponseIds && it.text.isNotBlank() }
            .take(4)
            .toMutableList()

        // 皇帝明明在问话，殿里也明明站着官员，却因为小模型漏了 npcResponses 而全员装死：本地兜底。
        if (normalizedType in setOf("CHAT", "CONSULT") && validResponses.isEmpty()) {
            chooseFallbackCourtOfficer(userText, context)?.let { officer ->
                validResponses += NpcResponse(
                    officerId = officer.id,
                    attitude = "neutral",
                    text = localCourtFallback(userText, context, normalizedType)
                )
            }
        }

        val commandsForType = if (normalizedType == "ORDER" || normalizedType == "CLARIFICATION") {
            validCommands
        } else {
            emptyList()
        }

        val orderHasNoCommand = normalizedType == "ORDER" && commandsForType.isEmpty()
        val shouldClarify = when {
            normalizedType == "CHAT" || normalizedType == "CONSULT" -> false
            orderHasNoCommand -> true
            else -> result.clarificationNeeded
        }
        val clarification = when {
            !shouldClarify -> ""
            result.clarificationHint.isNotBlank() -> result.clarificationHint
            orderHasNoCommand -> "这道旨意还没有形成可执行命令，请补充要办什么、由谁负责，以及必要的地点、兵力或数额。"
            else -> "请再补充圣意。"
        }

        return result.copy(
            summary = result.summary.ifBlank { userText.take(100) },
            commands = commandsForType,
            npcResponses = validResponses,
            interactionType = normalizedType,
            clarificationNeeded = shouldClarify,
            clarificationHint = clarification
        )
    }

    private fun chooseFallbackCourtOfficer(userText: String, context: GameContext): OfficerContext? {
        val inCourt = context.availableOfficers.filter { it.status == "IN_COURT" }
        if (inCourt.isEmpty()) return null
        return inCourt.firstOrNull { userText.contains(it.name) }
            ?: when {
                userText.contains("兵") || userText.contains("战") || userText.contains("金") || userText.contains("前线") ->
                    inCourt.firstOrNull { it.commandSummary.contains("统帅") || it.commandSummary.contains("猛将") }
                userText.contains("钱") || userText.contains("粮") || userText.contains("国库") || userText.contains("民") ->
                    inCourt.firstOrNull { it.commandSummary.contains("文臣") || it.commandSummary.contains("谋士") }
                else -> null
            }
            ?: inCourt.first()
    }

    private fun localCourtFallback(
        userText: String,
        context: GameContext,
        interactionType: String
    ): String = when {
        userText.contains("国库") || userText.contains("钱") || userText.contains("军费") ->
            "臣在。眼下国库尚有${context.gold}贯、粮草${context.grain}石；若问还能支应多久，还须把各路军费、转运与本旬支出一并核算，臣请有司即刻具数奏明。"
        userText.contains("粮") ->
            "臣在。眼下粮草约${context.grain}石，能否久支还要看前线耗粮和漕运是否畅通，不能只看府库总数。"
        userText.contains("金") || userText.contains("前线") || userText.contains("战") || userText.contains("兵") ->
            "臣在。眼下金国威胁为${context.jinThreat}，军心${context.troopMorale}。若要进退，须把各军所在、粮道与可用兵力一并核清，臣愿据实再奏。"
        userText.contains("你们") || userText.contains("诸卿") || userText.contains("话呢") || userText.contains("在吗") ->
            "臣在。陛下垂询，臣不敢不答，请陛下发问。"
        interactionType == "CONSULT" ->
            "臣在。陛下所问，臣请据眼下实情直陈；若要定策，可从军情、钱粮与朝局三端逐项议定。"
        else -> "臣在。陛下有话，臣听着。"
    }

    private fun requestText(
        apiKey: String,
        baseUrl: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int,
        temperature: Double,
        errorPrefix: String
    ): String {
        val body = buildJsonObject {
            put("model", JsonPrimitive(model))
            put("temperature", JsonPrimitive(temperature))
            put("max_tokens", JsonPrimitive(maxTokens))
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", JsonPrimitive("system"))
                    put("content", JsonPrimitive(systemPrompt))
                })
                add(buildJsonObject {
                    put("role", JsonPrimitive("user"))
                    put("content", JsonPrimitive(userPrompt))
                })
            })
        }.toString()

        val builder = Request.Builder()
            .url(normalizeChatUrl(baseUrl))
            .addHeader("Content-Type", "application/json")
        if (apiKey.isNotBlank()) builder.addHeader("Authorization", "Bearer $apiKey")

        val request = builder
            .post(body.toRequestBody("application/json".toMediaType()))
            .build()

        val response = try {
            client.newCall(request).execute()
        } catch (_: UnknownHostException) {
            throw IllegalStateException("$errorPrefix 无法连接服务地址，请检查 Base URL 与网络。")
        } catch (_: SocketTimeoutException) {
            throw IllegalStateException("$errorPrefix 响应超时，请稍后重试或换一个更快的模型。")
        } catch (_: IOException) {
            throw IllegalStateException("$errorPrefix 网络通信失败，请检查网络与接口地址。")
        }

        response.use {
            val responseText = it.body?.string().orEmpty()
            if (!it.isSuccessful) {
                val message = when (it.code) {
                    401, 403 -> "$errorPrefix 鉴权失败，请检查 API Key。"
                    404 -> "$errorPrefix 未找到模型或接口路径，请检查模型名与 Base URL。"
                    408 -> "$errorPrefix 请求超时，请稍后重试。"
                    429 -> "$errorPrefix 请求过于频繁或额度不足，请稍后再试。"
                    in 500..599 -> "$errorPrefix 服务端暂时不可用，请稍后重试。"
                    else -> "$errorPrefix 请求失败（${it.code}），请检查接口配置。"
                }
                throw IllegalStateException(message)
            }
            val parsed = runCatching { json.parseToJsonElement(responseText) as? JsonObject }
                .getOrNull()
                ?: throw IllegalStateException("$errorPrefix 已响应，但接口返回格式不兼容 OpenAI /chat/completions。")
            return AiResponseTextExtractor.extract(parsed)
                ?: throw IllegalStateException("$errorPrefix 已响应，但没有找到模型正文。若这是推理模型，请尝试更大的输出上限或非推理型号。")
        }
    }

    private fun normalizeChatUrl(baseUrl: String): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/chat/completions")) clean else "$clean/chat/completions"
    }

    private fun buildWorldSystemPrompt(context: WorldTurnContext): String {
        val factions = context.factions.joinToString("；") {
            "${it.id}:${it.name},对玩家${it.relationToPlayer},城${it.cityCount},军${it.armyCount}${if (it.isDestroyed) ",已亡" else ""}"
        }
        val cities = context.cities.joinToString("；") {
            val ns = if (it.neighbors.isEmpty()) "-" else it.neighbors.joinToString(",")
            "${it.id}/${it.name}[${it.owner}]兵${it.troops},防${it.defense},粮${it.grain},${it.terrain},${it.controlState},邻:$ns"
        }
        val armies = context.armies.joinToString("；") {
            val cmd = if (it.commanderName.isBlank()) "无主帅" else "${it.commanderName}/${it.commanderId}"
            val target = if (it.targetCityId.isBlank()) "-" else it.targetCityId
            "${it.id}[${it.owner}]@${it.currentCityId},兵${it.troops},士${it.morale},补${it.supply},${it.status},目标$target,帅$cmd"
        }
        val officers = context.officers.joinToString("；") {
            val skills = if (it.skills.isEmpty()) "-" else it.skills.joinToString(",")
            "${it.id}/${it.name}[${it.courtFaction}]@${it.currentCityId},${it.status},统${it.command},谋${it.strategy},政${it.politics},忠${it.loyalty},技:$skills"
        }

        return """
你是《南渡无悔》的世界战略AI。玩家只控制 ${context.playerFactionId}；你只为非玩家势力提出候选行动，本地规则负责最终裁决。
不得凭空造兵、造城、造人物、瞬移或替玩家下令。每旬最多4个 actions；补给过低应优先补给，明显劣势可按兵不动。
只输出一个JSON对象，禁止Markdown、解释、分析、前后文字。

可用动作：move_army、attack_city、resupply_army、hold_army。
当前：${context.era}，第${context.turn}旬
势力：$factions
城池：$cities
军团：$armies
已知人物：$officers

输出：
{"strategySummary":"一句话","actions":[{"type":"move_army/attack_city/resupply_army/hold_army","factionId":"jin","armyId":"army_id","targetCityId":"city_id","reason":"20字内"}],"npcInitiatives":[{"officerId":"officer_id","kind":"memorial/warning/request/advice","text":"20-60字奏言"}]}
""".trimIndent()
    }

    private fun buildEdictSystemPrompt(context: GameContext): String {
        val inCourtOfficers = context.availableOfficers
            .filter { it.status == "IN_COURT" }
            .joinToString("、") { o ->
                val role = if (o.currentRole.isNotBlank() && o.currentRole != "御前待命") " [${o.currentRole}]" else ""
                "${o.name}(${o.id}$role,${o.commandSummary})"
            }
            .ifBlank { "（本轮无实名官员实际在殿）" }
        val remoteOfficers = context.availableOfficers
            .filter { it.status == "DEPLOYED" }
            .joinToString("、") { o ->
                val role = if (o.currentRole.isNotBlank()) " [${o.currentRole}]" else ""
                "${o.name}(${o.id}$role)"
            }
            .ifBlank { "（无）" }
        val leadList = if (context.pendingRecruitLeads.isNotEmpty())
            "待征辟：${context.pendingRecruitLeads.joinToString("、")}" else ""
        val cityList = context.activeCities.filter { it.owner == "song" }
            .joinToString("、") { "${it.name}(${it.id},兵${it.troops / 1000}k)" }
        val armyList = if (context.songArmies.isEmpty()) "（目前无野战军团）"
        else context.songArmies.joinToString("；") { a ->
            val tgt = if (a.targetCityId.isNotBlank()) "→${a.targetCityId}" else ""
            "${a.name}:主帅${a.commanderName},${a.troops / 1000}k兵,补给${a.supplyLevel},${a.statusLabel}$tgt"
        }

        return """
你是《南渡无悔》的御前语言理解层。你的任务不是把玩家每句话都强行变成圣旨，而是先判断玩家正在做什么，再给本地规则一个很小、很稳定的结构化结果。

interactionType 只能四选一：
CHAT：感叹、闲谈、情绪表达。commands 必须为空。若玩家明显向殿中人说话且“实际在殿官员”非空，必须至少让1人自然接话，不得让满朝集体沉默。
CONSULT：问策、询问局势、点名问某位臣子。commands 必须为空。只要“实际在殿官员”非空，必须有1-3名在殿官员回答；玩家点名且该人在殿时优先由其回答。
ORDER：明确要求执行军政、人事、财政、军事动作。仅此类型允许 commands；臣子可有意见，也可不表态。
CLARIFICATION：对上一道未完整旨意补充兵力、军费、目标、期限等。若当前上下文无法确定上一道旨意，不要擅自拼接命令，可要求澄清。

当前局势：${context.era}，第${context.currentTurn}旬
国库：${context.gold}贯；粮草：${context.grain}石；军心：${context.troopMorale}；朝堂稳定：${context.courtStability}；金国威胁：${context.jinThreat}
实际在殿官员：$inCourtOfficers
外任/领军人物（只能作为奏札、军报背景，不得伪装成肉身站在殿里）：$remoteOfficers
$leadList
宋方城池：$cityList
我方军团：$armyList

AI只理解语言，不得修改世界数字，不得决定胜负，不得让名单之外的人物凭空发言。
CHAT / CONSULT 的 npcResponses 只能使用“实际在殿官员”的 officerId；外任人物除非玩家明确引用其既有奏札，否则不要生成即时口头回答。
命令白名单：dispatch_army、assign_officer、repair_city、raise_grain、suppress_officer、reward_officer、punish_officer、appoint_governor、appoint_garrison、dismiss_officer、transfer_officer、recruit_officer、form_army、move_army、disband_army、change_army_commander、resupply_army、attack_city、retreat_army、move_capital。

输出必须从 { 开始，以与之匹配的 } 结束。禁止Markdown代码块，禁止解释，禁止“我们需要回答用户”等分析，禁止前言后记。
字段允许为空；不要为了填字段而编造人物、军队、城池或数值。

JSON：
{"interactionType":"CHAT/CONSULT/ORDER/CLARIFICATION","summary":"简短理解","commands":[],"npcResponses":[{"officerId":"真实id","attitude":"support/oppose/neutral/concerned","text":"自然奏对"}],"riskTags":[],"confidence":0.9,"clarificationNeeded":false,"clarificationHint":""}
""".trimIndent()
    }

    private fun repairEdictSystemPrompt(): String = """
你是JSON修复器。把上一轮模型输出压缩成一个可解析JSON对象，不解释，不输出Markdown。
允许缺省信息留空，不得编造军队、人物、城市或数值。
固定结构：
{"interactionType":"CHAT/CONSULT/ORDER/CLARIFICATION","summary":"","commands":[],"npcResponses":[],"riskTags":[],"confidence":0.5,"clarificationNeeded":false,"clarificationHint":""}
commands中的type仅允许游戏白名单；若不确定，commands返回空数组。
""".trimIndent()
}

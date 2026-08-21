package com.xiemingxin.nandu.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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

            val rawText = requestText(
                apiKey = apiKey,
                baseUrl = baseUrl,
                model = model,
                systemPrompt = buildEdictSystemPrompt(gameContext),
                userPrompt = "玩家原话：$edictText",
                maxTokens = 900,
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
                    userPrompt = "玩家原话：$edictText\n上一轮模型输出：${AiJsonRecovery.compactDiagnostic(rawText, 900)}",
                    maxTokens = 650,
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

            normalizeEdictResult(decoded, edictText, gameContext)
        }
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
        context: GameContext
    ): EdictResult {
        val knownOfficerIds = context.availableOfficers.map { it.id }.toSet()
        val validResponses = result.npcResponses
            .filter { it.officerId in knownOfficerIds && it.text.isNotBlank() }
            .take(4)

        val validCommands = result.commands
            .filter { EdictCommand.isValid(it.type) }
            .take(8)

        val normalizedType = result.interactionType.uppercase().let {
            if (it in setOf("CHAT", "CONSULT", "ORDER", "CLARIFICATION")) it
            else when {
                validCommands.isNotEmpty() -> "ORDER"
                result.clarificationNeeded -> "CLARIFICATION"
                userText.contains("？") || userText.contains("?") || userText.contains("如何") || userText.contains("怎么看") -> "CONSULT"
                else -> "CHAT"
            }
        }

        val commandsForType = if (normalizedType == "ORDER" || normalizedType == "CLARIFICATION") {
            validCommands
        } else {
            emptyList()
        }

        val shouldClarify = when {
            normalizedType == "CHAT" || normalizedType == "CONSULT" -> false
            commandsForType.isNotEmpty() -> result.clarificationNeeded
            else -> result.clarificationNeeded
        }

        return result.copy(
            summary = result.summary.ifBlank { userText.take(100) },
            commands = commandsForType,
            npcResponses = validResponses,
            interactionType = normalizedType,
            clarificationNeeded = shouldClarify,
            clarificationHint = if (shouldClarify) result.clarificationHint else ""
        )
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

        client.newCall(request).execute().use { response ->
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IllegalStateException("$errorPrefix API错误 ${response.code}: ${responseText.take(800)}")
            }
            val parsed = json.parseToJsonElement(responseText) as? JsonObject
                ?: throw IllegalStateException("$errorPrefix 返回不是OpenAI-compatible JSON对象")
            return extractAssistantText(parsed)
                ?: throw IllegalStateException("$errorPrefix 已响应，但无法提取模型文本")
        }
    }

    private fun normalizeChatUrl(baseUrl: String): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/chat/completions")) clean else "$clean/chat/completions"
    }

    private fun extractAssistantText(root: JsonObject): String? {
        val choices = root["choices"] as? JsonArray
        val choice = choices?.firstOrNull() as? JsonObject
        val message = choice?.get("message") as? JsonObject
        val content = message?.get("content")

        extractTextFromContent(content)?.takeIf { it.isNotBlank() }?.let { return it }

        (choice?.get("text") as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        (root["output_text"] as? JsonPrimitive)?.contentOrNull
            ?.takeIf { it.isNotBlank() }
            ?.let { return it }

        return null
    }

    private fun extractTextFromContent(content: JsonElement?): String? = when (content) {
        is JsonPrimitive -> content.contentOrNull
        is JsonArray -> content.mapNotNull { part ->
            when (part) {
                is JsonPrimitive -> part.contentOrNull
                is JsonObject -> {
                    (part["text"] as? JsonPrimitive)?.contentOrNull
                        ?: (part["content"] as? JsonPrimitive)?.contentOrNull
                }
                else -> null
            }
        }.joinToString("")
        else -> null
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
        val courtOfficers = context.availableOfficers
            .filter { it.status == "IN_COURT" || it.status == "DEPLOYED" }
            .joinToString("、") { o ->
                val role = if (o.currentRole.isNotBlank() && o.currentRole != "御前待命") " [${o.currentRole}]" else ""
                "${o.name}(${o.id}$role,${o.commandSummary})"
            }
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
CHAT：感叹、闲谈、情绪表达，例如“天下大乱啊”。commands 必须为空；可让1-2名当前真实在场人物自然接话，也可以无人接话。
CONSULT：问策、询问局势、点名问某位臣子。commands 必须为空；只让当前真实可用人物回答。
ORDER：明确要求执行军政、人事、财政、军事动作。仅此类型允许 commands。
CLARIFICATION：对上一道未完整旨意补充兵力、军费、目标、期限等。若当前上下文无法确定上一道旨意，不要擅自拼接命令，可要求澄清。

当前局势：${context.era}，第${context.currentTurn}旬
国库：${context.gold}贯；粮草：${context.grain}石；军心：${context.troopMorale}；朝堂稳定：${context.courtStability}；金国威胁：${context.jinThreat}
当前可用人物：$courtOfficers
$leadList
宋方城池：$cityList
我方军团：$armyList

AI只理解语言，不得修改世界数字，不得决定胜负，不得让不在当前可用人物列表中的历史人物发言。
命令白名单：dispatch_army、assign_officer、repair_city、raise_grain、suppress_officer、reward_officer、punish_officer、appoint_governor、appoint_garrison、dismiss_officer、transfer_officer、recruit_officer、form_army、move_army、disband_army、change_army_commander、resupply_army、attack_city、retreat_army、move_capital。

输出必须从 { 开始，以与之匹配的 } 结束。禁止Markdown代码块，禁止解释，禁止“我们需要回答用户”等分析，禁止前言后记。
字段允许为空；不要为了填字段而编造内容。不要强迫每轮都让大臣表态。

JSON：
{"interactionType":"CHAT/CONSULT/ORDER/CLARIFICATION","summary":"简短理解","commands":[],"npcResponses":[],"riskTags":[],"confidence":0.9,"clarificationNeeded":false,"clarificationHint":""}
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

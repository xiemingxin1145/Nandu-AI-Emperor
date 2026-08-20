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
 *
 * apiKey 可以为空：局域网模型、本地网关或部分自建中转不需要鉴权。
 * baseUrl 既可以填 https://host/v1，也可以直接填完整 /chat/completions 地址。
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
 * 所有 OpenAI-compatible 通道共用的轻量客户端。
 * 圣旨解析与世界推演都走结构化 JSON，但不依赖 response_format，兼容更多小模型/中转站。
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
                userPrompt = "圣旨内容：$edictText",
                maxTokens = 1200,
                temperature = 0.30,
                errorPrefix = errorPrefix
            )
            val cleanJson = extractJson(rawText)
            val result = json.decodeFromString(EdictResult.serializer(), cleanJson)
            result.copy(commands = result.commands.filter { EdictCommand.isValid(it.type) })
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
                userPrompt = "推演第${context.turn}旬。只返回规定JSON，不写推理过程。",
                maxTokens = 900,
                temperature = 0.20,
                errorPrefix = errorPrefix
            )
            val cleanJson = extractJson(rawText)
            val result = json.decodeFromString(WorldTurnPlan.serializer(), cleanJson)
            result.copy(
                actions = result.actions.filter { WorldAction.isValid(it.type) }.take(4),
                npcInitiatives = result.npcInitiatives.filter { it.text.isNotBlank() }.take(3)
            )
        }
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
                throw IllegalStateException("$errorPrefix API错误 ${response.code}: ${responseText.take(1000)}")
            }
            val parsed = json.parseToJsonElement(responseText) as? JsonObject
                ?: throw IllegalStateException("$errorPrefix 返回不是JSON对象")
            return extractAssistantText(parsed)
                ?: throw IllegalStateException("无法提取模型返回文本，请确认接口兼容 OpenAI /chat/completions")
        }
    }

    internal fun normalizeChatUrl(baseUrl: String): String {
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

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
            .removePrefix("```json")
            .removePrefix("```JSON")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
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
            "${it.id}[${it.owner}]@$${it.currentCityId},兵${it.troops},士${it.morale},补${it.supply},${it.status},目标$target,帅$cmd"
                .replace("@$", "@")
        }
        val officers = context.officers.joinToString("；") {
            val skills = if (it.skills.isEmpty()) "-" else it.skills.joinToString(",")
            "${it.id}/${it.name}[${it.courtFaction}]@${it.currentCityId},${it.status},统${it.command},谋${it.strategy},政${it.politics},忠${it.loyalty},技:$skills"
        }

        return """
你是《南渡无悔》的“世界战略AI”。玩家只控制 ${context.playerFactionId}；你负责让所有非玩家势力像真正的战略对手一样自己思考，同时让重要人物主动上奏。

核心原则：代码世界是真实世界，模型只是决策层。
1. 绝对不得凭空增加/删除兵力、城池、粮草、人物，也不得瞬移。
2. 只能控制 owner != ${context.playerFactionId} 的军团；玩家军团只能观察，不能替玩家下令。
3. 每旬最多4个 actions。宁可按兵不动，也不要无脑送死。
4. 必须考虑：兵力、士气、补给、城防、道路邻接、当前目标、地形和后路。
5. attack_city 只用于已到敌前(ENGAGEMENT_PENDING)或确实相邻的敌城；远处目标先 move_army。
6. supply < 40 时优先考虑 resupply_army；明显劣势时 hold_army 或改道，不要硬冲。
7. npcInitiatives 是人物主动说话/上奏，只能表达建议、警告、请命，不直接改数值。
8. 不写思维链，不解释算法，只输出JSON。

可用动作：
move_army: armyId + targetCityId
attack_city: armyId + targetCityId
resupply_army: armyId
hold_army: armyId
factionId 必须与该军团 owner 一致。

当前：${context.era}，第${context.turn}旬
势力：$factions
城池：$cities
军团：$armies
已知人物：$officers

严格JSON格式：
{"strategySummary":"本旬战略一句话","actions":[{"type":"move_army/attack_city/resupply_army/hold_army","factionId":"jin","armyId":"army_id","targetCityId":"city_id","reason":"20字内"}],"npcInitiatives":[{"officerId":"officer_id","kind":"memorial/warning/request/advice","text":"符合时代身份的20-60字奏言"}]}
""".trimIndent()
    }

    private fun buildEdictSystemPrompt(context: GameContext): String {
        val courtOfficers = context.availableOfficers
            .filter { it.status == "IN_COURT" || it.status == "DEPLOYED" }
            .joinToString("、") { o ->
                val role = if (o.currentRole.isNotBlank() && o.currentRole != "御前待命") " [${o.currentRole}]" else ""
                "${o.name}(${o.id}${role},${o.commandSummary})"
            }
        val leadList = if (context.pendingRecruitLeads.isNotEmpty())
            "待征辟：${context.pendingRecruitLeads.joinToString("、")}" else ""
        val cityList = context.activeCities.filter { it.owner == "song" }
            .joinToString("、") { "${it.name}(${it.id},兵${it.troops / 1000}k)" }
        val armyList = if (context.songArmies.isEmpty()) "（目前无野战军团）"
        else context.songArmies.joinToString("；") { a ->
            val tgt = if (a.targetCityId.isNotBlank()) "→${a.targetCityId}【可进攻】" else ""
            "${a.name}:主帅${a.commanderName},${a.troops / 1000}k兵,补给${a.supplyLevel},${a.statusLabel}${tgt}"
        }
        return """
你是《南渡无悔》的御前推演官，负责把皇帝自然语言圣旨解析为本地规则引擎能执行的JSON，并让群臣按性格回应。

当前局势：${context.era}，第${context.currentTurn}旬
国库：${context.gold}贯；粮草：${context.grain}石；军心：${context.troopMorale}；朝堂稳定：${context.courtStability}；金国威胁：${context.jinThreat}
在朝将吏：$courtOfficers
$leadList
宋方城池：$cityList
我方军团：$armyList

你不负责修改世界数字，也不负责决定战斗胜负；只提出白名单命令，本地代码最终裁决。
命令：dispatch_army、assign_officer、repair_city、raise_grain、suppress_officer、reward_officer、punish_officer、appoint_governor、appoint_garrison、dismiss_officer、transfer_officer、recruit_officer、form_army、move_army、disband_army、change_army_commander、resupply_army、attack_city、retreat_army。

严格只返回JSON：{"summary":"摘要","commands":[{"type":"命令类型","officerId":"","fromCityId":"","toCityId":"","cityId":"","troops":0,"role":"","severity":"","amount":0,"deadlineTurns":0}],"npcResponses":[{"officerId":"","attitude":"support/oppose/neutral/concerned","text":"文言20-50字"}],"riskTags":[],"confidence":0.9,"clarificationNeeded":false,"clarificationHint":""}

人物性格：岳飞忠烈主战；秦桧主和善权衡；赵鼎重粮道与政务；韩世忠豪勇善水战；李纲刚烈守城；吴玠擅山地守关。至少1人表态，不用现代词。
""".trimIndent()
    }
}

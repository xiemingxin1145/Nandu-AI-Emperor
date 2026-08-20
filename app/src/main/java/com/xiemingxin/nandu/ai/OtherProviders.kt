package com.xiemingxin.nandu.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class OpenAiProvider(private val apiKey: String, private val model: String = "gpt-4o") : AiProvider {
    override val providerType = AiProviderType.OPENAI
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        return OpenAiCompatibleEngine.parse(
            apiKey = apiKey,
            baseUrl = "https://api.openai.com/v1",
            model = model.ifBlank { "gpt-4o" },
            edictText = edictText,
            gameContext = gameContext,
            errorPrefix = "OpenAI"
        )
    }
}

class GeminiProvider(private val apiKey: String) : AiProvider {
    override val providerType = AiProviderType.GEMINI
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        return Result.failure(NotImplementedError("Gemini官方接口下一版接入；当前请先用自定义 OpenAI-compatible 中转或 Mock。"))
    }
}

class OpenRouterProvider(
    private val apiKey: String,
    private val model: String = "anthropic/claude-3.5-sonnet"
) : AiProvider {
    override val providerType = AiProviderType.OPENROUTER
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        return OpenAiCompatibleEngine.parse(
            apiKey = apiKey,
            baseUrl = "https://openrouter.ai/api/v1",
            model = model.ifBlank { "anthropic/claude-3.5-sonnet" },
            edictText = edictText,
            gameContext = gameContext,
            errorPrefix = "OpenRouter"
        )
    }
}

class CustomApiProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : AiProvider {
    override val providerType = AiProviderType.CUSTOM
    override val isConfigured get() = baseUrl.isNotBlank() && model.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        return OpenAiCompatibleEngine.parse(
            apiKey = apiKey,
            baseUrl = baseUrl,
            model = model,
            edictText = edictText,
            gameContext = gameContext,
            errorPrefix = "自定义中转站"
        )
    }
}

private object OpenAiCompatibleEngine {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun parse(
        apiKey: String,
        baseUrl: String,
        model: String,
        edictText: String,
        gameContext: GameContext,
        errorPrefix: String
    ): Result<EdictResult> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) return@withContext Result.failure(Exception("$errorPrefix API Key未配置"))
            if (baseUrl.isBlank()) return@withContext Result.failure(Exception("$errorPrefix Base URL未配置"))
            if (model.isBlank()) return@withContext Result.failure(Exception("$errorPrefix 模型名未配置"))

            val body = buildJsonObject {
                put("model", JsonPrimitive(model))
                put("temperature", JsonPrimitive(0.35))
                put("max_tokens", JsonPrimitive(1600))
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", JsonPrimitive("system"))
                        put("content", JsonPrimitive(buildSystemPrompt(gameContext)))
                    })
                    add(buildJsonObject {
                        put("role", JsonPrimitive("user"))
                        put("content", JsonPrimitive("圣旨内容：$edictText"))
                    })
                })
            }.toString()

            val request = Request.Builder()
                .url(normalizeChatUrl(baseUrl))
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("$errorPrefix API错误 ${response.code}: $responseText"))
            }

            val parsed = json.parseToJsonElement(responseText).jsonObject
            val rawText = parsed["choices"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("message")
                ?.jsonObject?.get("content")
                ?.jsonPrimitive?.contentOrNull
                ?: parsed["choices"]
                    ?.jsonArray?.firstOrNull()
                    ?.jsonObject?.get("text")
                    ?.jsonPrimitive?.contentOrNull
                ?: return@withContext Result.failure(Exception("无法提取模型返回文本，请确认该接口是 OpenAI-compatible 格式"))

            val cleanJson = extractJson(rawText)
            val result = json.decodeFromString(EdictResult.serializer(), cleanJson)
            Result.success(result.copy(commands = result.commands.filter { EdictCommand.isValid(it.type) }))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeChatUrl(baseUrl: String): String {
        val clean = baseUrl.trim().trimEnd('/')
        return if (clean.endsWith("/chat/completions")) clean else "$clean/chat/completions"
    }

    private fun extractJson(text: String): String {
        val trimmed = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun buildSystemPrompt(context: GameContext): String {
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
            "${a.name}:主帅${a.commanderName},${a.troops / 1000}k兵,${a.statusLabel}${tgt}"
        }
        return """
你是《南渡无悔》的御前推演官，负责解析皇帝圣旨，并让群臣按性格回应。

当前局势：${context.era}，第${context.currentTurn}旬
国库：${context.gold}贯；粮草：${context.grain}石；军心：${context.troopMorale}；朝堂稳定：${context.courtStability}；金国威胁：${context.jinThreat}
在朝将吏：$courtOfficers
$leadList
宋方城池：$cityList
我方军团（必须基于此判断，不得重复创建已有军团）：
$armyList

命令说明：
attack_city(进攻目标城，需officerId主帅或军团id+toCityId目标城，仅限ENGAGEMENT_PENDING军团或相邻敌城；AI不决定胜负数字)
retreat_army(令军团撤退，需officerId)
form_army(组建新军团，需officerId主帅+fromCityId+troops+role军型)
move_army(移动军团，需officerId主帅+toCityId目标)
disband_army(解散军团，需officerId主帅)
change_army_commander(换帅，需fromCityId旧帅id+toCityId新帅id)
resupply_army(主动补给，需officerId)
appoint_governor/appoint_garrison/dismiss_officer/transfer_officer/recruit_officer: 人事任命

重要：如果有多支ENGAGEMENT_PENDING军团，必须明确指定officerId，不得模糊。

严格只返回JSON：{"summary":"摘要","commands":[{"type":"命令类型","officerId":"","fromCityId":"","toCityId":"","cityId":"","troops":0,"role":"","severity":"","amount":0,"deadlineTurns":0}],"npcResponses":[{"officerId":"","attitude":"support/oppose/neutral/concerned","text":"文言20-50字"}],"riskTags":[],"confidence":0.9,"clarificationNeeded":false,"clarificationHint":""}

命令类型：dispatch_army、assign_officer、repair_city、raise_grain、suppress_officer、reward_officer、punish_officer、appoint_governor、appoint_garrison、dismiss_officer、transfer_officer、recruit_officer、form_army、move_army、disband_army、change_army_commander、resupply_army、attack_city、retreat_army

NPC都是南宋朝臣，不用现代词。主战派支持进攻，主和派以粮耗劝阻。至少1人表态。
人物性格：岳飞忠烈主战 秦桧主和避战 赵鼎重粮道 韩世忠豪勇 李纲刚烈守城 吴玠擅山地
""".trimIndent()
    }
}

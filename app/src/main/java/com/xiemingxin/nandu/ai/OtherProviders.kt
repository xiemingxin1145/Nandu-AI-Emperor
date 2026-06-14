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
        val officerList = context.availableOfficers.joinToString("、") { "${it.name}(${it.id},${it.faction},${it.currentCityId})" }
        val cityList = context.activeCities.joinToString("、") { "${it.name}(${it.id},${it.owner},兵${it.troops},防${it.defense})" }
        return """
你是《南渡无悔》的御前推演官，负责解析皇帝圣旨，并让群臣按性格回应。

当前局势：${context.era}，第${context.currentTurn}旬
国库：${context.gold}贯；粮草：${context.grain}石；军心：${context.troopMorale}；朝堂稳定：${context.courtStability}；金国威胁：${context.jinThreat}
可用人物：$officerList
城池：$cityList

严格只返回 JSON，不要解释，不要 markdown。
格式：
{"summary":"摘要","commands":[{"type":"命令类型","officerId":"","fromCityId":"","toCityId":"","cityId":"","troops":0,"role":"","severity":"","amount":0,"deadlineTurns":0}],"npcResponses":[{"officerId":"","attitude":"support/oppose/neutral/concerned","text":"文言20到50字"}],"riskTags":[],"confidence":0.9,"clarificationNeeded":false,"clarificationHint":""}

命令类型只允许：dispatch_army、assign_officer、repair_city、raise_grain、suppress_officer、reward_officer、punish_officer、move_capital。
人物性格：岳飞忠烈主战，秦桧主和避战，赵鼎重粮道财计，韩世忠豪勇水战，李纲刚烈守城，宗泽北望中原，吴玠擅山地守关。
必须让至少1名大臣提出支持、担忧或反对，不能全部只会遵旨。
""".trimIndent()
    }
}

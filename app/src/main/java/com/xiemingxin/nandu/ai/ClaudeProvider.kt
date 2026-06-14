package com.xiemingxin.nandu.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ClaudeProvider(private val apiKey: String) : AiProvider {

    override val providerType = AiProviderType.CLAUDE
    override val isConfigured get() = apiKey.isNotBlank()

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun buildSystemPrompt(context: GameContext): String {
        val officerList = context.availableOfficers.joinToString("、") { "${it.name}(${it.currentCityId})" }
        val cityList = context.activeCities.joinToString("、") { "${it.name}兵${it.troops}" }
        return """你是《南渡无悔》的御前推演官，负责解析皇帝的圣旨并生成群臣反应。

当前局势（${context.era} 第${context.currentTurn}旬）：
国库：${context.gold}贯  粮草：${context.grain}石
军心：${context.troopMorale}  朝堂稳定：${context.courtStability}  金国威胁：${context.jinThreat}
可用武将：$officerList
城池状况：$cityList

严格返回JSON，无其他文字：
{"summary":"摘要","commands":[{"type":"命令类型","officerId":"","fromCityId":"","toCityId":"","cityId":"","troops":0,"role":"","severity":"","amount":0,"deadlineTurns":0}],"npcResponses":[{"officerId":"","attitude":"support/oppose/neutral/concerned","text":"文言20-40字"}],"riskTags":[],"confidence":0.9,"clarificationNeeded":false,"clarificationHint":""}

命令类型只能是：dispatch_army/assign_officer/repair_city/raise_grain/suppress_officer/reward_officer/punish_officer

武将性格（必须遵守）：
yue_fei忠烈主战铿锵 qin_hui主和阴柔暗指风险 zhao_ding稳重理财先问粮道 han_shizhong豪勇直爽短句有力 li_gang刚烈守城慷慨激昂

只选最相关2-4人回应。"""
    }

    override suspend fun parseEdict(
        edictText: String,
        gameContext: GameContext
    ): Result<EdictResult> = withContext(Dispatchers.IO) {
        try {
            if (!isConfigured) {
                return@withContext Result.failure(Exception("Claude API Key未配置"))
            }

            val systemPrompt = buildSystemPrompt(gameContext)
            val systemEscaped = systemPrompt
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
            val edictEscaped = edictText
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")

            val body = """{"model":"claude-sonnet-4-6","max_tokens":1500,"system":"$systemEscaped","messages":[{"role":"user","content":"圣旨内容：$edictEscaped"}]}"""

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(body.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string()
                ?: return@withContext Result.failure(Exception("API返回为空"))

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API错误 ${response.code}: $responseText"))
            }

            val parsed = json.parseToJsonElement(responseText).jsonObject
            val rawText = parsed["content"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("无法提取响应文本"))

            val cleanJson = rawText.trim()
                .removePrefix("```json").removePrefix("```")
                .removeSuffix("```").trim()

            val result = Json.decodeFromString<EdictResult>(cleanJson)

            Result.success(result.copy(
                commands = result.commands.filter { EdictCommand.isValid(it.type) }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

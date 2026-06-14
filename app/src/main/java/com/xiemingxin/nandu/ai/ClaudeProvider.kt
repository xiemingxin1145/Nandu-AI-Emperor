package com.xiemingxin.nandu.ai

import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ClaudeProvider(private val apiKey: String) : AiProvider {

    override val providerType = AiProviderType.CLAUDE
    override val isConfigured get() = apiKey.isNotBlank()

    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ══ 系统提示：御前推演官 ══
    private fun buildSystemPrompt(context: GameContext): String = """
你是《南渡无悔》的御前推演官，负责解析皇帝的圣旨并生成群臣反应。

当前局势（${context.era} 第${context.currentTurn}旬）：
- 国库：${context.gold}贯  粮草：${context.grain}石
- 军心：${context.troopMorale}  朝堂稳定：${context.courtStability}  金国威胁：${context.jinThreat}
- 可用武将：${context.availableOfficers.joinToString("、") { "${it.name}(${it.currentCityId})" }}
- 城池状况：${context.activeCities.joinToString("、") { "${it.name}兵${it.troops}" }}

你必须严格返回以下JSON结构，不得包含任何其他文字：
{
  "summary": "一句话概括圣旨核心意图",
  "commands": [
    {
      "type": "命令类型（只能是：dispatch_army/assign_officer/repair_city/raise_grain/suppress_officer/reward_officer/punish_officer）",
      "officerId": "武将id",
      "fromCityId": "出发城池id",
      "toCityId": "目标城池id",
      "cityId": "城池id",
      "troops": 0,
      "role": "职责",
      "severity": "light/medium/severe",
      "amount": 0,
      "deadlineTurns": 0
    }
  ],
  "npcResponses": [
    {
      "officerId": "武将id",
      "attitude": "support/oppose/neutral/concerned",
      "text": "武将以其性格发言，文言口吻，20-40字"
    }
  ],
  "riskTags": ["grain_pressure", "court_backlash", "jin_retaliation"],
  "confidence": 0.9,
  "clarificationNeeded": false,
  "clarificationHint": ""
}

武将性格规则（必须遵守）：
- yue_fei：忠烈主战，文言铿锵，直接表态是否能打
- qin_hui：主和自保，措辞阴柔，暗指风险
- zhao_ding：稳重理财，先问粮道，后议军事
- han_shizhong：豪勇直爽，短句有力，擅言水战
- li_gang：刚烈守城，慷慨激昂，以大局为重

只选与圣旨最相关的2-4人回应，不要让所有人都说话。
""".trimIndent()

    override suspend fun parseEdict(
        edictText: String,
        gameContext: GameContext
    ): Result<EdictResult> = withContext(Dispatchers.IO) {
        try {
            val requestBody = """
            {
                "model": "claude-sonnet-4-6",
                "max_tokens": 1500,
                "system": ${json.encodeToString(kotlinx.serialization.json.JsonPrimitive(buildSystemPrompt(gameContext)))},
                "messages": [
                    {
                        "role": "user",
                        "content": "圣旨内容：$edictText"
                    }
                ]
            }
            """.trimIndent()

            val request = Request.Builder()
                .url("https://api.anthropic.com/v1/messages")
                .addHeader("x-api-key", apiKey)
                .addHeader("anthropic-version", "2023-06-01")
                .addHeader("content-type", "application/json")
                .post(requestBody.toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseText = response.body?.string() ?: return@withContext Result.failure(
                Exception("API返回为空")
            )

            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API错误: ${response.code}"))
            }

            // 提取content文本
            val contentJson = json.parseToJsonElement(responseText)
            val rawText = contentJson
                .jsonObject["content"]
                ?.jsonArray?.firstOrNull()
                ?.jsonObject?.get("text")
                ?.jsonPrimitive?.content
                ?: return@withContext Result.failure(Exception("解析响应失败"))

            // 解析EdictResult
            val result = json.decodeFromString<EdictResult>(rawText)

            // 三道保险：命令白名单过滤
            val safeResult = result.copy(
                commands = result.commands.filter { EdictCommand.isValid(it.type) }
            )

            Result.success(safeResult)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

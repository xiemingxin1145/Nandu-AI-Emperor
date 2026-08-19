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
        val courtOfficers = context.availableOfficers
            .filter { it.status == "IN_COURT" || it.status == "DEPLOYED" }
            .joinToString("、") { o ->
                val role = if (o.currentRole.isNotBlank() && o.currentRole != "御前待命") " [${o.currentRole}]" else ""
                "${o.name}(${o.id}${role},${o.commandSummary},忠${o.loyaltyLabel})"
            }
        val leadList = if (context.pendingRecruitLeads.isNotEmpty())
            "待征辟人才：${context.pendingRecruitLeads.joinToString("、")}"
        else ""
        val cityList = context.activeCities.filter { it.owner == "song" }
            .joinToString("、") { "${it.name}(兵${it.troops / 1000}k)" }
        return "你是《南渡无悔》的御前推演官，负责解析皇帝的圣旨并生成群臣反应。\n" +
            "\n当前局势（${context.era} 第${context.currentTurn}旬）：\n" +
            "国库：${context.gold}贯  粮草：${context.grain}石\n" +
            "军心：${context.troopMorale}  朝堂稳定：${context.courtStability}  金国威胁：${context.jinThreat}\n" +
            "在朝将吏：$courtOfficers\n$leadList\n宋方城池：$cityList\n" +
            "\n【Stage 3 新命令说明】\n" +
            "appoint_governor:任命城池主官/太守，需officerId+cityId，人物须IN_COURT/DEPLOYED\n" +
            "appoint_garrison:任命驻城守将，需officerId+cityId\n" +
            "dismiss_officer:免职，需officerId\n" +
            "transfer_officer:调任，需officerId+cityId（目标城）\n" +
            "recruit_officer:征辟人才，需officerId+amount，人物须在talentLeads中\n" +
            "若圣旨涉及未入朝人物：在待征辟名单→recruit_officer；完全未知→assign_officer触发寻访\n" +
            "\n严格返回JSON，无其他文字：\n" +
            "{\"summary\":\"摘要\",\"commands\":[{\"type\":\"命令类型\",\"officerId\":\"\",\"fromCityId\":\"\",\"toCityId\":\"\",\"cityId\":\"\",\"troops\":0,\"role\":\"\",\"severity\":\"\",\"amount\":0,\"deadlineTurns\":0}],\"npcResponses\":[{\"officerId\":\"\",\"attitude\":\"support/oppose/neutral/concerned\",\"text\":\"文言20-40字\"}],\"riskTags\":[],\"confidence\":0.9,\"clarificationNeeded\":false,\"clarificationHint\":\"\"}\n" +
            "\n命令类型只能是：dispatch_army/assign_officer/repair_city/raise_grain/suppress_officer/reward_officer/punish_officer/appoint_governor/appoint_garrison/dismiss_officer/transfer_officer/recruit_officer\n" +
            "\n朝堂思维铁律（必须遵守）：\n" +
            "1. 所有NPC都是南宋朝臣或武将，不是现代人。\n" +
            "2. 不准使用现代词汇与管理话术。\n" +
            "3. 说话必须围绕君臣名分、社稷安危、祖宗法度、民力、粮道、军心、边防、朝局。\n" +
            "4. npcResponses.text必须像殿上奏对，半文半白，有古代官场语感。\n" +
            "5. 反对意见借民力、粮饷、边患、祖宗旧制劝谏。\n" +
            "\n武将性格：yue_fei忠烈主战铿锵 qin_hui主和阴柔暗指风险 zhao_ding稳重理财先问粮道 han_shizhong豪勇直爽 li_gang刚烈守城慷慨\n" +
            "只选最相关2-4人回应。"
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

            val result = json.decodeFromString(EdictResult.serializer(), cleanJson)

            Result.success(result.copy(
                commands = result.commands.filter { EdictCommand.isValid(it.type) }
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

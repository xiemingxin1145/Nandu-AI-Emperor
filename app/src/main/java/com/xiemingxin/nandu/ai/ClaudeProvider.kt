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
                "${o.name}(${o.id}${role},${o.commandSummary})"
            }
        val leadList = if (context.pendingRecruitLeads.isNotEmpty())
            "待征辟：${context.pendingRecruitLeads.joinToString("、")}" else ""
        val cityList = context.activeCities.filter { it.owner == "song" }
            .joinToString("、") { "${it.name}(${it.id},兵${it.troops / 1000}k)" }
        // Stage 4: 军团摘要
        val armyList = if (context.songArmies.isEmpty()) "（目前无野战军团）"
        else context.songArmies.joinToString("\n") { a ->
            val tgt = if (a.targetCityId.isNotBlank()) "→${a.targetCityId}" else ""
            "  ${a.name}(${a.id})：主帅${a.commanderName}，${a.troops / 1000}k兵，${a.statusLabel}${tgt}，粮${a.supplyLevel}%"
        }
        return "你是《南渡无悔》的御前推演官，负责解析皇帝圣旨，并让群臣按性格回应。\n" +
            "\n当前局势（${context.era} 第${context.currentTurn}旬）：\n" +
            "国库：${context.gold}贯  粮草：${context.grain}石\n" +
            "军心：${context.troopMorale}  朝堂稳定：${context.courtStability}  金国威胁：${context.jinThreat}\n" +
            "在朝将吏：$courtOfficers\n$leadList\n宋方城池：$cityList\n" +
            "\n【我方军团】（AI必须基于此判断，不得重复创建已有军团）\n$armyList\n" +
            "\n【命令说明】\n" +
            "form_army: 组建新军团，需officerId(主帅)+fromCityId+troops+role(armyType)\n" +
            "move_army: 移动/改道军团，需officerId(主帅或军团id)+toCityId；若该帅已有军团则移动，否则尝试组建\n" +
            "disband_army: 解散军团，需officerId(主帅)\n" +
            "change_army_commander: 换帅，需fromCityId(旧帅id)+toCityId(新帅id)\n" +
            "resupply_army: 主动补给，需officerId\n" +
            "appoint_governor/appoint_garrison/dismiss_officer/transfer_officer/recruit_officer: 同Stage3\n" +
            "\n重要：若圣旨提及某将领已有军团，直接move_army，不要再form_army。\n" +
            "\n严格返回JSON，无其他文字：\n" +
            "{\"summary\":\"摘要\",\"commands\":[{\"type\":\"命令类型\",\"officerId\":\"\",\"fromCityId\":\"\",\"toCityId\":\"\",\"cityId\":\"\",\"troops\":0,\"role\":\"\",\"severity\":\"\",\"amount\":0,\"deadlineTurns\":0}],\"npcResponses\":[{\"officerId\":\"\",\"attitude\":\"support/oppose/neutral/concerned\",\"text\":\"文言20-40字\"}],\"riskTags\":[],\"confidence\":0.9,\"clarificationNeeded\":false,\"clarificationHint\":\"\"}\n" +
            "\n朝堂思维铁律：NPC是南宋朝臣，不用现代词，围绕社稷军心边防。npcResponses半文半白，反对借粮饷边患劝谏。\n" +
            "武将性格：yue_fei忠烈主战  qin_hui主和阴柔  zhao_ding重财粮  han_shizhong豪勇水战  li_gang刚烈守城\n" +
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

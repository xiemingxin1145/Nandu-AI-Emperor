package com.xiemingxin.nandu.ai

import kotlinx.coroutines.delay

/**
 * MockProvider — 不需要API Key，用于开发和UI调试
 * 根据关键词返回预设响应，让界面可以离线跑通
 */
class MockProvider : AiProvider {

    override val providerType = AiProviderType.MOCK
    override val isConfigured = true

    override suspend fun parseEdict(
        edictText: String,
        gameContext: GameContext
    ): Result<EdictResult> {
        delay(800) // 模拟网络延迟

        // 简单关键词检测
        val text = edictText
        val commands = mutableListOf<EdictCommand>()
        val responses = mutableListOf<NpcResponse>()
        val risks = mutableListOf<String>()

        // 调兵检测
        if (text.contains("岳飞") && (text.contains("出兵") || text.contains("北伐") || text.contains("进兵"))) {
            commands.add(EdictCommand(
                type = "dispatch_army",
                officerId = "yue_fei",
                fromCityId = "ezhou",
                toCityId = "xiangyang",
                troops = 30000
            ))
            responses.add(NpcResponse("yue_fei", "support",
                "臣请即刻整军，若粮道不绝，襄阳可图，直捣黄龙亦非妄言。"))
            risks.add("grain_pressure")
            risks.add("jin_retaliation")
        }

        if (text.contains("韩世忠") && (text.contains("守") || text.contains("水路") || text.contains("建康"))) {
            commands.add(EdictCommand(
                type = "assign_officer",
                officerId = "han_shizhong",
                cityId = "jiankang",
                role = "defender"
            ))
            responses.add(NpcResponse("han_shizhong", "support",
                "陛下放心，长江水路有臣在，金人休想南渡半步！"))
        }

        if (text.contains("秦桧") && (text.contains("贬") || text.contains("压") || text.contains("退") || text.contains("闭嘴"))) {
            commands.add(EdictCommand(
                type = "suppress_officer",
                officerId = "qin_hui",
                severity = "medium"
            ))
            responses.add(NpcResponse("qin_hui", "oppose",
                "陛下轻启边衅，恐金廷震怒再度南侵，江南百姓不堪其苦，望陛下三思。"))
            risks.add("court_backlash")
        }

        if (text.contains("修城") || text.contains("加固") || text.contains("城防")) {
            val cityId = when {
                text.contains("建康") -> "jiankang"
                text.contains("临安") -> "linan"
                text.contains("鄂州") -> "ezhou"
                text.contains("襄阳") -> "xiangyang"
                else -> "jiankang"
            }
            commands.add(EdictCommand(type = "repair_city", cityId = cityId, resourceFocus = "defense"))
            responses.add(NpcResponse("zhao_ding", "support",
                "修缮城防乃固本之举，臣以为可行，然须计算工料所费。"))
        }

        if (text.contains("筹粮") || text.contains("粮草") || text.contains("军粮")) {
            commands.add(EdictCommand(type = "raise_grain", officerId = "zhao_ding", amount = 100000, deadlineTurns = 3))
            responses.add(NpcResponse("zhao_ding", "concerned",
                "筹粮可行，然三旬之期颇紧，若强征恐扰民心，臣当尽力周旋。"))
            risks.add("grain_pressure")
        }

        // 默认响应（解析不出命令时）
        if (commands.isEmpty()) {
            return Result.success(EdictResult(
                summary = "陛下之旨意尚待厘清",
                commands = emptyList(),
                npcResponses = listOf(
                    NpcResponse("li_gang", "neutral", "臣请陛下明示，旨意所指，臣等方可奉行。")
                ),
                clarificationNeeded = true,
                clarificationHint = "可尝试：命岳飞出兵、命韩世忠守建康、修城、筹粮"
            ))
        }

        return Result.success(EdictResult(
            summary = "圣旨已解析，共${commands.size}项命令待执行",
            commands = commands,
            npcResponses = responses,
            riskTags = risks,
            confidence = 0.85f
        ))
    }
}

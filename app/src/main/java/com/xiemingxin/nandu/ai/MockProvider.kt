package com.xiemingxin.nandu.ai

import kotlinx.coroutines.delay

class MockProvider : AiProvider {

    override val providerType = AiProviderType.MOCK
    override val isConfigured = true

    override suspend fun parseEdict(
        edictText: String,
        gameContext: GameContext
    ): Result<EdictResult> {
        delay(800)

        val text = edictText
        val commands = mutableListOf<EdictCommand>()
        val responses = mutableListOf<NpcResponse>()
        val risks = mutableListOf<String>()

        if (text.contains("寻访") || text.contains("访才") || text.contains("搜索") || text.contains("找人") || text.contains("招募") || text.contains("在野") || text.contains("人才")) {
            val cityId = when {
                text.contains("襄阳") -> "xiangyang"
                text.contains("建康") -> "jiankang"
                text.contains("兴元") || text.contains("川陕") -> "xinguan"
                text.contains("淮") -> "huaihe"
                text.contains("临安") -> "linan"
                text.contains("开封") -> "kaifeng"
                else -> ""
            }
            val officerId = when {
                text.contains("岳飞") -> "yue_fei"
                text.contains("韩世忠") -> "han_shizhong"
                text.contains("吴玠") -> "wu_jie"
                text.contains("刘锜") -> "liu_qi"
                text.contains("宗泽") -> "zong_ze"
                text.contains("秦桧") -> "qin_hui"
                else -> ""
            }
            commands.add(EdictCommand(type = "assign_officer", officerId = officerId, cityId = cityId, role = "search", amount = 3000))
            responses.add(NpcResponse("zhao_ding", "support", "乱世用人，不可只看旧名册。臣请遣使入军中、乡里、流民营访求可造之才。"))
        }

        if (text.contains("拔擢") || text.contains("授官") || text.contains("升官") || text.contains("提拔") || text.contains("封赏") || text.contains("赏赐")) {
            val officerId = when {
                text.contains("岳飞") -> "yue_fei"
                text.contains("韩世忠") -> "han_shizhong"
                text.contains("吴玠") -> "wu_jie"
                text.contains("刘锜") -> "liu_qi"
                text.contains("宗泽") -> "zong_ze"
                text.contains("李纲") -> "li_gang"
                text.contains("赵鼎") -> "zhao_ding"
                text.contains("秦桧") -> "qin_hui"
                else -> ""
            }
            val cityId = when {
                text.contains("襄阳") -> "xiangyang"
                text.contains("建康") -> "jiankang"
                text.contains("鄂州") -> "ezhou"
                text.contains("临安") -> "linan"
                text.contains("开封") -> "kaifeng"
                else -> "ezhou"
            }
            if (officerId.isNotBlank()) {
                commands.add(EdictCommand(type = "assign_officer", officerId = officerId, cityId = cityId, role = "promotion", amount = 5000))
                commands.add(EdictCommand(type = "reward_officer", officerId = officerId, amount = 5000))
                responses.add(NpcResponse("li_gang", "support", "有功可赏，有才可拔。陛下若能破格用人，则军中士气必振。"))
            }
        }

        if (text.contains("岳飞") && (text.contains("出兵") || text.contains("北伐") || text.contains("进兵"))) {
            commands.add(EdictCommand(type = "dispatch_army", officerId = "yue_fei", fromCityId = "ezhou", toCityId = "xiangyang", troops = 30000))
            responses.add(NpcResponse("yue_fei", "support", "臣请整军听命，若粮道不绝，可进取襄汉。"))
            risks.add("grain_pressure")
            risks.add("jin_retaliation")
        }

        if (text.contains("韩世忠") && (text.contains("守") || text.contains("水路") || text.contains("建康"))) {
            commands.add(EdictCommand(type = "assign_officer", officerId = "han_shizhong", cityId = "jiankang", role = "defender"))
            responses.add(NpcResponse("han_shizhong", "support", "臣愿守江防。"))
        }

        if (text.contains("秦桧") && (text.contains("贬") || text.contains("压") || text.contains("退") || text.contains("闭嘴"))) {
            commands.add(EdictCommand(type = "suppress_officer", officerId = "qin_hui", severity = "medium"))
            responses.add(NpcResponse("qin_hui", "oppose", "臣以为当慎动兵戈。"))
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
            responses.add(NpcResponse("zhao_ding", "support", "修缮城防乃固本之举。"))
        }

        if (text.contains("筹粮") || text.contains("粮草") || text.contains("军粮")) {
            commands.add(EdictCommand(type = "raise_grain", officerId = "zhao_ding", amount = 100000, deadlineTurns = 3))
            responses.add(NpcResponse("zhao_ding", "concerned", "筹粮可行，然须防扰民。"))
            risks.add("grain_pressure")
        }

        if (commands.isEmpty()) {
            return Result.success(EdictResult(
                summary = "陛下之旨意尚待厘清",
                commands = emptyList(),
                npcResponses = listOf(NpcResponse("li_gang", "neutral", "臣请陛下明示，旨意所指，臣等方可奉行。")),
                clarificationNeeded = true,
                clarificationHint = "可尝试：寻访岳飞、拔擢岳飞、访求襄阳人才、命韩世忠守建康、修城、筹粮"
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

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
            responses.add(NpcResponse("li_gang", "support", "若得忠勇之士，便当破格录用。朝廷不可再误英雄于草莽。"))
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
                responses.add(NpcResponse("qin_hui", "concerned", "骤然拔擢，恐旧臣心中不平。臣请先观其功，再授其实任。"))
            }
        }

        if (text.contains("岳飞") && (text.contains("出兵") || text.contains("北伐") || text.contains("进兵"))) {
            commands.add(EdictCommand(type = "dispatch_army", officerId = "yue_fei", fromCityId = "ezhou", toCityId = "xiangyang", troops = 30000))
            responses.add(NpcResponse("yue_fei", "support", "臣请整军听命。粮道若续，襄汉可图；军心若振，中原未必不可望。"))
            responses.add(NpcResponse("zhao_ding", "concerned", "兵可动，粮不可虚。臣请先核江陵、鄂州存粮，再定进止。"))
            responses.add(NpcResponse("qin_hui", "oppose", "兵锋一启，金人必有报复。臣恐江淮未稳，反伤社稷根本。"))
            risks.add("grain_pressure")
            risks.add("jin_retaliation")
            risks.add("court_debate")
        }

        if (text.contains("韩世忠") && (text.contains("守") || text.contains("水路") || text.contains("建康"))) {
            commands.add(EdictCommand(type = "assign_officer", officerId = "han_shizhong", cityId = "jiankang", role = "defender"))
            responses.add(NpcResponse("han_shizhong", "support", "臣愿守江防。金人若来，便叫他先问过江上战船。"))
            responses.add(NpcResponse("zhao_ding", "support", "建康为江防门户，韩帅镇守，军民皆可稍安。"))
        }

        if (text.contains("秦桧") && (text.contains("贬") || text.contains("压") || text.contains("退") || text.contains("闭嘴"))) {
            commands.add(EdictCommand(type = "suppress_officer", officerId = "qin_hui", severity = "medium"))
            responses.add(NpcResponse("qin_hui", "oppose", "臣所言不过慎兵惜民，若以此获罪，恐朝中从此人人自危。"))
            responses.add(NpcResponse("li_gang", "support", "议和之论若压过国耻，朝廷便再无血气。臣以为当正其位分。"))
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
            responses.add(NpcResponse("zhao_ding", "support", "修缮城防乃固本之举。耗钱虽多，却比临敌仓促要便宜得多。"))
            responses.add(NpcResponse("han_shizhong", "support", "城墙高一尺，将士胆便壮一分。臣请并修军械。"))
        }

        if (text.contains("筹粮") || text.contains("粮草") || text.contains("军粮")) {
            commands.add(EdictCommand(type = "raise_grain", officerId = "zhao_ding", amount = 100000, deadlineTurns = 3))
            responses.add(NpcResponse("zhao_ding", "concerned", "筹粮可行，然须防扰民。臣请先调漕运，再议加征。"))
            responses.add(NpcResponse("yue_fei", "support", "军中所急，首在粮道。粮至则兵动，粮绝则胜机皆空。"))
            risks.add("grain_pressure")
        }

        if (commands.isEmpty()) {
            return Result.success(EdictResult(
                summary = "陛下之旨意尚待厘清，群臣请再问明所指",
                commands = emptyList(),
                npcResponses = listOf(
                    NpcResponse("li_gang", "neutral", "臣请陛下明示，旨意所指，臣等方可奉行。"),
                    NpcResponse("zhao_ding", "concerned", "若涉钱粮军务，须有城池、将领与限期，方不误国。"),
                    NpcResponse("qin_hui", "neutral", "圣意若未定，臣等不敢妄议轻重。")
                ),
                clarificationNeeded = true,
                clarificationHint = "可尝试：寻访岳飞、拔擢岳飞、访求襄阳人才、命韩世忠守建康、修城、筹粮"
            ))
        }

        if (responses.none { it.officerId == "zhao_ding" }) {
            responses.add(NpcResponse("zhao_ding", "concerned", "臣请先核钱粮，再令有司奉行，免得旨意落地成空。"))
        }

        return Result.success(EdictResult(
            summary = "垂拱殿已议定圣旨，共${commands.size}项命令待陛下朱批",
            commands = commands,
            npcResponses = responses.distinctBy { it.officerId + it.text }.take(4),
            riskTags = risks.distinct(),
            confidence = 0.85f
        ))
    }
}

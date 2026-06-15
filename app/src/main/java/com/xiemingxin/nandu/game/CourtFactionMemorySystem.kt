package com.xiemingxin.nandu.game

/**
 * V1.9 朝堂派系与人物长期记忆骨架。
 *
 * 这一版先不改存档结构，而是从现有 GameState 派生：
 * - 主战 / 主和 / 财政 / 内廷 / 耳目五类势力态势
 * - 重要 NPC 的长期态度提示
 * - 后续版本可把这些派生信息持久化为真正关系网
 */
data class CourtFactionSnapshot(
    val id: String,
    val name: String,
    val power: Int,
    val mood: String,
    val trend: String,
    val summary: String,
    val memberIds: List<String>
)

data class OfficerMemoryNote(
    val officerId: String,
    val factionId: String,
    val label: String,
    val summary: String,
    val severity: Int
)

object CourtFactionMemorySystem {

    fun snapshots(state: GameState): List<CourtFactionSnapshot> {
        val warChoices = state.flagCount("council_choice_chuigongdian_war") + state.flagCount("council_choice_taimiao_war_oath")
        val peaceChoices = state.flagCount("council_choice_chuigongdian_peace")
        val fiscalChoices = state.flagCount("council_choice_zhengshitang")
        val innerChoices = state.flagCount("inner_palace") + state.flagCount("empress_advice")
        val bureauChoices = state.flagCount("bureau_") + state.flagCount("council_choice_huangchengsi")

        val warPower = (state.warFactionPower + warChoices * 6 - peaceChoices * 3).coerceIn(0, 100)
        val peacePower = (state.peaceFactionPower + peaceChoices * 7 - warChoices * 2).coerceIn(0, 100)
        val grainPressure = if (state.grain < 130000) 8 else 0
        val goldPressure = if (state.gold < 40000) 8 else 0
        val fiscalPower = (45 + fiscalChoices * 7 + grainPressure + goldPressure).coerceIn(0, 100)
        val innerPower = (25 + innerChoices * 8 + if (state.storyFlags.contains("inner_palace_frugal")) -6 else 0).coerceIn(0, 100)
        val bureauPower = (22 + bureauChoices * 7 + state.flagCount("secret_memorial") * 3).coerceIn(0, 100)

        return listOf(
            CourtFactionSnapshot(
                id = "war",
                name = "主战诸臣",
                power = warPower,
                mood = when {
                    warPower >= 70 -> "请缨甚急"
                    warPower >= 50 -> "士气渐振"
                    peacePower >= 65 -> "心有郁结"
                    else -> "静候圣断"
                },
                trend = if (warChoices > peaceChoices) "上升" else if (peaceChoices > warChoices) "受抑" else "持平",
                summary = "岳飞、韩世忠、李纲等重军心与国耻。若陛下久缓兵议，此派会生不满。",
                memberIds = listOf("yue_fei", "han_shizhong", "li_gang")
            ),
            CourtFactionSnapshot(
                id = "peace",
                name = "主和文臣",
                power = peacePower,
                mood = when {
                    peacePower >= 70 -> "声势渐张"
                    warPower >= 70 -> "暗自忧惧"
                    else -> "持慎重之论"
                },
                trend = if (peaceChoices > warChoices) "上升" else if (warChoices > peaceChoices) "受抑" else "持平",
                summary = "秦桧等以民力、粮饷、边患为辞，常借稳局之名牵制军议。",
                memberIds = listOf("qin_hui")
            ),
            CourtFactionSnapshot(
                id = "fiscal",
                name = "财政执政",
                power = fiscalPower,
                mood = when {
                    state.grain < 120000 -> "忧粮甚切"
                    state.gold < 30000 -> "忧财甚切"
                    fiscalPower >= 65 -> "纲纪渐立"
                    else -> "谨慎理财"
                },
                trend = if (fiscalChoices > 0) "上升" else "持平",
                summary = "赵鼎一系主张先核钱粮、安漕运、保军食，能稳国本但易被武臣嫌慢。",
                memberIds = listOf("zhao_ding")
            ),
            CourtFactionSnapshot(
                id = "inner",
                name = "后苑内廷",
                power = innerPower,
                mood = when {
                    state.storyFlags.contains("inner_palace_frugal") -> "略有怨言"
                    state.storyFlags.contains("inner_palace_comforted") -> "宫中稍安"
                    state.storyFlags.contains("empress_advice_open") -> "中宫可问"
                    else -> "静观外朝"
                },
                trend = if (innerChoices > 0) "已入局" else "未动",
                summary = "皇后、太后、内侍会影响内廷稳定与外朝观感，不能只当装饰。",
                memberIds = listOf("empress", "dowager", "eunuch")
            ),
            CourtFactionSnapshot(
                id = "bureau",
                name = "皇城司耳目",
                power = bureauPower,
                mood = when {
                    state.storyFlags.contains("bureau_observing") -> "暗中留意"
                    state.storyFlags.contains("secret_memorial_verified") -> "密折渐明"
                    else -> "耳目未张"
                },
                trend = if (bureauChoices > 0) "上升" else "持平",
                summary = "皇城司能验证传闻、察朝臣动向，但过重则易伤朝局。",
                memberIds = listOf("bureau_clerk")
            )
        )
    }

    fun summaryForCouncil(state: GameState): String {
        val top = snapshots(state).maxByOrNull { it.power }
        return top?.let { "当前朝中以「${it.name}」声势最重：${it.mood}。" } ?: "朝中诸派尚未分明。"
    }

    fun noteForSpeaker(state: GameState, speakerId: String): OfficerMemoryNote? {
        val officer = state.officers.firstOrNull { it.id == speakerId }
        val warChoices = state.flagCount("council_choice_chuigongdian_war") + state.flagCount("council_choice_taimiao_war_oath")
        val peaceChoices = state.flagCount("council_choice_chuigongdian_peace")
        val fiscalChoices = state.flagCount("council_choice_zhengshitang")

        val direct = when (speakerId) {
            "yue_fei", "han_shizhong", "li_gang" -> when {
                peaceChoices > warChoices -> OfficerMemoryNote(speakerId, "war", "前议受抑", "陛下曾暂缓兵议，主战诸臣心中未必无郁。", 2)
                warChoices > 0 -> OfficerMemoryNote(speakerId, "war", "蒙恩可战", "陛下曾明定主战或誓师，武臣多感振奋。", 1)
                else -> null
            }
            "qin_hui" -> when {
                warChoices > peaceChoices -> OfficerMemoryNote(speakerId, "peace", "暗自不满", "陛下屡重军议，秦桧一派口称谨慎，心中实有不安。", 2)
                peaceChoices > 0 -> OfficerMemoryNote(speakerId, "peace", "主和得势", "此前暂缓兵议，使主和之论在朝中更敢抬头。", 1)
                else -> null
            }
            "zhao_ding" -> when {
                fiscalChoices > 0 -> OfficerMemoryNote(speakerId, "fiscal", "理财受信", "政事堂近来多承圣断，赵鼎理财之权渐重。", 1)
                state.grain < 120000 || state.gold < 30000 -> OfficerMemoryNote(speakerId, "fiscal", "忧国用", "府库与粮储吃紧，赵鼎奏对必先问钱粮。", 2)
                else -> null
            }
            "empress" -> if (state.storyFlags.contains("empress_advice_open")) {
                OfficerMemoryNote(speakerId, "inner", "中宫可问", "陛下曾许中宫密陈宫中近事，皇后线已开。", 1)
            } else null
            "dowager" -> if (state.storyFlags.contains("inner_palace_frugal")) {
                OfficerMemoryNote(speakerId, "inner", "节俭入局", "内廷用度已裁，太后更重宫中规矩与外朝观感。", 1)
            } else null
            "bureau_clerk" -> if (state.storyFlags.contains("bureau_observing")) {
                OfficerMemoryNote(speakerId, "bureau", "耳目已布", "皇城司已奉旨暗察，后续密奏会更多。", 1)
            } else null
            else -> null
        }

        if (direct != null) return direct
        if (officer != null && officer.loyalty <= 40) {
            return OfficerMemoryNote(speakerId, "court", "忠心摇动", "此臣忠诚偏低，奏对虽恭，仍需留意。", 2)
        }
        if (officer != null && officer.ambition >= 75) {
            return OfficerMemoryNote(speakerId, "court", "野心渐露", "此臣野心偏高，遇事或另有盘算。", 2)
        }
        return null
    }

    private fun GameState.flagCount(keyword: String): Int = storyFlags.count { it.contains(keyword) }
}

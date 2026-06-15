package com.xiemingxin.nandu.game

/**
 * V1.8 朝会选择后果系统。
 *
 * 作用：让“入殿听奏”的皇帝裁断不再只是生成圣旨草稿，
 * 而是立即对朝局、钱粮、军心、名望、人物态度产生轻量影响。
 *
 * 注意：
 * - 这里先做轻量即时后果，不推进旬期，不替代 GameRuleEngine 的圣旨执行。
 * - 后续可扩展为持久化任务、连续事件、派系关系网。
 */
data class CouncilConsequenceResult(
    val newState: GameState,
    val outcomes: List<String>
)

object CouncilConsequenceSystem {

    fun apply(state: GameState, scene: CouncilScene, choice: CouncilChoice): CouncilConsequenceResult {
        var current = state
        val outcomes = mutableListOf<String>()

        fun setState(
            goldDelta: Int = 0,
            grainDelta: Int = 0,
            moraleDelta: Int = 0,
            stabilityDelta: Int = 0,
            jinDelta: Int = 0,
            warDelta: Int = 0,
            peaceDelta: Int = 0,
            prestigeDelta: Int = 0
        ) {
            current = current.copy(
                gold = (current.gold + goldDelta).coerceAtLeast(0),
                grain = (current.grain + grainDelta).coerceAtLeast(0),
                troopMorale = (current.troopMorale + moraleDelta).coerceIn(0, 100),
                courtStability = (current.courtStability + stabilityDelta).coerceIn(0, 100),
                jinThreat = (current.jinThreat + jinDelta).coerceIn(0, 100),
                warFactionPower = (current.warFactionPower + warDelta).coerceIn(0, 100),
                peaceFactionPower = (current.peaceFactionPower + peaceDelta).coerceIn(0, 100),
                prestige = (current.prestige + prestigeDelta).coerceIn(0, 100)
            )
        }

        fun adjustOfficer(officerId: String, loyaltyDelta: Int = 0, ambitionDelta: Int = 0, meritDelta: Int = 0) {
            current = current.copy(
                officers = current.officers.map { officer ->
                    if (officer.id == officerId) {
                        officer.copy(
                            loyalty = (officer.loyalty + loyaltyDelta).coerceIn(0, 100),
                            ambition = (officer.ambition + ambitionDelta).coerceIn(0, 100),
                            merit = (officer.merit + meritDelta).coerceAtLeast(0)
                        )
                    } else officer
                }
            )
        }

        fun boostCities(cityIds: Set<String>, goldDelta: Int = 0, grainDelta: Int = 0, supportDelta: Int = 0, commerceDelta: Int = 0, defenseDelta: Int = 0) {
            current = current.copy(
                cities = current.cities.map { city ->
                    if (city.id in cityIds) {
                        city.copy(
                            gold = (city.gold + goldDelta).coerceAtLeast(0),
                            grain = (city.grain + grainDelta).coerceAtLeast(0),
                            popularSupport = (city.popularSupport + supportDelta).coerceIn(0, 100),
                            commerce = (city.commerce + commerceDelta).coerceIn(0, 100),
                            defense = (city.defense + defenseDelta).coerceIn(0, 100)
                        )
                    } else city
                }
            )
        }

        when (scene.palaceId) {
            PalaceIds.CHUIGONG -> when (choice.id) {
                "war" -> {
                    setState(goldDelta = -2000, grainDelta = -6000, moraleDelta = 6, stabilityDelta = -3, jinDelta = 3, warDelta = 8, peaceDelta = -5, prestigeDelta = 3)
                    adjustOfficer("yue_fei", loyaltyDelta = 3, meritDelta = 2)
                    adjustOfficer("li_gang", loyaltyDelta = 2)
                    adjustOfficer("qin_hui", loyaltyDelta = -3, ambitionDelta = 2)
                    outcomes += "垂拱殿定主战方略：军心振作，主战派得势，但钱粮压力上升。"
                }
                "balance" -> {
                    setState(grainDelta = -2500, moraleDelta = 2, stabilityDelta = 5, warDelta = 2, peaceDelta = 1)
                    adjustOfficer("zhao_ding", loyaltyDelta = 2)
                    outcomes += "垂拱殿定先守后图：朝局稍稳，赵鼎可先核粮道。"
                }
                "peace" -> {
                    setState(moraleDelta = -4, stabilityDelta = 3, jinDelta = 2, warDelta = -6, peaceDelta = 8)
                    adjustOfficer("qin_hui", loyaltyDelta = 3, ambitionDelta = 2)
                    adjustOfficer("yue_fei", loyaltyDelta = -2)
                    adjustOfficer("li_gang", loyaltyDelta = -2)
                    outcomes += "垂拱殿暂缓兵议：主和派抬头，朝局暂稳，但军中锐气受挫。"
                }
            }

            PalaceIds.SHUMI -> when (choice.id) {
                "defend" -> {
                    setState(goldDelta = -3000, grainDelta = -4000, moraleDelta = 2, stabilityDelta = 2, jinDelta = -2)
                    boostCities(setOf("jiankang", "hefei", "shouchun", "xiangyang"), defenseDelta = 3, grainDelta = -800)
                    adjustOfficer("han_shizhong", loyaltyDelta = 2, meritDelta = 1)
                    outcomes += "枢密院加固江淮防线：要城防御略升，金军威胁稍缓。"
                }
                "train" -> {
                    setState(goldDelta = -2500, grainDelta = -5000, moraleDelta = 6, jinDelta = 1)
                    adjustOfficer("yue_fei", loyaltyDelta = 2, meritDelta = 1)
                    adjustOfficer("han_shizhong", loyaltyDelta = 2, meritDelta = 1)
                    outcomes += "枢密院整军练兵：军心提升，但粮草消耗明显。"
                }
                "scout" -> {
                    setState(goldDelta = -1000, stabilityDelta = 1, jinDelta = -1)
                    current = current.copy(storyFlags = current.storyFlags + "scout_jin_front")
                    outcomes += "枢密院先探敌势：边报更清，后续军报将更容易判断。"
                }
            }

            PalaceIds.ZHENGSHI -> when (choice.id) {
                "trade" -> {
                    setState(goldDelta = 6000, grainDelta = -1000, stabilityDelta = -1, prestigeDelta = 1)
                    boostCities(setOf("quanzhou", "mingzhou", "guangzhou"), goldDelta = 1500, commerceDelta = 4)
                    current = current.copy(storyFlags = current.storyFlags + "sea_trade_opened")
                    outcomes += "政事堂整顿市舶：国库收入上升，东南港市商业更盛。"
                }
                "grain" -> {
                    setState(goldDelta = -1500, grainDelta = 9000, moraleDelta = 1, stabilityDelta = 2)
                    adjustOfficer("zhao_ding", loyaltyDelta = 2, meritDelta = 1)
                    outcomes += "政事堂先保军粮：粮储增加，军政更稳。"
                }
                "light_tax" -> {
                    setState(goldDelta = -2500, grainDelta = -1000, stabilityDelta = 4, prestigeDelta = 3)
                    boostCities(current.cities.filter { it.owner == "song" }.map { it.id }.toSet(), supportDelta = 2)
                    outcomes += "政事堂安民轻敛：民心略升，短期财政吃紧。"
                }
            }

            PalaceIds.WENDE -> when (choice.id) {
                "summon" -> {
                    setState(goldDelta = -800, stabilityDelta = 1, prestigeDelta = 1)
                    current = current.copy(storyFlags = current.storyFlags + "talent_summon_ordered")
                    outcomes += "文德殿召见考校：在野人才线索进入朝廷视野。"
                }
                "promote_fast" -> {
                    setState(stabilityDelta = -3, warDelta = 3, prestigeDelta = 2)
                    current = current.copy(talentLeads = current.talentLeads + current.officers.filter { it.status == OfficerStatus.HIDDEN || it.status == OfficerStatus.WANDERING }.map { it.id }.take(1))
                    outcomes += "文德殿破格拔擢：求才速度加快，旧臣疑虑上升。"
                }
                "delay" -> {
                    setState(stabilityDelta = 1, prestigeDelta = -1)
                    outcomes += "文德殿留档再议：朝局无波，但可能错过豪杰。"
                }
            }

            PalaceIds.YUSHU -> when (choice.id) {
                "envoy" -> {
                    setState(goldDelta = -1800, stabilityDelta = 1, jinDelta = -2, prestigeDelta = 2)
                    current = current.copy(storyFlags = current.storyFlags + "foreign_envoy_sent")
                    outcomes += "御书房遣使探问：西夏与海东外交线索开启，金国压力稍缓。"
                }
                "verify" -> {
                    setState(goldDelta = -500, stabilityDelta = 2)
                    current = current.copy(storyFlags = current.storyFlags + "secret_memorial_verified")
                    outcomes += "御书房先核密折：朝局更稳，风险暂缓。"
                }
                "shelve" -> {
                    setState(stabilityDelta = 1, prestigeDelta = -1)
                    outcomes += "御书房留中不发：短期无风波，但机事可能延误。"
                }
            }

            PalaceIds.HUANGCHENG -> when (choice.id) {
                "observe" -> {
                    setState(goldDelta = -500, stabilityDelta = 1)
                    current = current.copy(storyFlags = current.storyFlags + "bureau_observing")
                    outcomes += "皇城司暗中留意：未惊动外朝，情报线继续潜行。"
                }
                "warn" -> {
                    setState(stabilityDelta = -2, prestigeDelta = 1)
                    adjustOfficer("qin_hui", loyaltyDelta = -2, ambitionDelta = 1)
                    outcomes += "皇城司召见敲打：震慑朝臣，但派系疑惧上升。"
                }
                "ignore" -> {
                    setState(stabilityDelta = 2, prestigeDelta = -1)
                    outcomes += "皇城司暂不追究：外朝稍安，隐患仍在。"
                }
            }

            PalaceIds.HOUYUAN -> when (choice.id) {
                "frugal" -> {
                    setState(goldDelta = 2500, grainDelta = 1500, stabilityDelta = -1, prestigeDelta = 2)
                    current = current.copy(storyFlags = current.storyFlags + "inner_palace_frugal")
                    outcomes += "后苑裁减内廷用度：国库稍宽，外朝称善，宫中略有怨言。"
                }
                "comfort" -> {
                    setState(goldDelta = -800, stabilityDelta = 2)
                    current = current.copy(storyFlags = current.storyFlags + "inner_palace_comforted")
                    outcomes += "后苑安抚宫中：内廷人心稍定，外朝无大波。"
                }
                "queen_advice" -> {
                    setState(stabilityDelta = 1, prestigeDelta = 1)
                    current = current.copy(storyFlags = current.storyFlags + "empress_advice_open")
                    outcomes += "后苑听中宫建议：皇后线开启，后续可密陈宫中近事。"
                }
            }

            PalaceIds.TAIMIAO -> when (choice.id) {
                "simple_rite" -> {
                    setState(goldDelta = -1000, grainDelta = -1000, stabilityDelta = 2, prestigeDelta = 3)
                    outcomes += "太庙从简祭告：名望提升，耗费可控。"
                }
                "war_oath" -> {
                    setState(goldDelta = -2000, grainDelta = -2000, moraleDelta = 5, jinDelta = 2, warDelta = 4, prestigeDelta = 5)
                    adjustOfficer("li_gang", loyaltyDelta = 2)
                    adjustOfficer("yue_fei", loyaltyDelta = 2)
                    outcomes += "太庙誓师雪耻：军心振奋，主战声势更盛。"
                }
                "delay" -> {
                    setState(stabilityDelta = -1, prestigeDelta = -1)
                    outcomes += "太庙礼议暂缓：省下用度，但正统声望未增。"
                }
            }
        }

        if (outcomes.isEmpty()) {
            outcomes += "${scene.title}已裁断：${choice.preview}"
        }

        current = current.copy(storyFlags = current.storyFlags + "council_choice_${scene.palaceId}_${choice.id}_turn_${state.turn}")
        return CouncilConsequenceResult(current, outcomes)
    }
}

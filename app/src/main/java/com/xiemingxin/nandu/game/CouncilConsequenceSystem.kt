package com.xiemingxin.nandu.game

/**
 * 朝会 / 宫殿选择后果系统。
 *
 * STAB-004 补齐旧 UI choice id 与后果分支之间的兼容别名，避免“按钮能点、只显示一句话、
 * 专属世界后果实际没命中”。所有正式 choice 至少会改变一项 GameState/关系/路线/记录。
 */
data class CouncilConsequenceResult(
    val newState: GameState,
    val outcomes: List<String>
)

object CouncilConsequenceSystem {

    const val SHUNCHANG_BATTLE_COUNCIL_ID = "battle_shunchang"

    fun apply(state: GameState, scene: CouncilScene, choice: CouncilChoice): CouncilConsequenceResult {
        if (scene.palaceId == SHUNCHANG_BATTLE_COUNCIL_ID) {
            val directive = when (choice.id) {
                "hold" -> ShunchangDirective.HOLD
                "reinforce" -> ShunchangDirective.REINFORCE
                "deliberate" -> ShunchangDirective.DELIBERATE
                else -> null
            }
            if (directive == null) {
                return CouncilConsequenceResult(state, listOf("【军令未发】无法识别的顺昌军令：${choice.id}"))
            }
            val result = BattleDirectiveSystem.applyShunchang(state, directive)
            return CouncilConsequenceResult(result.newState, listOf(result.message))
        }

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

        fun boostCities(
            cityIds: Set<String>,
            goldDelta: Int = 0,
            grainDelta: Int = 0,
            supportDelta: Int = 0,
            commerceDelta: Int = 0,
            defenseDelta: Int = 0
        ) {
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
                "war", "restore" -> {
                    setState(goldDelta = -2000, grainDelta = -6000, moraleDelta = 6, stabilityDelta = -3, jinDelta = 3, warDelta = 8, peaceDelta = -5, prestigeDelta = 3)
                    adjustOfficer("yue_fei", loyaltyDelta = 3, meritDelta = 2)
                    adjustOfficer("li_gang", loyaltyDelta = 2)
                    adjustOfficer("qin_hui", loyaltyDelta = -3, ambitionDelta = 2)
                    outcomes += "行在定经营中原方略：军心振作，主战派得势，但钱粮与前线风险上升。"
                }
                "balance" -> {
                    setState(grainDelta = -2500, moraleDelta = 2, stabilityDelta = 5, warDelta = 2, peaceDelta = 1)
                    adjustOfficer("zhao_ding", loyaltyDelta = 2)
                    outcomes += "行在定守备兼筹退路：朝局稍稳，军粮转运开始优先核查。"
                }
                "peace", "south" -> {
                    setState(moraleDelta = -4, stabilityDelta = 3, jinDelta = 2, warDelta = -6, peaceDelta = 8)
                    adjustOfficer("qin_hui", loyaltyDelta = 3, ambitionDelta = 2)
                    adjustOfficer("yue_fei", loyaltyDelta = -2)
                    adjustOfficer("li_gang", loyaltyDelta = -2)
                    outcomes += "行在准备南行退路：朝局暂稳，主和声势上升，但中原军心与锐气受挫。"
                }
            }

            PalaceIds.SHUMI -> when (choice.id) {
                "defend" -> {
                    setState(goldDelta = -3000, grainDelta = -4000, moraleDelta = 2, stabilityDelta = 2, jinDelta = -2)
                    boostCities(setOf("jiankang", "hefei", "shouchun", "xiangyang"), defenseDelta = 3, grainDelta = -800)
                    adjustOfficer("han_shizhong", loyaltyDelta = 2, meritDelta = 1)
                    outcomes += "枢密院加固江淮防线：要城防御略升，北方压力稍缓。"
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
                    current = DiplomacyTradeSystem.updateDiplomacy(current, WorldPowerIds.SEA_TRADE, relationDelta = 6, trustDelta = 4, status = "港市议利")
                    current = DiplomacyTradeSystem.updateTradeRoute(current, "quanzhou_spice", incomeDelta = 8, controlDelta = 6, smugglingDelta = -3)
                    current = DiplomacyTradeSystem.updateTradeRoute(current, "mingzhou_ship", incomeDelta = 4, controlDelta = 4)
                    current = DiplomacyTradeSystem.updateTradeRoute(current, "guangzhou_south", incomeDelta = 6, riskDelta = 2, controlDelta = 3)
                    current = current.copy(storyFlags = current.storyFlags + "sea_trade_opened")
                    outcomes += "政事堂整顿市舶：国库收入上升，东南港市商业更盛，外贸关系已入账。"
                }
                "grain" -> {
                    setState(goldDelta = -1500, grainDelta = 9000, moraleDelta = 1, stabilityDelta = 2)
                    adjustOfficer("zhao_ding", loyaltyDelta = 2, meritDelta = 1)
                    outcomes += "政事堂先保军粮：粮储增加，军政更稳。"
                }
                "audit" -> {
                    setState(goldDelta = -500, stabilityDelta = 3, prestigeDelta = 1)
                    current = current.copy(storyFlags = current.storyFlags + "fiscal_audit_turn_${state.turn}")
                    outcomes += "政事堂清查行在用度：花费少量查核成本，财政秩序与朝局稳定改善。"
                }
                "light_tax" -> {
                    setState(goldDelta = -2500, grainDelta = -1000, stabilityDelta = 4, prestigeDelta = 3)
                    boostCities(current.cities.filter { it.owner == "song" }.map { it.id }.toSet(), supportDelta = 2)
                    current = DiplomacyTradeSystem.updateTradeRoute(current, "guangzhou_south", riskDelta = -2, smugglingDelta = -4)
                    outcomes += "政事堂安民轻敛：民心略升，短期财政吃紧，南海私商风险略降。"
                }
            }

            PalaceIds.WENDE -> when (choice.id) {
                "summon" -> {
                    setState(goldDelta = -800, stabilityDelta = 1, prestigeDelta = 1)
                    current = current.copy(storyFlags = current.storyFlags + "talent_summon_ordered")
                    outcomes += "文班公署召见考校：在野人才线索进入朝廷视野。"
                }
                "promote_fast", "field" -> {
                    setState(stabilityDelta = -3, warDelta = 3, prestigeDelta = 2)
                    current = current.copy(
                        talentLeads = current.talentLeads + current.officers
                            .filter { it.status == OfficerStatus.HIDDEN || it.status == OfficerStatus.WANDERING }
                            .map { it.id }
                            .take(1),
                        storyFlags = current.storyFlags + "talent_field_trial_turn_${state.turn}"
                    )
                    outcomes += "文班公署准就地试用：人才起用加快，但中央掌握与旧臣接受度承受压力。"
                }
                "delay" -> {
                    setState(stabilityDelta = 1, prestigeDelta = -1)
                    outcomes += "文班公署留档再察：朝局无波，但可能错过豪杰。"
                }
            }

            PalaceIds.YUSHU -> when (choice.id) {
                "envoy" -> {
                    setState(goldDelta = -1800, stabilityDelta = 1, jinDelta = -2, prestigeDelta = 2)
                    current = DiplomacyTradeSystem.updateDiplomacy(current, WorldPowerIds.XIXIA, relationDelta = 8, pressureDelta = -2, trustDelta = 5, status = "遣使通问")
                    current = DiplomacyTradeSystem.updateDiplomacy(current, WorldPowerIds.GORYEO, relationDelta = 5, trustDelta = 4, status = "海东使节")
                    current = current.copy(storyFlags = current.storyFlags + "foreign_envoy_sent")
                    outcomes += "御前便阁遣使探问：西夏与海东外交线索开启，北方压力稍缓。"
                }
                "verify" -> {
                    setState(goldDelta = -500, stabilityDelta = 2)
                    current = current.copy(storyFlags = current.storyFlags + "secret_memorial_verified")
                    outcomes += "御前便阁先核密折：朝局更稳，风险暂缓。"
                }
                "shelve" -> {
                    setState(stabilityDelta = 1, prestigeDelta = -1)
                    current = DiplomacyTradeSystem.updateDiplomacy(current, WorldPowerIds.XIXIA, trustDelta = -1)
                    outcomes += "御前便阁留中不发：短期无风波，但机事可能延误。"
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
                    outcomes += "行在内廷裁减用度：国库稍宽，外朝称善，宫中略有怨言。"
                }
                "comfort" -> {
                    setState(goldDelta = -800, stabilityDelta = 2)
                    current = current.copy(storyFlags = current.storyFlags + "inner_palace_comforted")
                    outcomes += "行在内廷安抚宫中：内廷人心稍定，外朝无大波。"
                }
                "queen_advice", "advice" -> {
                    setState(stabilityDelta = 1, prestigeDelta = 1)
                    current = current.copy(storyFlags = current.storyFlags + "empress_advice_open")
                    outcomes += "行在内廷听取近事：中宫建议线开启，宫中信息可以继续密陈。"
                }
            }

            PalaceIds.TAIMIAO -> when (choice.id) {
                "simple_rite" -> {
                    setState(goldDelta = -1000, grainDelta = -1000, stabilityDelta = 2, prestigeDelta = 3)
                    outcomes += "礼制事务从简祭告：名望提升，耗费可控。"
                }
                "war_oath", "restore_oath" -> {
                    setState(goldDelta = -2000, grainDelta = -2000, moraleDelta = 5, jinDelta = 2, warDelta = 4, prestigeDelta = 5)
                    adjustOfficer("li_gang", loyaltyDelta = 2)
                    adjustOfficer("yue_fei", loyaltyDelta = 2)
                    outcomes += "礼制事务申明恢复之志：军心振奋，主战声势更盛，同时提高对金压力。"
                }
                "delay" -> {
                    setState(stabilityDelta = -1, prestigeDelta = -1)
                    outcomes += "礼议暂缓：省下用度，但正统声望未增。"
                }
            }
        }

        if (outcomes.isEmpty()) {
            // Generic fallback still records the choice, but a regression test must ensure all current
            // PalaceRegistry scene choices hit a dedicated branch before V1.6.2 release.
            outcomes += "${scene.title}已裁断：${choice.preview}"
        }

        current = current.copy(storyFlags = current.storyFlags + "council_choice_${scene.palaceId}_${choice.id}_turn_${state.turn}")
        return CouncilConsequenceResult(current, outcomes)
    }
}

package com.xiemingxin.nandu.game

/**
 * V1.6.2 STAB-003：战役军令必须真实写回世界状态。
 *
 * 旧顺昌 Demo 的“固守 / 驰援 / 再议”只改 Compose 局部变量，离开页面即消失。
 * 本系统只接受当前世界中确实可出现的顺昌候选，并通过 GameState 的正式字段落地：
 * - 固守：扣寿春真实粮草、提升真实城防与战区军团士气；
 * - 驰援：从真实宋军军团中选择可达援军，调用 ArmyMovementSystem 规划路线，不瞬移；
 * - 再议：不伪造调兵，但产生真实的朝局/军心/金军主动权代价；
 * - 所有军令写入 chronicle，随 GameSaveCodec 存档持久化。
 */
enum class ShunchangDirective(val label: String) {
    HOLD("固守顺昌"),
    REINFORCE("调军驰援"),
    DELIBERATE("暂缓再议")
}

data class BattleDirectiveResult(
    val success: Boolean,
    val newState: GameState,
    val message: String,
    val directive: ShunchangDirective,
    val affectedArmyId: String? = null
)

object BattleDirectiveSystem {
    private const val TARGET_CITY_ID = "shouchun"
    private const val HOLD_GRAIN_COST = 1200
    private const val REINFORCE_GRAIN_COST = 1000

    private val theaterCityIds = setOf(
        "shouchun", "hefei", "xinyang", "haozhou", "sizhou", "chuzhou", "yangzhou"
    )

    /** 同一旬只允许对同一战役下达一次正式军令，防止反复点击刷数值。 */
    fun directiveIssuedThisTurn(state: GameState): Boolean =
        state.chronicle.any { it.turn == state.turn && it.summary.startsWith("【顺昌军令】") }

    fun latestDirectiveEntry(state: GameState): ChronicleEntry? =
        state.chronicle.lastOrNull { it.summary.startsWith("【顺昌军令】") }

    fun applyShunchang(state: GameState, directive: ShunchangDirective): BattleDirectiveResult {
        val availability = HistoricalBattleAvailability.forShunchang(state)
        if (!availability.available) {
            return failure(state, directive, "【军令未发】当前世界已不具备顺昌战役条件：${availability.reason}")
        }
        if (directiveIssuedThisTurn(state)) {
            return failure(state, directive, "【军令未发】本旬已对顺昌方向下过正式军令，请待下一旬根据军情再议。")
        }

        return when (directive) {
            ShunchangDirective.HOLD -> hold(state)
            ShunchangDirective.REINFORCE -> reinforce(state)
            ShunchangDirective.DELIBERATE -> deliberate(state)
        }
    }

    private fun hold(state: GameState): BattleDirectiveResult {
        val city = state.cities.firstOrNull { it.id == TARGET_CITY_ID }
            ?: return failure(state, ShunchangDirective.HOLD, "【固守失败】寿春/顺昌方向城池数据不存在。")
        if (city.owner != "song") {
            return failure(state, ShunchangDirective.HOLD, "【固守失败】${city.name}已非宋土，无法下达守城军令。")
        }
        if (city.grain < HOLD_GRAIN_COST) {
            return failure(state, ShunchangDirective.HOLD, "【固守失败】${city.name}现粮${city.grain}，不足以支应加固城防所需${HOLD_GRAIN_COST}。")
        }

        val localArmyIds = state.armies
            .filter { army ->
                army.ownerFactionId == "song" &&
                    army.statusCode != ArmyStatus.DISBANDED &&
                    (army.currentCityId in theaterCityIds || army.targetCityId in theaterCityIds)
            }
            .map { it.id }
            .toSet()

        val newCities = state.cities.map {
            if (it.id == TARGET_CITY_ID) {
                it.copy(
                    grain = it.grain - HOLD_GRAIN_COST,
                    defense = (it.defense + 8).coerceAtMost(100)
                )
            } else it
        }
        val newArmies = state.armies.map {
            if (it.id in localArmyIds) it.copy(morale = (it.morale + 6).coerceAtMost(100)) else it
        }
        val base = state.copy(
            cities = newCities,
            armies = newArmies,
            troopMorale = (state.troopMorale + 2).coerceAtMost(100)
        )
        val cityAfter = newCities.first { it.id == TARGET_CITY_ID }
        val message = buildString {
            append("【顺昌军令】准固守。${city.name}拨粮${HOLD_GRAIN_COST}加固城垣，城防${city.defense}→${cityAfter.defense}")
            if (localArmyIds.isNotEmpty()) append("，战区${localArmyIds.size}支宋军整备守城、士气提升")
            append("。此变化已写入世界状态。")
        }
        val newState = appendChronicle(base, ShunchangDirective.HOLD, message)
        return BattleDirectiveResult(true, newState, message, ShunchangDirective.HOLD)
    }

    private fun reinforce(state: GameState): BattleDirectiveResult {
        data class Candidate(val army: Army, val route: List<String>, val days: Int)

        val candidates = state.armies.mapNotNull { army ->
            if (army.ownerFactionId != "song") return@mapNotNull null
            if (army.statusCode !in setOf(ArmyStatus.GARRISONED, ArmyStatus.MARCHING, ArmyStatus.ENGAGEMENT_PENDING)) return@mapNotNull null
            if (army.currentCityId in theaterCityIds) return@mapNotNull null
            if (army.targetCityId == TARGET_CITY_ID) return@mapNotNull null
            val source = state.cities.firstOrNull { it.id == army.currentCityId && it.owner == "song" }
                ?: return@mapNotNull null
            if (source.grain < REINFORCE_GRAIN_COST) return@mapNotNull null
            val route = ArmyMovementSystem.findRoute(army.currentCityId, TARGET_CITY_ID, army.armyType)
                ?: return@mapNotNull null
            val commander = state.officers.firstOrNull { it.id == army.commanderId }
            val days = ArmyMovementSystem.calcRouteDays(
                route,
                army.armyType,
                state.season,
                state.weather,
                commander?.command ?: 60
            )
            Candidate(army, route, days)
        }

        val chosen = candidates.minWithOrNull(
            compareBy<Candidate> { it.days }.thenByDescending { it.army.troops }
        ) ?: return failure(
            state,
            ShunchangDirective.REINFORCE,
            "【驰援失败】当前没有一支位于战区外、粮草足够且道路可达的宋军可调。不会凭空生成援军或瞬移名将。"
        )

        val sourceCity = state.cities.first { it.id == chosen.army.currentCityId }
        val preparedState = state.copy(
            cities = state.cities.map {
                if (it.id == sourceCity.id) it.copy(grain = it.grain - REINFORCE_GRAIN_COST) else it
            }
        )
        val (routedState, routeMessage) = ArmyMovementSystem.rerouteArmy(preparedState, chosen.army.id, TARGET_CITY_ID)
        val routedArmy = routedState.armies.firstOrNull { it.id == chosen.army.id }
        if (routedArmy == null || routedArmy.statusCode != ArmyStatus.MARCHING || routedArmy.targetCityId != TARGET_CITY_ID) {
            return failure(state, ShunchangDirective.REINFORCE, "【驰援失败】军团行军系统未能建立有效路线：$routeMessage")
        }

        val commanderName = state.officers.firstOrNull { it.id == chosen.army.commanderId }?.name ?: chosen.army.name
        val message = "【顺昌军令】着${commanderName}部（${chosen.army.troops}兵）自${sourceCity.name}驰援顺昌方向；出发城拨粮${REINFORCE_GRAIN_COST}，预计行军约${routedArmy.marchDaysTotal}日。军团已进入真实行军路线，不会瞬移。"
        val newState = appendChronicle(routedState, ShunchangDirective.REINFORCE, "$message\n$routeMessage")
        return BattleDirectiveResult(true, newState, message, ShunchangDirective.REINFORCE, chosen.army.id)
    }

    private fun deliberate(state: GameState): BattleDirectiveResult {
        val newMorale = (state.troopMorale - 2).coerceAtLeast(0)
        val newStability = (state.courtStability + 1).coerceAtMost(100)
        val newThreat = (state.jinThreat + 2).coerceAtMost(100)
        val base = state.copy(
            troopMorale = newMorale,
            courtStability = newStability,
            jinThreat = newThreat
        )
        val message = "【顺昌军令】暂缓定策，命枢密院复核军情。朝议程序稍稳，但前线等待使全军士气${state.troopMorale}→${newMorale}，金军主动权压力${state.jinThreat}→${newThreat}。未伪造任何调兵结果。"
        val newState = appendChronicle(base, ShunchangDirective.DELIBERATE, message)
        return BattleDirectiveResult(true, newState, message, ShunchangDirective.DELIBERATE)
    }

    private fun appendChronicle(state: GameState, directive: ShunchangDirective, message: String): GameState {
        val entry = ChronicleEntry(
            turn = state.turn,
            era = state.calendar.displayText(),
            edictText = "【战役军令】${directive.label}",
            summary = "【顺昌军令】${directive.label}",
            outcomes = listOf(message),
            season = state.season,
            weather = state.weather
        )
        return state.copy(chronicle = state.chronicle + entry)
    }

    private fun failure(state: GameState, directive: ShunchangDirective, message: String) =
        BattleDirectiveResult(false, state, message, directive)
}

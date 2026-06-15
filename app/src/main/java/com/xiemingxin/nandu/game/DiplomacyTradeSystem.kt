package com.xiemingxin.nandu.game

/**
 * V2.0 外交与外贸持久化骨架。
 *
 * 目标：把西夏、大理、高丽、海贸诸商从“地图显示/待办提示”升级为可保存、可变化的状态。
 *
 * 为了不改 GameState 主结构，这一版把状态编码进已持久化的 Rumor 列表中：
 * - diplomacy_*：外交状态
 * - trade_*：贸易路线状态
 * 后续大版本可迁移成 GameState 的正式字段。
 */
data class DiplomacyState(
    val powerId: String,
    val relation: Int,
    val pressure: Int,
    val trust: Int,
    val status: String,
    val lastTouchedTurn: Int = 0
)

data class TradeRouteState(
    val routeId: String,
    val isOpen: Boolean,
    val income: Int,
    val risk: Int,
    val control: Int,
    val smuggling: Int,
    val lastTouchedTurn: Int = 0
)

data class DiplomacyTradeTickResult(
    val newState: GameState,
    val reports: List<String>
)

object DiplomacyTradeSystem {
    private const val DIPLOMACY_CATEGORY = "system_diplomacy"
    private const val TRADE_CATEGORY = "system_trade"

    val initialDiplomacy: Map<String, DiplomacyState> = WorldStrategySystem.powers.associate { power ->
        power.id to DiplomacyState(
            powerId = power.id,
            relation = power.relation,
            pressure = power.militaryThreat,
            trust = (50 + power.relation / 2).coerceIn(0, 100),
            status = power.diplomaticStatus
        )
    }

    val initialTradeRoutes: Map<String, TradeRouteState> = WorldStrategySystem.tradeRoutes.associate { route ->
        route.id to TradeRouteState(
            routeId = route.id,
            isOpen = route.isOpen,
            income = route.profit,
            risk = route.risk,
            control = if (route.isOpen) 50 else 20,
            smuggling = (route.risk / 2).coerceIn(0, 100)
        )
    }

    fun diplomacyStates(state: GameState): Map<String, DiplomacyState> =
        initialDiplomacy + state.rumors
            .filter { it.category == DIPLOMACY_CATEGORY }
            .mapNotNull { it.toDiplomacyStateOrNull()?.let { parsed -> parsed.powerId to parsed } }
            .toMap()

    fun tradeRouteStates(state: GameState): Map<String, TradeRouteState> =
        initialTradeRoutes + state.rumors
            .filter { it.category == TRADE_CATEGORY }
            .mapNotNull { it.toTradeRouteStateOrNull()?.let { parsed -> parsed.routeId to parsed } }
            .toMap()

    fun diplomacyOf(state: GameState, powerId: String): DiplomacyState =
        diplomacyStates(state)[powerId] ?: initialDiplomacy.getValue(powerId)

    fun tradeOf(state: GameState, routeId: String): TradeRouteState =
        tradeRouteStates(state)[routeId] ?: initialTradeRoutes.getValue(routeId)

    fun tradeIncome(state: GameState): Int = tradeRouteStates(state).values
        .filter { it.isOpen }
        .sumOf { route -> ((route.income * route.control) / 100) - route.smuggling / 2 }
        .coerceAtLeast(0)

    fun tradeRisk(state: GameState): Int {
        val open = tradeRouteStates(state).values.filter { it.isOpen }
        if (open.isEmpty()) return 0
        return (open.sumOf { it.risk + it.smuggling - it.control / 3 } / open.size).coerceIn(0, 100)
    }

    fun updateDiplomacy(
        state: GameState,
        powerId: String,
        relationDelta: Int = 0,
        pressureDelta: Int = 0,
        trustDelta: Int = 0,
        status: String? = null
    ): GameState {
        val cur = diplomacyOf(state, powerId)
        val next = cur.copy(
            relation = (cur.relation + relationDelta).coerceIn(-100, 100),
            pressure = (cur.pressure + pressureDelta).coerceIn(0, 100),
            trust = (cur.trust + trustDelta).coerceIn(0, 100),
            status = status ?: cur.status,
            lastTouchedTurn = state.turn
        )
        return state.copy(rumors = state.rumors.upsertSystemRumor(next.toRumor(state.turn)))
    }

    fun updateTradeRoute(
        state: GameState,
        routeId: String,
        open: Boolean? = null,
        incomeDelta: Int = 0,
        riskDelta: Int = 0,
        controlDelta: Int = 0,
        smugglingDelta: Int = 0
    ): GameState {
        val cur = tradeOf(state, routeId)
        val next = cur.copy(
            isOpen = open ?: cur.isOpen,
            income = (cur.income + incomeDelta).coerceAtLeast(0),
            risk = (cur.risk + riskDelta).coerceIn(0, 100),
            control = (cur.control + controlDelta).coerceIn(0, 100),
            smuggling = (cur.smuggling + smugglingDelta).coerceIn(0, 100),
            lastTouchedTurn = state.turn
        )
        return state.copy(rumors = state.rumors.upsertSystemRumor(next.toRumor(state.turn)))
    }

    fun applySeasonalTrade(state: GameState): DiplomacyTradeTickResult {
        val income = tradeIncome(state)
        val risk = tradeRisk(state)
        val reports = mutableListOf<String>()
        var next = state
        if (income > 0) {
            next = next.copy(gold = next.gold + income)
            reports += "市舶与互市本旬入库${income}贯。"
        }
        if (risk >= 70) {
            next = next.copy(courtStability = (next.courtStability - 2).coerceIn(0, 100))
            reports += "外贸走私与豪商坐大，朝局稳定略降。"
        } else if (risk <= 35 && income > 0) {
            next = next.copy(prestige = (next.prestige + 1).coerceIn(0, 100))
            reports += "市舶有序，东南商税入公库，朝廷名望略增。"
        }
        return DiplomacyTradeTickResult(next, reports)
    }

    fun briefs(state: GameState): List<String> {
        val xixia = diplomacyOf(state, WorldPowerIds.XIXIA)
        val dali = diplomacyOf(state, WorldPowerIds.DALI)
        val sea = diplomacyOf(state, WorldPowerIds.SEA_TRADE)
        val income = tradeIncome(state)
        val risk = tradeRisk(state)
        return listOf(
            "西夏：${xixia.status}｜关系${xixia.relation}｜压力${xixia.pressure}",
            "大理：${dali.status}｜关系${dali.relation}｜互信${dali.trust}",
            "海贸：${sea.status}｜关系${sea.relation}｜本旬预期${income}贯｜风险${risk}"
        )
    }

    private fun DiplomacyState.toRumor(turn: Int): Rumor = Rumor(
        id = "diplomacy_$powerId",
        text = listOf(relation, pressure, trust, status, lastTouchedTurn).joinToString("|"),
        category = DIPLOMACY_CATEGORY,
        sourceCityId = powerId,
        turn = turn
    )

    private fun TradeRouteState.toRumor(turn: Int): Rumor = Rumor(
        id = "trade_$routeId",
        text = listOf(isOpen, income, risk, control, smuggling, lastTouchedTurn).joinToString("|"),
        category = TRADE_CATEGORY,
        sourceCityId = routeId,
        turn = turn
    )

    private fun Rumor.toDiplomacyStateOrNull(): DiplomacyState? = runCatching {
        val parts = text.split("|", limit = 5)
        DiplomacyState(
            powerId = sourceCityId,
            relation = parts.getOrNull(0)?.toIntOrNull() ?: initialDiplomacy.getValue(sourceCityId).relation,
            pressure = parts.getOrNull(1)?.toIntOrNull() ?: initialDiplomacy.getValue(sourceCityId).pressure,
            trust = parts.getOrNull(2)?.toIntOrNull() ?: initialDiplomacy.getValue(sourceCityId).trust,
            status = parts.getOrNull(3).orEmpty().ifBlank { initialDiplomacy.getValue(sourceCityId).status },
            lastTouchedTurn = parts.getOrNull(4)?.toIntOrNull() ?: turn
        )
    }.getOrNull()

    private fun Rumor.toTradeRouteStateOrNull(): TradeRouteState? = runCatching {
        val parts = text.split("|", limit = 6)
        TradeRouteState(
            routeId = sourceCityId,
            isOpen = parts.getOrNull(0)?.toBooleanStrictOrNull() ?: initialTradeRoutes.getValue(sourceCityId).isOpen,
            income = parts.getOrNull(1)?.toIntOrNull() ?: initialTradeRoutes.getValue(sourceCityId).income,
            risk = parts.getOrNull(2)?.toIntOrNull() ?: initialTradeRoutes.getValue(sourceCityId).risk,
            control = parts.getOrNull(3)?.toIntOrNull() ?: initialTradeRoutes.getValue(sourceCityId).control,
            smuggling = parts.getOrNull(4)?.toIntOrNull() ?: initialTradeRoutes.getValue(sourceCityId).smuggling,
            lastTouchedTurn = parts.getOrNull(5)?.toIntOrNull() ?: turn
        )
    }.getOrNull()

    private fun List<Rumor>.upsertSystemRumor(rumor: Rumor): List<Rumor> = filterNot { it.id == rumor.id } + rumor
}

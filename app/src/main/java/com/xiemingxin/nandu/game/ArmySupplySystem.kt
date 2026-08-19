package com.xiemingxin.nandu.game

/**
 * Stage 4 军团补给系统
 *
 * 原则：简单可扩展，不做物流模拟器。
 * supplyLevel: 0..100，每旬结算一次（唯一入口）。
 */
object ArmySupplySystem {

    // ─── 每旬补给消耗 ────────────────────────────────────────
    private fun baseConsumption(army: Army): Int = when (army.statusCode) {
        ArmyStatus.GARRISONED         -> 3   // 驻扎己方城：基本维持
        ArmyStatus.MARCHING           -> 12  // 行军：消耗大
        ArmyStatus.ENGAGEMENT_PENDING -> 15  // 敌前待战：最高消耗
        ArmyStatus.STANDBY            -> 2
        ArmyStatus.DISBANDED          -> 0
    }

    private fun seasonBonus(season: Season): Int = when (season) {
        Season.WINTER -> -6   // 冬季粮耗加重（负值=额外扣）
        Season.AUTUMN -> 2    // 秋收恢复略好
        else          -> 0
    }

    private fun weatherPenalty(weather: WeatherType): Int = when (weather) {
        WeatherType.SNOW  -> -5
        WeatherType.STORM -> -3
        else              -> 0
    }

    /**
     * 每旬补给结算：扣除消耗，驻扎时从城池补粮（如城池有粮）
     * 返回：(新Army, 是否从城池扣粮, 扣粮量, 警告消息?)
     */
    fun tickSupply(
        army: Army,
        state: GameState
    ): Triple<Army, Int, String?> {
        if (army.ownerFactionId != "song") return Triple(army, 0, null)

        val consume = (baseConsumption(army)
                - seasonBonus(state.season)
                - weatherPenalty(state.weather))
            .coerceAtLeast(0)

        val cityGrainDeducted: Int
        val newSupply: Int

        if (army.statusCode == ArmyStatus.GARRISONED) {
            // 驻扎：从驻城补粮
            val supplyCity = state.cities.find { it.id == army.currentCityId }
            val cityGrain = supplyCity?.grain ?: 0
            // 所需粮食 = troops * 0.1 每旬（简化比例）
            val grainNeeded = (army.troops / 100).coerceAtLeast(500)
            cityGrainDeducted = grainNeeded.coerceAtMost(cityGrain)
            // 补满补给（按城池有粮程度）
            val recovery = (cityGrainDeducted * 100 / grainNeeded.coerceAtLeast(1))
                .coerceIn(0, 100 - army.supplyLevel)
            newSupply = (army.supplyLevel - consume + recovery / 5).coerceIn(0, 100)
        } else {
            // 行军/待战：不自动补给，只消耗
            cityGrainDeducted = 0
            newSupply = (army.supplyLevel - consume).coerceIn(0, 100)
        }

        val warning: String? = when {
            newSupply < 25 ->
                "【粮警】${army.name}补给严重不足（${newSupply}%），士气动摇，行军受阻！"
            newSupply < 50 ->
                "【粮情】${army.name}粮道告急（${newSupply}%），需尽快补充。"
            else -> null
        }

        // 补给不足的士气影响
        val moralePenalty = when {
            newSupply < 25 -> 8
            newSupply < 50 -> 3
            else -> 0
        }
        val newMorale = (army.morale - moralePenalty).coerceIn(0, 100)

        return Triple(
            army.copy(
                supplyLevel = newSupply,
                morale = newMorale,
                lastSuppliedTurn = state.turn
            ),
            cityGrainDeducted,
            warning
        )
    }

    /**
     * 对所有宋方军团进行补给结算
     * 唯一入口，由 advanceTurn 调用
     */
    fun tickAllSupply(state: GameState): Pair<GameState, List<String>> {
        var cities = state.cities
        val reports = mutableListOf<String>()
        val newArmies = state.armies.map { army ->
            val (newArmy, grainDeducted, warning) = tickSupply(army, state)
            if (grainDeducted > 0) {
                cities = cities.map { c ->
                    if (c.id == army.currentCityId)
                        c.copy(grain = (c.grain - grainDeducted).coerceAtLeast(0))
                    else c
                }
            }
            warning?.let { reports.add(it) }
            newArmy
        }
        return state.copy(armies = newArmies, cities = cities) to reports
    }

    /**
     * 主动补给命令：从supplyCityId补满军团（消耗城池粮草）
     */
    fun resupplyArmy(state: GameState, armyId: String): Pair<GameState, String> {
        val army = state.armies.find { it.id == armyId }
            ?: return state to "【补给失败】找不到该军团。"
        if (army.statusCode !in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY))
            return state to "【补给失败】军团正在行军或交战，无法后方补给。"
        val supplyCity = state.cities.find { it.id == (army.supplyCityId.ifBlank { army.currentCityId }) }
            ?: return state to "【补给失败】补给城池不存在。"
        if (supplyCity.owner != "song")
            return state to "【补给失败】补给城${supplyCity.name}已不在我方控制下。"

        val deficit = 100 - army.supplyLevel
        val grainNeeded = (army.troops / 50 * deficit).coerceAtLeast(1000)
        val grainAvail = supplyCity.grain
        if (grainAvail < grainNeeded / 2)
            return state to "【补给不足】${supplyCity.name}粮草不足，仅能部分补充。"

        val actualGrain = grainNeeded.coerceAtMost(grainAvail)
        val supplyGain = (actualGrain * 100 / grainNeeded).coerceIn(0, deficit)
        val newCities = state.cities.map {
            if (it.id == supplyCity.id) it.copy(grain = it.grain - actualGrain) else it
        }
        val newArmies = state.armies.map {
            if (it.id == armyId) it.copy(supplyLevel = (it.supplyLevel + supplyGain).coerceAtMost(100)) else it
        }
        return state.copy(cities = newCities, armies = newArmies) to
            "【补给】从${supplyCity.name}调拨${actualGrain/1000}k石粮草，${army.name}补给度回至${(army.supplyLevel + supplyGain).coerceAtMost(100)}%。"
    }
}

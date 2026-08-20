package com.xiemingxin.nandu.game

/**
 * Stage 5 战争系统（修复版）
 *
 * 修复内容（PR审查）：
 *  1. 野战失败方残军 → 退往己方相邻节点或溃散，不留在目标城
 *  2. 攻克后 City.troops = 0，旧守军不转换为攻方城防
 *  3. 敌方守将/太守 → 退往己方城池或WANDERING，不变IN_COURT
 *  4. 多Army伤亡精确分配（sum == defenderLosses，无舍入误差）
 *  5. handleDefeat 无退路 → 军团溃散，不在敌方节点GARRISONED
 */
object WarSystem {

    sealed class WarResult {
        data class Success(val message: String, val newState: GameState, val outcome: BattleOutcome) : WarResult()
        data class Failure(val reason: String) : WarResult()
    }

    fun executeAttack(
        state: GameState,
        attackerArmyId: String,
        targetCityId: String
    ): WarResult {
        // ── 合法性验证 ─────────────────────────────────────────────────────────
        val attackerArmy = state.armies.find { it.id == attackerArmyId }
            ?: return WarResult.Failure("【进攻失败】找不到该军团。")
        if (attackerArmy.troops <= 0)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}已无兵可战。")
        if (attackerArmy.supplyLevel < 10)
            return WarResult.Failure("【进攻失败】粮道断绝，将士饥疲，${attackerArmy.name}不宜强攻，须先补给。")

        val targetCity = state.cities.find { it.id == targetCityId }
            ?: return WarResult.Failure("【进攻失败】目标城池不存在：$targetCityId")
        if (targetCity.owner == attackerArmy.ownerFactionId)
            return WarResult.Failure("【进攻失败】${targetCity.name}是己方城池，不得进攻。")

        val isEngaged = attackerArmy.statusCode == ArmyStatus.ENGAGEMENT_PENDING &&
                        attackerArmy.targetCityId == targetCityId
        val isAdjacent = MapData.neighborsOf(attackerArmy.currentCityId).contains(targetCityId)
        if (!isEngaged && !isAdjacent)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}距${targetCity.name}尚远，须先行军至敌境前方。")

        if (attackerArmy.lastBattleTurn == state.turn)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}本旬已经历过一场战斗，不得连续进攻。")

        val seed = state.turn * 1000031L + attackerArmyId.hashCode() * 997L + targetCityId.hashCode() * 31L

        val attackerCommander = state.officers.find { it.id == attackerArmy.commanderId }
        val garrisonOfficerId = state.cityGarrisons[targetCityId]
        val garrisonOfficer = garrisonOfficerId?.let { state.officers.find { o -> o.id == it } }

        val defenderArmies = state.armies.filter {
            it.ownerFactionId == targetCity.owner &&
            it.currentCityId == targetCityId &&
            it.troops > 0
        }

        var workingState = state
        val battleLog = mutableListOf<String>()

        // ── 野战 ─────────────────────────────────────────────────────────────
        var fieldOutcome: BattleOutcome? = null
        if (defenderArmies.isNotEmpty()) {
            val defenderCommanders = defenderArmies.associate { da ->
                da.id to state.officers.find { it.id == da.commanderId }
            }
            fieldOutcome = BattleResolver.resolveFieldBattle(
                attackerArmy = attackerArmy,
                attackerCommander = attackerCommander,
                defenderArmies = defenderArmies,
                defenderCommanders = defenderCommanders,
                city = targetCity,
                state = state,
                seed = seed
            )
            battleLog.add(fieldOutcome.report)

            // 写回野战伤亡 + 失败方残军退却（Fix #1 #4）
            workingState = applyFieldOutcome(workingState, attackerArmy.id, defenderArmies, fieldOutcome, state.turn, targetCityId)

            if (!fieldOutcome.attackerWins) {
                workingState = handleDefeat(workingState, attackerArmy.id)
                return WarResult.Success(battleLog.joinToString("\n\n"), workingState, fieldOutcome)
            }
        }

        // ── 攻城 ─────────────────────────────────────────────────────────────
        val currentAtk = workingState.armies.find { it.id == attackerArmy.id }
            ?: return WarResult.Failure("【攻城中止】我军在野战中折损殆尽。")
        val currentCity = workingState.cities.find { it.id == targetCityId } ?: targetCity

        val siegeOutcome = BattleResolver.resolveSiege(
            attackerArmy = currentAtk,
            attackerCommander = attackerCommander,
            city = currentCity,
            garrisonOfficer = garrisonOfficer,
            state = workingState,
            seed = seed + 1L
        )
        battleLog.add(siegeOutcome.report)

        workingState = applySiegeOutcome(workingState, currentAtk.id, currentCity.id, siegeOutcome, state.turn)

        val finalOutcome = if (fieldOutcome != null) {
            siegeOutcome.copy(
                attackerLosses = fieldOutcome.attackerLosses + siegeOutcome.attackerLosses,
                defenderLosses = fieldOutcome.defenderLosses + siegeOutcome.defenderLosses,
                report = battleLog.joinToString("\n\n")
            )
        } else siegeOutcome.copy(report = battleLog.joinToString("\n\n"))

        return WarResult.Success(finalOutcome.report, workingState, finalOutcome)
    }

    // ─── 精确分配伤亡（Fix #4：sum == defenderLosses，无舍入误差）─────────────
    /**
     * 按兵力比例分配 totalLoss 到多支 Army，保证 sum 精确等于 totalLoss。
     * 使用 floor + remainder 逐一补足，确定性（不依赖随机）。
     */
    internal fun distributeExactLoss(armies: List<Army>, totalLoss: Int): Map<String, Int> {
        if (armies.isEmpty() || totalLoss <= 0) return emptyMap()
        val totalTroops = armies.sumOf { it.troops }.coerceAtLeast(1)
        // floor 分配
        val floors = armies.associate { a ->
            a.id to (totalLoss.toLong() * a.troops / totalTroops).toInt().coerceIn(0, a.troops)
        }
        var remainder = totalLoss - floors.values.sum()
        val result = floors.toMutableMap()
        // 把余数按顺序补给仍有可扣空间的 Army
        for (a in armies) {
            if (remainder <= 0) break
            val canTake = a.troops - result.getValue(a.id)
            val give = minOf(remainder, canTake)
            result[a.id] = result.getValue(a.id) + give
            remainder -= give
        }
        return result
    }

    // ─── 写回野战伤亡（Fix #1 #4）────────────────────────────────────────────
    private fun applyFieldOutcome(
        state: GameState,
        attackerArmyId: String,
        defenderArmies: List<Army>,
        outcome: BattleOutcome,
        currentTurn: Int,
        targetCityId: String
    ): GameState {
        // 精确分配伤亡
        val lossMap = distributeExactLoss(defenderArmies, outcome.defenderLosses)

        var officers = state.officers
        val newArmies = state.armies.mapNotNull { army ->
            when {
                army.id == attackerArmyId -> {
                    val after = army.copy(
                        troops = outcome.attackerRemaining.coerceAtLeast(0),
                        morale = outcome.attackerMoraleAfter,
                        lastBattleTurn = currentTurn
                    )
                    if (after.troops <= 0) null else after
                }
                defenderArmies.any { it.id == army.id } -> {
                    val loss = lossMap.getValue(army.id)
                    val remaining = army.troops - loss
                    if (remaining <= 0) {
                        // 溃散：主帅回散状态
                        officers = disperseCommander(officers, army.commanderId, state)
                        null
                    } else {
                        // Fix #1：失败方残军必须撤往己方节点，不得留在目标城
                        val afterBase = army.copy(troops = remaining, morale = outcome.defenderMoraleAfter)
                        retreatDefenderArmy(afterBase, state, targetCityId)
                        // retreatDefenderArmy 返回 null 表示溃散
                        val retreated = retreatDefenderArmy(afterBase, state, targetCityId)
                        if (retreated == null) {
                            officers = disperseCommander(officers, army.commanderId, state)
                        }
                        retreated
                    }
                }
                else -> army
            }
        }
        return state.copy(armies = newArmies, officers = officers)
    }

    /**
     * 野战失败方残军退却：找相邻己方城市或溃散（Fix #1）
     * 返回 null = 溃散
     */
    private fun retreatDefenderArmy(army: Army, state: GameState, fightCityId: String): Army? {
        // 找相邻己方节点（排除战场本身）
        val safeNode = MapData.neighborsOf(fightCityId)
            .firstOrNull { nbId ->
                nbId != fightCityId &&
                state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
            }
            ?: run {
                // 也试试army.currentCityId的邻居（ENGAGEMENT_PENDING时可能不在fightCity）
                MapData.neighborsOf(army.currentCityId)
                    .firstOrNull { nbId ->
                        nbId != fightCityId &&
                        state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
                    }
            }
            ?: return null   // 无退路 → 溃散

        return army.copy(
            currentCityId = safeNode,
            statusCode = ArmyStatus.GARRISONED,
            status = ArmyStatus.GARRISONED.label,
            targetCityId = "",
            routeNodeIds = emptyList(),
            routeIndex = 0,
            marchDaysRemaining = 0
        )
    }

    // ─── 写回攻城伤亡（Fix #2 #3）────────────────────────────────────────────
    private fun applySiegeOutcome(
        state: GameState,
        attackerArmyId: String,
        targetCityId: String,
        outcome: BattleOutcome,
        currentTurn: Int
    ): GameState {
        var cities = state.cities
        var armies = state.armies
        var officers = state.officers
        var newCityGarrisons = state.cityGarrisons
        var newCityGovernors = state.cityGovernors

        // 攻击方伤亡
        armies = armies.mapNotNull { army ->
            if (army.id != attackerArmyId) return@mapNotNull army
            val after = army.copy(
                troops = outcome.attackerRemaining.coerceAtLeast(0),
                morale = outcome.attackerMoraleAfter,
                lastBattleTurn = currentTurn,
                statusCode = if (outcome.cityCaptured) ArmyStatus.GARRISONED else ArmyStatus.ENGAGEMENT_PENDING,
                status = if (outcome.cityCaptured) ArmyStatus.GARRISONED.label else ArmyStatus.ENGAGEMENT_PENDING.label,
                currentCityId = if (outcome.cityCaptured) targetCityId else army.currentCityId,
                targetCityId = if (outcome.cityCaptured) "" else army.targetCityId
            )
            if (after.troops <= 0) null else after
        }

        if (outcome.cityCaptured) {
            // Fix #2：占领后 City.troops = 0，旧守军不转换为攻方士兵
            cities = cities.map { city ->
                if (city.id != targetCityId) city
                else city.copy(
                    owner = outcome.attackerFactionId,
                    troops = 0,                                            // ← Fix #2
                    defense = (city.defense * 0.7).toInt().coerceIn(10, city.defense),
                    popularSupport = (city.popularSupport * 0.75).toInt().coerceIn(10, city.popularSupport),
                    controlState = "FRONTLINE"
                )
            }

            // Fix #3：敌方守将/太守 → 退往己方城市或WANDERING，不变IN_COURT
            val garId = state.cityGarrisons[targetCityId]
            val govId = state.cityGovernors[targetCityId]

            garId?.let { id ->
                val garOfficer = officers.find { it.id == id }
                if (garOfficer != null) {
                    officers = retreatOfficerToFaction(officers, garOfficer, state, targetCityId)
                }
                newCityGarrisons = newCityGarrisons - targetCityId
            }
            govId?.let { id ->
                val govOfficer = officers.find { it.id == id }
                if (govOfficer != null && id != garId) {  // 避免重复处理
                    officers = retreatOfficerToFaction(officers, govOfficer, state, targetCityId)
                }
                newCityGovernors = newCityGovernors - targetCityId
            }

            // 移除目标城内所有残余守方 Army（野战后应已撤退，但防御性清理）
            armies = armies.filter { a ->
                !(a.ownerFactionId == outcome.defenderFactionId && a.currentCityId == targetCityId)
            }
        } else {
            // 攻城失败：只更新城池剩余守军
            cities = cities.map { city ->
                if (city.id != targetCityId) city
                else city.copy(troops = outcome.defenderRemaining.coerceAtLeast(0))
            }
        }

        return state.copy(
            armies = armies,
            cities = cities,
            officers = officers,
            cityGarrisons = newCityGarrisons,
            cityGovernors = newCityGovernors
        )
    }

    /**
     * Fix #3：敌方人物退往己方控制的相邻城市，找不到则变WANDERING
     */
    private fun retreatOfficerToFaction(
        officers: List<Officer>,
        officer: Officer,
        state: GameState,
        lostCityId: String
    ): List<Officer> {
        val factionCities = state.cities.filter { it.owner == officer.faction }
        // 尝试退往原驻城相邻己方节点
        val safeCity = MapData.neighborsOf(lostCityId)
            .mapNotNull { nbId -> factionCities.find { it.id == nbId } }
            .firstOrNull()
            ?: factionCities.firstOrNull()  // 退到任意己方城市

        val newStatus = if (safeCity != null) OfficerStatus.DEPLOYED else OfficerStatus.WANDERING
        val newCity = safeCity?.id ?: officer.currentCityId

        return officers.map {
            if (it.id == officer.id) it.copy(status = newStatus, currentCityId = newCity) else it
        }
    }

    /**
     * 溃散时武将处置：退往己方城市待命（不死不俘，Stage5阶段）
     */
    private fun disperseCommander(officers: List<Officer>, commanderId: String, state: GameState): List<Officer> {
        val officer = officers.find { it.id == commanderId } ?: return officers
        val safeCity = state.cities.filter { it.owner == officer.faction }.firstOrNull()
        return officers.map {
            if (it.id == commanderId) it.copy(
                status = if (safeCity != null) OfficerStatus.IN_COURT else OfficerStatus.WANDERING,
                currentCityId = safeCity?.id ?: it.currentCityId
            ) else it
        }
    }

    // ─── 败军退却（Fix #5）────────────────────────────────────────────────────
    /**
     * Fix #5：无退路则溃散（不在敌方节点GARRISONED）
     */
    internal fun handleDefeat(state: GameState, armyId: String): GameState {
        val army = state.armies.find { it.id == armyId } ?: return state
        val currentCity = state.cities.find { it.id == army.currentCityId }

        val safeNode: String? = when {
            currentCity != null && currentCity.owner == army.ownerFactionId ->
                army.currentCityId  // 已在友方节点，原地
            else ->
                MapData.neighborsOf(army.currentCityId)
                    .firstOrNull { nbId ->
                        state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
                    }
            // null = 无退路
        }

        if (safeNode == null) {
            // 无退路 → 溃散，移除军团，武将散去
            val newOfficers = disperseCommander(state.officers, army.commanderId, state)
            return state.copy(
                armies = state.armies.filter { it.id != armyId },
                officers = newOfficers
            )
        }

        val newArmies = state.armies.mapNotNull { a ->
            if (a.id != armyId) return@mapNotNull a
            if (a.troops <= 0) return@mapNotNull null
            a.copy(
                currentCityId = safeNode,
                statusCode = ArmyStatus.GARRISONED,
                status = ArmyStatus.GARRISONED.label,
                targetCityId = "",
                routeNodeIds = emptyList(),
                routeIndex = 0,
                marchDaysRemaining = 0
            )
        }
        return state.copy(armies = newArmies)
    }

    /** 撤退命令 */
    fun executeRetreat(state: GameState, armyId: String): Pair<GameState, String> {
        val army = state.armies.find { it.id == armyId }
            ?: return state to "【撤退失败】找不到该军团。"
        if (army.statusCode !in setOf(ArmyStatus.ENGAGEMENT_PENDING, ArmyStatus.MARCHING))
            return state to "【撤退】${army.name}当前不在前线，无需撤退。"

        val newState = handleDefeat(state, armyId)
        val retreatCityId = newState.armies.find { it.id == armyId }?.currentCityId
        return if (retreatCityId != null) {
            val cityName = state.cities.find { it.id == retreatCityId }?.name ?: retreatCityId
            newState to "【撤退】${army.name}奉命后撤，退守${cityName}，整军待命。"
        } else {
            newState to "【撤退】${army.name}无退路，部队溃散，主帅自行归建。"
        }
    }
}

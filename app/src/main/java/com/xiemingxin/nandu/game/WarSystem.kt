package com.xiemingxin.nandu.game

/**
 * Stage 5 战争系统（Round3 最终修复版）
 *
 * 修复:
 *  R3-Fix1: applyFieldOutcome 按 attackerWins 分支处理守方残军
 *           （攻方输 → 守方只扣血留城；攻方赢 → 守方撤退/溃散）
 *  R3-Fix2: retreatDefenderArmy 退却后主帅跟着移动
 *  R3-Fix3: handleDefeat 零兵分支 officers 变更写回 state；退却后主帅同步
 *  R3-Fix4: disperseCommander 支持 excludedCityId
 *  R3-Fix5: retreatOfficerToFaction 排除 lostCityId
 *  R3-Fix6: Officer.faction 口径 — 全部使用显式 ownerFactionId / factionId
 */
object WarSystem {

    sealed class WarResult {
        data class Success(val message: String, val newState: GameState, val outcome: BattleOutcome) : WarResult()
        data class Failure(val reason: String) : WarResult()
    }

    // ─── 公共入口 ─────────────────────────────────────────────────────────────

    fun executeAttack(state: GameState, attackerArmyId: String, targetCityId: String): WarResult {
        val attackerArmy = state.armies.find { it.id == attackerArmyId }
            ?: return WarResult.Failure("【进攻失败】找不到该军团。")
        if (attackerArmy.troops <= 0)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}已无兵可战。")
        if (attackerArmy.supplyLevel < 10)
            return WarResult.Failure("【进攻失败】粮道断绝，${attackerArmy.name}不宜强攻，须先补给。")

        val targetCity = state.cities.find { it.id == targetCityId }
            ?: return WarResult.Failure("【进攻失败】目标城池不存在：$targetCityId")
        if (targetCity.owner == attackerArmy.ownerFactionId)
            return WarResult.Failure("【进攻失败】${targetCity.name}是己方城池，不得进攻。")

        val isEngaged = attackerArmy.statusCode == ArmyStatus.ENGAGEMENT_PENDING &&
                        attackerArmy.targetCityId == targetCityId
        val isAdjacent = MapData.neighborsOf(attackerArmy.currentCityId).contains(targetCityId)
        if (!isEngaged && !isAdjacent)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}距${targetCity.name}尚远。")
        if (attackerArmy.lastBattleTurn == state.turn)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}本旬已战，不得连续进攻。")

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

        // ── 野战 ──────────────────────────────────────────────────────────────
        var fieldOutcome: BattleOutcome? = null
        if (defenderArmies.isNotEmpty()) {
            val defenderCommanders = defenderArmies.associate { da ->
                da.id to state.officers.find { it.id == da.commanderId }
            }
            fieldOutcome = BattleResolver.resolveFieldBattle(
                attackerArmy, attackerCommander, defenderArmies,
                defenderCommanders, targetCity, state, seed
            )
            battleLog.add(fieldOutcome.report)
            workingState = applyFieldOutcome(
                workingState, attackerArmy.id, defenderArmies,
                fieldOutcome, state.turn, targetCityId, targetCity.owner
            )
            if (!fieldOutcome.attackerWins) {
                workingState = handleDefeat(workingState, attackerArmy.id)
                return WarResult.Success(battleLog.joinToString("\n\n"), workingState, fieldOutcome)
            }
        }

        // ── 攻城 ──────────────────────────────────────────────────────────────
        val currentAtk = workingState.armies.find { it.id == attackerArmy.id }
            ?: return WarResult.Failure("【攻城中止】我军在野战中折损殆尽。")
        val currentCity = workingState.cities.find { it.id == targetCityId } ?: targetCity

        val siegeOutcome = BattleResolver.resolveSiege(
            currentAtk, attackerCommander, currentCity,
            garrisonOfficer, workingState, seed + 1L
        )
        battleLog.add(siegeOutcome.report)
        workingState = applySiegeOutcome(
            workingState, currentAtk.id, currentCity.id,
            siegeOutcome, state.turn, targetCity.owner
        )

        val finalOutcome = if (fieldOutcome != null) {
            siegeOutcome.copy(
                attackerLosses = fieldOutcome.attackerLosses + siegeOutcome.attackerLosses,
                defenderLosses = fieldOutcome.defenderLosses + siegeOutcome.defenderLosses,
                report = battleLog.joinToString("\n\n")
            )
        } else siegeOutcome.copy(report = battleLog.joinToString("\n\n"))

        return WarResult.Success(finalOutcome.report, workingState, finalOutcome)
    }

    // ─── 精确伤亡分配 ─────────────────────────────────────────────────────────
    internal fun distributeExactLoss(armies: List<Army>, totalLoss: Int): Map<String, Int> {
        if (armies.isEmpty() || totalLoss <= 0) return emptyMap()
        val totalTroops = armies.sumOf { it.troops }.coerceAtLeast(1)
        val floors = armies.associate { a ->
            a.id to (totalLoss.toLong() * a.troops / totalTroops).toInt().coerceIn(0, a.troops)
        }
        var remainder = totalLoss - floors.values.sum()
        val result = floors.toMutableMap()
        for (a in armies) {
            if (remainder <= 0) break
            val canTake = a.troops - result.getValue(a.id)
            val give = minOf(remainder, canTake)
            result[a.id] = result.getValue(a.id) + give
            remainder -= give
        }
        return result
    }

    // ─── 写回野战伤亡（R3-Fix1：按胜负分支；R3-Fix2：主帅跟随）──────────────
    private fun applyFieldOutcome(
        state: GameState,
        attackerArmyId: String,
        defenderArmies: List<Army>,
        outcome: BattleOutcome,
        currentTurn: Int,
        targetCityId: String,
        defenderFactionId: String
    ): GameState {
        val lossMap = distributeExactLoss(defenderArmies, outcome.defenderLosses)
        var officers = state.officers

        val newArmies = state.armies.mapNotNull { army ->
            when {
                army.id == attackerArmyId -> {
                    val remaining = outcome.attackerRemaining.coerceAtLeast(0)
                    val after = army.copy(
                        troops = remaining,
                        morale = outcome.attackerMoraleAfter,
                        lastBattleTurn = currentTurn
                    )
                    if (after.troops <= 0) {
                        // Fix4: 攻击方全灭，主帅处理（soldiers writers below handle state）
                        officers = disperseCommander(
                            officers, army.commanderId, army.ownerFactionId, state, ""
                        )
                        null
                    } else after
                }

                defenderArmies.any { it.id == army.id } -> {
                    val loss = lossMap.getValue(army.id)
                    val remaining = army.troops - loss
                    if (remaining <= 0) {
                        // 全灭 → 溃散（R3-Fix3: excludedCityId = targetCityId）
                        officers = disperseCommander(
                            officers, army.commanderId, army.ownerFactionId, state, targetCityId
                        )
                        null
                    } else if (outcome.attackerWins) {
                        // R3-Fix1: 攻方赢 → 守方残军撤退
                        val afterBase = army.copy(troops = remaining, morale = outcome.defenderMoraleAfter)
                        val retreated = retreatDefenderArmy(afterBase, state, targetCityId)
                        if (retreated != null) {
                            // R3-Fix2: 主帅跟随军团移动
                            officers = syncCommander(
                                officers, army.commanderId, retreated.currentCityId, OfficerStatus.DEPLOYED
                            )
                        } else {
                            // 无退路溃散（R3-Fix4: excludedCityId = targetCityId）
                            officers = disperseCommander(
                                officers, army.commanderId, army.ownerFactionId, state, targetCityId
                            )
                        }
                        retreated
                    } else {
                        // R3-Fix1: 攻方输 → 守方残军原地留守，不撤退，只更新士气
                        army.copy(troops = remaining, morale = outcome.defenderMoraleAfter)
                    }
                }

                else -> army
            }
        }
        return state.copy(armies = newArmies, officers = officers)
    }

    // ─── 守方残军退却（R3-Fix2：不修commander，由调用方syncCommander）──────────
    private fun retreatDefenderArmy(army: Army, state: GameState, fightCityId: String): Army? {
        val safeNode = MapData.neighborsOf(fightCityId)
            .firstOrNull { nbId ->
                nbId != fightCityId &&
                state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
            }
            ?: MapData.neighborsOf(army.currentCityId)
                .firstOrNull { nbId ->
                    nbId != fightCityId &&
                    state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
                }
            ?: return null

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

    // ─── 写回攻城伤亡 ─────────────────────────────────────────────────────────
    private fun applySiegeOutcome(
        state: GameState,
        attackerArmyId: String,
        targetCityId: String,
        outcome: BattleOutcome,
        currentTurn: Int,
        defenderFactionId: String
    ): GameState {
        var cities = state.cities
        var armies = state.armies
        var officers = state.officers
        var cityGarrisons = state.cityGarrisons
        var cityGovernors = state.cityGovernors

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
            if (after.troops <= 0) {
                officers = disperseCommander(officers, army.commanderId, army.ownerFactionId, state, "")
                null
            } else {
                // R3-Fix2: 攻城胜利入城，主帅跟随
                if (outcome.cityCaptured) {
                    officers = syncCommander(officers, army.commanderId, targetCityId, OfficerStatus.DEPLOYED)
                }
                after
            }
        }

        if (outcome.cityCaptured) {
            cities = cities.map { city ->
                if (city.id != targetCityId) city
                else city.copy(
                    owner = outcome.attackerFactionId,
                    troops = 0,   // R3 Fix2: 旧守军不转化
                    defense = (city.defense * 0.7).toInt().coerceIn(0, city.defense.coerceAtLeast(0)),
                    popularSupport = (city.popularSupport * 0.75).toInt().coerceIn(0, city.popularSupport.coerceAtLeast(0)),
                    controlState = "FRONTLINE"
                )
            }

            // R3-Fix5: 守将/太守退回己方城市（排除 lostCity）
            val garId = state.cityGarrisons[targetCityId]
            val govId = state.cityGovernors[targetCityId]
            garId?.let { id ->
                val o = officers.find { it.id == id }
                if (o != null) officers = retreatOfficerToFaction(officers, o, state, targetCityId, defenderFactionId)
                cityGarrisons = cityGarrisons - targetCityId
            }
            govId?.let { id ->
                if (id != garId) {
                    val o = officers.find { it.id == id }
                    if (o != null) officers = retreatOfficerToFaction(officers, o, state, targetCityId, defenderFactionId)
                }
                cityGovernors = cityGovernors - targetCityId
            }

            // 清除目标城内残余守方 Army
            armies = armies.filter { !(it.ownerFactionId == defenderFactionId && it.currentCityId == targetCityId) }
        } else {
            cities = cities.map { city ->
                if (city.id != targetCityId) city
                else city.copy(troops = outcome.defenderRemaining.coerceAtLeast(0))
            }
        }

        return state.copy(armies = armies, cities = cities, officers = officers,
                          cityGarrisons = cityGarrisons, cityGovernors = cityGovernors)
    }

    // ─── R3-Fix5: 退往己方城市（排除 lostCityId）────────────────────────────
    private fun retreatOfficerToFaction(
        officers: List<Officer>, officer: Officer,
        state: GameState, lostCityId: String, factionId: String
    ): List<Officer> {
        val factionCities = state.cities.filter { it.owner == factionId && it.id != lostCityId }
        val safeCity = MapData.neighborsOf(lostCityId)
            .mapNotNull { nbId -> factionCities.find { it.id == nbId } }
            .firstOrNull()
            ?: factionCities.firstOrNull()
        val newStatus = if (safeCity != null) OfficerStatus.DEPLOYED else OfficerStatus.WANDERING
        val newCity = safeCity?.id ?: officer.currentCityId
        return officers.map {
            if (it.id == officer.id) it.copy(status = newStatus, currentCityId = newCity) else it
        }
    }

    // ─── R3-Fix4: disperseCommander 支持 excludedCityId ─────────────────────
    internal fun disperseCommander(
        officers: List<Officer>, commanderId: String,
        ownerFactionId: String, state: GameState, excludedCityId: String
    ): List<Officer> {
        val safeCity = state.cities.filter {
            it.owner == ownerFactionId && (excludedCityId.isBlank() || it.id != excludedCityId)
        }.firstOrNull()
        return officers.map {
            if (it.id != commanderId) it
            else it.copy(
                status = if (safeCity != null) OfficerStatus.IN_COURT else OfficerStatus.WANDERING,
                currentCityId = safeCity?.id ?: it.currentCityId
            )
        }
    }

    // ─── R3-Fix2: 主帅与军团位置同步 helper ─────────────────────────────────
    internal fun syncCommander(
        officers: List<Officer>, commanderId: String,
        newCityId: String, newStatus: OfficerStatus
    ): List<Officer> = officers.map {
        if (it.id == commanderId) it.copy(currentCityId = newCityId, status = newStatus) else it
    }

    // ─── R3-Fix3: handleDefeat（零兵分支 officers 写回）──────────────────────
    internal fun handleDefeat(state: GameState, armyId: String): GameState {
        val army = state.armies.find { it.id == armyId } ?: return state
        val currentCity = state.cities.find { it.id == army.currentCityId }

        val safeNode: String? = when {
            currentCity != null && currentCity.owner == army.ownerFactionId -> army.currentCityId
            else -> MapData.neighborsOf(army.currentCityId)
                .firstOrNull { nbId -> state.cities.find { it.id == nbId }?.owner == army.ownerFactionId }
        }

        return if (safeNode == null) {
            // 无退路 → 溃散（officers 变更写回 state）
            val newOfficers = disperseCommander(state.officers, army.commanderId, army.ownerFactionId, state, army.currentCityId)
            state.copy(
                armies = state.armies.filter { it.id != armyId },
                officers = newOfficers
            )
        } else {
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
            // R3-Fix3: 主帅跟随退却
            val newOfficers = if (newArmies.any { it.id == armyId }) {
                syncCommander(state.officers, army.commanderId, safeNode, OfficerStatus.DEPLOYED)
            } else {
                disperseCommander(state.officers, army.commanderId, army.ownerFactionId, state, army.currentCityId)
            }
            state.copy(armies = newArmies, officers = newOfficers)
        }
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

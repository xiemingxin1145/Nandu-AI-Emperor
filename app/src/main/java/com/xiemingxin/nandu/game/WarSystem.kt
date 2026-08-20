package com.xiemingxin.nandu.game

/**
 * Stage 5 战争系统（第三轮修复版）
 *
 * 修复内容：
 *  Round2-Fix1/2: applyFieldOutcome 按 attackerWins 分支处理守方残军
 *               + 删除重复 retreatDefenderArmy 调用
 *  Round2-Fix3: disperseCommander / retreatOfficerToFaction 显式传入
 *               ownerFactionId / defenderFactionId，不再依赖 officer.faction
 *  Round2-Fix4: 攻击方全灭时主帅同步处理
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

            // Round2-Fix1: 按胜负分支处理守方
            workingState = applyFieldOutcome(
                workingState, attackerArmy.id, defenderArmies,
                fieldOutcome, state.turn, targetCityId
            )

            if (!fieldOutcome.attackerWins) {
                // 攻方失败 → 退却
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

    // ─── 精确伤亡分配 ──────────────────────────────────────────────────────────
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

    // ─── 写回野战伤亡（Round2-Fix1 + Round2-Fix2）────────────────────────────
    private fun applyFieldOutcome(
        state: GameState,
        attackerArmyId: String,
        defenderArmies: List<Army>,
        outcome: BattleOutcome,
        currentTurn: Int,
        targetCityId: String
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
                        // Round2-Fix4: 攻击方全灭，主帅同步处理
                        officers = disperseCommander(officers, army.commanderId, army.ownerFactionId, state)
                        null
                    } else after
                }

                defenderArmies.any { it.id == army.id } -> {
                    val loss = lossMap.getValue(army.id)
                    val remaining = army.troops - loss
                    if (remaining <= 0) {
                        // 守方全灭 → 溃散（Round2-Fix3：传 ownerFactionId）
                        officers = disperseCommander(officers, army.commanderId, army.ownerFactionId, state)
                        null
                    } else if (outcome.attackerWins) {
                        // Round2-Fix1: 攻方赢了 → 守方残军撤退（Fix2: 只调用一次）
                        val afterBase = army.copy(troops = remaining, morale = outcome.defenderMoraleAfter)
                        val retreated = retreatDefenderArmy(afterBase, state, targetCityId)
                        if (retreated == null) {
                            officers = disperseCommander(officers, army.commanderId, army.ownerFactionId, state)
                        }
                        retreated  // null → 溃散移除
                    } else {
                        // Round2-Fix1: 攻方输了 → 守方残军原地留守，不撤退不溃散
                        army.copy(troops = remaining, morale = outcome.defenderMoraleAfter)
                    }
                }

                else -> army
            }
        }
        return state.copy(armies = newArmies, officers = officers)
    }

    /** 野战胜利后守方残军退却（仅在 attackerWins==true 时调用） */
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

    // ─── 写回攻城伤亡 ─────────────────────────────────────────────────────────
    private fun applySiegeOutcome(
        state: GameState,
        attackerArmyId: String,
        targetCityId: String,
        outcome: BattleOutcome,
        currentTurn: Int,
        defenderFactionId: String   // Round2-Fix3: 显式传入，不依赖officer.faction
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
            if (after.troops <= 0) {
                // Round2-Fix4: 攻击方攻城中全灭，主帅处理
                officers = disperseCommander(officers, army.commanderId, army.ownerFactionId, state)
                null
            } else after
        }

        if (outcome.cityCaptured) {
            // City.troops = 0（旧守军不转化为攻方）
            cities = cities.map { city ->
                if (city.id != targetCityId) city
                else city.copy(
                    owner = outcome.attackerFactionId,
                    troops = 0,
                    defense = (city.defense * 0.7).toInt().coerceIn(10, city.defense),
                    popularSupport = (city.popularSupport * 0.75).toInt().coerceIn(10, city.popularSupport),
                    controlState = "FRONTLINE"
                )
            }

            // Round2-Fix3: 守将/太守用 defenderFactionId 退往己方城市
            val garId = state.cityGarrisons[targetCityId]
            val govId = state.cityGovernors[targetCityId]

            garId?.let { id ->
                val garOfficer = officers.find { it.id == id }
                if (garOfficer != null) {
                    officers = retreatOfficerToFaction(
                        officers, garOfficer, state, targetCityId, defenderFactionId
                    )
                }
                newCityGarrisons = newCityGarrisons - targetCityId
            }
            govId?.let { id ->
                if (id != garId) {
                    val govOfficer = officers.find { it.id == id }
                    if (govOfficer != null) {
                        officers = retreatOfficerToFaction(
                            officers, govOfficer, state, targetCityId, defenderFactionId
                        )
                    }
                }
                newCityGovernors = newCityGovernors - targetCityId
            }

            // 清除目标城内残余守方 Army
            armies = armies.filter { a ->
                !(a.ownerFactionId == defenderFactionId && a.currentCityId == targetCityId)
            }
        } else {
            cities = cities.map { city ->
                if (city.id != targetCityId) city
                else city.copy(troops = outcome.defenderRemaining.coerceAtLeast(0))
            }
        }

        return state.copy(
            armies = armies, cities = cities, officers = officers,
            cityGarrisons = newCityGarrisons, cityGovernors = newCityGovernors
        )
    }

    // ─── Round2-Fix3: 退往己方城市，用显式 factionId ─────────────────────────
    private fun retreatOfficerToFaction(
        officers: List<Officer>,
        officer: Officer,
        state: GameState,
        lostCityId: String,
        factionId: String             // 显式传入，不用 officer.faction
    ): List<Officer> {
        val factionCities = state.cities.filter { it.owner == factionId }
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

    // ─── Round2-Fix3+Fix4: 溃散主帅，用显式 ownerFactionId ──────────────────
    private fun disperseCommander(
        officers: List<Officer>,
        commanderId: String,
        ownerFactionId: String,        // 显式传入，不用 officer.faction
        state: GameState
    ): List<Officer> {
        val safeCity = state.cities.filter { it.owner == ownerFactionId }.firstOrNull()
        return officers.map {
            if (it.id != commanderId) it
            else it.copy(
                status = if (safeCity != null) OfficerStatus.IN_COURT else OfficerStatus.WANDERING,
                currentCityId = safeCity?.id ?: it.currentCityId
            )
        }
    }

    // ─── 败军退却（Round2-Fix3: 用 army.ownerFactionId）──────────────────────
    internal fun handleDefeat(state: GameState, armyId: String): GameState {
        val army = state.armies.find { it.id == armyId } ?: return state
        val currentCity = state.cities.find { it.id == army.currentCityId }

        val safeNode: String? = when {
            currentCity != null && currentCity.owner == army.ownerFactionId ->
                army.currentCityId
            else ->
                MapData.neighborsOf(army.currentCityId)
                    .firstOrNull { nbId ->
                        state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
                    }
        }

        if (safeNode == null) {
            // 无退路 → 溃散（Round2-Fix3: ownerFactionId）
            val newOfficers = disperseCommander(state.officers, army.commanderId, army.ownerFactionId, state)
            return state.copy(
                armies = state.armies.filter { it.id != armyId },
                officers = newOfficers
            )
        }

        val newArmies = state.armies.mapNotNull { a ->
            if (a.id != armyId) return@mapNotNull a
            if (a.troops <= 0) {
                disperseCommander(state.officers, a.commanderId, a.ownerFactionId, state)
                return@mapNotNull null
            }
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

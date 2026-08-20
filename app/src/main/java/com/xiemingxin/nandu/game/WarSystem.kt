package com.xiemingxin.nandu.game

/**
 * Stage 5 战争系统
 *
 * 负责：
 *  1. 战争命令合法性验证
 *  2. 野战/攻城流程控制（先野战再攻城）
 *  3. BattleOutcome写回GameState（伤亡、占领、士气、退却）
 *  4. 撤退命令
 */
object WarSystem {

    sealed class WarResult {
        data class Success(val message: String, val newState: GameState, val outcome: BattleOutcome) : WarResult()
        data class Failure(val reason: String) : WarResult()
    }

    /**
     * 主入口：attack_city 命令
     *
     * 流程：
     *   验证合法性
     *   → 检查目标城是否有敌方Army（野战）
     *   → 若有，先野战
     *   → 野战胜利后 or 无敌方Army → 攻城
     *   → 写回GameState
     */
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

        // 距离检查：军团必须在目标城的邻近节点（或ENGAGEMENT_PENDING指向该城）
        val isEngaged = attackerArmy.statusCode == ArmyStatus.ENGAGEMENT_PENDING &&
                        attackerArmy.targetCityId == targetCityId
        val isAdjacent = MapData.neighborsOf(attackerArmy.currentCityId).contains(targetCityId)
        if (!isEngaged && !isAdjacent)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}距${targetCity.name}尚远，须先行军至敌境前方。")

        // 每旬一战限制
        if (attackerArmy.lastBattleTurn == state.turn)
            return WarResult.Failure("【进攻失败】${attackerArmy.name}本旬已经历过一场战斗，不得连续进攻。")

        // ── 确定seed ─────────────────────────────────────────────────────────
        val seed = state.turn * 1000031L + attackerArmyId.hashCode() * 997L + targetCityId.hashCode() * 31L

        // ── 获取相关人物 ──────────────────────────────────────────────────────
        val attackerCommander = state.officers.find { it.id == attackerArmy.commanderId }
        val garrisonOfficerId = state.cityGarrisons[targetCityId]
        val garrisonOfficer = garrisonOfficerId?.let { state.officers.find { o -> o.id == it } }

        // ── 检查敌方野战Army ──────────────────────────────────────────────────
        val defenderArmies = state.armies.filter {
            it.ownerFactionId == targetCity.owner &&
            it.currentCityId == targetCityId &&
            it.troops > 0
        }

        var workingState = state
        var battleLog = mutableListOf<String>()

        // ── 野战（如有敌方Army） ───────────────────────────────────────────────
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

            // 写回野战伤亡
            workingState = applyFieldOutcome(workingState, attackerArmy.id, defenderArmies, fieldOutcome, state.turn)

            // 野战失败 → 退却，不再攻城
            if (!fieldOutcome.attackerWins) {
                workingState = handleDefeat(workingState, attackerArmy.id)
                return WarResult.Success(
                    battleLog.joinToString("\n\n"),
                    workingState,
                    fieldOutcome
                )
            }
        }

        // ── 攻城（野战胜利后，或无敌方Army） ────────────────────────────────────
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

        // 写回攻城伤亡
        workingState = applySiegeOutcome(workingState, currentAtk.id, currentCity.id, siegeOutcome, state.turn)

        val finalOutcome = if (fieldOutcome != null) {
            // 合并战报
            siegeOutcome.copy(
                attackerLosses = (fieldOutcome.attackerLosses + siegeOutcome.attackerLosses),
                defenderLosses = (fieldOutcome.defenderLosses + siegeOutcome.defenderLosses),
                report = battleLog.joinToString("\n\n")
            )
        } else siegeOutcome.copy(report = battleLog.joinToString("\n\n"))

        return WarResult.Success(finalOutcome.report, workingState, finalOutcome)
    }

    // ─── 写回野战伤亡 ─────────────────────────────────────────────────────────
    private fun applyFieldOutcome(
        state: GameState,
        attackerArmyId: String,
        defenderArmies: List<Army>,
        outcome: BattleOutcome,
        currentTurn: Int
    ): GameState {
        val defTotal = defenderArmies.sumOf { it.troops }.coerceAtLeast(1)

        val newArmies = state.armies.mapNotNull { army ->
            when {
                army.id == attackerArmyId -> {
                    val after = army.copy(
                        troops = outcome.attackerRemaining.coerceAtLeast(0),
                        morale = outcome.attackerMoraleAfter,
                        lastBattleTurn = currentTurn
                    )
                    if (after.troops <= 0) null else after   // 全灭则移除
                }
                defenderArmies.any { it.id == army.id } -> {
                    // 按兵力比例分配伤亡
                    val proportion = army.troops.toDouble() / defTotal
                    val loss = (outcome.defenderLosses * proportion).toInt().coerceIn(0, army.troops)
                    val after = army.copy(
                        troops = army.troops - loss,
                        morale = outcome.defenderMoraleAfter
                    )
                    // 野战失败，守方残军可留在城内（不退却，等攻城）
                    if (after.troops <= 0) null else after
                }
                else -> army
            }
        }
        return state.copy(armies = newArmies)
    }

    // ─── 写回攻城伤亡 ─────────────────────────────────────────────────────────
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

        // 守方城池伤亡 + 可能换主
        cities = cities.map { city ->
            if (city.id != targetCityId) return@map city
            if (outcome.cityCaptured) {
                // 占领：owner更换，城防受损，controlState=FRONTLINE
                city.copy(
                    owner = outcome.attackerFactionId,
                    troops = outcome.defenderRemaining.coerceAtLeast(0),
                    defense = (city.defense * 0.7).toInt().coerceIn(10, city.defense),
                    popularSupport = (city.popularSupport * 0.75).toInt().coerceIn(10, city.popularSupport),
                    controlState = "FRONTLINE"
                )
            } else {
                city.copy(troops = outcome.defenderRemaining.coerceAtLeast(0))
            }
        }

        // 占领后：守将归属变化（若守将属于旧势力，解除职务）
        var newCityGarrisons = state.cityGarrisons
        var newCityGovernors = state.cityGovernors
        if (outcome.cityCaptured) {
            val garId = state.cityGarrisons[targetCityId]
            val govId = state.cityGovernors[targetCityId]
            // 守将回待命
            garId?.let { id ->
                officers = officers.map { if (it.id == id) it.copy(status = OfficerStatus.IN_COURT) else it }
                newCityGarrisons = newCityGarrisons - targetCityId
            }
            govId?.let { id ->
                officers = officers.map { if (it.id == id) it.copy(status = OfficerStatus.IN_COURT) else it }
                newCityGovernors = newCityGovernors - targetCityId
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

    // ─── 败军退却 ─────────────────────────────────────────────────────────────
    private fun handleDefeat(state: GameState, armyId: String): GameState {
        val army = state.armies.find { it.id == armyId } ?: return state
        // 找最近己方节点（当前节点若已是友方，直接驻扎）
        val currentCity = state.cities.find { it.id == army.currentCityId }
        val safeNode = when {
            currentCity != null && currentCity.owner == army.ownerFactionId ->
                army.currentCityId  // 已在友方节点
            else -> {
                // 找相邻己方节点
                MapData.neighborsOf(army.currentCityId)
                    .firstOrNull { nbId ->
                        state.cities.find { it.id == nbId }?.owner == army.ownerFactionId
                    } ?: army.currentCityId  // 找不到则原地
            }
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

    /**
     * 撤退命令：ENGAGEMENT_PENDING → 退回安全节点
     */
    fun executeRetreat(state: GameState, armyId: String): Pair<GameState, String> {
        val army = state.armies.find { it.id == armyId }
            ?: return state to "【撤退失败】找不到该军团。"
        if (army.statusCode !in setOf(ArmyStatus.ENGAGEMENT_PENDING, ArmyStatus.MARCHING))
            return state to "【撤退】${army.name}当前不在前线，无需撤退。"

        val newState = handleDefeat(state, armyId)
        val retreatCity = newState.armies.find { it.id == armyId }?.currentCityId
        val cityName = state.cities.find { it.id == retreatCity }?.name ?: retreatCity ?: "后方"
        return newState to "【撤退】${army.name}奉命后撤，退守${cityName}，整军待命。"
    }
}

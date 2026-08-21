package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.NpcInitiative
import com.xiemingxin.nandu.ai.WorldAction
import com.xiemingxin.nandu.ai.WorldTurnPlan

/**
 * Stage 6：AI 世界动作执行器。
 *
 * 大模型没有写世界状态的权限。它只能提交 WorldAction；这里逐条检查军团归属、
 * 城池归属、路线、补给和战争规则，合法才落地。这样便宜模型也不会靠幻觉改数值。
 */
data class WorldAiExecutionResult(
    val newState: GameState,
    val reports: List<String>,
    val npcInitiatives: List<NpcInitiative>
)

object WorldAiTurnExecutor {
    private const val MAX_ACTIONS_PER_TURN = 4
    private const val MAX_NPC_INITIATIVES = 3

    fun execute(state: GameState, plan: WorldTurnPlan): WorldAiExecutionResult {
        val playerFactionId = state.factions.firstOrNull { it.isPlayable }?.id ?: "song"
        var working = state
        val reports = mutableListOf<String>()

        val actions = plan.actions
            .filter { WorldAction.isValid(it.type) }
            .take(MAX_ACTIONS_PER_TURN)

        for (action in actions) {
            // DELEGATION-001：先判断这个动作到底是提给谁的——不能只看 armyId
            // （募兵等新动作类型可能压根还没有军团，只有 officerId/targetCityId）。
            val targetFactionId = action.factionId.ifBlank {
                working.armies.firstOrNull { it.id == action.armyId }?.ownerFactionId ?: ""
            }

            if (targetFactionId == playerFactionId) {
                // 玩家（宋）势力：一律走授权校验——包括原有的 move/attack/resupply，
                // 不再无条件驳回，也不允许绕开圣旨直接执行。
                when (val v = DelegatedActionValidator.validate(working, action, playerFactionId)) {
                    is DelegatedActionValidator.ValidationResult.Rejected -> {
                        reports += "【AI军议驳回】${v.reason}"
                    }
                    is DelegatedActionValidator.ValidationResult.Approved -> {
                        val (newState, record) = DelegatedActionValidator.execute(working, action, v.mandate, playerFactionId)
                        working = newState.copy(mandateExecutionLog = newState.mandateExecutionLog + record)
                        reports += if (record.success) "【奉旨】${record.description}"
                        else "【奉旨未成】${record.description}${if (record.failureReason.isNotBlank()) "：${record.failureReason}" else ""}"
                    }
                }
                continue
            }

            // 非玩家势力（金/西夏/义军等）：不受宋廷圣旨约束，新增的委任类动作
            // （募兵/修防/任将）直接自主执行，走同样的权威系统但跳过 Mandate 校验。
            if (action.type in setOf("recruit_troops", "repair_defense", "assign_commander")) {
                val (newState, msg) = executeNonPlayerDelegatedAction(working, action, targetFactionId)
                working = newState
                reports += msg
                continue
            }

            val army = working.armies.firstOrNull { it.id == action.armyId }
            if (army == null) {
                reports += "【AI军议驳回】找不到军团 ${action.armyId}。"
                continue
            }
            if (action.factionId.isNotBlank() && action.factionId != army.ownerFactionId) {
                reports += "【AI军议驳回】${army.name}归属与计划势力不符。"
                continue
            }

            when (action.type) {
                "move_army" -> {
                    val target = working.cities.firstOrNull { it.id == action.targetCityId }
                    if (target == null) {
                        reports += "【AI军议驳回】${army.name}目标城不存在：${action.targetCityId}。"
                        continue
                    }
                    val (newState, message) = ArmyMovementSystem.rerouteArmy(
                        working,
                        army.id,
                        target.id
                    )
                    if (newState !== working || message.startsWith("【改道】")) working = newState
                    reports += decorateReason(message, action.reason)
                }

                "attack_city" -> {
                    val target = working.cities.firstOrNull { it.id == action.targetCityId }
                    if (target == null || target.owner == army.ownerFactionId) {
                        reports += "【AI军议驳回】${army.name}的攻击目标非法。"
                        continue
                    }
                    when (val result = WarSystem.executeAttack(working, army.id, target.id)) {
                        is WarSystem.WarResult.Success -> {
                            working = result.newState
                            reports += decorateReason(
                                "【敌方军报】${army.name}对${target.name}发动攻势。\n${result.message}",
                                action.reason
                            )
                        }
                        is WarSystem.WarResult.Failure -> {
                            reports += "【AI军议驳回】${army.name}未能攻击${target.name}：${result.reason}"
                        }
                    }
                }

                "resupply_army" -> {
                    val result = resupplyNonPlayerArmy(working, army.id, playerFactionId)
                    working = result.first
                    reports += decorateReason(result.second, action.reason)
                }

                "hold_army" -> {
                    val where = working.cities.firstOrNull { it.id == army.currentCityId }?.name ?: army.currentCityId
                    reports += "【敌情】${army.name}在${where}按兵不动${reasonSuffix(action.reason)}"
                }
            }
        }

        // 非玩家军团也真实走地图，不允许“AI一句话瞬移”。每旬推进一个道路节点。
        val movement = tickNonPlayerMovement(working, playerFactionId)
        working = movement.first
        reports += movement.second

        // 非玩家军团同样吃粮、掉补给与士气，避免 AI 无视后勤作弊。
        val supply = tickNonPlayerSupply(working, playerFactionId)
        working = supply.first
        reports += supply.second

        // DELEGATION-001：便宜从事的负责人不需要等 AI 模型"想起"授权范围内的事——
        // 这是确定性的委托代理，不是博弈选择。AI 模型若已经在 plan.actions 里主动
        // 提议了同一件事，上面的循环已经处理过；这里只补"AI 没提但明显该做"的部分，
        // 目前只做募兵这一项（任务书里"自行募义勇补军"的原始示例），其余仍要 AI 提议。
        val autoDelegated = autoExecuteMandates(working, playerFactionId)
        working = autoDelegated.first
        reports += autoDelegated.second

        val initiatives = plan.npcInitiatives
            .asSequence()
            .filter { it.text.isNotBlank() }
            .filter { init ->
                val officer = working.officers.firstOrNull { it.id == init.officerId }
                officer != null && officer.status !in setOf(OfficerStatus.HIDDEN, OfficerStatus.DECEASED)
            }
            .take(MAX_NPC_INITIATIVES)
            .toList()

        return WorldAiExecutionResult(working, reports, initiatives)
    }

    /**
     * 没有真 AI、接口超时或模型返回坏 JSON 时使用的本地战略脑。
     * 不追求“会说话”，只保证敌军不会随机送头：先看补给，再看兵力，再选最近薄弱目标。
     */
    fun heuristicPlan(state: GameState): WorldTurnPlan {
        val playerFactionId = state.factions.firstOrNull { it.isPlayable }?.id ?: "song"
        val actions = mutableListOf<WorldAction>()

        val aiArmies = state.armies
            .filter { it.ownerFactionId != playerFactionId && it.statusCode != ArmyStatus.DISBANDED && it.troops > 0 }
            .sortedByDescending { it.troops }

        for (army in aiArmies) {
            if (actions.size >= 3) break

            if (army.supplyLevel < 40 && army.statusCode in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY)) {
                actions += WorldAction(
                    type = "resupply_army",
                    factionId = army.ownerFactionId,
                    armyId = army.id,
                    reason = "补给不足，先整顿粮道"
                )
                continue
            }

            if (army.statusCode == ArmyStatus.ENGAGEMENT_PENDING && army.targetCityId.isNotBlank()) {
                val target = state.cities.firstOrNull { it.id == army.targetCityId }
                if (target != null && target.owner != army.ownerFactionId) {
                    val attackPower = army.troops *
                        (0.55 + army.morale / 100.0 * 0.45) *
                        (0.50 + army.supplyLevel / 100.0 * 0.50)
                    val fieldDefenders = state.armies
                        .filter { it.ownerFactionId == target.owner && it.currentCityId == target.id }
                        .sumOf { it.troops }
                    val defendPower = target.troops * (1.0 + target.defense / 100.0) + fieldDefenders
                    val shouldAttack = attackPower >= defendPower * 0.82 ||
                        (target.controlState == "CONTESTED" && attackPower >= defendPower * 0.68)
                    actions += if (shouldAttack) {
                        WorldAction(
                            type = "attack_city",
                            factionId = army.ownerFactionId,
                            armyId = army.id,
                            targetCityId = target.id,
                            reason = "兵力与补给尚可，目标防线存在突破机会"
                        )
                    } else {
                        WorldAction(
                            type = "hold_army",
                            factionId = army.ownerFactionId,
                            armyId = army.id,
                            reason = "敌城守备偏强，避免无谓强攻"
                        )
                    }
                    continue
                }
            }

            if (army.statusCode == ArmyStatus.MARCHING) {
                actions += WorldAction(
                    type = "hold_army",
                    factionId = army.ownerFactionId,
                    armyId = army.id,
                    reason = "按既定路线继续行军"
                )
                continue
            }

            val targets = state.cities.filter { it.owner == playerFactionId }
            val bestTarget = targets.mapNotNull { city ->
                val route = ArmyMovementSystem.findRoute(army.currentCityId, city.id, army.armyType)
                    ?: return@mapNotNull null
                val frontierBonus = if (city.controlState == "FRONTLINE" || city.controlState == "CONTESTED") 35.0 else 0.0
                val capitalPenalty = if (city.isCapital) 80.0 else 0.0
                val score = route.size * 18.0 + city.defense * 0.65 + city.troops / 900.0 + capitalPenalty - frontierBonus
                Triple(city, route.size, score)
            }.minByOrNull { it.third }?.first

            if (bestTarget != null) {
                actions += WorldAction(
                    type = "move_army",
                    factionId = army.ownerFactionId,
                    armyId = army.id,
                    targetCityId = bestTarget.id,
                    reason = "选择路线较近且守备较薄的${bestTarget.name}施压"
                )
            }
        }

        return WorldTurnPlan(
            strategySummary = "本地战略脑按补给、兵力、城防与道路距离推演非玩家势力行动。",
            actions = actions,
            npcInitiatives = emptyList()
        )
    }

    private fun tickNonPlayerMovement(
        state: GameState,
        playerFactionId: String
    ): Pair<GameState, List<String>> {
        var officers = state.officers
        val reports = mutableListOf<String>()

        val armies = state.armies.map { army ->
            if (army.ownerFactionId == playerFactionId || army.statusCode != ArmyStatus.MARCHING) return@map army
            if (army.routeNodeIds.isEmpty() || army.routeIndex >= army.routeNodeIds.size - 1) {
                return@map army.copy(statusCode = ArmyStatus.GARRISONED, status = ArmyStatus.GARRISONED.label)
            }

            val nextIndex = army.routeIndex + 1
            val nextNode = army.routeNodeIds[nextIndex]
            val nextCity = state.cities.firstOrNull { it.id == nextNode }

            if (nextCity != null && nextCity.owner.isNotBlank() && nextCity.owner != army.ownerFactionId) {
                val targetName = nextCity.name
                reports += "【敌情】${army.name}推进至${state.cities.firstOrNull { it.id == army.currentCityId }?.name ?: army.currentCityId}一线，前方${targetName}已进入攻击范围。"
                army.copy(
                    statusCode = ArmyStatus.ENGAGEMENT_PENDING,
                    status = ArmyStatus.ENGAGEMENT_PENDING.label,
                    targetCityId = nextNode,
                    marchDaysRemaining = 0
                )
            } else {
                val arrived = nextIndex >= army.routeNodeIds.size - 1
                val newStatus = if (arrived) ArmyStatus.GARRISONED else ArmyStatus.MARCHING
                officers = officers.map { officer ->
                    if (officer.id == army.commanderId) {
                        officer.copy(
                            currentCityId = nextNode,
                            status = OfficerStatus.DEPLOYED
                        )
                    } else officer
                }
                if (arrived) {
                    reports += "【敌情】${army.name}抵达${nextCity?.name ?: nextNode}，重新集结。"
                }
                army.copy(
                    currentCityId = nextNode,
                    routeIndex = nextIndex,
                    statusCode = newStatus,
                    status = newStatus.label,
                    targetCityId = if (arrived) "" else army.targetCityId,
                    marchDaysRemaining = if (arrived) 0 else (army.routeNodeIds.size - 1 - nextIndex) * 8,
                    supplyCityId = if (arrived) nextNode else army.supplyCityId
                )
            }
        }

        return state.copy(armies = armies, officers = officers) to reports
    }

    private fun tickNonPlayerSupply(
        state: GameState,
        playerFactionId: String
    ): Pair<GameState, List<String>> {
        var cities = state.cities
        val reports = mutableListOf<String>()

        val armies = state.armies.map { army ->
            if (army.ownerFactionId == playerFactionId || army.statusCode == ArmyStatus.DISBANDED) return@map army

            val baseConsume = when (army.statusCode) {
                ArmyStatus.GARRISONED -> 3
                ArmyStatus.MARCHING -> 11
                ArmyStatus.ENGAGEMENT_PENDING -> 14
                ArmyStatus.STANDBY -> 2
                ArmyStatus.DISBANDED -> 0
            }
            val weatherExtra = when (state.weather) {
                WeatherType.SNOW -> 5
                WeatherType.STORM -> 3
                else -> 0
            }
            val winterExtra = if (state.season == Season.WINTER) 5 else 0
            var supply = (army.supplyLevel - baseConsume - weatherExtra - winterExtra).coerceIn(0, 100)

            if (army.statusCode == ArmyStatus.GARRISONED) {
                val city = cities.firstOrNull { it.id == army.currentCityId && it.owner == army.ownerFactionId }
                if (city != null && city.grain > 0) {
                    val grainCost = (army.troops / 120).coerceIn(300, 2500).coerceAtMost(city.grain)
                    cities = cities.map { if (it.id == city.id) it.copy(grain = it.grain - grainCost) else it }
                    supply = (supply + 10).coerceAtMost(100)
                }
            }

            val moralePenalty = when {
                supply < 20 -> 7
                supply < 40 -> 3
                else -> 0
            }
            if (supply < 35) {
                reports += "【敌情】${army.name}粮道吃紧，补给仅余${supply}%。"
            }
            army.copy(
                supplyLevel = supply,
                morale = (army.morale - moralePenalty).coerceIn(0, 100),
                lastSuppliedTurn = state.turn
            )
        }

        return state.copy(armies = armies, cities = cities) to reports
    }

    private fun resupplyNonPlayerArmy(
        state: GameState,
        armyId: String,
        playerFactionId: String
    ): Pair<GameState, String> {
        val army = state.armies.firstOrNull { it.id == armyId }
            ?: return state to "【AI补给失败】找不到军团。"
        if (army.ownerFactionId == playerFactionId)
            return state to "【AI补给驳回】不得控制玩家军团。"
        if (army.statusCode !in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY))
            return state to "【AI补给失败】${army.name}正在行军或敌前待战。"

        val cityId = army.supplyCityId.ifBlank { army.currentCityId }
        val city = state.cities.firstOrNull { it.id == cityId && it.owner == army.ownerFactionId }
            ?: return state to "【AI补给失败】${army.name}没有己方补给城。"
        if (army.supplyLevel >= 95)
            return state to "【敌情】${army.name}粮秣充足，无需额外补给。"

        val deficit = 100 - army.supplyLevel
        val grainNeeded = (army.troops / 60 * deficit).coerceAtLeast(800)
        val actual = grainNeeded.coerceAtMost(city.grain)
        if (actual <= 0) return state to "【敌情】${city.name}无粮可调，${army.name}补给未成。"

        val gain = (actual * 100 / grainNeeded.coerceAtLeast(1)).coerceIn(1, deficit)
        val newCities = state.cities.map { if (it.id == city.id) it.copy(grain = it.grain - actual) else it }
        val newArmies = state.armies.map {
            if (it.id == army.id) it.copy(supplyLevel = (it.supplyLevel + gain).coerceAtMost(100)) else it
        }
        return state.copy(cities = newCities, armies = newArmies) to
            "【敌情】${army.name}自${city.name}补充粮秣，补给升至${(army.supplyLevel + gain).coerceAtMost(100)}%。"
    }

    /**
     * DELEGATION-001 第三部分：非玩家势力（金/西夏等）自主执行募兵/修防/任将，
     * 不受宋廷圣旨约束，不查 Mandate——但同样必须走真实权威系统，不能直接改数值。
     * 预算从该势力自己的城池财政（City.gold/grain）出，因为非玩家势力没有
     * 统一的"中央国库"概念（那是 GameState.gold/grain，专属玩家）。
     */
    private fun executeNonPlayerDelegatedAction(
        state: GameState,
        action: WorldAction,
        factionId: String
    ): Pair<GameState, String> {
        return when (action.type) {
            "recruit_troops" -> {
                val cityId = action.targetCityId
                val city = state.cities.firstOrNull { it.id == cityId && it.owner == factionId }
                    ?: return state to "【敌情】募兵未行：目标城池不在其治下。"
                val commanderId = action.officerId.ifBlank {
                    state.armies.firstOrNull { it.id == action.armyId }?.commanderId ?: ""
                }
                if (commanderId.isBlank()) return state to "【敌情】募兵未行：未指明领兵之人。"
                val troopsWanted = action.amount.coerceIn(0, 20000)
                if (troopsWanted <= 0) return state to "【敌情】募兵未行：未明确实际人数。"
                val goldCost = ((troopsWanted + 999) / 1000) * 100
                val grainCost = troopsWanted * 2
                if (city.gold < goldCost || city.grain < grainCost)
                    return state to "【敌情】${city.name}钱粮不足，募兵未成。"
                when (val result = ArmySystem.recruitOrReinforce(state, factionId, cityId, commanderId, troopsWanted, "infantry")) {
                    is ArmySystem.ArmyResult.Success -> {
                        val actual = city.troops - (result.newState.cities.firstOrNull { it.id == cityId }?.troops ?: city.troops)
                        val actualGold = ((actual + 999) / 1000) * 100
                        val actualGrain = actual * 2
                        val newCities = result.newState.cities.map {
                            if (it.id == cityId) it.copy(gold = it.gold - actualGold, grain = it.grain - actualGrain) else it
                        }
                        result.newState.copy(cities = newCities) to "【敌情】${city.name}方向：${result.message}"
                    }
                    is ArmySystem.ArmyResult.Failure -> state to "【敌情】募兵未行：${result.reason}"
                }
            }
            "repair_defense" -> {
                val city = state.cities.firstOrNull { it.id == action.targetCityId && it.owner == factionId }
                    ?: return state to "【敌情】修防未行：目标城池不在其治下。"
                if (city.defense >= 100) return state to "【敌情】${city.name}城防已固。"
                val raise = action.amount.coerceIn(1, 100 - city.defense)
                val goldCost = raise * 80
                if (city.gold < goldCost) return state to "【敌情】${city.name}钱粮不足，修防未成。"
                val newCities = state.cities.map {
                    if (it.id == city.id) it.copy(defense = (it.defense + raise).coerceAtMost(100), gold = (it.gold - goldCost).coerceAtLeast(0)) else it
                }
                state.copy(cities = newCities) to "【敌情】${city.name}加固城防+$raise。"
            }
            "assign_commander" -> {
                if (action.armyId.isBlank() || action.officerId.isBlank())
                    return state to "【敌情】任将未行：未指明军团或人选。"
                when (val result = ArmySystem.changeCommander(state, action.armyId, action.officerId)) {
                    is ArmySystem.ArmyResult.Success -> result.newState to "【敌情】${result.message}"
                    is ArmySystem.ArmyResult.Failure -> state to "【敌情】任将未行：${result.reason}"
                }
            }
            else -> state to "【AI军议驳回】未知委任动作：${action.type}"
        }
    }

    /**
     * DELEGATION-001：便宜从事级别的授权，负责人在预算/范围内自主判断该不该做。
     * 只使用负责人本人所在、仍归本方控制的真实城池；既可补既有军团，也可依法
     * 募兵成军、修缮城防、同城任将、真实道路调军和当地补给。
     * 一旬最多一件，所有动作仍先经过正式授权 validator。
     */
    private fun autoExecuteMandates(
        state: GameState,
        playerFactionId: String
    ): Pair<GameState, List<String>> {
        val candidate = state.imperialMandates.asSequence()
            .filter {
                it.isActive && !it.isExpired(state.turn) &&
                    it.autonomyLevel in setOf(MandateAutonomyLevel.BY_THE_BOOK, MandateAutonomyLevel.DISCRETIONARY) &&
                    state.mandateExecutionLog.none { record -> record.turn == state.turn && record.mandateId == it.id }
            }
            .mapNotNull { mandate ->
                val commander = state.officers.firstOrNull { it.id == mandate.responsibleOfficerId }
                    ?: return@mapNotNull null
                val city = state.cities.firstOrNull { it.id == commander.currentCityId && it.owner == playerFactionId }
                    ?: return@mapNotNull null
                if (mandate.regionCityIds.isNotEmpty() && city.id !in mandate.regionCityIds) return@mapNotNull null
                val army = state.armies.firstOrNull {
                    it.commanderId == commander.id && it.ownerFactionId == playerFactionId &&
                        it.currentCityId == city.id && it.statusCode in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY)
                }
                val recruit = if (MandateActionKind.RECRUIT in mandate.allowedActions) {
                    val gap = commander.commandLimit() - (army?.troops ?: 0)
                    val amount = minOf(gap, 3000, (city.troops - 2000).coerceAtLeast(0), city.population)
                    val goldCost = ((amount + 999) / 1000) * 100
                    val grainCost = amount * 2
                    if (amount >= 1000 && mandate.remainingGold() >= goldCost && mandate.remainingGrain() >= grainCost &&
                        state.gold >= goldCost && state.grain >= grainCost) {
                        WorldAction("recruit_troops", playerFactionId, army?.id.orEmpty(), city.id,
                            "便宜从事：依据实际兵源与预算就地募兵", amount, commander.id)
                    } else null
                } else null
                val repair = if (MandateActionKind.REPAIR_DEFENSE in mandate.allowedActions && city.defense < 90) {
                    val raise = minOf(5, 100 - city.defense, mandate.remainingGold() / 80, state.gold / 80)
                    if (raise > 0) WorldAction("repair_defense", playerFactionId, "", city.id,
                        "便宜从事：加固实际驻地城防", raise, commander.id) else null
                } else null
                val assign = if (army == null && MandateActionKind.ASSIGN_COMMANDER in mandate.allowedActions) {
                    val vacant = state.armies.firstOrNull {
                        it.ownerFactionId == playerFactionId && it.currentCityId == city.id &&
                            it.commanderId.isBlank() && it.id !in mandate.prohibitedArmyIds &&
                            it.statusCode in setOf(ArmyStatus.GARRISONED, ArmyStatus.STANDBY)
                    }
                    vacant?.let { WorldAction("assign_commander", playerFactionId, it.id, city.id,
                        "奉旨接掌同城无人统领的军团", 0, commander.id) }
                } else null
                val supply = if (army != null && MandateActionKind.RESUPPLY in mandate.allowedActions && army.supplyLevel < 65) {
                    val required = ((army.troops / 50) * (100 - army.supplyLevel)).coerceAtLeast(1000)
                    if (mandate.remainingGrain() >= required && city.grain >= required) {
                        WorldAction("resupply_army", playerFactionId, army.id, city.id,
                            "奉旨以当地真实粮草补足军需", 0, commander.id)
                    } else null
                } else null
                val reposition = if (army != null && MandateActionKind.REPOSITION_ARMY in mandate.allowedActions &&
                    army.supplyLevel >= 50 && army.id !in mandate.prohibitedArmyIds) {
                    val destination = state.cities.asSequence()
                        .filter { it.owner == playerFactionId && it.id != city.id &&
                            (mandate.regionCityIds.isEmpty() || it.id in mandate.regionCityIds) &&
                            it.id !in mandate.prohibitedCityIds && it.controlState in setOf("FRONTLINE", "CONTESTED") }
                        .mapNotNull { target ->
                            val route = ArmyMovementSystem.findRoute(city.id, target.id, army.armyType)
                                ?: return@mapNotNull null
                            if (route.drop(1).any { node ->
                                    state.cities.firstOrNull { it.id == node }?.owner?.let { it != playerFactionId } == true
                                }) return@mapNotNull null
                            target to route.size
                        }
                        .sortedWith(compareBy<Pair<City, Int>> { if (it.first.controlState == "CONTESTED") 0 else 1 }
                            .thenBy { it.second }.thenBy { it.first.defense })
                        .firstOrNull()?.first
                    destination?.let { WorldAction("move_army", playerFactionId, army.id, it.id,
                        "奉旨沿真实道路增援${it.name}", 0, commander.id) }
                } else null
                val action = when {
                    army == null -> recruit ?: assign ?: repair
                    supply != null -> supply
                    mandate.autonomyLevel == MandateAutonomyLevel.DISCRETIONARY && state.turn % 3 == 0 ->
                        reposition ?: repair ?: recruit
                    mandate.autonomyLevel == MandateAutonomyLevel.DISCRETIONARY && state.turn % 2 == 0 ->
                        repair ?: reposition ?: recruit
                    else -> recruit ?: reposition ?: repair
                }
                action?.let { mandate to it }
            }.firstOrNull() ?: return state to emptyList()

        val (_, action) = candidate
        val approved = DelegatedActionValidator.validate(state, action, playerFactionId)
        if (approved !is DelegatedActionValidator.ValidationResult.Approved) return state to emptyList()
        val (newState, record) = DelegatedActionValidator.execute(state, action, approved.mandate, playerFactionId)
        val label = approved.mandate.autonomyLevel.label
        val message = if (record.success) "【$label】${record.description}"
        else "【${label}未成】${record.description}${if (record.failureReason.isNotBlank()) "：${record.failureReason}" else ""}"
        return newState.copy(mandateExecutionLog = newState.mandateExecutionLog + record) to listOf(message)
    }

    private fun decorateReason(message: String, reason: String): String =
        if (reason.isBlank()) message else "$message（军议：$reason）"

    private fun reasonSuffix(reason: String): String =
        if (reason.isBlank()) "。" else "（$reason）。"
}

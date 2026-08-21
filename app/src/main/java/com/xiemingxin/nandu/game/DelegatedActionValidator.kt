package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.WorldAction

/**
 * DELEGATION-001 核心：校验 + 执行"被授权动作"。
 *
 * 这里不信任模型/AI 提出的任何数值——每一步都重新对照 [ImperialMandate] 和真实
 * [GameState] 校验一遍，通过才调用对应的权威系统（Army/Recruitment/Appointment/
 * Movement/Supply/War）。模型只负责"提出候选"，落地权在这里。
 */
object DelegatedActionValidator {

    sealed class ValidationResult {
        data class Approved(val mandate: ImperialMandate) : ValidationResult()
        data class Rejected(val reason: String) : ValidationResult()
    }

    /**
     * 校验一个针对玩家（宋）军团/城池的候选动作是否落在某道有效圣旨的授权范围内。
     * 不检查预算是否够花（那是执行时的事，先看"能不能做这类事"）。
     */
    fun validate(state: GameState, action: WorldAction, playerFactionId: String): ValidationResult {
        val kind = MandateActionKind.fromActionType(action.type)
            ?: return ValidationResult.Rejected("未知动作类型：${action.type}")

        val responsibleId = resolveResponsibleOfficerId(state, action)
            ?: return ValidationResult.Rejected("找不到负责执行此事的人物。")

        val mandate = ImperialMandateSystem.activeMandateFor(state, responsibleId)
            ?: return ValidationResult.Rejected("此人当前没有有效圣旨授权，皇帝未曾委以此事。")

        if (mandate.autonomyLevel == MandateAutonomyLevel.IMPERIAL_DECREE)
            return ValidationResult.Rejected("圣旨注明「御前亲断」，此事只能由皇帝亲自下令。")

        if (kind !in mandate.allowedActions)
            return ValidationResult.Rejected("圣旨未授权「${kind.label}」一事。")

        if (action.armyId.isNotBlank()) {
            val army = state.armies.firstOrNull { it.id == action.armyId }
            if (army == null) return ValidationResult.Rejected("找不到军团：${action.armyId}")
            if (army.id in mandate.prohibitedArmyIds)
                return ValidationResult.Rejected("圣旨明令不得调动「${army.name}」。")
            if (!ImperialMandateSystem.isArmyCoveredByMandate(state, army, mandate))
                return ValidationResult.Rejected("「${army.name}」不在此人受权统辖的范围内。")
        }

        if (action.targetCityId.isNotBlank() && action.targetCityId in mandate.prohibitedCityIds)
            return ValidationResult.Rejected("圣旨明令不得涉及此城。")

        // 地域限制：涉及城池的动作（募兵/修防/移动目的地），目标城必须落在授权地域内。
        if (mandate.regionCityIds.isNotEmpty() && action.targetCityId.isNotBlank() &&
            kind in setOf(MandateActionKind.RECRUIT, MandateActionKind.REPAIR_DEFENSE, MandateActionKind.REPOSITION_ARMY)
        ) {
            if (action.targetCityId !in mandate.regionCityIds)
                return ValidationResult.Rejected("此事超出圣旨划定的地域范围。")
        }

        return ValidationResult.Approved(mandate)
    }

    /**
     * 真正落地一个已批准的候选动作。调用方必须先拿到 [ValidationResult.Approved]，
     * 这里不重复做授权范围检查，只管预算和权威系统执行——双重防线，但职责分开。
     */
    fun execute(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        playerFactionId: String
    ): Pair<GameState, MandateExecutionRecord> {
        val kind = MandateActionKind.fromActionType(action.type)!!
        val responsibleId = resolveResponsibleOfficerId(state, action) ?: mandate.responsibleOfficerId
        val responsibleName = state.officers.firstOrNull { it.id == responsibleId }?.name ?: "受命之人"

        return when (kind) {
            MandateActionKind.RECRUIT -> executeRecruit(state, action, mandate, playerFactionId, responsibleId, responsibleName)
            MandateActionKind.RESUPPLY -> executeResupply(state, action, mandate, responsibleId, responsibleName)
            MandateActionKind.REPAIR_DEFENSE -> executeRepairDefense(state, action, mandate, playerFactionId, responsibleId, responsibleName)
            MandateActionKind.ASSIGN_COMMANDER -> executeAssignCommander(state, action, mandate, responsibleId, responsibleName)
            MandateActionKind.REPOSITION_ARMY -> executeReposition(state, action, mandate, responsibleId, responsibleName)
            MandateActionKind.INITIATE_BATTLE -> executeInitiateBattle(state, action, mandate, responsibleId, responsibleName)
        }
    }

    private fun resolveResponsibleOfficerId(state: GameState, action: WorldAction): String? {
        if (action.armyId.isNotBlank()) {
            return state.armies.firstOrNull { it.id == action.armyId }?.commanderId
        }
        if (action.officerId.isNotBlank()) return action.officerId
        return null
    }

    private fun spendBudget(mandate: ImperialMandate, gold: Int, grain: Int): ImperialMandate =
        mandate.copy(spentGold = mandate.spentGold + gold, spentGrain = mandate.spentGrain + grain)

    private fun withMandateUpdated(state: GameState, updated: ImperialMandate): GameState =
        state.copy(imperialMandates = state.imperialMandates.map { if (it.id == updated.id) updated else it })

    private fun record(
        state: GameState,
        mandate: ImperialMandate,
        responsibleId: String,
        kind: MandateActionKind,
        description: String,
        success: Boolean,
        failureReason: String = ""
    ): MandateExecutionRecord = MandateExecutionRecord(
        turn = state.turn,
        mandateId = mandate.id,
        responsibleOfficerId = responsibleId,
        actionKind = kind,
        description = description,
        success = success,
        failureReason = failureReason
    )

    private fun executeRecruit(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        playerFactionId: String,
        responsibleId: String,
        responsibleName: String
    ): Pair<GameState, MandateExecutionRecord> {
        val cityId = action.targetCityId.ifBlank {
            state.officers.firstOrNull { it.id == responsibleId }?.currentCityId ?: ""
        }
        val troopsWanted = action.amount.coerceIn(0, 20000)
        // 每千兵约百贯军费 + 相应粮草——跟游戏内既有招募成本量级保持一致，不凭空定价。
        val goldCost = (troopsWanted / 1000) * 100
        val grainCost = troopsWanted * 2

        if (mandate.remainingGold() < goldCost || mandate.remainingGrain() < grainCost) {
            return state to record(
                state, mandate, responsibleId, MandateActionKind.RECRUIT,
                "$responsibleName 请旨募兵未行", false, "圣旨预算已不足以支应此次募兵所需钱粮"
            )
        }

        return when (val result = ArmySystem.recruitOrReinforce(state, playerFactionId, cityId, responsibleId, troopsWanted, "infantry")) {
            is ArmySystem.ArmyResult.Success -> {
                val newMandate = spendBudget(mandate, goldCost, grainCost)
                val newState = withMandateUpdated(result.newState, newMandate)
                    .let { it.copy(gold = (it.gold - goldCost).coerceAtLeast(0), grain = (it.grain - grainCost).coerceAtLeast(0)) }
                newState to record(
                    state, mandate, responsibleId, MandateActionKind.RECRUIT,
                    "$responsibleName 奉旨募兵，耗钱粮约${goldCost}贯、${grainCost}石", true
                )
            }
            is ArmySystem.ArmyResult.Failure -> state to record(
                state, mandate, responsibleId, MandateActionKind.RECRUIT,
                "$responsibleName 请旨募兵未行", false, result.reason
            )
        }
    }

    private fun executeResupply(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        responsibleId: String,
        responsibleName: String
    ): Pair<GameState, MandateExecutionRecord> {
        if (action.armyId.isBlank()) {
            return state to record(state, mandate, responsibleId, MandateActionKind.RESUPPLY, "$responsibleName 调粮未行", false, "未指明补给对象")
        }
        val (newState, message) = ArmySupplySystem.resupplyArmy(state, action.armyId)
        val success = message.startsWith("【补给】")
        return newState to record(
            state, mandate, responsibleId, MandateActionKind.RESUPPLY,
            if (success) "$responsibleName 奉旨调粮：$message" else "$responsibleName 调粮未行",
            success, if (success) "" else message
        )
    }

    private fun executeRepairDefense(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        playerFactionId: String,
        responsibleId: String,
        responsibleName: String
    ): Pair<GameState, MandateExecutionRecord> {
        val cityId = action.targetCityId
        val city = state.cities.firstOrNull { it.id == cityId }
        if (city == null || city.owner != playerFactionId) {
            return state to record(
                state, mandate, responsibleId, MandateActionKind.REPAIR_DEFENSE,
                "$responsibleName 请修城防未行", false, "目标城池不存在或不在治下"
            )
        }
        if (city.defense >= 100) {
            return state to record(
                state, mandate, responsibleId, MandateActionKind.REPAIR_DEFENSE,
                "${city.name}城防已固，无需再修", true
            )
        }
        val raise = action.amount.coerceIn(1, 100 - city.defense)
        val goldCost = raise * 80
        if (mandate.remainingGold() < goldCost || state.gold < goldCost) {
            return state to record(
                state, mandate, responsibleId, MandateActionKind.REPAIR_DEFENSE,
                "$responsibleName 请修${city.name}城防未行", false, "圣旨预算或国库钱粮不足"
            )
        }
        val newCities = state.cities.map { if (it.id == cityId) it.copy(defense = (it.defense + raise).coerceAtMost(100)) else it }
        val newMandate = spendBudget(mandate, goldCost, 0)
        val newState = withMandateUpdated(state, newMandate)
            .copy(cities = newCities, gold = (state.gold - goldCost).coerceAtLeast(0))
        return newState to record(
            state, mandate, responsibleId, MandateActionKind.REPAIR_DEFENSE,
            "$responsibleName 奉旨修缮${city.name}城防+$raise，耗钱${goldCost}贯", true
        )
    }

    private fun executeAssignCommander(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        responsibleId: String,
        responsibleName: String
    ): Pair<GameState, MandateExecutionRecord> {
        if (action.armyId.isBlank() || action.officerId.isBlank()) {
            return state to record(state, mandate, responsibleId, MandateActionKind.ASSIGN_COMMANDER, "$responsibleName 请旨任将未行", false, "未指明军团或人选")
        }
        return when (val result = ArmySystem.changeCommander(state, action.armyId, action.officerId)) {
            is ArmySystem.ArmyResult.Success -> result.newState to record(
                state, mandate, responsibleId, MandateActionKind.ASSIGN_COMMANDER,
                "$responsibleName 奉旨任将：${result.message}", true
            )
            is ArmySystem.ArmyResult.Failure -> state to record(
                state, mandate, responsibleId, MandateActionKind.ASSIGN_COMMANDER,
                "$responsibleName 请旨任将未行", false, result.reason
            )
        }
    }

    private fun executeReposition(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        responsibleId: String,
        responsibleName: String
    ): Pair<GameState, MandateExecutionRecord> {
        if (action.armyId.isBlank() || action.targetCityId.isBlank()) {
            return state to record(state, mandate, responsibleId, MandateActionKind.REPOSITION_ARMY, "$responsibleName 调兵未行", false, "未指明军团或目的地")
        }
        val (newState, message) = ArmyMovementSystem.rerouteArmy(state, action.armyId, action.targetCityId)
        val success = message.startsWith("【改道】")
        return newState to record(
            state, mandate, responsibleId, MandateActionKind.REPOSITION_ARMY,
            "$responsibleName 奉旨调兵：$message", success, if (success) "" else message
        )
    }

    private fun executeInitiateBattle(
        state: GameState,
        action: WorldAction,
        mandate: ImperialMandate,
        responsibleId: String,
        responsibleName: String
    ): Pair<GameState, MandateExecutionRecord> {
        if (action.armyId.isBlank() || action.targetCityId.isBlank()) {
            return state to record(state, mandate, responsibleId, MandateActionKind.INITIATE_BATTLE, "$responsibleName 请战未行", false, "未指明军团或目标")
        }
        return when (val result = WarSystem.executeAttack(state, action.armyId, action.targetCityId)) {
            is WarSystem.WarResult.Success -> result.newState to record(
                state, mandate, responsibleId, MandateActionKind.INITIATE_BATTLE,
                "$responsibleName 奉旨出战：${result.message}", true
            )
            is WarSystem.WarResult.Failure -> state to record(
                state, mandate, responsibleId, MandateActionKind.INITIATE_BATTLE,
                "$responsibleName 请战未行", false, result.reason
            )
        }
    }
}

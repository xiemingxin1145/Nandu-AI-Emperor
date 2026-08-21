package com.xiemingxin.nandu.game

/**
 * DELEGATION-001：皇帝授权制。
 *
 * 核心原则（跟 AGENTS.md 第5条"AI 负责思考，本地规则负责裁决"一致）：
 * 模型/AI 只能在 Imperial Mandate 划定的范围内提出候选动作；候选是否真正落地，
 * 永远由 [DelegatedActionValidator] + 各权威系统（Army/Recruitment/Appointment/
 * Movement/Supply/Building/War）本地校验决定。GameState 是唯一真相源，
 * Mandate 本身不直接修改任何数值。
 */

/** 玩家可以给某个负责人设置的自治程度，决定 AI 在授权范围内还要不要多问。 */
enum class MandateAutonomyLevel(val label: String) {
    /** 御前亲断：这道圣旨只是备案，AI 不会自动执行任何动作，一切等玩家亲自下令。 */
    IMPERIAL_DECREE("御前亲断"),
    /** 奉旨而行：AI 只能执行圣旨里明确列出的动作类型，一步都不能多走。 */
    BY_THE_BOOK("奉旨而行"),
    /** 便宜从事：负责人可以在预算与授权动作范围内自行组合、判断先后顺序。 */
    DISCRETIONARY("便宜从事")
}

/** 圣旨里能明确授权的动作类型；跟 [com.xiemingxin.nandu.ai.WorldAction.ALLOWED_TYPES] 一一对应。 */
enum class MandateActionKind(val actionType: String, val label: String) {
    RECRUIT("recruit_troops", "募兵"),
    RESUPPLY("resupply_army", "补给/调粮"),
    REPAIR_DEFENSE("repair_defense", "修缮城防"),
    ASSIGN_COMMANDER("assign_commander", "任将"),
    REPOSITION_ARMY("move_army", "调动军团"),
    INITIATE_BATTLE("attack_city", "主动交战");

    companion object {
        fun fromActionType(type: String): MandateActionKind? = entries.firstOrNull { it.actionType == type }
    }
}

/**
 * 一道圣旨。示例："命宗泽经营河北，准其自行募义勇补军，但不得擅动江淮守军，
 * 军费以三万贯为限" 对应：responsibleOfficerId=宗泽, regionCityIds=河北诸城,
 * allowedActions={RECRUIT, RESUPPLY, REPAIR_DEFENSE}, allowMoveOtherArmies=false,
 * budgetGold=30000。
 */
data class ImperialMandate(
    val id: String,
    val issuedTurn: Int,
    /** null = 无时限，直到被撤销。 */
    val expiresTurn: Int? = null,
    /** 人类可读的战略目标，会原样显示给玩家，不需要额外翻译。 */
    val goal: String,
    /** 负责人（Officer.id）。AI 执行记录会写"谁"做的，就是这个人。 */
    val responsibleOfficerId: String,
    /** 地域限制：负责人统领的军团、以及要调动/募兵/修防的城池必须落在这个集合里。 */
    val regionCityIds: Set<String>,
    val autonomyLevel: MandateAutonomyLevel,
    val allowedActions: Set<MandateActionKind>,
    /** 预算上限；0 表示这项资源完全不授权支用。 */
    val budgetGold: Int = 0,
    val budgetGrain: Int = 0,
    val spentGold: Int = 0,
    val spentGrain: Int = 0,
    /** 是否允许调动"负责人本人未统领"的其它军团——默认不允许，避免一道圣旨变成全军指挥权。 */
    val allowMoveOtherArmies: Boolean = false,
    /** 禁止事项：armyId 或 cityId 黑名单，命中即拒绝，不论其余条件是否满足。 */
    val prohibitedArmyIds: Set<String> = emptySet(),
    val prohibitedCityIds: Set<String> = emptySet(),
    val isActive: Boolean = true
) {
    fun isExpired(currentTurn: Int): Boolean = expiresTurn != null && currentTurn > expiresTurn

    fun remainingGold(): Int = (budgetGold - spentGold).coerceAtLeast(0)
    fun remainingGrain(): Int = (budgetGrain - spentGrain).coerceAtLeast(0)
}

/** 可追责记录：AI 每执行一个被授权动作，都要在这里留一笔账，皇帝下一旬能看清"谁凭什么做了什么"。 */
data class MandateExecutionRecord(
    val turn: Int,
    val mandateId: String,
    val responsibleOfficerId: String,
    val actionKind: MandateActionKind,
    /** 人类可读描述，例如"宗泽奉旨募兵3000，耗钱粮1200贯"——不含任何内部 id/key。 */
    val description: String,
    val success: Boolean,
    /** 失败时的人类可读原因；成功时留空。 */
    val failureReason: String = ""
)

object ImperialMandateSystem {

    fun issue(state: GameState, mandate: ImperialMandate): GameState =
        state.copy(imperialMandates = state.imperialMandates + mandate)

    fun revoke(state: GameState, mandateId: String): GameState =
        state.copy(imperialMandates = state.imperialMandates.map {
            if (it.id == mandateId) it.copy(isActive = false) else it
        })

    /** 某个负责人当前有效（未撤销、未过期）的授权，取最新下发的一道——同一人同一时刻只认最新圣旨。 */
    fun activeMandateFor(state: GameState, officerId: String): ImperialMandate? =
        state.imperialMandates
            .filter { it.responsibleOfficerId == officerId && it.isActive && !it.isExpired(state.turn) }
            .maxByOrNull { it.issuedTurn }

    /**
     * 这支军团是否落在某道圣旨的授权范围内。
     * 默认只认"军团主帅就是负责人本人"；[ImperialMandate.allowMoveOtherArmies] 打开时，
     * 只要军团当前所在城池落在授权地域内也算数（负责人可以调度辖区内的其它部队）。
     */
    fun isArmyCoveredByMandate(state: GameState, army: Army, mandate: ImperialMandate): Boolean {
        if (army.commanderId == mandate.responsibleOfficerId) return true
        if (!mandate.allowMoveOtherArmies) return false
        if (mandate.regionCityIds.isEmpty()) return false
        return army.currentCityId in mandate.regionCityIds
    }
}

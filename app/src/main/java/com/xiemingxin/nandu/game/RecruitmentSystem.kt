package com.xiemingxin.nandu.game

import kotlin.math.max
import kotlin.random.Random

/**
 * Stage 3 征辟/招募系统
 *
 * 核心原则：
 *  1. 发现（talentLead 已知）≠ 自动加入，需要主动征辟
 *  2. 不同人物有不同招募倾向，影响成功率
 *  3. 招募有成功/拒绝/暂缓三种结果
 *  4. 玩家名望 prestige + 投入费用 + 人物属性 共同决定结果
 */
object RecruitmentSystem {

    sealed class RecruitResult {
        data class Success(val message: String, val newState: GameState) : RecruitResult()
        data class Declined(val message: String) : RecruitResult()          // 明确拒绝
        data class Deferred(val message: String, val newState: GameState) : RecruitResult() // 暂缓，可以再试
        data class NotFound(val reason: String) : RecruitResult()
    }

    /**
     * 征辟人才
     * @param officerId 目标人物ID（必须已在 talentLeads 中，或 status==WANDERING）
     * @param goldOffered 投入招募的金钱
     * @param seed 随机种子
     */
    fun recruit(
        state: GameState,
        officerId: String,
        goldOffered: Int,
        seed: Long
    ): RecruitResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return RecruitResult.NotFound("【征辟失败】朝廷无此人之档案，须先获得人才线索。")

        // 状态检查：只有talentLeads已知，或 WANDERING/SOLDIER 才能征辟
        val isKnown = officerId in state.talentLeads
        val recruitableStatuses = setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
        if (officer.status !in recruitableStatuses) {
            return when (officer.status) {
                OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED ->
                    RecruitResult.NotFound("【征辟失败】${officer.name}已在朝廷任职，无需再行征辟。")
                OfficerStatus.DISMISSED ->
                    RecruitResult.NotFound("【征辟失败】${officer.name}已遭罢黜，需先平反方可重用。")
                OfficerStatus.DECEASED ->
                    RecruitResult.NotFound("【征辟失败】${officer.name}已殁，天不假年。")
                else -> RecruitResult.NotFound("【征辟失败】${officer.name}当前状态无法征辟。")
            }
        }
        if (!isKnown && officer.status == OfficerStatus.HIDDEN) {
            return RecruitResult.NotFound(
                "【征辟失败】此人尚未被发现，宫中无其踪迹。须先派人寻访、获得线索，方可遣使征辟。"
            )
        }

        if (state.gold < goldOffered) {
            return RecruitResult.NotFound("【征辟失败】国库不足，无法投入${goldOffered}贯延揽人才。")
        }

        val rng = Random(seed)

        // 计算基础成功率
        val baseRate = computeSuccessRate(officer, state.prestige, goldOffered)
        val roll = rng.nextInt(100)

        val newState = state.copy(gold = state.gold - goldOffered)

        return when {
            roll < baseRate -> {
                // 成功入朝
                val newOfficers = newState.officers.map {
                    if (it.id == officerId) it.copy(
                        status = OfficerStatus.IN_COURT,
                        currentCityId = "linan" // 征辟后先到临安候命
                    ) else it
                }
                val newLeads = newState.talentLeads - officerId
                RecruitResult.Success(
                    buildSuccessMsg(officer, goldOffered),
                    newState.copy(
                        officers = newOfficers,
                        talentLeads = newLeads
                    )
                )
            }
            roll < baseRate + 20 -> {
                // 暂缓：表达不情愿但未完全拒绝，可以再试
                RecruitResult.Deferred(
                    buildDeferredMsg(officer),
                    newState
                )
            }
            else -> {
                // 明确拒绝（此次征辟失败，talentLead仍在，可重试）
                RecruitResult.Declined(buildDeclinedMsg(officer))
            }
        }
    }

    /**
     * 计算征辟成功率（0-100）
     *
     * 影响因子：
     *  - 玩家名望 prestige（高名望更容易吸引人才）
     *  - 投入金钱（对野心型有效）
     *  - 人物忠诚（忠义者更看重抗金大义）
     *  - 人物野心（野心型要看官职前景）
     *  - 人物出身
     *  - 人物状态（WANDERING比HIDDEN更容易）
     */
    private fun computeSuccessRate(
        officer: Officer,
        prestige: Int,
        goldOffered: Int
    ): Int {
        var rate = 30  // 基础

        // 名望加成
        rate += prestige / 5   // 最多+20

        // 金钱加成（金钱对野心型更有效）
        val goldBonus = when {
            goldOffered >= 10000 -> if (officer.ambition > 60) 20 else 12
            goldOffered >= 5000  -> if (officer.ambition > 60) 14 else 8
            goldOffered >= 2000  -> if (officer.ambition > 60) 8  else 4
            else                 -> 2
        }
        rate += goldBonus

        // 忠诚加成：忠义高的人更愿意为朝廷效力（尤其抗金局势下）
        rate += officer.loyalty / 10  // 最多+10

        // 野心加成：野心高的人渴望出仕，更好招
        rate += officer.ambition / 12  // 最多+8

        // 状态加成
        if (officer.status == OfficerStatus.WANDERING) rate += 10
        if (officer.status == OfficerStatus.SOLDIER) rate += 5

        // 出身限制：士族更看重名望，寒门更看重机会
        when (officer.origin) {
            "士族" -> rate -= if (prestige < 40) 10 else 0
            "寒门" -> rate += 5
            "归正人" -> rate += 8  // 归正人（从金方归来）更迫切投效
        }

        return rate.coerceIn(10, 88)  // 最低10%，最高88%，避免必然成功
    }

    private fun buildSuccessMsg(officer: Officer, gold: Int): String {
        val phrase = when {
            officer.loyalty >= 85 -> "感朝廷忠义，慨然应命"
            officer.ambition >= 70 -> "见朝廷诚意，欣然赴召"
            officer.status == OfficerStatus.WANDERING -> "流落已久，得此延揽，如鱼得水"
            officer.origin == "归正人" -> "感朝廷不弃，痛哭拜谢"
            else -> "奉旨入朝，愿效犬马之劳"
        }
        return "【征辟成功】${officer.name}${phrase}，耗资${gold}贯，已录入御前名册，候旨任命。"
    }

    private fun buildDeferredMsg(officer: Officer): String {
        val phrase = when {
            officer.loyalty >= 70 -> "言称尚有家事未了，婉拒急来，然言语间犹豫"
            officer.ambition >= 65 -> "询问官职前景，未得满意答复，暂不首肯"
            officer.origin == "士族" -> "以名望不足为由婉谢，或可再加礼遇"
            else -> "称需考量，暂未首肯，或可来日再试"
        }
        return "【征辟暂缓】${officer.name}${phrase}。此次花费已出，可来日再行征辟。"
    }

    private fun buildDeclinedMsg(officer: Officer): String {
        val phrase = when {
            officer.loyalty < 40 -> "明言不愿仕此，拒绝入朝"
            officer.status == OfficerStatus.HIDDEN -> "踪迹隐秘，使者未能见其本人"
            officer.origin == "士族" -> "以朝廷名望不足为由，婉言谢绝"
            else -> "诸般理由推辞，此番征辟未成"
        }
        return "【征辟拒绝】${officer.name}${phrase}，花费已出，人才线索仍在，可改日再试。"
    }

    /**
     * 快速探察某城是否可能有未发现人才（给UI提示用）
     */
    fun cityHasHiddenTalent(state: GameState, cityId: String): Boolean {
        return state.officers.any {
            it.currentCityId == cityId &&
            it.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.WANDERING) &&
            it.id !in state.talentLeads
        }
    }

    /**
     * 生成城池中已知但未征辟的人才线索列表（用于城池详情UI）
     */
    fun knownTalentsInCity(state: GameState, cityId: String): List<Officer> {
        return state.officers.filter {
            it.currentCityId == cityId &&
            it.id in state.talentLeads &&
            it.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
        }
    }

    /**
     * 人才探察概况（不泄露隐藏人物详情，只给模糊提示）
     */
    fun hiddenTalentHint(state: GameState, cityId: String): String? {
        val hidden = state.officers.filter {
            it.currentCityId == cityId &&
            it.status == OfficerStatus.HIDDEN &&
            it.id !in state.talentLeads
        }
        if (hidden.isEmpty()) return null
        return when (hidden.size) {
            1 -> "此地或有未发现之才"
            2 -> "此地似有数位可造之材，深藏未露"
            else -> "此地人杰地灵，多有俊彦潜伏，可多方寻访"
        }
    }
}

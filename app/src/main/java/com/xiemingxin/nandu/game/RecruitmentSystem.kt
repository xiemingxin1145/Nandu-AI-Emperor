package com.xiemingxin.nandu.game

import kotlin.random.Random

/**
 * Stage 3 征辟/延揽系统。
 *
 * 核心原则：
 *  1. 发现（talentLead 已知）≠ 自动加入，需要主动延揽。
 *  2. 不同人物有不同倾向，影响成功率。
 *  3. 成功延揽也不等于瞬移进朝堂：人在外地时必须先赴当前行在。
 *  4. 当前行在统一由 CharacterStateSource.CAPITAL_CITY_ID 决定，禁止硬编码临安。
 */
object RecruitmentSystem {

    sealed class RecruitResult {
        data class Success(val message: String, val newState: GameState) : RecruitResult()
        data class Declined(val message: String) : RecruitResult()
        data class Deferred(val message: String, val newState: GameState) : RecruitResult()
        data class NotFound(val reason: String) : RecruitResult()
    }

    fun recruit(
        state: GameState,
        officerId: String,
        goldOffered: Int,
        seed: Long
    ): RecruitResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return RecruitResult.NotFound("【延揽失败】朝廷无此人之档案，须先获得人才线索。")

        val isKnown = officerId in state.talentLeads
        val recruitableStatuses = setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
        if (officer.status !in recruitableStatuses) {
            return when (officer.status) {
                OfficerStatus.IN_COURT, OfficerStatus.IN_CAPITAL, OfficerStatus.DEPLOYED ->
                    RecruitResult.NotFound("【延揽失败】${officer.name}已在宋廷任职体系内，无需再行征辟。")
                OfficerStatus.CAPTIVE ->
                    RecruitResult.NotFound("【延揽失败】${officer.name}身陷敌境，当前无法奉诏入宋。")
                OfficerStatus.NOT_YET_RELEVANT ->
                    RecruitResult.NotFound("【延揽失败】此人尚未进入本局时代视野，不可因后世声名提前点将。")
                OfficerStatus.DISMISSED ->
                    RecruitResult.NotFound("【延揽失败】${officer.name}已遭罢黜，需先平反方可重用。")
                OfficerStatus.DECEASED ->
                    RecruitResult.NotFound("【延揽失败】${officer.name}已殁，天不假年。")
                else -> RecruitResult.NotFound("【延揽失败】${officer.name}当前状态无法征辟。")
            }
        }
        if (!isKnown && officer.status == OfficerStatus.HIDDEN) {
            return RecruitResult.NotFound(
                "【延揽失败】此人尚未被发现，宫中无其踪迹。须先派人寻访、获得线索，方可遣使征辟。"
            )
        }

        if (state.gold < goldOffered) {
            return RecruitResult.NotFound("【延揽失败】国库不足，无法投入${goldOffered}贯延揽人才。")
        }

        val rng = Random(seed)
        val baseRate = computeSuccessRate(officer, state.prestige, goldOffered)
        val roll = rng.nextInt(100)
        val paidState = state.copy(gold = state.gold - goldOffered)

        return when {
            roll < baseRate -> {
                val capitalId = CharacterStateSource.CAPITAL_CITY_ID
                val capitalName = paidState.cities.find { it.id == capitalId }?.name ?: "行在"
                val alreadyThere = officer.currentCityId == capitalId
                val travelTurns = if (alreadyThere) 0 else AppointmentSystem.estimateTravelTurns(
                    paidState, officer.currentCityId, capitalId
                )
                val newOfficers = paidState.officers.map {
                    if (it.id != officerId) it
                    else if (alreadyThere) {
                        it.copy(
                            status = OfficerStatus.IN_COURT,
                            currentCityId = capitalId,
                            travelDestinationCityId = null,
                            travelArrivalTurn = null
                        )
                    } else {
                        // 已应召，但肉身仍在原地；visibilityFor 会把这种 IN_COURT+在途状态视作 SEEN，
                        // CharacterStateSource.isAtCourt 也会阻止其提前出现在朝堂。
                        it.copy(
                            status = OfficerStatus.IN_COURT,
                            travelDestinationCityId = capitalId,
                            travelArrivalTurn = paidState.turn + travelTurns
                        )
                    }
                }
                val newLeads = paidState.talentLeads - officerId
                val travelNote = if (alreadyThere) {
                    "已在${capitalName}候旨。"
                } else {
                    "已奉召启程赴$capitalName，约${travelTurns}旬后抵达；途中不得列班奏对。"
                }
                RecruitResult.Success(
                    buildSuccessMsg(officer, goldOffered) + travelNote,
                    paidState.copy(officers = newOfficers, talentLeads = newLeads)
                )
            }
            roll < baseRate + 20 -> RecruitResult.Deferred(buildDeferredMsg(officer), paidState)
            else -> RecruitResult.Declined(buildDeclinedMsg(officer))
        }
    }

    private fun computeSuccessRate(officer: Officer, prestige: Int, goldOffered: Int): Int {
        var rate = 30
        rate += prestige / 5
        rate += when {
            goldOffered >= 10000 -> if (officer.ambition > 60) 20 else 12
            goldOffered >= 5000 -> if (officer.ambition > 60) 14 else 8
            goldOffered >= 2000 -> if (officer.ambition > 60) 8 else 4
            else -> 2
        }
        rate += officer.loyalty / 10
        rate += officer.ambition / 12
        if (officer.status == OfficerStatus.WANDERING) rate += 10
        if (officer.status == OfficerStatus.SOLDIER) rate += 5
        when (officer.origin) {
            "士族" -> rate -= if (prestige < 40) 10 else 0
            "寒门" -> rate += 5
            "归正人" -> rate += 8
        }
        return rate.coerceIn(10, 88)
    }

    private fun buildSuccessMsg(officer: Officer, gold: Int): String {
        val verb = if (officer.id == "yue_fei") "擢用" else "延揽"
        val phrase = when {
            officer.loyalty >= 85 -> "感朝廷忠义，慨然应命"
            officer.ambition >= 70 -> "见朝廷诚意，欣然赴召"
            officer.status == OfficerStatus.WANDERING -> "流落已久，得此延揽，如鱼得水"
            officer.origin == "归正人" -> "感朝廷不弃，痛哭拜谢"
            else -> "奉旨入朝，愿效犬马之劳"
        }
        return "【${verb}成功】${officer.name}${phrase}，耗资${gold}贯。"
    }

    private fun buildDeferredMsg(officer: Officer): String {
        val phrase = when {
            officer.loyalty >= 70 -> "言称尚有家事未了，婉拒急来，然言语间犹豫"
            officer.ambition >= 65 -> "询问官职前景，未得满意答复，暂不首肯"
            officer.origin == "士族" -> "以名望不足为由婉谢，或可再加礼遇"
            else -> "称需考量，暂未首肯，或可来日再试"
        }
        return "【延揽暂缓】${officer.name}${phrase}。此次花费已出，可来日再行征辟。"
    }

    private fun buildDeclinedMsg(officer: Officer): String {
        val phrase = when {
            officer.loyalty < 40 -> "明言不愿仕此，拒绝入朝"
            officer.status == OfficerStatus.HIDDEN -> "踪迹隐秘，使者未能见其本人"
            officer.origin == "士族" -> "以朝廷名望不足为由，婉言谢绝"
            else -> "诸般理由推辞，此番征辟未成"
        }
        return "【延揽拒绝】${officer.name}${phrase}，花费已出，人才线索仍在，可改日再试。"
    }

    fun cityHasHiddenTalent(state: GameState, cityId: String): Boolean = state.officers.any {
        it.currentCityId == cityId &&
            it.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.WANDERING) &&
            it.id !in state.talentLeads
    }

    fun knownTalentsInCity(state: GameState, cityId: String): List<Officer> = state.officers.filter {
        it.currentCityId == cityId &&
            it.id in state.talentLeads &&
            it.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING)
    }

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

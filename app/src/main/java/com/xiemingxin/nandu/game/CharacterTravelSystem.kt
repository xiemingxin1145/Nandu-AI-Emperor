package com.xiemingxin.nandu.game

/**
 * V1.0 人物赶路推进系统（活朝堂 / living-world-court-v1）。
 *
 * 目前只服务一种真实移动场景：AppointmentSystem.recallToCourt 把外任/领军人物
 * 召回入朝时，不会瞬间把人摆进垂拱殿，而是先记录 travelDestinationCityId /
 * travelArrivalTurn。这里在每旬推进（EmperorViewModel.advanceTurn）时检查
 * 是否已经抵达——真正抵达那一旬，才把人物切换为 IN_COURT 并挪到目的地城市。
 *
 * 调用时机：必须在 state.turn 已经+1（也就是"新的一旬"）之后再调用，
 * 这样 travelArrivalTurn 记录的就是"抵达时应处于的旬数"，语义清晰。
 */
object CharacterTravelSystem {

    fun tickArrivals(state: GameState): Pair<GameState, List<String>> {
        val reports = mutableListOf<String>()
        val newOfficers = state.officers.map { officer ->
            val dest = officer.travelDestinationCityId
            val arrival = officer.travelArrivalTurn
            if (dest != null && arrival != null && state.turn >= arrival) {
                val destName = state.cities.find { it.id == dest }?.name ?: dest
                reports += "【回京】${officer.name}已奉诏抵达$destName，即日可入朝奏对。"
                officer.copy(
                    currentCityId = dest,
                    status = OfficerStatus.IN_COURT,
                    travelDestinationCityId = null,
                    travelArrivalTurn = null
                )
            } else officer
        }
        if (reports.isEmpty()) return state to emptyList()
        return state.copy(officers = newOfficers) to reports
    }

    /**
     * V1.1 历史 Canon：处理"人物当前在场，但已知会在某旬后转为另一状态/地点"的预定迁移。
     * 典型场景：宗泽开局当天入对在朝，随后按史实外任、转东京留守——不经历"途中不可见"的过程，
     * 到点直接切换，转场前一直正常可见/可参与朝会。
     */
    fun tickScheduledTransitions(state: GameState): Pair<GameState, List<String>> {
        val reports = mutableListOf<String>()
        val newOfficers = state.officers.map { officer ->
            val turn = officer.scheduledTurn
            val newStatus = officer.scheduledStatus
            if (turn != null && newStatus != null && state.turn >= turn) {
                val newCityId = officer.scheduledCityId ?: officer.currentCityId
                val cityName = state.cities.find { it.id == newCityId }?.name ?: newCityId
                val statusLabel = when (newStatus) {
                    OfficerStatus.DEPLOYED -> "外任$cityName"
                    OfficerStatus.IN_COURT -> "入朝"
                    else -> "转任"
                }
                reports += "【任命】${officer.name}$statusLabel。"
                officer.copy(
                    status = newStatus,
                    currentCityId = newCityId,
                    scheduledStatus = null,
                    scheduledCityId = null,
                    scheduledTurn = null
                )
            } else officer
        }
        if (reports.isEmpty()) return state to emptyList()
        return state.copy(officers = newOfficers) to reports
    }
}

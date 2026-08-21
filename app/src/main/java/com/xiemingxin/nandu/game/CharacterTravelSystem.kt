package com.xiemingxin.nandu.game

/**
 * V1.0 人物赶路推进系统（活朝堂 / living-world-court-v1）。
 *
 * 召回入朝与 WORLD-CORE-001 派任外地统一走真实 travel 字段：
 * 抵达前人物不能肉身出现在不该出现的宫殿；抵达那一旬才真正改变位置/身份/任职。
 */
object CharacterTravelSystem {

    fun tickArrivals(state: GameState): Pair<GameState, List<String>> {
        val reports = mutableListOf<String>()
        var cityGovernors = state.cityGovernors
        var cityGarrisons = state.cityGarrisons
        val newOfficers = state.officers.map { officer ->
            val dest = officer.travelDestinationCityId
            val arrival = officer.travelArrivalTurn
            if (dest != null && arrival != null && state.turn >= arrival) {
                val destName = state.cities.find { it.id == dest }?.name ?: dest
                val arrivalStatus = officer.travelArrivalStatus ?: OfficerStatus.IN_COURT
                val travelPost = OfficerDispatchSystem.decodeTravelPost(officer.travelArrivalPostTitle)

                // 抵达后才真正占据新任席位。先清残留，避免同一人物同时挂两城/两职。
                if (travelPost.title.isNotBlank() && arrivalStatus == OfficerStatus.DEPLOYED) {
                    cityGovernors = cityGovernors.filterValues { it != officer.id }
                    cityGarrisons = cityGarrisons.filterValues { it != officer.id }
                    when (travelPost.garrisonPost) {
                        false -> cityGovernors = cityGovernors + (dest to officer.id)
                        true -> cityGarrisons = cityGarrisons + (dest to officer.id)
                        null -> Unit
                    }
                }

                reports += if (arrivalStatus == OfficerStatus.IN_COURT) {
                    "【回京】${officer.name}已奉诏抵达$destName，即日可入朝奏对。"
                } else {
                    val roleText = if (travelPost.title.isNotBlank()) "，就任${travelPost.title}" else ""
                    "【履职】${officer.name}已抵达$destName$roleText，自此常驻，不再肉身入朝，唯以奏札上闻。"
                }
                officer.copy(
                    currentCityId = dest,
                    status = arrivalStatus,
                    travelDestinationCityId = null,
                    travelArrivalTurn = null,
                    travelArrivalStatus = null,
                    travelArrivalPostTitle = ""
                )
            } else officer
        }
        if (reports.isEmpty()) return state to emptyList()
        return state.copy(officers = newOfficers, cityGovernors = cityGovernors, cityGarrisons = cityGarrisons) to reports
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

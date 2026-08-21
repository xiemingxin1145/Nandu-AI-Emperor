package com.xiemingxin.nandu.game

/**
 * WORLD-CORE-001：人物调度器。
 *
 * "命宗泽赴东京任留守"不该只是一张文本卡——这里统一处理"派某人去某地任某职"，
 * 产生一条真实的、跨旬的世界任务：接旨→离任启程→在途（从朝堂/原驻地消失）
 * →抵达→履职生效。不需要任何 AI 调用，纯本地规则驱动。
 */
object OfficerDispatchSystem {

    sealed class DispatchResult {
        data class Success(val message: String, val newState: GameState) : DispatchResult()
        data class Failure(val reason: String) : DispatchResult()
    }

    /**
     * travelArrivalPostTitle 已经进入存档格式。为了不再扩一次存档 schema，
     * 文职主官仅在内部存储时加 GOVERNOR 前缀；武职守将继续保存原始标题，
     * 从而保持 #72 初版存档和现有测试完全向后兼容。对外展示时统一解码。
     */
    internal data class TravelPost(val title: String, val garrisonPost: Boolean?)

    private const val GOVERNOR_PREFIX = "__GOVERNOR__::"

    internal fun encodeTravelPost(postTitle: String, garrisonPost: Boolean): String {
        if (postTitle.isBlank()) return ""
        return if (garrisonPost) postTitle else GOVERNOR_PREFIX + postTitle
    }

    internal fun decodeTravelPost(raw: String): TravelPost = when {
        raw.isBlank() -> TravelPost("", null)
        raw.startsWith(GOVERNOR_PREFIX) -> TravelPost(raw.removePrefix(GOVERNOR_PREFIX), false)
        // 旧存档和武职沿用纯标题：按守将处理。
        else -> TravelPost(raw, true)
    }

    /**
     * @param arrivalStatus 抵达后的状态。DEPLOYED=外任/镇守一方，IN_CAPITAL=在京任军职。
     * @param postTitle 人类可读职务名（如"东京留守"）。
     * @param garrisonPost true=驻城守将（cityGarrisons），false=民政主官（cityGovernors）。
     */
    fun dispatch(
        state: GameState,
        officerId: String,
        targetCityId: String,
        arrivalStatus: OfficerStatus,
        postTitle: String = "",
        garrisonPost: Boolean = true
    ): DispatchResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return DispatchResult.Failure("找不到此人物。")
        if (!CharacterStateSource.isAlive(officer))
            return DispatchResult.Failure("${officer.name}已不在人世。")
        if (officer.status in setOf(
                OfficerStatus.HIDDEN, OfficerStatus.WANDERING, OfficerStatus.SOLDIER,
                OfficerStatus.CAPTIVE, OfficerStatus.NOT_YET_RELEVANT
            )
        ) return DispatchResult.Failure("${officer.name}尚未入朝籍，无法委以此任。")
        if (CharacterStateSource.isTraveling(officer))
            return DispatchResult.Failure("${officer.name}正在赶路途中，须先抵达方可再派新命。")

        val targetCity = state.cities.find { it.id == targetCityId }
            ?: return DispatchResult.Failure("目的地城池不存在：$targetCityId")
        if (targetCity.owner != "song")
            return DispatchResult.Failure("${targetCity.name}已非我方治下，不得委任。")

        // 一人不能同时在旧城和新城占两个正式席位。旨意生效、本人离任启程时，旧任即腾缺。
        val clearedGovernors = state.cityGovernors.filterValues { it != officerId }
        val clearedGarrisons = state.cityGarrisons.filterValues { it != officerId }

        if (officer.currentCityId == targetCityId) {
            // 本来就在当地：不需要赶路，即刻履职；同时清掉此人可能残留的旧任记录。
            val newOfficers = state.officers.map { if (it.id == officerId) it.copy(status = arrivalStatus) else it }
            val newGarrisons = if (postTitle.isNotBlank() && garrisonPost) {
                clearedGarrisons + (targetCityId to officerId)
            } else clearedGarrisons
            val newGovernors = if (postTitle.isNotBlank() && !garrisonPost) {
                clearedGovernors + (targetCityId to officerId)
            } else clearedGovernors
            val roleText = if (postTitle.isNotBlank()) "，就任$postTitle" else ""
            return DispatchResult.Success(
                "【任命】${officer.name}本在${targetCity.name}，即日履新$roleText。",
                state.copy(officers = newOfficers, cityGarrisons = newGarrisons, cityGovernors = newGovernors)
            )
        }

        val travelTurns = AppointmentSystem.estimateTravelTurns(state, officer.currentCityId, targetCityId)
        val arrivalTurn = state.turn + travelTurns
        val encodedPost = if (arrivalStatus == OfficerStatus.DEPLOYED) {
            encodeTravelPost(postTitle, garrisonPost)
        } else ""
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                travelDestinationCityId = targetCityId,
                travelArrivalTurn = arrivalTurn,
                travelArrivalStatus = arrivalStatus,
                travelArrivalPostTitle = encodedPost
            ) else it
        }
        val roleText = if (postTitle.isNotBlank()) "，任$postTitle" else ""
        return DispatchResult.Success(
            "【诏命】命${officer.name}赴${targetCity.name}$roleText，路程约${travelTurns}旬。" +
                "旨意已发，本人即刻离任启程；途中不再肉身参加朝议，抵达后方才实际履职。",
            state.copy(
                officers = newOfficers,
                cityGovernors = clearedGovernors,
                cityGarrisons = clearedGarrisons
            )
        )
    }
}

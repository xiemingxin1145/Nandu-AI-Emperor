package com.xiemingxin.nandu.game

/**
 * Stage 3 城池任职体系
 *
 * 区分两种概念：
 *  - 人在城里（Officer.currentCityId）：物理位置
 *  - 正式担任职务（AppointmentSystem 记录）：制度身份
 *
 * 初期只支持两个职务：
 *  - 城池主官 / 太守（governor）
 *  - 驻城守将（garrison）
 *
 * 数据保存在 GameState.cityGovernors / GameState.cityGarrisons
 * 两个 Map<cityId, officerId>，存档走 GameSaveCodec V4。
 */
object AppointmentSystem {

    /** 主官任命结果 */
    sealed class AppointResult {
        data class Success(val message: String, val newState: GameState) : AppointResult()
        data class Failure(val reason: String) : AppointResult()
    }

    /**
     * 任命城池主官（太守）
     * 约束：
     *  - 人物必须 IN_COURT 或 DEPLOYED
     *  - 城池必须属于宋方
     *  - 同一人物同一时刻只能担任一城主官（先自动免去旧职）
     */
    fun appointGovernor(
        state: GameState,
        officerId: String,
        cityId: String
    ): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【任命失败】找不到此人：$officerId")
        val city = state.cities.find { it.id == cityId }
            ?: return AppointResult.Failure("【任命失败】找不到城池：$cityId")
        if (city.owner != "song")
            return AppointResult.Failure("【任命失败】${city.name}不在宋廷控制之下，无法任命主官。")
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【任命失败】${officer.name}尚未入朝，不可直接任命。须先延揽入御前名册。")

        // 清除该人物原有的主官职务
        val clearedGovernors = state.cityGovernors.filterValues { it != officerId }
        // 设置新职务
        val newGovernors = clearedGovernors + (cityId to officerId)

        // 将人物迁往该城，状态设为 DEPLOYED
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                currentCityId = cityId,
                status = OfficerStatus.DEPLOYED
            ) else it
        }

        val newState = state.copy(
            cityGovernors = newGovernors,
            officers = newOfficers
        )
        return AppointResult.Success(
            "【任命】${officer.name}出任${city.name}主官，即赴任所，总领政务。",
            newState
        )
    }

    /**
     * 任命驻城守将
     * 约束类似主官，但偏重武将（command > politics）
     */
    fun appointGarrison(
        state: GameState,
        officerId: String,
        cityId: String
    ): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【任命失败】找不到此人：$officerId")
        val city = state.cities.find { it.id == cityId }
            ?: return AppointResult.Failure("【任命失败】找不到城池：$cityId")
        if (city.owner != "song")
            return AppointResult.Failure("【任命失败】${city.name}不在宋廷控制之下。")
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【任命失败】${officer.name}尚未入朝登册，无法直接委任守将之职。")

        val clearedGarrisons = state.cityGarrisons.filterValues { it != officerId }
        val newGarrisons = clearedGarrisons + (cityId to officerId)
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                currentCityId = cityId,
                status = OfficerStatus.DEPLOYED
            ) else it
        }
        val newState = state.copy(
            cityGarrisons = newGarrisons,
            officers = newOfficers
        )
        return AppointResult.Success(
            "【任命】${officer.name}奉命镇守${city.name}，领兵驻防，守卫边疆。",
            newState
        )
    }

    /**
     * 解除职务（免职）
     * 人物状态回到 IN_COURT（仍在朝廷名册），城池该职位清空。
     */
    fun dismissOfficer(
        state: GameState,
        officerId: String
    ): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【免职失败】找不到此人。")
        if (officer.status == OfficerStatus.DISMISSED)
            return AppointResult.Failure("【免职失败】${officer.name}已处于罢黜状态。")

        val newGovernors = state.cityGovernors.filterValues { it != officerId }
        val newGarrisons = state.cityGarrisons.filterValues { it != officerId }
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(status = OfficerStatus.DISMISSED) else it
        }
        return AppointResult.Success(
            "【免职】${officer.name}奉旨解除职务，候朝廷另行安排。",
            state.copy(
                cityGovernors = newGovernors,
                cityGarrisons = newGarrisons,
                officers = newOfficers
            )
        )
    }

    /**
     * 调任：将人物从当前城迁往新城（同时更新职务）
     * 若原来有守将职务，守将跟着迁；若无，只迁人。
     */
    fun transferOfficer(
        state: GameState,
        officerId: String,
        targetCityId: String
    ): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【调任失败】找不到此人。")
        val targetCity = state.cities.find { it.id == targetCityId }
            ?: return AppointResult.Failure("【调任失败】找不到目标城池：$targetCityId")
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【调任失败】${officer.name}当前状态不允许调任。")

        val isGarrison = state.cityGarrisons.values.contains(officerId)
        val isGovernor = state.cityGovernors.values.contains(officerId)

        val newGovernors = if (isGovernor) {
            val cleared = state.cityGovernors.filterValues { it != officerId }
            cleared + (targetCityId to officerId)
        } else state.cityGovernors

        val newGarrisons = if (isGarrison) {
            val cleared = state.cityGarrisons.filterValues { it != officerId }
            cleared + (targetCityId to officerId)
        } else state.cityGarrisons

        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(currentCityId = targetCityId, status = OfficerStatus.DEPLOYED) else it
        }
        val roleDesc = when {
            isGovernor -> "主官"
            isGarrison -> "守将"
            else -> "职"
        }
        return AppointResult.Success(
            "【调任】${officer.name}由旧任奉旨移驻${targetCity.name}，继续担任$roleDesc。",
            state.copy(
                cityGovernors = newGovernors,
                cityGarrisons = newGarrisons,
                officers = newOfficers
            )
        )
    }

    /** 获取某城的主官人物（如有） */
    fun getGovernor(state: GameState, cityId: String): Officer? {
        val govId = state.cityGovernors[cityId] ?: return null
        return state.officers.find { it.id == govId }
    }

    /** 获取某城的守将人物（如有） */
    fun getGarrison(state: GameState, cityId: String): Officer? {
        val garId = state.cityGarrisons[cityId] ?: return null
        return state.officers.find { it.id == garId }
    }

    /** 某人物当前担任的职务描述（用于UI显示） */
    fun currentRole(state: GameState, officerId: String): String {
        val asGov = state.cityGovernors.entries.find { it.value == officerId }
        val asGar = state.cityGarrisons.entries.find { it.value == officerId }
        val cityName: (String) -> String = { id ->
            state.cities.find { it.id == id }?.name ?: id
        }
        return when {
            asGov != null && asGar != null -> "${cityName(asGov.key)}主官 / ${cityName(asGar.key)}守将"
            asGov != null -> "${cityName(asGov.key)} 主官"
            asGar != null -> "${cityName(asGar.key)} 守将"
            else -> when (state.officers.find { it.id == officerId }?.status) {
                OfficerStatus.IN_COURT -> "御前待命"
                OfficerStatus.DISMISSED -> "罢黜"
                OfficerStatus.DECEASED -> "已故"
                else -> "无职"
            }
        }
    }

    /**
     * 忠诚度风险检查（供UI显示警告，不强制阻止任命）
     */
    fun loyaltyRiskLabel(officer: Officer): String? = when {
        officer.loyalty < 30 && officer.ambition > 65 ->
            "⚠ 此人忠诚极低、野心甚重，委以重职风险极大"
        officer.loyalty < 45 ->
            "⚠ 此人忠诚不高，建议慎重委派边疆要职"
        else -> null
    }
}

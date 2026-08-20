package com.xiemingxin.nandu.game

import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Stage 3 城池任职体系。
 *
 * 区分两种概念：
 *  - 人在城里（Officer.currentCityId）：物理位置
 *  - 正式担任职务（AppointmentSystem 记录）：制度身份
 *
 * 建炎元年开局的“京城/行在”由 CharacterStateSource.CAPITAL_CITY_ID 统一决定，
 * 禁止任何召回逻辑自行写死“临安”。
 */
object AppointmentSystem {

    sealed class AppointResult {
        data class Success(val message: String, val newState: GameState) : AppointResult()
        data class Failure(val reason: String) : AppointResult()
    }

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
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.IN_CAPITAL, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【任命失败】${officer.name}尚未进入宋廷任官体系，不可直接委任。")

        val clearedGovernors = state.cityGovernors.filterValues { it != officerId }
        val newGovernors = clearedGovernors + (cityId to officerId)
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                currentCityId = cityId,
                status = OfficerStatus.DEPLOYED,
                travelDestinationCityId = null,
                travelArrivalTurn = null
            ) else it
        }

        return AppointResult.Success(
            "【任命】${officer.name}出任${city.name}主官，离开行在赴任，总领政务。",
            state.copy(cityGovernors = newGovernors, officers = newOfficers)
        )
    }

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
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.IN_CAPITAL, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【任命失败】${officer.name}尚未进入宋廷任官体系，无法委任守将之职。")

        val clearedGarrisons = state.cityGarrisons.filterValues { it != officerId }
        val newGarrisons = clearedGarrisons + (cityId to officerId)
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                currentCityId = cityId,
                status = OfficerStatus.DEPLOYED,
                travelDestinationCityId = null,
                travelArrivalTurn = null
            ) else it
        }
        return AppointResult.Success(
            "【任命】${officer.name}奉命镇守${city.name}，离开行在领兵驻防。",
            state.copy(cityGarrisons = newGarrisons, officers = newOfficers)
        )
    }

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

    fun transferOfficer(
        state: GameState,
        officerId: String,
        targetCityId: String
    ): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【调任失败】找不到此人。")
        val targetCity = state.cities.find { it.id == targetCityId }
            ?: return AppointResult.Failure("【调任失败】找不到目标城池：$targetCityId")
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.IN_CAPITAL, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【调任失败】${officer.name}当前状态不允许调任。")

        val isGarrison = state.cityGarrisons.values.contains(officerId)
        val isGovernor = state.cityGovernors.values.contains(officerId)

        val newGovernors = if (isGovernor) {
            state.cityGovernors.filterValues { it != officerId } + (targetCityId to officerId)
        } else state.cityGovernors
        val newGarrisons = if (isGarrison) {
            state.cityGarrisons.filterValues { it != officerId } + (targetCityId to officerId)
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
            state.copy(cityGovernors = newGovernors, cityGarrisons = newGarrisons, officers = newOfficers)
        )
    }

    /**
     * 召回入朝：外任/领军人物奉诏还朝。
     * 若人已在当前行在，直接转 IN_COURT；否则记录在途状态，抵达前不得肉身参加朝会。
     */
    fun recallToCourt(state: GameState, officerId: String): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【召回失败】找不到此人。")
        if (officer.status != OfficerStatus.DEPLOYED)
            return AppointResult.Failure("【召回失败】${officer.name}当前并非外任/领军状态，无需召回。")

        val capitalId = CharacterStateSource.CAPITAL_CITY_ID
        val capitalName = state.cities.find { it.id == capitalId }?.name ?: "行在"
        if (officer.currentCityId == capitalId) {
            val newOfficers = state.officers.map {
                if (it.id == officerId) it.copy(
                    status = OfficerStatus.IN_COURT,
                    travelDestinationCityId = null,
                    travelArrivalTurn = null
                ) else it
            }
            return AppointResult.Success(
                "【召回】${officer.name}本在$capitalName任所，即日入朝待命。",
                state.copy(officers = newOfficers)
            )
        }

        val travelTurns = estimateTravelTurns(state, officer.currentCityId, capitalId)
        val arrivalTurn = state.turn + travelTurns
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                travelDestinationCityId = capitalId,
                travelArrivalTurn = arrivalTurn
            ) else it
        }
        return AppointResult.Success(
            "【诏令】遣使赴任所召${officer.name}还朝，路程约${travelTurns}旬。" +
                "未抵$capitalName前不能列班奏对，只可由军报、奏折陈情。",
            state.copy(officers = newOfficers)
        )
    }

    /** 两城之间大致赶路旬数：按坐标直线距离粗算，1~4旬封顶。 */
    fun estimateTravelTurns(state: GameState, fromCityId: String, toCityId: String): Int {
        val from = state.cities.find { it.id == fromCityId }
        val to = state.cities.find { it.id == toCityId }
        if (from == null || to == null) return 2
        val dx = (from.x - to.x).toDouble()
        val dy = (from.y - to.y).toDouble()
        val dist = sqrt(dx * dx + dy * dy)
        return ceil(dist / 900.0).toInt().coerceIn(1, 4)
    }

    fun getGovernor(state: GameState, cityId: String): Officer? {
        val govId = state.cityGovernors[cityId] ?: return null
        return state.officers.find { it.id == govId }
    }

    fun getGarrison(state: GameState, cityId: String): Officer? {
        val garId = state.cityGarrisons[cityId] ?: return null
        return state.officers.find { it.id == garId }
    }

    fun currentRole(state: GameState, officerId: String): String {
        val asGov = state.cityGovernors.entries.find { it.value == officerId }
        val asGar = state.cityGarrisons.entries.find { it.value == officerId }
        val cityName: (String) -> String = { id -> state.cities.find { it.id == id }?.name ?: id }
        return when {
            asGov != null && asGar != null -> "${cityName(asGov.key)}主官 / ${cityName(asGar.key)}守将"
            asGov != null -> "${cityName(asGov.key)} 主官"
            asGar != null -> "${cityName(asGar.key)} 守将"
            else -> when (state.officers.find { it.id == officerId }?.status) {
                OfficerStatus.IN_COURT -> "御前待命"
                OfficerStatus.IN_CAPITAL -> "行在军职"
                OfficerStatus.DEPLOYED -> "外任"
                OfficerStatus.CAPTIVE -> "羁留敌营"
                OfficerStatus.NOT_YET_RELEVANT -> "尚未入局"
                OfficerStatus.DISMISSED -> "罢黜"
                OfficerStatus.DECEASED -> "已故"
                OfficerStatus.SOLDIER -> "军中服役"
                OfficerStatus.WANDERING -> "在野"
                OfficerStatus.HIDDEN -> "未登场"
                null -> "无职"
            }
        }
    }

    fun loyaltyRiskLabel(officer: Officer): String? = when {
        officer.loyalty < 30 && officer.ambition > 65 -> "⚠ 此人忠诚极低、野心甚重，委以重职风险极大"
        officer.loyalty < 45 -> "⚠ 此人忠诚不高，建议慎重委派边疆要职"
        else -> null
    }
}

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
 * WORLD-CORE-001 合流后，跨城任命/调任不再瞬移：正式人事命令统一交给
 * OfficerDispatchSystem 产生“离任→赶路→抵达→履职”的真实世界过程。
 * 建炎元年开局的“京城/行在”由 CharacterStateSource.CAPITAL_CITY_ID 统一决定。
 */
object AppointmentSystem {

    sealed class AppointResult {
        data class Success(val message: String, val newState: GameState) : AppointResult()
        data class Failure(val reason: String) : AppointResult()
    }

    private fun fromDispatch(result: OfficerDispatchSystem.DispatchResult): AppointResult = when (result) {
        is OfficerDispatchSystem.DispatchResult.Success -> AppointResult.Success(result.message, result.newState)
        is OfficerDispatchSystem.DispatchResult.Failure -> AppointResult.Failure(result.reason)
    }

    /**
     * DELEGATION-001：剧情事件效果里的忠诚度调整走正式校验入口。
     */
    fun adjustLoyalty(state: GameState, officerId: String, amount: Int): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("找不到此人物。")
        if (!CharacterStateSource.isAlive(officer))
            return AppointResult.Failure("${officer.name}已不在人世，忠诚无从谈起。")
        val newLoyalty = (officer.loyalty + amount).coerceIn(0, 100)
        val newOfficers = state.officers.map { if (it.id == officerId) it.copy(loyalty = newLoyalty) else it }
        val sign = if (amount >= 0) "+" else ""
        return AppointResult.Success(
            "${officer.name}忠诚度$sign$amount，现为$newLoyalty。",
            state.copy(officers = newOfficers)
        )
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

        return fromDispatch(
            OfficerDispatchSystem.dispatch(
                state = state,
                officerId = officerId,
                targetCityId = cityId,
                arrivalStatus = OfficerStatus.DEPLOYED,
                postTitle = "${city.name}主官",
                garrisonPost = false
            )
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

        return fromDispatch(
            OfficerDispatchSystem.dispatch(
                state = state,
                officerId = officerId,
                targetCityId = cityId,
                arrivalStatus = OfficerStatus.DEPLOYED,
                postTitle = "${city.name}守将",
                garrisonPost = true
            )
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
            if (it.id == officerId) it.copy(
                status = OfficerStatus.DISMISSED,
                travelDestinationCityId = null,
                travelArrivalTurn = null,
                travelArrivalStatus = null,
                travelArrivalPostTitle = ""
            ) else it
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
        if (targetCity.owner != "song")
            return AppointResult.Failure("【调任失败】${targetCity.name}不在宋廷控制之下。")
        if (officer.status !in setOf(OfficerStatus.IN_COURT, OfficerStatus.IN_CAPITAL, OfficerStatus.DEPLOYED))
            return AppointResult.Failure("【调任失败】${officer.name}当前状态不允许调任。")

        val isGovernor = state.cityGovernors.values.contains(officerId)
        val isGarrison = state.cityGarrisons.values.contains(officerId)
        val postTitle = when {
            isGovernor -> "${targetCity.name}主官"
            isGarrison -> "${targetCity.name}守将"
            else -> ""
        }
        val garrisonPost = when {
            isGovernor -> false
            isGarrison -> true
            else -> true // postTitle 为空时不会落正式席位，只用于保持参数稳定。
        }

        return fromDispatch(
            OfficerDispatchSystem.dispatch(
                state = state,
                officerId = officerId,
                targetCityId = targetCityId,
                arrivalStatus = OfficerStatus.DEPLOYED,
                postTitle = postTitle,
                garrisonPost = garrisonPost
            )
        )
    }

    /**
     * 召回入朝：外任/领军人物奉诏还朝。
     * 若人已在当前行在，直接转 IN_COURT；否则记录真实在途状态。
     * 一旦奉诏离任，原城正式席位立即腾缺，不能出现“人已在路上，旧城仍挂名主官”的假状态。
     */
    fun recallToCourt(state: GameState, officerId: String): AppointResult {
        val officer = state.officers.find { it.id == officerId }
            ?: return AppointResult.Failure("【召回失败】找不到此人。")
        if (officer.status != OfficerStatus.DEPLOYED)
            return AppointResult.Failure("【召回失败】${officer.name}当前并非外任/领军状态，无需召回。")
        if (CharacterStateSource.isTraveling(officer))
            return AppointResult.Failure("【召回失败】${officer.name}正在赶路途中，须先抵达方可再下新命。")

        val capitalId = CharacterStateSource.CAPITAL_CITY_ID
        val capitalName = state.cities.find { it.id == capitalId }?.name ?: "行在"
        val clearedGovernors = state.cityGovernors.filterValues { it != officerId }
        val clearedGarrisons = state.cityGarrisons.filterValues { it != officerId }

        if (officer.currentCityId == capitalId) {
            val newOfficers = state.officers.map {
                if (it.id == officerId) it.copy(
                    status = OfficerStatus.IN_COURT,
                    travelDestinationCityId = null,
                    travelArrivalTurn = null,
                    travelArrivalStatus = null,
                    travelArrivalPostTitle = ""
                ) else it
            }
            return AppointResult.Success(
                "【召回】${officer.name}本在${capitalName}任所，即日入朝待命。",
                state.copy(
                    officers = newOfficers,
                    cityGovernors = clearedGovernors,
                    cityGarrisons = clearedGarrisons
                )
            )
        }

        val travelTurns = estimateTravelTurns(state, officer.currentCityId, capitalId)
        val arrivalTurn = state.turn + travelTurns
        val newOfficers = state.officers.map {
            if (it.id == officerId) it.copy(
                travelDestinationCityId = capitalId,
                travelArrivalTurn = arrivalTurn,
                travelArrivalStatus = OfficerStatus.IN_COURT,
                travelArrivalPostTitle = ""
            ) else it
        }
        return AppointResult.Success(
            "【诏令】遣使赴任所召${officer.name}还朝，路程约${travelTurns}旬。" +
                "本人已离任启程；未抵${capitalName}前不能列班奏对，只可由奏折、军报陈情。",
            state.copy(
                officers = newOfficers,
                cityGovernors = clearedGovernors,
                cityGarrisons = clearedGarrisons
            )
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

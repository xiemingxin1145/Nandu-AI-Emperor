package com.xiemingxin.nandu.game

/**
 * STAB-002：战役展示层唯一状态源。
 *
 * 战役 UI 不允许再自行写死年份、官职或参战人物。这里仅从 GameState 中读取：
 * - 当前日历；
 * - 当前军团与战区；
 * - 城池驻防任命；
 * - 人物生死、位置、在途状态与官阶。
 *
 * 这不是战役触发器；战役是否应该出现由 STAB-001 / 后续动态历史系统决定。
 */
data class BattleSceneParticipant(
    val officerId: String,
    val name: String,
    val rankText: String,
    val dutyText: String,
    val locationText: String,
    val armyName: String? = null
) {
    val displayTitle: String
        get() = listOf(rankText, dutyText)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(" · ")
}

data class BattleScenePresentation(
    val battleName: String,
    val dateText: String,
    val theaterText: String,
    val mainParticipant: BattleSceneParticipant?,
    val supportingParticipants: List<BattleSceneParticipant>,
    val friendlyTroops: Int,
    val friendlyGrain: Int,
    val friendlyMorale: Int,
    val enemyTroops: Int,
    val enemySummary: String,
    val situationText: String,
    val reportText: String
) {
    val allParticipants: List<BattleSceneParticipant>
        get() = listOfNotNull(mainParticipant) + supportingParticipants
}

object BattleScenePresentationSystem {

    /**
     * 当前地图还没有独立的“顺昌府”节点，因此先使用淮西/淮南相关节点作为战区集合。
     * 后续地图扩充顺昌节点时只需替换这里，不需要再改 UI。
     */
    private val shunchangTheaterCityIds = setOf(
        "shouchun", "hefei", "xinyang", "haozhou", "sizhou", "chuzhou", "yangzhou"
    )

    private val jinApproachCityIds = setOf("kaifeng", "daming", "luoyang")

    fun shunchang(state: GameState): BattleScenePresentation {
        val theaterCities = state.cities.filter { it.id in shunchangTheaterCityIds }
        val cityNameById = state.cities.associate { it.id to it.name }

        val songArmies = state.armies.filter { army ->
            army.ownerFactionId == "song" &&
                army.statusCode != ArmyStatus.DISBANDED &&
                isArmyInTheater(army, shunchangTheaterCityIds)
        }

        val jinArmies = state.armies.filter { army ->
            army.ownerFactionId == "jin" &&
                army.statusCode != ArmyStatus.DISBANDED &&
                (
                    isArmyInTheater(army, shunchangTheaterCityIds) ||
                        (army.statusCode == ArmyStatus.MARCHING && army.currentCityId in jinApproachCityIds)
                    )
        }

        val garrisonOfficerIds = state.cityGarrisons
            .filterKeys { it in shunchangTheaterCityIds }
            .values
            .toSet()

        val armyCommanderIds = songArmies
            .map { it.commanderId }
            .filter { it.isNotBlank() }
            .toSet()

        val candidateOfficers = state.officers
            .filter { officer ->
                officer.currentCityId in shunchangTheaterCityIds &&
                    eligibleForBattleScene(officer) &&
                    (
                        officer.id in armyCommanderIds ||
                            officer.id in garrisonOfficerIds ||
                            officer.status == OfficerStatus.DEPLOYED
                        )
            }
            .distinctBy { it.id }

        val shouchunGarrisonId = state.cityGarrisons["shouchun"]
        val mainOfficer = candidateOfficers.maxByOrNull { officer ->
            val army = songArmies.firstOrNull { it.commanderId == officer.id }
            when {
                officer.id == shouchunGarrisonId -> 100_000 + officer.command
                army?.currentCityId == "shouchun" -> 90_000 + officer.command
                army != null -> 80_000 + officer.command
                else -> officer.command
            }
        }

        val main = mainOfficer?.let { toParticipant(state, it, songArmies, cityNameById) }
        val supports = candidateOfficers
            .filter { it.id != mainOfficer?.id }
            .sortedByDescending { it.command }
            .take(2)
            .map { toParticipant(state, it, songArmies, cityNameById) }

        val friendlyTroops = theaterCities
            .filter { it.owner == "song" }
            .sumOf { it.troops } + songArmies.sumOf { it.troops }

        val friendlyGrain = theaterCities
            .filter { it.owner == "song" }
            .sumOf { it.grain }

        val weightedMorale = if (songArmies.isNotEmpty()) {
            songArmies.sumOf { it.morale * it.troops } / songArmies.sumOf { it.troops }.coerceAtLeast(1)
        } else {
            state.troopMorale
        }.coerceIn(0, 100)

        val enemyTroops = jinArmies.sumOf { it.troops }
        val enemySummary = if (jinArmies.isEmpty()) {
            "尚未侦得进入该战区的金军军团"
        } else {
            jinArmies.joinToString("、") { army ->
                val commander = state.officers.firstOrNull { it.id == army.commanderId }?.name
                if (commander.isNullOrBlank()) army.name else "${army.name}（$commander）"
            }
        }

        val situation = when {
            enemyTroops <= 0 -> "敌情未明"
            friendlyTroops <= 0 -> "无兵可守"
            enemyTroops >= friendlyTroops * 2 -> "兵力悬殊"
            enemyTroops > friendlyTroops -> "敌众我寡"
            enemyTroops * 2 < friendlyTroops -> "我军占优"
            else -> "兵力相当"
        }

        val report = when (main) {
            null -> "枢密院尚未登记该战区的实际主帅。为避免历史人物凭空出现，本界面不再使用岳飞、刘锜、韩世忠等固定占位。请以当前军团与任命状态为准。"
            else -> buildString {
                append("${main.name}现驻${main.locationText}，身份为${main.displayTitle.ifBlank { "军中任事" }}。")
                append("战区可核实宋军约${friendlyTroops}，军粮${friendlyGrain}，士气${weightedMorale}。")
                if (enemyTroops > 0) append("已侦得金军约${enemyTroops}，当前判断：${situation}。")
                else append("目前尚无可核实的金军军团进入战区，不得伪造‘十万铁骑已至城下’。")
            }
        }

        return BattleScenePresentation(
            battleName = "顺昌方向军情",
            dateText = state.calendar.displayText(),
            theaterText = "淮西 / 淮南前线",
            mainParticipant = main,
            supportingParticipants = supports,
            friendlyTroops = friendlyTroops,
            friendlyGrain = friendlyGrain,
            friendlyMorale = weightedMorale,
            enemyTroops = enemyTroops,
            enemySummary = enemySummary,
            situationText = situation,
            reportText = report
        )
    }

    private fun isArmyInTheater(army: Army, theaterIds: Set<String>): Boolean =
        army.currentCityId in theaterIds ||
            army.targetCityId in theaterIds ||
            army.routeNodeIds.any { it in theaterIds }

    private fun eligibleForBattleScene(officer: Officer): Boolean {
        if (!CharacterStateSource.isAlive(officer)) return false
        if (CharacterStateSource.isTraveling(officer)) return false
        return officer.status !in setOf(
            OfficerStatus.HIDDEN,
            OfficerStatus.WANDERING,
            OfficerStatus.SOLDIER,
            OfficerStatus.DISMISSED,
            OfficerStatus.CAPTIVE,
            OfficerStatus.NOT_YET_RELEVANT,
            OfficerStatus.DECEASED
        )
    }

    private fun toParticipant(
        state: GameState,
        officer: Officer,
        theaterArmies: List<Army>,
        cityNameById: Map<String, String>
    ): BattleSceneParticipant {
        val army = theaterArmies.firstOrNull { it.commanderId == officer.id }
        val duty = when {
            army != null -> "${army.name}统帅"
            state.cityGarrisons.any { it.key == officer.currentCityId && it.value == officer.id } ->
                "${cityNameById[officer.currentCityId] ?: officer.currentCityId}守将"
            state.cityGovernors.any { it.key == officer.currentCityId && it.value == officer.id } ->
                "${cityNameById[officer.currentCityId] ?: officer.currentCityId}主官"
            else -> AppointmentSystem.currentRole(state, officer.id)
        }

        return BattleSceneParticipant(
            officerId = officer.id,
            name = officer.name,
            rankText = officer.profile().rank,
            dutyText = duty,
            locationText = cityNameById[officer.currentCityId] ?: officer.currentCityId,
            armyName = army?.name
        )
    }
}
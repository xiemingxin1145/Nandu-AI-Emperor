package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BattleScenePresentationSystemTest {

    private fun city(id: String, name: String, owner: String = "song", troops: Int = 10000, grain: Int = 20000) = City(
        id = id,
        name = name,
        owner = owner,
        troops = troops,
        defense = 70,
        grain = grain,
        gold = 10000,
        popularSupport = 70,
        controlState = if (owner == "song") "FRONTLINE" else "FALLEN"
    )

    private fun officer(
        id: String,
        name: String,
        cityId: String,
        status: OfficerStatus = OfficerStatus.DEPLOYED,
        rankLevel: Int = 2,
        command: Int = 85
    ) = Officer(
        id = id,
        name = name,
        faction = "宋廷",
        command = command,
        force = 80,
        strategy = 80,
        politics = 55,
        loyalty = 90,
        currentCityId = cityId,
        status = status,
        charm = 65,
        ambition = 30,
        rankLevel = rankLevel,
        origin = "军户",
        skills = listOf("统兵"),
        bio = ""
    )

    private fun army(id: String, name: String, commanderId: String, cityId: String, troops: Int = 12000) = Army(
        id = id,
        name = name,
        ownerFactionId = "song",
        commanderId = commanderId,
        homeCityId = cityId,
        currentCityId = cityId,
        troops = troops,
        morale = 72,
        armyType = "field_army",
        supplyCityId = cityId,
        statusCode = ArmyStatus.GARRISONED,
        status = "驻防"
    )

    private fun baseState(
        officers: List<Officer>,
        armies: List<Army> = emptyList(),
        garrisons: Map<String, String> = emptyMap(),
        calendar: GameCalendar = GameCalendar("建炎元年", 1, 6, 1)
    ) = GameState(
        officers = officers,
        armies = armies,
        cities = listOf(
            city("shouchun", "寿春", troops = 16000, grain = 30000),
            city("hefei", "庐州", troops = 9000, grain = 18000),
            city("xinyang", "信阳", troops = 8000, grain = 16000),
            city("yingtianfu", "南京应天府", troops = 20000, grain = 70000),
            city("xiangyang", "襄阳", troops = 15000, grain = 40000),
            city("kaifeng", "开封", owner = "jin", troops = 40000, grain = 80000)
        ),
        cityGarrisons = garrisons,
        factions = emptyList(),
        calendar = calendar,
        season = Season.SUMMER,
        weather = WeatherType.CLEAR,
        troopMorale = 66,
        jinThreat = 70
    )

    @Test
    fun openingStateDoesNotFabricateFamousGeneralsIntoBattleScene() {
        val yueFei = officer("yue_fei", "岳飞", "xiangyang", status = OfficerStatus.WANDERING, rankLevel = 0)
        val liuQi = officer("liu_qi", "刘锜", "shouchun", status = OfficerStatus.NOT_YET_RELEVANT, rankLevel = 1)
        val han = officer("han_shizhong", "韩世忠", "yingtianfu", status = OfficerStatus.IN_CAPITAL, rankLevel = 2)
        val presentation = BattleScenePresentationSystem.shunchang(baseState(listOf(yueFei, liuQi, han)))

        assertNull(presentation.mainParticipant)
        assertTrue(presentation.supportingParticipants.isEmpty())
        assertFalse(presentation.allParticipants.any { it.officerId in setOf("yue_fei", "liu_qi", "han_shizhong") })
        assertTrue(presentation.reportText.contains("不再使用岳飞、刘锜、韩世忠等固定占位"))
    }

    @Test
    fun actualTheaterCommanderBecomesMainParticipant() {
        val liuQi = officer("liu_qi", "刘锜", "shouchun", status = OfficerStatus.DEPLOYED, rankLevel = 3, command = 92)
        val state = baseState(
            officers = listOf(liuQi),
            armies = listOf(army("army_liu", "淮西前军", "liu_qi", "shouchun", 15000)),
            garrisons = mapOf("shouchun" to "liu_qi")
        )
        val presentation = BattleScenePresentationSystem.shunchang(state)

        assertEquals("liu_qi", presentation.mainParticipant?.officerId)
        assertEquals("寿春", presentation.mainParticipant?.locationText)
        assertTrue(presentation.mainParticipant?.dutyText?.contains("淮西前军") == true)
        assertFalse(presentation.mainParticipant?.displayTitle?.contains("清远军节度使") == true)
        assertEquals("建炎元年 六月上旬", presentation.dateText)
    }

    @Test
    fun relocatedOrDeadOfficerCannotAppearEvenIfHistoricallyFamous() {
        val yueRemote = officer("yue_fei", "岳飞", "xiangyang", status = OfficerStatus.DEPLOYED, command = 99)
        val deadLiu = officer("liu_qi", "刘锜", "shouchun", status = OfficerStatus.DECEASED, command = 95)
        val local = officer("local_general", "王统制", "hefei", status = OfficerStatus.DEPLOYED, rankLevel = 2, command = 75)
        val state = baseState(
            officers = listOf(yueRemote, deadLiu, local),
            armies = listOf(army("army_local", "庐州守军", "local_general", "hefei"))
        )
        val presentation = BattleScenePresentationSystem.shunchang(state)

        assertEquals("local_general", presentation.mainParticipant?.officerId)
        assertFalse(presentation.allParticipants.any { it.officerId == "yue_fei" })
        assertFalse(presentation.allParticipants.any { it.officerId == "liu_qi" })
    }

    @Test
    fun displayedRankChangesWithLiveRankLevelInsteadOfFixedHistoricalTitle() {
        val junior = officer("test_general", "试将", "shouchun", rankLevel = 2)
        val senior = junior.copy(rankLevel = 5, merit = 80)

        val juniorState = baseState(listOf(junior), listOf(army("army_test", "寿春军", junior.id, "shouchun")))
        val seniorState = baseState(listOf(senior), listOf(army("army_test", "寿春军", senior.id, "shouchun")))

        val juniorTitle = BattleScenePresentationSystem.shunchang(juniorState).mainParticipant!!.displayTitle
        val seniorTitle = BattleScenePresentationSystem.shunchang(seniorState).mainParticipant!!.displayTitle

        assertNotEquals(juniorTitle, seniorTitle)
        assertTrue(juniorTitle.contains("偏将"))
        assertTrue(seniorTitle.contains("方面大将"))
    }

    @Test
    fun battleDateAlwaysUsesCurrentGameCalendar() {
        val general = officer("general", "守将", "shouchun")
        val calendar = GameCalendar(eraName = "绍兴十年", year = 14, month = 5, tenDay = 2)
        val state = baseState(
            officers = listOf(general),
            armies = listOf(army("army_general", "淮西军", general.id, "shouchun")),
            calendar = calendar
        )

        val presentation = BattleScenePresentationSystem.shunchang(state)
        assertEquals("绍兴十年 五月中旬", presentation.dateText)
        assertFalse(presentation.dateText.contains("建炎四年"))
    }

    @Test
    fun onlyCurrentTheaterParticipantsAreRenderedAsSupport() {
        val main = officer("main", "主将", "shouchun", command = 90)
        val support = officer("support", "援将", "hefei", command = 80)
        val remote = officer("remote", "远将", "xiangyang", command = 99)
        val state = baseState(
            officers = listOf(main, support, remote),
            armies = listOf(
                army("army_main", "寿春军", main.id, "shouchun"),
                army("army_support", "庐州军", support.id, "hefei"),
                army("army_remote", "襄阳军", remote.id, "xiangyang")
            )
        )

        val presentation = BattleScenePresentationSystem.shunchang(state)
        assertEquals("main", presentation.mainParticipant?.officerId)
        assertTrue(presentation.supportingParticipants.any { it.officerId == "support" })
        assertFalse(presentation.allParticipants.any { it.officerId == "remote" })
    }
}

package com.xiemingxin.nandu.game

import org.junit.Test
import org.junit.Assert.*

/**
 * V1.1 活朝堂 + 建炎元年应天开局测试。
 *
 * 重点保证：人物只有一个真实位置；1127 行在是南京应天府；外任/赶路/俘虏/尚未入局的人
 * 不能因为后世名气被塞进普通朝会。
 */
class CharacterStateSourceTest {

    private fun yingtian() = City(
        "yingtianfu", "南京应天府", "song", troops = 20000, defense = 68, grain = 70000, gold = 26000,
        popularSupport = 68, controlState = "FRONTLINE", isCapital = true, x = 10000, y = 4000
    )

    private fun hangzhou() = City(
        "linan", "杭州", "song", troops = 12000, defense = 62, grain = 100000, gold = 55000,
        popularSupport = 82, controlState = "STABLE", isCapital = false, x = 11000, y = 6800
    )

    private fun xiangyang() = City(
        "xiangyang", "襄阳", "song", troops = 15000, defense = 80, grain = 50000, gold = 15000,
        popularSupport = 70, controlState = "FRONTLINE", x = 8200, y = 5000
    )

    private fun kaifeng() = City(
        "kaifeng", "开封", "jin", troops = 50000, defense = 90, grain = 200000, gold = 100000,
        popularSupport = 30, controlState = "FALLEN", x = 9400, y = 3400
    )

    private fun officer(
        id: String,
        name: String = id,
        status: OfficerStatus = OfficerStatus.IN_COURT,
        currentCityId: String = "yingtianfu",
        travelDestinationCityId: String? = null,
        travelArrivalTurn: Int? = null,
        scheduledStatus: OfficerStatus? = null,
        scheduledCityId: String? = null,
        scheduledTurn: Int? = null
    ) = Officer(
        id = id, name = name, faction = "宋廷", command = 80, force = 80, strategy = 80,
        politics = 60, loyalty = 90, currentCityId = currentCityId, status = status,
        charm = 65, ambition = 30, rankLevel = 3, origin = "将门", skills = emptyList(), bio = "",
        travelDestinationCityId = travelDestinationCityId, travelArrivalTurn = travelArrivalTurn,
        scheduledStatus = scheduledStatus, scheduledCityId = scheduledCityId, scheduledTurn = scheduledTurn
    )

    private fun state(officers: List<Officer>, turn: Int = 10) = GameState(
        cities = listOf(yingtian(), hangzhou(), xiangyang(), kaifeng()), officers = officers, armies = emptyList(),
        factions = emptyList(), turn = turn, troopMorale = 70, courtStability = 60, jinThreat = 40,
        gold = 50000, grain = 100000, prestige = 50,
        season = Season.SUMMER, weather = WeatherType.CLEAR,
        calendar = GameCalendar(eraName = "建炎元年", year = 1, month = 6, tenDay = 1)
    )

    @Test
    fun openingCapitalIsYingtianNotHangzhou() {
        assertEquals("yingtianfu", CharacterStateSource.CAPITAL_CITY_ID)
        val song = InitialData.factions.first { it.id == "song" }
        assertEquals("yingtianfu", song.capitalCityId)
        assertTrue(InitialData.cities.first { it.id == "yingtianfu" }.isCapital)
        assertFalse(InitialData.cities.first { it.id == "linan" }.isCapital)
        assertEquals("杭州", InitialData.cities.first { it.id == "linan" }.name)
    }

    @Test
    fun openingCalendarIsJianyanFirstYearSixthMonth() {
        val state = GameState()
        assertEquals("建炎元年", state.calendar.eraName)
        assertEquals(1, state.calendar.year)
        assertEquals(6, state.calendar.month)
        assertEquals(1, state.calendar.tenDay)
        assertEquals(Season.SUMMER, state.season)
    }

    @Test
    fun deployedOfficerCannotAttendCourt() {
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(yueFei))
        assertFalse(
            "外任领军的岳飞不应出现在应天行在普通朝会",
            CharacterAppearanceSystem.canAppearInPalace(st, "yue_fei", PalaceIds.CHUIGONG)
        )
        assertFalse(CharacterStateSource.isAtCourt(yueFei))
    }

    @Test
    fun deployedOfficerCannotBeRecruited() {
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(yueFei)).copy(talentLeads = setOf("yue_fei"), gold = 50000)
        val result = RecruitmentSystem.recruit(st, "yue_fei", goldOffered = 5000, seed = 1L)
        assertTrue(result is RecruitmentSystem.RecruitResult.NotFound)
        assertFalse(CharacterStateSource.isRecruitable(yueFei))
    }

    @Test
    fun wanderingOfficerCannotAttendCourt() {
        val wandering = officer("wandering_officer", status = OfficerStatus.WANDERING, currentCityId = "yingtianfu")
        val st = state(listOf(wandering))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, wandering.id, PalaceIds.CHUIGONG))
    }

    @Test
    fun deceasedOfficerCannotAttendCourt() {
        val dead = officer("dead_officer", status = OfficerStatus.DECEASED, currentCityId = "yingtianfu")
        val st = state(listOf(dead))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, dead.id, PalaceIds.CHUIGONG))
        assertFalse(CharacterStateSource.isAlive(dead))
    }

    @Test
    fun inCourtOfficerInYingtianCanAttend() {
        val liGang = officer("li_gang", "李纲", status = OfficerStatus.IN_COURT, currentCityId = "yingtianfu")
        val st = state(listOf(liGang))
        assertTrue(CharacterStateSource.isAtCourt(liGang))
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(st, "li_gang", PalaceIds.CHUIGONG))
    }

    @Test
    fun inCourtStatusButBodyInHangzhouCannotAttendYingtianCourt() {
        val liGang = officer("li_gang", "李纲", status = OfficerStatus.IN_COURT, currentCityId = "linan")
        val st = state(listOf(liGang))
        assertFalse(CharacterStateSource.isAtCourt(liGang))
        assertEquals(CharacterVisibility.SEEN, CharacterAppearanceSystem.visibilityFor(st, "li_gang"))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, "li_gang", PalaceIds.CHUIGONG))
    }

    @Test
    fun recalledOfficerNeedsTravelTimeBeforeAttendingCourt() {
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(yueFei), turn = 10)

        val recallResult = AppointmentSystem.recallToCourt(st, "yue_fei")
        assertTrue(recallResult is AppointmentSystem.AppointResult.Success)
        val afterRecall = (recallResult as AppointmentSystem.AppointResult.Success).newState
        val travelingOfficer = afterRecall.officers.first { it.id == "yue_fei" }

        assertEquals(OfficerStatus.DEPLOYED, travelingOfficer.status)
        assertEquals("yingtianfu", travelingOfficer.travelDestinationCityId)
        assertNotNull(travelingOfficer.travelArrivalTurn)
        assertTrue(travelingOfficer.travelArrivalTurn!! > afterRecall.turn)
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(afterRecall, "yue_fei", PalaceIds.CHUIGONG))

        val notYetArrived = afterRecall.copy(turn = travelingOfficer.travelArrivalTurn!! - 1)
        val (stillTraveling, noReports) = CharacterTravelSystem.tickArrivals(notYetArrived)
        assertTrue(noReports.isEmpty())
        assertEquals(OfficerStatus.DEPLOYED, stillTraveling.officers.first { it.id == "yue_fei" }.status)

        val arrivedState = afterRecall.copy(turn = travelingOfficer.travelArrivalTurn!!)
        val (afterArrival, reports) = CharacterTravelSystem.tickArrivals(arrivedState)
        val arrivedOfficer = afterArrival.officers.first { it.id == "yue_fei" }
        assertTrue(reports.isNotEmpty())
        assertEquals(OfficerStatus.IN_COURT, arrivedOfficer.status)
        assertEquals("yingtianfu", arrivedOfficer.currentCityId)
        assertNull(arrivedOfficer.travelArrivalTurn)
        assertNull(arrivedOfficer.travelDestinationCityId)
        assertTrue(CharacterStateSource.isAtCourt(arrivedOfficer))
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(afterArrival, "yue_fei", PalaceIds.CHUIGONG))
    }

    @Test
    fun courtAttendeesHaveNoDuplicateInstances() {
        val liGang = officer("li_gang", "李纲", status = OfficerStatus.IN_COURT)
        val huang = officer("huang_qianshan", "黄潜善", status = OfficerStatus.IN_COURT)
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(liGang, huang, yueFei))

        val attendeeIds = st.officers
            .filter { CharacterAppearanceSystem.canAppearInPalace(st, it.id, PalaceIds.CHUIGONG) }
            .map { it.id }

        assertEquals(attendeeIds.distinct().size, attendeeIds.size)
        assertTrue(attendeeIds.contains("li_gang"))
        assertTrue(attendeeIds.contains("huang_qianshan"))
        assertFalse(attendeeIds.contains("yue_fei"))
    }

    @Test
    fun zongZeCanAttendCourtOnOpeningDayDespiteScheduledTransition() {
        val zongZe = officer(
            "zong_ze", "宗泽", status = OfficerStatus.IN_COURT, currentCityId = "yingtianfu",
            scheduledStatus = OfficerStatus.DEPLOYED, scheduledCityId = "kaifeng", scheduledTurn = 3
        )
        val st = state(listOf(zongZe), turn = 1)
        assertTrue(CharacterStateSource.isAtCourt(zongZe))
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(st, "zong_ze", PalaceIds.CHUIGONG))
    }

    @Test
    fun zongZeScheduledTransitionFiresOnlyAtScheduledTurn() {
        val zongZe = officer(
            "zong_ze", "宗泽", status = OfficerStatus.IN_COURT, currentCityId = "yingtianfu",
            scheduledStatus = OfficerStatus.DEPLOYED, scheduledCityId = "kaifeng", scheduledTurn = 3
        )
        val early = state(listOf(zongZe), turn = 2)
        val (stillAtCourt, noReports) = CharacterTravelSystem.tickScheduledTransitions(early)
        assertTrue(noReports.isEmpty())
        assertEquals(OfficerStatus.IN_COURT, stillAtCourt.officers.first { it.id == "zong_ze" }.status)
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(stillAtCourt, "zong_ze", PalaceIds.CHUIGONG))

        val onTime = state(listOf(zongZe), turn = 3)
        val (departed, reports) = CharacterTravelSystem.tickScheduledTransitions(onTime)
        val zongZeAfter = departed.officers.first { it.id == "zong_ze" }
        assertTrue(reports.isNotEmpty())
        assertEquals(OfficerStatus.DEPLOYED, zongZeAfter.status)
        assertEquals("kaifeng", zongZeAfter.currentCityId)
        assertNull(zongZeAfter.scheduledStatus)
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(departed, "zong_ze", PalaceIds.CHUIGONG))
    }

    @Test
    fun captiveOfficerCannotAttendCourtOrBeRecruited() {
        val qinHui = officer("qin_hui", "秦桧", status = OfficerStatus.CAPTIVE, currentCityId = "kaifeng")
        val st = state(listOf(qinHui)).copy(gold = 50000)
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, "qin_hui", PalaceIds.CHUIGONG))
        assertFalse(CharacterStateSource.isRecruitable(qinHui))
        assertEquals(CharacterVisibility.HIDDEN, CharacterAppearanceSystem.visibilityFor(st, "qin_hui"))
        assertTrue(RecruitmentSystem.recruit(st, "qin_hui", 5000, 1L) is RecruitmentSystem.RecruitResult.NotFound)
    }

    @Test
    fun inCapitalOfficerOnlyAppearsInMilitaryPalace() {
        val han = officer("han_shizhong", "韩世忠", status = OfficerStatus.IN_CAPITAL, currentCityId = "yingtianfu")
        val st = state(listOf(han))
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(st, "han_shizhong", PalaceIds.SHUMI))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, "han_shizhong", PalaceIds.CHUIGONG))
    }

    @Test
    fun notYetRelevantOfficerIsFullyHiddenFromEarlyGame() {
        val zhaoDing = officer("zhao_ding", "赵鼎", status = OfficerStatus.NOT_YET_RELEVANT, currentCityId = "linan")
        val st = state(listOf(zhaoDing))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, "zhao_ding", PalaceIds.CHUIGONG))
        assertFalse(CharacterStateSource.isRecruitable(zhaoDing))
        assertEquals(CharacterVisibility.HIDDEN, CharacterAppearanceSystem.visibilityFor(st, "zhao_ding"))
    }

    @Test
    fun initialCanonActorsHaveExpectedOpeningStates() {
        val byId = InitialData.officers.associateBy { it.id }
        assertEquals(OfficerStatus.IN_COURT, byId.getValue("li_gang").status)
        assertEquals("yingtianfu", byId.getValue("li_gang").currentCityId)
        assertEquals(OfficerStatus.IN_COURT, byId.getValue("zong_ze").status)
        assertEquals("yingtianfu", byId.getValue("zong_ze").currentCityId)
        assertEquals(OfficerStatus.IN_COURT, byId.getValue("huang_qianshan").status)
        assertEquals(OfficerStatus.IN_COURT, byId.getValue("wang_boyan").status)
        assertEquals(OfficerStatus.WANDERING, byId.getValue("yue_fei").status)
        assertEquals(OfficerStatus.CAPTIVE, byId.getValue("qin_hui").status)
        assertEquals(OfficerStatus.NOT_YET_RELEVANT, byId.getValue("zhao_ding").status)
        assertEquals(OfficerStatus.NOT_YET_RELEVANT, byId.getValue("wu_jie").status)
        assertEquals(OfficerStatus.NOT_YET_RELEVANT, byId.getValue("liu_qi").status)
    }
}

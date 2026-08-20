package com.xiemingxin.nandu.game

import org.junit.Test
import org.junit.Assert.*

/**
 * V1.0 活朝堂 / living-world-court-v1 测试。
 *
 * 覆盖任务书要求的 7 项：
 *  1. 岳飞 DEPLOYED 时不得参加普通朝会
 *  2. 岳飞 DEPLOYED 时不得被征辟
 *  3. WANDERING 人物不得参加朝会
 *  4. DECEASED 人物不得参加朝会
 *  5. IN_COURT 且位于临安的人物允许参加对应朝会
 *  6. 外地人物奉诏回京需要经过移动时间
 *  7. 朝会出席名单不会出现同人物重复实例
 */
class CharacterStateSourceTest {

    private fun linan() = City(
        "linan", "临安", "song", troops = 20000, defense = 75, grain = 150000, gold = 80000,
        popularSupport = 85, controlState = "STABLE", isCapital = true, x = 11000, y = 6800
    )

    private fun xiangyang() = City(
        "xiangyang", "襄阳", "song", troops = 15000, defense = 80, grain = 50000, gold = 15000,
        popularSupport = 70, controlState = "FRONTLINE", x = 8200, y = 5000
    )

    private fun officer(
        id: String,
        name: String = id,
        status: OfficerStatus = OfficerStatus.IN_COURT,
        currentCityId: String = "linan",
        travelDestinationCityId: String? = null,
        travelArrivalTurn: Int? = null
    ) = Officer(
        id = id, name = name, faction = "宋廷", command = 80, force = 80, strategy = 80,
        politics = 60, loyalty = 90, currentCityId = currentCityId, status = status,
        charm = 65, ambition = 30, rankLevel = 3, origin = "将门", skills = emptyList(), bio = "",
        travelDestinationCityId = travelDestinationCityId, travelArrivalTurn = travelArrivalTurn
    )

    private fun state(officers: List<Officer>, turn: Int = 10) = GameState(
        cities = listOf(linan(), xiangyang()), officers = officers, armies = emptyList(),
        factions = emptyList(), turn = turn, troopMorale = 70, courtStability = 60, jinThreat = 40,
        gold = 50000, grain = 100000, prestige = 50,
        season = Season.SPRING, weather = WeatherType.CLEAR,
        calendar = GameCalendar(eraName = "建炎元年", year = 1127, month = 1, tenDay = 1)
    )

    // 1. 岳飞 DEPLOYED 时不得参加普通朝会
    @Test
    fun deployedOfficerCannotAttendCourt() {
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(yueFei))
        assertFalse(
            "外任领军的岳飞不应出现在垂拱殿朝会",
            CharacterAppearanceSystem.canAppearInPalace(st, "yue_fei", PalaceIds.CHUIGONG)
        )
        assertFalse(CharacterStateSource.isAtCourt(yueFei))
    }

    // 2. 岳飞 DEPLOYED 时不得被征辟
    @Test
    fun deployedOfficerCannotBeRecruited() {
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(yueFei)).copy(talentLeads = setOf("yue_fei"), gold = 50000)
        val result = RecruitmentSystem.recruit(st, "yue_fei", goldOffered = 5000, seed = 1L)
        assertTrue(
            "已在外任的岳飞再次征辟应直接失败",
            result is RecruitmentSystem.RecruitResult.NotFound
        )
        assertFalse(CharacterStateSource.isRecruitable(yueFei))
    }

    // 3. WANDERING 人物不得参加朝会
    @Test
    fun wanderingOfficerCannotAttendCourt() {
        val zongZe = officer("zong_ze", "宗泽", status = OfficerStatus.WANDERING, currentCityId = "linan")
        val st = state(listOf(zongZe))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, "zong_ze", PalaceIds.CHUIGONG))
    }

    // 4. DECEASED 人物不得参加朝会
    @Test
    fun deceasedOfficerCannotAttendCourt() {
        val dead = officer("dead_officer", status = OfficerStatus.DECEASED, currentCityId = "linan")
        val st = state(listOf(dead))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(st, "dead_officer", PalaceIds.CHUIGONG))
        assertFalse(CharacterStateSource.isAlive(dead))
    }

    // 5. IN_COURT 且位于临安的人物允许参加对应朝会
    @Test
    fun inCourtOfficerInCapitalCanAttend() {
        val liGang = officer("li_gang", "李纲", status = OfficerStatus.IN_COURT, currentCityId = "linan")
        val st = state(listOf(liGang))
        assertTrue(CharacterStateSource.isAtCourt(liGang))
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(st, "li_gang", PalaceIds.CHUIGONG))
    }

    // 6. 外地人物奉诏回京需要经过移动时间
    @Test
    fun recalledOfficerNeedsTravelTimeBeforeAttendingCourt() {
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(yueFei), turn = 10)

        val recallResult = AppointmentSystem.recallToCourt(st, "yue_fei")
        assertTrue(recallResult is AppointmentSystem.AppointResult.Success)
        val afterRecall = (recallResult as AppointmentSystem.AppointResult.Success).newState
        val travelingOfficer = afterRecall.officers.first { it.id == "yue_fei" }

        // 诏令已发，但人还没到——不能瞬移，status 不应立刻变成 IN_COURT
        assertEquals(OfficerStatus.DEPLOYED, travelingOfficer.status)
        assertNotNull("应记录抵达旬数", travelingOfficer.travelArrivalTurn)
        assertTrue("抵达旬数应晚于当前旬", travelingOfficer.travelArrivalTurn!! > afterRecall.turn)
        assertFalse(
            "途中不能出现在朝会",
            CharacterAppearanceSystem.canAppearInPalace(afterRecall, "yue_fei", PalaceIds.CHUIGONG)
        )

        // 还没到旬数：tick 不应生效
        val notYetArrived = afterRecall.copy(turn = travelingOfficer.travelArrivalTurn!! - 1)
        val (stillTraveling, noReports) = CharacterTravelSystem.tickArrivals(notYetArrived)
        assertTrue(noReports.isEmpty())
        assertEquals(OfficerStatus.DEPLOYED, stillTraveling.officers.first { it.id == "yue_fei" }.status)

        // 抵达旬数：tick 后才真正转为 IN_COURT，且人在临安
        val arrivedState = afterRecall.copy(turn = travelingOfficer.travelArrivalTurn!!)
        val (afterArrival, reports) = CharacterTravelSystem.tickArrivals(arrivedState)
        val arrivedOfficer = afterArrival.officers.first { it.id == "yue_fei" }
        assertTrue(reports.isNotEmpty())
        assertEquals(OfficerStatus.IN_COURT, arrivedOfficer.status)
        assertEquals("linan", arrivedOfficer.currentCityId)
        assertNull(arrivedOfficer.travelArrivalTurn)
        assertTrue(CharacterStateSource.isAtCourt(arrivedOfficer))
        assertTrue(CharacterAppearanceSystem.canAppearInPalace(afterArrival, "yue_fei", PalaceIds.CHUIGONG))
    }

    // 7. 朝会出席名单不会出现同人物重复实例
    @Test
    fun courtAttendeesHaveNoDuplicateInstances() {
        val liGang = officer("li_gang", "李纲", status = OfficerStatus.IN_COURT, currentCityId = "linan")
        val zhaoDing = officer("zhao_ding", "赵鼎", status = OfficerStatus.IN_COURT, currentCityId = "linan")
        val yueFei = officer("yue_fei", "岳飞", status = OfficerStatus.DEPLOYED, currentCityId = "xiangyang")
        val st = state(listOf(liGang, zhaoDing, yueFei))

        val attendeeIds = st.officers
            .filter { CharacterAppearanceSystem.canAppearInPalace(st, it.id, PalaceIds.CHUIGONG) }
            .map { it.id }

        assertEquals("出席名单不应有重复", attendeeIds.distinct().size, attendeeIds.size)
        assertTrue(attendeeIds.contains("li_gang"))
        assertTrue(attendeeIds.contains("zhao_ding"))
        assertFalse("外任的岳飞不应混入出席名单", attendeeIds.contains("yue_fei"))
    }
}

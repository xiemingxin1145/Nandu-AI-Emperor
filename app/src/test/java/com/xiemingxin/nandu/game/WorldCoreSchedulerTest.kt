package com.xiemingxin.nandu.game

import org.junit.Test
import org.junit.Assert.*

/**
 * WORLD-CORE-001：待办生命周期 + 人物调度器测试。
 */
class WorldCoreSchedulerTest {

    private fun songCapital(gold: Int = 80000, grain: Int = 200000) = City(
        "yingtianfu", "应天府", "song", troops = 20000, defense = 60, grain = grain, gold = gold, isCapital = true
    )

    private fun songFrontier() = City(
        "kaifeng", "开封", "song", troops = 15000, defense = 55, grain = 50000, gold = 10000, controlState = "FRONTLINE"
    )

    private fun jinCity() = City("taiyuan", "太原", "jin", troops = 20000, defense = 70, grain = 80000, gold = 20000)

    private fun officer(id: String, currentCityId: String = "yingtianfu", status: OfficerStatus = OfficerStatus.IN_COURT) = Officer(
        id = id, name = id, faction = "宋廷", command = 80, force = 80, strategy = 80,
        politics = 60, loyalty = 90, currentCityId = currentCityId, status = status
    )

    // ══════ 待办生命周期 ══════

    @Test
    fun dismissedTaskDoesNotReappearWithinCooldownWhenNotWorsened() {
        var state = GameState().copy(turn = 5, gold = 30000, grain = 145000) // MEDIUM 级钱粮待办
        val before = PalaceTaskSystem.generate(state)
        val fiscalBefore = before.first { it.signature == "fiscal" }
        assertEquals(TaskSeverity.MEDIUM, fiscalBefore.severity)

        state = PalaceTaskSystem.markDismissed(state, "fiscal", fiscalBefore.severity)
        state = state.copy(turn = 6) // 冷却期内，情况没有变化（甚至没变差）

        val after = PalaceTaskSystem.generate(state)
        assertFalse("冷却期内、未恶化，不应重新出现同一条待办", after.any { it.signature == "fiscal" })
    }

    @Test
    fun dismissedTaskReappearsWhenSeverityWorsens() {
        var state = GameState().copy(turn = 5, gold = 30000, grain = 145000) // MEDIUM 级钱粮待办
        state = PalaceTaskSystem.markDismissed(state, "fiscal", TaskSeverity.MEDIUM)
        state = state.copy(turn = 6, grain = 50000) // 情况恶化到 HIGH 级

        val tasks = PalaceTaskSystem.generate(state)
        assertTrue("情况明显恶化时，即使在冷却期内也应该重新提示", tasks.any { it.signature == "fiscal" })
    }

    @Test
    fun dismissedTaskReappearsAfterCooldownExpires() {
        var state = GameState().copy(turn = 1, gold = 30000, grain = 145000)
        state = PalaceTaskSystem.markDismissed(state, "fiscal", TaskSeverity.MEDIUM)
        state = state.copy(turn = 10) // 远超冷却期，情况也没变

        val tasks = PalaceTaskSystem.generate(state)
        assertTrue("超过冷却期后，即使情况没变也应该重新提示", tasks.any { it.signature == "fiscal" })
    }

    @Test
    fun badgeCountStillMatchesTaskListAfterDismissFiltering() {
        // 回归：确认新增的冷却过滤没有破坏 countByPalace 与 tasksForPalace 的一致性。
        var state = GameState().copy(turn = 5, jinThreat = 92, gold = 25000, courtStability = 32)
        state = PalaceTaskSystem.markDismissed(state, "fiscal", TaskSeverity.URGENT)
        val counts = PalaceTaskSystem.countByPalace(state)
        PalaceRegistry.palaces.forEach { palace ->
            assertEquals(counts[palace.id] ?: 0, PalaceTaskSystem.tasksForPalace(state, palace.id).size)
        }
    }

    // ══════ 人物调度器 OfficerDispatchSystem ══════

    @Test
    fun dispatchAcrossCitiesStartsRealTravelNotTeleport() {
        val zongZe = officer("zong_ze", currentCityId = "yingtianfu")
        val state = GameState(
            turn = 5,
            cities = listOf(songCapital(), songFrontier()),
            officers = listOf(zongZe)
        )

        val result = OfficerDispatchSystem.dispatch(
            state, "zong_ze", "kaifeng", OfficerStatus.DEPLOYED, postTitle = "东京留守"
        )
        assertTrue(result is OfficerDispatchSystem.DispatchResult.Success)
        val newState = (result as OfficerDispatchSystem.DispatchResult.Success).newState
        val zongZeAfter = newState.officers.first { it.id == "zong_ze" }

        // 没有瞬移：还在原城，状态还没变成 DEPLOYED，只是记录了在途状态
        assertEquals("yingtianfu", zongZeAfter.currentCityId)
        assertEquals(OfficerStatus.IN_COURT, zongZeAfter.status)
        assertNotNull("应该记录赶路目的地", zongZeAfter.travelDestinationCityId)
        assertEquals("kaifeng", zongZeAfter.travelDestinationCityId)
        assertEquals(OfficerStatus.DEPLOYED, zongZeAfter.travelArrivalStatus)
        assertEquals("东京留守", zongZeAfter.travelArrivalPostTitle)

        // 途中不能出现在朝会
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(newState, "zong_ze", PalaceIds.CHUIGONG))
    }

    @Test
    fun arrivalGrantsRealPostAndRemovesFromCourtPermanently() {
        val zongZe = officer("zong_ze", currentCityId = "yingtianfu")
        var state = GameState(
            turn = 5,
            cities = listOf(songCapital(), songFrontier()),
            officers = listOf(zongZe)
        )
        val dispatchResult = OfficerDispatchSystem.dispatch(
            state, "zong_ze", "kaifeng", OfficerStatus.DEPLOYED, postTitle = "东京留守"
        ) as OfficerDispatchSystem.DispatchResult.Success
        state = dispatchResult.newState
        val arrivalTurn = state.officers.first().travelArrivalTurn!!

        // 还没到旬数：tick 不应生效
        val notYetArrived = CharacterTravelSystem.tickArrivals(state.copy(turn = arrivalTurn - 1))
        assertEquals(OfficerStatus.IN_COURT, notYetArrived.first.officers.first().status)

        // 抵达那一旬：真正履职生效
        val (arrivedState, reports) = CharacterTravelSystem.tickArrivals(state.copy(turn = arrivalTurn))
        val zongZeAfter = arrivedState.officers.first { it.id == "zong_ze" }
        assertEquals(OfficerStatus.DEPLOYED, zongZeAfter.status)
        assertEquals("kaifeng", zongZeAfter.currentCityId)
        assertNull(zongZeAfter.travelDestinationCityId)
        assertTrue(reports.any { it.contains("东京留守") })
        // 履职记录真的写进了 cityGarrisons，不是只改了个 status 字面量
        assertEquals("zong_ze", arrivedState.cityGarrisons["kaifeng"])
        // 履职之后，不再能肉身出现在应天朝会
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(arrivedState, "zong_ze", PalaceIds.CHUIGONG))
    }

    @Test
    fun dispatchRejectsNonSongTargetCity() {
        val officer1 = officer("li_gang")
        val state = GameState(turn = 5, cities = listOf(songCapital(), jinCity()), officers = listOf(officer1))
        val result = OfficerDispatchSystem.dispatch(state, "li_gang", "taiyuan", OfficerStatus.DEPLOYED)
        assertTrue(result is OfficerDispatchSystem.DispatchResult.Failure)
    }

    @Test
    fun dispatchRejectsOfficerAlreadyTraveling() {
        val traveling = officer("zong_ze").copy(travelDestinationCityId = "kaifeng", travelArrivalTurn = 8)
        val state = GameState(turn = 5, cities = listOf(songCapital(), songFrontier()), officers = listOf(traveling))
        val result = OfficerDispatchSystem.dispatch(state, "zong_ze", "kaifeng", OfficerStatus.DEPLOYED)
        assertTrue("已经在赶路的人不能被重复派遣", result is OfficerDispatchSystem.DispatchResult.Failure)
    }

    @Test
    fun dispatchRejectsHiddenOrCaptiveOfficer() {
        val hidden = officer("wu_jie", status = OfficerStatus.NOT_YET_RELEVANT)
        val state = GameState(turn = 5, cities = listOf(songCapital(), songFrontier()), officers = listOf(hidden))
        val result = OfficerDispatchSystem.dispatch(state, "wu_jie", "kaifeng", OfficerStatus.DEPLOYED)
        assertTrue(result is OfficerDispatchSystem.DispatchResult.Failure)
    }

    @Test
    fun dispatchToSameCityTakesEffectImmediatelyWithoutTravel() {
        val hanShizhong = officer("han_shizhong", currentCityId = "yingtianfu")
        val state = GameState(turn = 5, cities = listOf(songCapital()), officers = listOf(hanShizhong))
        val result = OfficerDispatchSystem.dispatch(
            state, "han_shizhong", "yingtianfu", OfficerStatus.IN_CAPITAL, postTitle = "御营都统制"
        ) as OfficerDispatchSystem.DispatchResult.Success
        val after = result.newState.officers.first()
        assertEquals(OfficerStatus.IN_CAPITAL, after.status)
        assertNull("同城任命不需要赶路", after.travelDestinationCityId)
    }

    // ══════ 存档往返 ══════

    @Test
    fun dismissedTaskMemoryAndTravelFieldsSurviveSaveRoundTrip() {
        val zongZe = officer("zong_ze")
        var state = GameState(
            turn = 5,
            cities = listOf(songCapital(), songFrontier()),
            officers = listOf(zongZe)
        )
        state = PalaceTaskSystem.markDismissed(state, "fiscal", TaskSeverity.HIGH)
        val dispatchResult = OfficerDispatchSystem.dispatch(
            state, "zong_ze", "kaifeng", OfficerStatus.DEPLOYED, postTitle = "东京留守"
        ) as OfficerDispatchSystem.DispatchResult.Success
        state = dispatchResult.newState

        val exported = GameSaveCodec.export(state)
        val imported = GameSaveCodec.import(exported).getOrThrow()

        assertEquals(1, imported.dismissedTaskSignatures.size)
        assertEquals(TaskSeverity.HIGH.ordinal, imported.dismissedTaskSignatures["fiscal"]?.severityOrdinal)

        val restoredZongZe = imported.officers.first { it.id == "zong_ze" }
        assertEquals("kaifeng", restoredZongZe.travelDestinationCityId)
        assertEquals(OfficerStatus.DEPLOYED, restoredZongZe.travelArrivalStatus)
        assertEquals("东京留守", restoredZongZe.travelArrivalPostTitle)
    }
}

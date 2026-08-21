package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.ai.NpcResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldPresentationPolicyTest {
    private val state = GameState()

    private fun result(clarification: Boolean = false): EdictResult = EdictResult(
        summary = "经营河北，修整边防",
        commands = listOf(EdictCommand(type = "repair_city", officerId = "li_gang", cityId = "yingtianfu")),
        npcResponses = listOf(
            NpcResponse("li_gang", "support", "请先固应天城防。"),
            NpcResponse("zong_ze", "concerned", "河北钱粮亦不可缓。")
        ),
        clarificationNeeded = clarification,
        clarificationHint = if (clarification) "请明确军费上限。" else ""
    )

    @Test
    fun councilOpinionsCanBeIndividuallySelectedAndRemoved() {
        val selected = ImperialDecision().toggleOfficer("li_gang")
        assertTrue("li_gang" in selected.selectedOfficerIds)
        assertFalse("li_gang" in selected.toggleOfficer("li_gang").selectedOfficerIds)
    }

    @Test
    fun severalMinistersCanBeSelectedTogether() {
        val selected = ImperialDecision().toggleOfficer("li_gang").toggleOfficer("zong_ze")
        assertEquals(setOf("li_gang", "zong_ze"), selected.selectedOfficerIds)
    }

    @Test
    fun synthesizeAdoptsEveryActualReturnedOpinion() {
        val decision = ImperialDecision().synthesize(result())
        assertTrue(decision.synthesizeOpinions)
        assertEquals(setOf("li_gang", "zong_ze"), decision.selectedOfficerIds)
    }

    @Test
    fun emperorMustChooseAnOpinionBeforeExecution() {
        assertFalse(ImperialDecision().canExecute(result()))
        assertTrue(ImperialDecision().toggleOfficer("li_gang").canExecute(result()))
    }

    @Test
    fun clarificationAlwaysBlocksExecutionEvenAfterSelectingAnOpinion() {
        assertFalse(ImperialDecision().toggleOfficer("li_gang").canExecute(result(clarification = true)))
    }

    @Test
    fun amendmentReturnsDecisionToAnEditableState() {
        assertTrue(ImperialDecision().toggleOfficer("li_gang").requestAmendment().amendmentRequested)
    }

    @Test
    fun noCommandsCannotBeApproved() {
        val empty = result().copy(commands = emptyList())
        assertFalse(ImperialDecision().synthesize(empty).canExecute(empty))
    }

    @Test
    fun unchangedWorldCannotProduceInventedActions() {
        val replay = WorldPresentationPolicy.replay(state, state.copy(turn = state.turn + 1))
        assertTrue(replay.actions.isEmpty())
    }

    @Test
    fun actualEnemyMovementCreatesOnlyAStateBackedMapAction() {
        val oldArmy = state.armies.first { it.id == "army_jin_kaifeng" }
        val neighbor = MapData.neighborsOf(oldArmy.currentCityId).first { node -> state.cities.any { it.id == node } }
        val moved = oldArmy.copy(currentCityId = neighbor, routeNodeIds = listOf(oldArmy.currentCityId, neighbor))
        val after = state.copy(armies = state.armies.map { if (it.id == moved.id) moved else it })
        val action = WorldPresentationPolicy.replay(state, after).actions.single()

        assertEquals(WorldTurnActionKind.MARCH, action.kind)
        assertEquals("jin", action.factionId)
        assertEquals(listOf(oldArmy.currentCityId, neighbor), action.routeNodeIds)
        assertFalse(action.detail.contains(oldArmy.id))
    }

    @Test
    fun nonAdjacentMovementNeverInventsAMapRoute() {
        val oldArmy = state.armies.first { it.id == "army_jin_kaifeng" }
        val moved = oldArmy.copy(currentCityId = "linan", routeNodeIds = emptyList())
        val after = state.copy(armies = state.armies.map { if (it.id == moved.id) moved else it })

        assertTrue(WorldPresentationPolicy.replay(state, after).actions.single().routeNodeIds.isEmpty())
    }

    @Test
    fun actualCityCaptureIsReportedUsingFactionAndCityNames() {
        val city = state.cities.first { it.owner == "song" && it.id != "yingtianfu" }
        val after = state.copy(cities = state.cities.map { if (it.id == city.id) it.copy(owner = "jin") else it })
        val action = WorldPresentationPolicy.replay(state, after).actions.single()

        assertEquals(WorldTurnActionKind.CITY_CAPTURE, action.kind)
        assertTrue(action.detail.contains(city.name))
        assertTrue(action.detail.contains("金国"))
        assertFalse(action.detail.contains(city.id))
    }

    @Test
    fun realResupplyRequiresBothResourceChangeAndSupplyRecord() {
        val previous = state.armies.first { it.id == "army_jin_kaifeng" }.copy(supplyLevel = 40)
        val before = state.copy(armies = state.armies.map { if (it.id == previous.id) previous else it })
        val replenished = previous.copy(supplyLevel = 70, lastSuppliedTurn = state.turn)
        val after = before.copy(armies = before.armies.map { if (it.id == replenished.id) replenished else it })

        assertEquals(WorldTurnActionKind.RESUPPLY, WorldPresentationPolicy.replay(before, after).actions.single().kind)
    }

    @Test
    fun seasonCutsceneOnlyAppearsWhenSeasonActuallyChanges() {
        assertNull(WorldPresentationPolicy.seasonalTransition(state, state.copy(turn = state.turn + 1)))

        val autumn = state.copy(season = Season.AUTUMN, calendar = GameCalendar(month = 7))
        val transition = WorldPresentationPolicy.seasonalTransition(state, autumn)
        assertNotNull(transition)
        assertEquals("videos/seasons/V06_season_autumn.mp4", transition!!.videoPath)
        assertEquals("ui_textures/season_autumn_bg.webp", transition.fallbackImagePath)
        assertNull(WorldPresentationPolicy.seasonalTransition(autumn, autumn.copy(turn = autumn.turn + 1)))
    }

    @Test
    fun allFourSeasonsHaveExistingVideoAndStaticCgFallbackSlots() {
        Season.values().forEach { season ->
            val previous = if (season == Season.SPRING) Season.WINTER else Season.values()[season.ordinal - 1]
            val transition = WorldPresentationPolicy.seasonalTransition(
                state.copy(season = previous),
                state.copy(season = season)
            )
            assertNotNull(transition)
            assertTrue(transition!!.videoPath.startsWith("videos/seasons/"))
            assertTrue(transition.fallbackImagePath.startsWith("ui_textures/season_"))
        }
    }

    @Test
    fun commandPreviewShowsOfficerAndCityNamesWithoutInternalIds() {
        val text = WorldPresentationPolicy.commandDescription(state, result().commands.single())
        assertTrue(text.contains("李纲"))
        assertTrue(text.contains("应天府"))
        assertFalse(text.contains("li_gang"))
        assertFalse(text.contains("yingtianfu"))
    }

    @Test
    fun reportHumanizationNeverExposesInternalKeys() {
        val text = WorldPresentationPolicy.humanizeReport(state, "zong_ze 于 yingtianfu 处理 zong_ze_loyalty")
        assertTrue(text.contains("宗泽"))
        assertTrue(text.contains("应天府"))
        assertFalse(text.contains("zong_ze"))
        assertFalse(text.contains("yingtianfu"))
    }
}

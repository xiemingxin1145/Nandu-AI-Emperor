package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.ai.NpcResponse
import com.xiemingxin.nandu.ai.WorldTurnPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ImperialMandateIntegrationTest {
    private val state = GameState()
    private val northernOrder = "命宗泽经营北线，准便宜从事，自行募兵修城、调度所属军队，军费以三万贯为限；不得擅调江淮守军，不得主动发动大战。"

    @Test
    fun imperialOrderBecomesPersistentBoundedAuthorization() {
        val mandate = ImperialMandatePolicy.draft(state, northernOrder)

        assertNotNull(mandate)
        assertEquals("zong_ze", mandate!!.responsibleOfficerId)
        assertEquals(MandateAutonomyLevel.DISCRETIONARY, mandate.autonomyLevel)
        assertEquals(30_000, mandate.budgetGold)
        assertTrue(MandateActionKind.RECRUIT in mandate.allowedActions)
        assertTrue(MandateActionKind.REPAIR_DEFENSE in mandate.allowedActions)
        assertTrue(MandateActionKind.REPOSITION_ARMY in mandate.allowedActions)
        assertFalse(MandateActionKind.INITIATE_BATTLE in mandate.allowedActions)
        assertTrue("yingtianfu" in mandate.regionCityIds)
    }

    @Test
    fun ordinaryOneOffImperialOrderDoesNotInventMandate() {
        assertNull(ImperialMandatePolicy.draft(state, "命李纲修缮应天府城防。"))
    }

    @Test
    fun captiveOrUnavailableOfficerCannotReceiveAuthority() {
        assertNull(ImperialMandatePolicy.draft(state, "命秦桧经营北线，准便宜从事，自行募兵，军费三万贯。"))
        assertNull(ImperialMandatePolicy.draft(state, "命岳飞经营北线，准便宜从事，自行募兵，军费三万贯。"))
    }

    @Test
    fun positiveBattleAuthorizationDoesNotOverrideExplicitProhibition() {
        val mandate = ImperialMandatePolicy.draft(state,
            "命宗泽经营北线，准便宜从事，自行募兵，可主动进攻，但不得主动交战，军费三万贯。")!!
        assertFalse(MandateActionKind.INITIATE_BATTLE in mandate.allowedActions)
    }

    @Test
    fun longTermOrderWithoutImmediateModelCommandCanStillBeApproved() {
        val result = EdictResult("经营北线", emptyList(), listOf(NpcResponse("zong_ze", "support", "臣愿奉诏经营。")))
        val selected = ImperialDecision().toggleOfficer("zong_ze")

        assertFalse(selected.canExecute(result))
        assertTrue(selected.canExecute(result, hasLongTermMandate = true))
        assertFalse(selected.canExecute(result.copy(clarificationNeeded = true), hasLongTermMandate = true))
    }

    @Test
    fun mandateCancelsConflictingScriptedTeleportIntoEnemyCity() {
        val mandate = ImperialMandatePolicy.draft(state, northernOrder)!!
        val authorized = ImperialMandateSystem.issue(state, mandate)
        val zongZe = authorized.officers.first { it.id == "zong_ze" }

        assertEquals("yingtianfu", zongZe.currentCityId)
        assertNull(zongZe.scheduledTurn)
        assertEquals("yingtianfu", CharacterTravelSystem.tickScheduledTransitions(authorized.copy(turn = 3))
            .first.officers.first { it.id == "zong_ze" }.currentCityId)
    }

    @Test
    fun directImperialOrderImmediatelyOverridesConflictingAutomation() {
        val mandate = ImperialMandatePolicy.draft(state, northernOrder)!!
        val authorized = ImperialMandateSystem.issue(state, mandate)
        val (overridden, reports) = ImperialMandatePolicy.prioritizeManualCommands(
            authorized, listOf(EdictCommand("move_army", officerId = "zong_ze", toCityId = "shouchun"))
        )

        assertFalse(overridden.imperialMandates.single().isActive)
        assertTrue(reports.single().contains("宗泽"))
        assertFalse(reports.single().contains("zong_ze"))
    }

    @Test
    fun realDelegatedRecruitmentAppearsInWorldReplayWithoutInternalIds() {
        val authorized = ImperialMandateSystem.issue(state, ImperialMandatePolicy.draft(state, northernOrder)!!)
        val result = WorldAiTurnExecutor.execute(authorized, WorldTurnPlan())
        val replay = WorldPresentationPolicy.replay(authorized, result.newState, result.reports)
        val recruitment = replay.actions.first { it.kind == WorldTurnActionKind.RECRUIT }

        assertTrue(recruitment.detail.contains("宗泽"))
        assertTrue(recruitment.detail.contains("募兵"))
        assertEquals("yingtianfu", recruitment.targetCityId)
        assertFalse(recruitment.detail.contains("zong_ze"))
        assertFalse(recruitment.detail.contains("recruit_troops"))
    }

    @Test
    fun recordsAloneCannotFabricateWorldReplayActions() {
        val record = MandateExecutionRecord(state.turn, "mandate_1", "zong_ze", MandateActionKind.RECRUIT,
            "宗泽奉旨于应天募兵三千", true)
        val unchanged = state.copy(mandateExecutionLog = listOf(record))

        assertTrue(WorldPresentationPolicy.replay(state, unchanged).actions.isEmpty())
    }

    @Test
    fun actualFundedDefenseRepairAppearsOnWorldMap() {
        val city = state.cities.first { it.id == "yingtianfu" }
        val after = state.copy(gold = state.gold - 400,
            cities = state.cities.map { if (it.id == city.id) it.copy(defense = it.defense + 5) else it })
        val action = WorldPresentationPolicy.replay(state, after).actions.single()

        assertEquals(WorldTurnActionKind.REPAIR_DEFENSE, action.kind)
        assertTrue(action.detail.contains("应天府"))
    }

    @Test
    fun playerFacingRestrictionsContainNamesRatherThanTechnicalKeys() {
        val mandate = ImperialMandatePolicy.draft(state, northernOrder)!!
        val description = ImperialMandatePolicy.describeTerritory(state, mandate) +
            ImperialMandatePolicy.describeRestrictions(state, mandate)

        assertTrue(description.contains("不得主动交战"))
        assertFalse(description.contains("zong_ze"))
        assertFalse(description.contains("yingtianfu"))
        assertFalse(description.contains("recruit_troops"))
    }

    @Test
    fun chineseAndArabicMilitaryBudgetsAreParsedWithoutInventingMoney() {
        assertEquals(30_000, ImperialMandatePolicy.parseAmount("三万"))
        assertEquals(12_500, ImperialMandatePolicy.parseAmount("一万二千五百"))
        assertEquals(30_000, ImperialMandatePolicy.parseAmount("30,000"))
        assertNull(ImperialMandatePolicy.parseAmount("若干"))
    }

    @Test
    fun authorizedArmyAutonomouslyChoosesFriendlyFrontierAlongExistingRoads() {
        val commander = state.officers.first { it.id == "zong_ze" }.copy(status = OfficerStatus.DEPLOYED,
            scheduledStatus = null, scheduledCityId = null, scheduledTurn = null)
        val army = state.armies.first { it.id == "army_song_linan" }.copy(commanderId = commander.id,
            name = "宗泽部", troops = 5000, supplyLevel = 90)
        val mandate = ImperialMandate("mandate_move", 1, goal = "增援江淮北线", responsibleOfficerId = commander.id,
            regionCityIds = setOf("yingtianfu", "chuzhou", "yangzhou"),
            autonomyLevel = MandateAutonomyLevel.DISCRETIONARY,
            allowedActions = setOf(MandateActionKind.REPOSITION_ARMY), budgetGold = 10000, budgetGrain = 20000)
        val before = state.copy(turn = 3,
            officers = state.officers.map { if (it.id == commander.id) commander else it },
            armies = state.armies.map { if (it.id == army.id) army else it },
            imperialMandates = listOf(mandate))
        val result = WorldAiTurnExecutor.execute(before, WorldTurnPlan())
        val moving = result.newState.armies.first { it.id == army.id }

        assertEquals(ArmyStatus.MARCHING, moving.statusCode)
        assertEquals("yingtianfu", moving.currentCityId)
        assertTrue(moving.routeNodeIds.size >= 2)
        assertTrue(moving.routeNodeIds.zipWithNext().all { (from, to) -> to in MapData.neighborsOf(from) })
        assertTrue(result.newState.cities.first { it.id == moving.targetCityId }.owner == "song")
        assertEquals(MandateActionKind.REPOSITION_ARMY, result.newState.mandateExecutionLog.single().actionKind)
    }
}

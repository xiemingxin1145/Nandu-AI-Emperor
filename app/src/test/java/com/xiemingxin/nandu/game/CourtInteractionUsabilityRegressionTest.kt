package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.EdictCommand
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.ai.NpcResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 真机交互回归：大臣意见不能锁死皇帝朱批；离宫赶路者不能继续被当成在殿。 */
class CourtInteractionUsabilityRegressionTest {

    @Test
    fun executableOrderCanBeApprovedWithoutAdoptingAnyMinisterOpinion() {
        val result = EdictResult(
            interactionType = "ORDER",
            summary = "修缮广州城防",
            commands = listOf(EdictCommand(type = "repair_city", cityId = "guangzhou")),
            npcResponses = listOf(
                NpcResponse(officerId = "li_gang", attitude = "support", text = "臣以为可行。")
            ),
            clarificationNeeded = false
        )

        val decision = ImperialDecision(selectedOfficerIds = emptySet())
        assertTrue("有可执行命令时，大臣意见只能作参考，不能锁死朱批", decision.canExecute(result))
    }

    @Test
    fun incompleteOrderStillCannotBeApproved() {
        val result = EdictResult(
            interactionType = "ORDER",
            summary = "修城",
            commands = emptyList(),
            clarificationNeeded = true,
            clarificationHint = "请说明修哪座城。"
        )
        assertFalse(ImperialDecision().canExecute(result))
    }

    @Test
    fun officerLeavesCourtStatusAsSoonAsCrossCityDispatchStarts() {
        val officer = Officer(
            id = "zong_ze",
            name = "宗泽",
            faction = "宋廷",
            command = 90,
            force = 72,
            strategy = 86,
            politics = 82,
            loyalty = 95,
            currentCityId = "yingtianfu",
            status = OfficerStatus.IN_COURT
        )
        val state = GameState(
            turn = 1,
            cities = listOf(
                City("yingtianfu", "应天府", "song", 20_000, 70, 100_000, 30_000, isCapital = true),
                City("kaifeng", "开封", "song", 15_000, 65, 60_000, 20_000)
            ),
            officers = listOf(officer)
        )

        val dispatched = OfficerDispatchSystem.dispatch(
            state = state,
            officerId = "zong_ze",
            targetCityId = "kaifeng",
            arrivalStatus = OfficerStatus.DEPLOYED,
            postTitle = "东京留守",
            garrisonPost = true
        ) as OfficerDispatchSystem.DispatchResult.Success

        val traveler = dispatched.newState.officers.first { it.id == "zong_ze" }
        assertEquals(OfficerStatus.DEPLOYED, traveler.status)
        assertTrue(CharacterStateSource.isTraveling(traveler))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(dispatched.newState, "zong_ze", PalaceIds.CHUIGONG))
    }
}

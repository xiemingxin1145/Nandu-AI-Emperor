package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.WorldAction
import com.xiemingxin.nandu.ai.WorldContextFactory
import com.xiemingxin.nandu.ai.WorldTurnPlan
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldAiTurnExecutorTest {

    @Test
    fun `world ai cannot command player armies`() {
        val state = GameState()
        val before = state.armies.first { it.id == "army_song_linan" }
        val plan = WorldTurnPlan(
            strategySummary = "越权测试",
            actions = listOf(
                WorldAction(
                    type = "move_army",
                    factionId = "song",
                    armyId = before.id,
                    targetCityId = "kaifeng",
                    reason = "不应执行"
                )
            )
        )

        val result = WorldAiTurnExecutor.execute(state, plan)
        val after = result.newState.armies.first { it.id == before.id }

        assertEquals(before.currentCityId, after.currentCityId)
        assertEquals(before.statusCode, after.statusCode)
        assertTrue(result.reports.any { it.contains("不得越俎代庖") })
    }

    @Test
    fun `heuristic planner only controls non player factions`() {
        val state = GameState()
        val plan = WorldAiTurnExecutor.heuristicPlan(state)

        assertTrue(plan.actions.isNotEmpty())
        plan.actions.forEach { action ->
            val army = state.armies.firstOrNull { it.id == action.armyId }
            assertNotNull(army)
            assertFalse(army!!.ownerFactionId == "song")
        }
    }

    @Test
    fun `world context includes enemy armies but hides undiscovered people`() {
        val state = GameState()
        val context = WorldContextFactory.fromState(state)

        assertTrue(context.armies.any { it.owner == "jin" })
        assertTrue(context.armies.any { it.owner == "song" })
        // V1.1 历史 Canon：岳飞现在是 WANDERING（在野已知，非隐藏），韩世忠是 IN_CAPITAL（在京军职），
        // 两者都不属于"世界模型不该看到"的范围，应该出现在 context 里。
        assertTrue(context.officers.any { it.id == "yue_fei" })
        assertTrue(context.officers.any { it.id == "han_shizhong" })
        // 真正该被过滤掉的是：CAPTIVE（秦桧仍羁留敌营）、NOT_YET_RELEVANT（赵鼎此时尚未登场）。
        assertFalse(context.officers.any { it.id == "qin_hui" })
        assertFalse(context.officers.any { it.id == "zhao_ding" })
    }

    @Test
    fun `enemy movement is validated and does not teleport across the map`() {
        val state = GameState()
        val army = state.armies.first { it.id == "army_jin_kaifeng" }
        val plan = WorldTurnPlan(
            actions = listOf(
                WorldAction(
                    type = "move_army",
                    factionId = "jin",
                    armyId = army.id,
                    targetCityId = "linan",
                    reason = "南下施压"
                )
            )
        )

        val result = WorldAiTurnExecutor.execute(state, plan)
        val after = result.newState.armies.first { it.id == army.id }

        assertFalse(after.currentCityId == "linan")
        assertTrue(after.statusCode in setOf(ArmyStatus.MARCHING, ArmyStatus.ENGAGEMENT_PENDING, ArmyStatus.GARRISONED))
        assertTrue(after.supplyLevel <= army.supplyLevel)
    }
}

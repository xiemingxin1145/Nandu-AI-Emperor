package com.xiemingxin.nandu.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PalaceTaskCouncilLifecycleTest {

    @Test
    fun councilDecisionMarksSourceTaskHandledSoItDoesNotImmediatelyRespawn() {
        val capital = City(
            id = "yingtianfu",
            name = "应天府",
            owner = "song",
            troops = 20_000,
            defense = 60,
            grain = 20_000,
            gold = 20_000,
            isCapital = true
        )
        val state = GameState(
            turn = 5,
            gold = 80_000,
            grain = 200_000,
            cities = listOf(capital),
            officers = emptyList(),
            jinThreat = 60,
            prestige = 50
        )

        val task = PalaceTaskSystem.generate(state).firstOrNull { it.signature == "fiscal" }
        assertNotNull("真实地方缺粮应生成财政待办", task)

        val scene = CourtCouncilSystem.sceneForTask(state, task!!)
        val choice = scene.choices.first { it.id == "grain" }
        val resolved = CouncilConsequenceSystem.apply(state, scene, choice).newState

        assertTrue(resolved.dismissedTaskSignatures.containsKey("fiscal"))
        assertFalse(
            "已经正式裁断的同一待办不能关闭再打开就立刻重刷",
            PalaceTaskSystem.generate(resolved).any { it.signature == "fiscal" }
        )
    }
}

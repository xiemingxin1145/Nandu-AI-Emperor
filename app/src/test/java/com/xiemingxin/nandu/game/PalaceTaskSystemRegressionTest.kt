package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PalaceTaskSystemRegressionTest {

    @Test
    fun countBadgesAlwaysMatchActualTaskListsForAllEightPalaces() {
        val states = listOf(
            GameState(),
            GameState().copy(turn = 9, jinThreat = 92, gold = 25000, courtStability = 32),
            GameState().copy(turn = 20, jinThreat = 40, gold = 90000, courtStability = 75, prestige = 70)
        )

        states.forEach { state ->
            val counts = PalaceTaskSystem.countByPalace(state)
            PalaceRegistry.palaces.forEach { palace ->
                assertEquals(
                    "${palace.name} badge must equal actual visible task list",
                    counts[palace.id] ?: 0,
                    PalaceTaskSystem.tasksForPalace(state, palace.id).size
                )
            }
        }
    }

    @Test
    fun quietOpeningDoesNotFabricateClockworkInnerPalaceTask() {
        val state = GameState().copy(turn = 1, jinThreat = 60, gold = 80000, courtStability = 60)
        val tasks = PalaceTaskSystem.tasksForPalace(state, PalaceIds.HOUYUAN)

        assertTrue("quiet inner palace should show an explicit empty state instead of a fake periodic task", tasks.isEmpty())
    }

    @Test
    fun innerPalaceTaskAppearsOnlyFromRealWorldPressureAndExplainsCause() {
        val state = GameState().copy(turn = 2, jinThreat = 91, gold = 80000, courtStability = 60)
        val tasks = PalaceTaskSystem.tasksForPalace(state, PalaceIds.HOUYUAN)

        assertEquals(1, tasks.size)
        assertTrue(tasks.single().title.contains("军报"))
        assertTrue(tasks.single().description.contains("91"))
        assertFalse(tasks.single().title.contains("有事待闻"))
    }

    @Test
    fun everyGeneratedTaskHasVisibleTextAndCouncilChoices() {
        val state = GameState().copy(turn = 9, jinThreat = 92, gold = 25000, courtStability = 32)
        val tasks = PalaceTaskSystem.generate(state)

        assertTrue(tasks.isNotEmpty())
        tasks.forEach { task ->
            assertTrue("task ${task.id} title blank", task.title.isNotBlank())
            assertTrue("task ${task.id} description blank", task.description.isNotBlank())
            val scene = CourtCouncilSystem.sceneForTask(state, task)
            assertTrue("task ${task.id} has no actionable council choices", scene.choices.isNotEmpty())
        }
    }

    @Test
    fun tradeTaskOffersRealTradeChoice() {
        val state = GameState()
        val task = PalaceTask(
            id = "test_trade",
            palaceId = PalaceIds.ZHENGSHI,
            title = "港市商税待议",
            description = "测试贸易待办",
            severity = TaskSeverity.MEDIUM,
            source = TaskSource.TRADE
        )
        val scene = CourtCouncilSystem.sceneForTask(state, task)
        assertTrue(scene.choices.any { it.id == "trade" })
    }

    @Test
    fun everyCurrentSpecializedPalaceChoiceHitsDedicatedConsequenceBranch() {
        val base = GameState().copy(
            turn = 12,
            gold = 100000,
            grain = 300000,
            troopMorale = 60,
            courtStability = 55,
            jinThreat = 70,
            prestige = 50
        )
        val tasks = listOf(
            task(PalaceIds.CHUIGONG, TaskSource.COURT),
            task(PalaceIds.SHUMI, TaskSource.WAR_REPORT),
            task(PalaceIds.ZHENGSHI, TaskSource.FISCAL),
            task(PalaceIds.ZHENGSHI, TaskSource.TRADE),
            task(PalaceIds.WENDE, TaskSource.TALENT),
            task(PalaceIds.YUSHU, TaskSource.DIPLOMACY),
            task(PalaceIds.HUANGCHENG, TaskSource.RUMOR),
            task(PalaceIds.HOUYUAN, TaskSource.PALACE),
            task(PalaceIds.TAIMIAO, TaskSource.RITUAL)
        )

        tasks.forEach { palaceTask ->
            val scene = CourtCouncilSystem.sceneForTask(base, palaceTask)
            scene.choices.forEach { choice ->
                val result = CouncilConsequenceSystem.apply(base, scene, choice)
                val generic = "${scene.title}已裁断：${choice.preview}"
                assertFalse(
                    "${palaceTask.palaceId}/${choice.id} fell through to generic placeholder consequence",
                    result.outcomes.contains(generic)
                )
                assertNotEquals(
                    "${palaceTask.palaceId}/${choice.id} did not mutate any world state",
                    base,
                    result.newState
                )
            }
        }
    }

    private fun task(palaceId: String, source: TaskSource) = PalaceTask(
        id = "test_${palaceId}_${source.name.lowercase()}",
        palaceId = palaceId,
        title = "测试待办",
        description = "用于验证宫殿事件选择真实生效。",
        severity = TaskSeverity.MEDIUM,
        source = source,
        edictDraft = "传朕旨意：依议施行。"
    )
}

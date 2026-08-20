package com.xiemingxin.nandu.agent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UtilityDecisionEngineTest {

    @Test
    fun `high threat makes defensive option win`() {
        val defend = UtilityOption(
            id = "defend",
            payload = "defend",
            factors = listOf(UtilityFactor("threat", weight = 2.0))
        )
        val socialize = UtilityOption(
            id = "socialize",
            payload = "socialize",
            factors = listOf(UtilityFactor("socialNeed", weight = 1.0))
        )

        val choice = UtilityDecisionEngine.choose(
            metrics = mapOf("threat" to 0.9, "socialNeed" to 0.4),
            options = listOf(socialize, defend)
        )

        assertEquals("defend", choice?.option?.id)
    }

    @Test
    fun `continuity bonus prevents trivial plan thrashing`() {
        val a = UtilityOption(id = "north", payload = "north", baseScore = 0.50, continuityBonus = 0.08)
        val b = UtilityOption(id = "east", payload = "east", baseScore = 0.55, continuityBonus = 0.08)

        val choice = UtilityDecisionEngine.choose(
            metrics = emptyMap(),
            options = listOf(a, b),
            currentOptionId = "north"
        )

        assertEquals("north", choice?.option?.id)
        assertTrue((choice?.score ?: 0.0) > 0.55)
    }

    @Test
    fun `invert factor rewards low metric`() {
        val rest = UtilityOption(
            id = "rest",
            payload = "rest",
            factors = listOf(UtilityFactor("supply", weight = 1.0, invert = true))
        )
        val push = UtilityOption(
            id = "push",
            payload = "push",
            factors = listOf(UtilityFactor("supply", weight = 1.0))
        )

        val choice = UtilityDecisionEngine.choose(
            metrics = mapOf("supply" to 0.2),
            options = listOf(rest, push)
        )

        assertEquals("rest", choice?.option?.id)
    }
}

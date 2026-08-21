package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.InitialData
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryEventEffectApplierTest {

    @Test
    fun snakeCaseWorldEffectsMutateGameState() {
        val state = GameState().copy(
            cities = InitialData.cities,
            courtStability = 50,
            troopMorale = 50,
            jinThreat = 50,
            warFactionPower = 40,
            peaceFactionPower = 40
        )
        val storyEvent = eventWithEffects(
            JsonObject(
                mapOf(
                    "court_stability" to JsonPrimitive(5),
                    "troop_morale" to JsonPrimitive(7),
                    "jin_threat" to JsonPrimitive(-4),
                    "war_faction_power" to JsonPrimitive(3),
                    "peace_faction_power" to JsonPrimitive(-2)
                )
            )
        )

        val result = StoryEventEffectApplier.applyChoice(state, storyEvent, "choose")

        assertEquals(55, result.newState.courtStability)
        assertEquals(57, result.newState.troopMorale)
        assertEquals(46, result.newState.jinThreat)
        assertEquals(43, result.newState.warFactionPower)
        assertEquals(38, result.newState.peaceFactionPower)
        assertTrue(result.pendingEffects.isEmpty())
    }

    @Test
    fun nestedCityEffectsMutateTheDeclaredCityOnly() {
        val state = GameState().copy(cities = InitialData.cities)
        val linanBefore = state.cities.first { it.id == "linan" }
        val jiankangBefore = state.cities.first { it.id == "jiankang" }

        val storyEvent = eventWithEffects(
            JsonObject(
                mapOf(
                    "city" to JsonObject(
                        mapOf(
                            "linan" to JsonObject(
                                mapOf(
                                    "popularSupport" to JsonPrimitive(7),
                                    "commerce" to JsonPrimitive(-4),
                                    "defense" to JsonPrimitive(3)
                                )
                            )
                        )
                    )
                )
            )
        )

        val result = StoryEventEffectApplier.applyChoice(state, storyEvent, "choose")
        val linanAfter = result.newState.cities.first { it.id == "linan" }
        val jiankangAfter = result.newState.cities.first { it.id == "jiankang" }

        assertEquals((linanBefore.popularSupport + 7).coerceIn(0, 100), linanAfter.popularSupport)
        assertEquals((linanBefore.commerce - 4).coerceIn(0, 100), linanAfter.commerce)
        assertEquals((linanBefore.defense + 3).coerceIn(0, 100), linanAfter.defense)
        assertEquals(jiankangBefore, jiankangAfter)
    }

    @Test
    fun authorityBypassingOfficerAndTroopEffectsStayExplicitlyPending() {
        val state = GameState().copy(cities = InitialData.cities, officers = InitialData.officers)
        val storyEvent = eventWithEffects(
            JsonObject(
                mapOf(
                    "yue_fei_rank" to JsonPrimitive(1),
                    "yue_fei_troops" to JsonPrimitive(100)
                )
            )
        )

        val result = StoryEventEffectApplier.applyChoice(state, storyEvent, "choose")

        assertEquals(1, result.pendingEffects["yue_fei_rank"])
        assertEquals(100, result.pendingEffects["yue_fei_troops"])
        assertTrue(result.outcomes.any { it.contains("待接入") })
        assertEquals(state.officers, result.newState.officers)
    }

    private fun eventWithEffects(effects: JsonObject) = StoryEvent(
        eventId = "effect_test",
        title = "效果测试",
        description = "测试",
        choices = listOf(
            StoryChoice(
                id = "choose",
                text = "选择",
                effects = effects
            )
        )
    )
}

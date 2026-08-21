package com.xiemingxin.nandu.story

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.InitialData
import com.xiemingxin.nandu.game.OfficerStatus
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class StoryEventTriggerGuardTest {

    private fun baseState(): GameState = GameState().copy(
        cities = InitialData.cities,
        officers = InitialData.officers,
        jinThreat = 60,
        storyFlags = emptySet()
    )

    private fun event(
        id: String = "test_event",
        type: String = "main_story",
        trigger: JsonObject
    ) = StoryEvent(
        eventId = id,
        title = id,
        type = type,
        trigger = trigger,
        description = "test"
    )

    @Test
    fun blockedFlagSuppressesEventFromStateStoryFlags() {
        val storyEvent = event(trigger = JsonObject(mapOf(
            "blocked_flags" to JsonArray(listOf(JsonPrimitive("already_done")))
        )))

        assertEquals(1, EventDirector.candidates(baseState(), listOf(storyEvent)).size)
        assertTrue(
            EventDirector.candidates(
                baseState().copy(storyFlags = setOf("already_done")),
                listOf(storyEvent)
            ).isEmpty()
        )
    }

    @Test
    fun triggerEventMustActuallyHaveOccurred() {
        val storyEvent = event(trigger = JsonObject(mapOf(
            "trigger_event" to JsonPrimitive("edict_military_inspection")
        )))

        assertTrue(EventDirector.candidates(baseState(), listOf(storyEvent)).isEmpty())
        assertEquals(
            1,
            EventDirector.candidates(
                baseState().copy(storyFlags = setOf("edict_military_inspection")),
                listOf(storyEvent)
            ).size
        )
    }

    @Test
    fun requiredNpcAliveRejectsDeceasedOfficer() {
        val storyEvent = event(trigger = JsonObject(mapOf(
            "required_npc_alive" to JsonPrimitive("zong_ze")
        )))

        assertEquals(1, EventDirector.candidates(baseState(), listOf(storyEvent)).size)

        val deadState = baseState().copy(
            officers = InitialData.officers.map {
                if (it.id == "zong_ze") it.copy(status = OfficerStatus.DECEASED) else it
            }
        )
        assertTrue(EventDirector.candidates(deadState, listOf(storyEvent)).isEmpty())
    }

    @Test
    fun cityOwnerGuardRejectsEventAfterCityChangesHands() {
        val storyEvent = event(trigger = JsonObject(mapOf(
            "city_owner" to JsonObject(mapOf("linan" to JsonPrimitive("song")))
        )))

        assertEquals(1, EventDirector.candidates(baseState(), listOf(storyEvent)).size)
        val occupied = baseState().copy(
            cities = InitialData.cities.map { if (it.id == "linan") it.copy(owner = "jin") else it }
        )
        assertTrue(EventDirector.candidates(occupied, listOf(storyEvent)).isEmpty())
    }

    @Test
    fun structuredThreatThresholdIsEnforced() {
        val storyEvent = event(trigger = JsonObject(mapOf(
            "jin_threat_gte" to JsonPrimitive(80)
        )))

        assertTrue(EventDirector.candidates(baseState().copy(jinThreat = 79), listOf(storyEvent)).isEmpty())
        assertEquals(1, EventDirector.candidates(baseState().copy(jinThreat = 80), listOf(storyEvent)).size)
    }

    @Test
    fun zeroRandomChanceNeverEntersSelectionPool() {
        val storyEvent = event(
            type = "random_court",
            trigger = JsonObject(mapOf("random_chance" to JsonPrimitive(0.0)))
        )

        repeat(10) { seed ->
            assertTrue(
                EventDirector.selectForTurn(
                    baseState(),
                    listOf(storyEvent),
                    rng = Random(seed)
                ).isEmpty()
            )
        }
    }

    @Test
    fun requiredFlagsMayComeFromExplicitRuntimeFlags() {
        val storyEvent = event(trigger = JsonObject(mapOf(
            "required_flags" to JsonArray(listOf(JsonPrimitive("route_open")))
        )))

        assertFalse(EventDirector.candidates(baseState(), listOf(storyEvent), flags = setOf("route_open")).isEmpty())
        assertTrue(EventDirector.candidates(baseState(), listOf(storyEvent)).isEmpty())
    }
}

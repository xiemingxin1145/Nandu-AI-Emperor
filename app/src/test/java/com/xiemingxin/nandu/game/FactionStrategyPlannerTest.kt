package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FactionStrategyPlannerTest {

    @Test
    fun `low supply faction prefers resupply over reckless expansion`() {
        val base = GameState()
        val jin = base.armies.first { it.ownerFactionId == "jin" }
        val state = base.copy(
            armies = base.armies.map {
                if (it.id == jin.id) it.copy(
                    supplyLevel = 12,
                    morale = 48,
                    statusCode = ArmyStatus.GARRISONED,
                    status = ArmyStatus.GARRISONED.label,
                    targetCityId = ""
                ) else it
            }
        )

        val best = FactionStrategyPlanner.chooseBest(state, "jin")
        assertNotNull(best)
        assertEquals(StrategicIntent.RESUPPLY, best!!.intent)
        assertTrue(best.actions.all { it.type == "resupply_army" })
    }

    @Test
    fun `enemy close to own city creates defensive strategic candidate`() {
        val base = GameState()
        val jinCity = base.cities.first { it.owner == "jin" }
        val neighbor = MapData.neighborsOf(jinCity.id).first()
        val songArmy = base.armies.first { it.ownerFactionId == "song" }
        val state = base.copy(
            armies = base.armies.map {
                when (it.id) {
                    songArmy.id -> it.copy(currentCityId = neighbor, targetCityId = jinCity.id)
                    else -> it
                }
            }
        )

        val candidates = FactionStrategyPlanner.candidates(state, "jin")
        assertTrue(candidates.any { it.intent == StrategicIntent.DEFEND })
        assertTrue(FactionStrategyPlanner.assess(state, "jin").threatenedCityIds.contains(jinCity.id))
    }

    @Test
    fun `weak adjacent enemy city creates expansion opportunity`() {
        val base = GameState()
        val jinArmy = base.armies.first { it.ownerFactionId == "jin" }
        val neighborId = MapData.neighborsOf(jinArmy.currentCityId).first()
        val state = base.copy(
            cities = base.cities.map { city ->
                if (city.id == neighborId) city.copy(
                    owner = "song",
                    troops = 300,
                    defense = 5,
                    grain = 8000,
                    controlState = "FRONTLINE"
                ) else city
            },
            armies = base.armies.map {
                if (it.id == jinArmy.id) it.copy(
                    troops = 18000,
                    morale = 80,
                    supplyLevel = 90,
                    statusCode = ArmyStatus.GARRISONED,
                    status = ArmyStatus.GARRISONED.label,
                    targetCityId = ""
                ) else it
            }
        )

        val candidates = FactionStrategyPlanner.candidates(state, "jin")
        assertTrue(candidates.any {
            it.intent in setOf(StrategicIntent.EXPAND, StrategicIntent.PRESS_ADVANTAGE, StrategicIntent.RAID) &&
                it.actions.any { action -> action.targetCityId == neighborId }
        })
    }

    @Test
    fun `marching army keeps its existing campaign target`() {
        val base = GameState()
        val jinArmy = base.armies.first { it.ownerFactionId == "jin" }
        val target = base.cities.first { it.owner == "song" }
        val state = base.copy(
            armies = base.armies.map {
                if (it.id == jinArmy.id) it.copy(
                    statusCode = ArmyStatus.MARCHING,
                    status = ArmyStatus.MARCHING.label,
                    targetCityId = target.id,
                    supplyLevel = 88,
                    morale = 75
                ) else it
            }
        )

        val continuity = FactionStrategyPlanner.candidates(state, "jin")
            .firstOrNull { it.intent == StrategicIntent.CONTINUE_OPERATION }
        assertNotNull(continuity)
        assertTrue(continuity!!.actions.any { it.armyId == jinArmy.id && it.targetCityId == target.id })
    }

    @Test
    fun `stage7 planner never emits actions for player faction`() {
        val state = GameState()
        val plan = FactionStrategyPlanner.heuristicWorldPlan(state)
        assertFalse(plan.actions.isEmpty())
        plan.actions.forEach { action ->
            val army = state.armies.first { it.id == action.armyId }
            assertFalse(army.ownerFactionId == "song")
        }
        assertTrue(plan.selectedStrategyIds.isNotEmpty())
    }
}

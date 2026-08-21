package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldCoreAppointmentBridgeTest {

    private fun city(id: String, name: String, x: Int, y: Int, isCapital: Boolean = false) = City(
        id = id,
        name = name,
        owner = "song",
        troops = 20_000,
        defense = 60,
        grain = 80_000,
        gold = 20_000,
        isCapital = isCapital,
        x = x,
        y = y
    )

    private fun officer(id: String, cityId: String, status: OfficerStatus = OfficerStatus.IN_COURT) = Officer(
        id = id,
        name = id,
        faction = "宋廷",
        command = 80,
        force = 70,
        strategy = 80,
        politics = 85,
        loyalty = 90,
        currentCityId = cityId,
        status = status
    )

    @Test
    fun appointGovernorStartsTravelInsteadOfTeleporting() {
        val state = GameState(
            turn = 3,
            cities = listOf(
                city("yingtianfu", "应天府", 0, 0, true),
                city("kaifeng", "开封", 1200, 0)
            ),
            officers = listOf(officer("zhao_ding", "yingtianfu"))
        )

        val result = AppointmentSystem.appointGovernor(state, "zhao_ding", "kaifeng")
        assertTrue(result is AppointmentSystem.AppointResult.Success)
        val next = (result as AppointmentSystem.AppointResult.Success).newState
        val person = next.officers.first()

        assertEquals("yingtianfu", person.currentCityId)
        assertEquals("kaifeng", person.travelDestinationCityId)
        assertNotNull(person.travelArrivalTurn)
        assertFalse(next.cityGovernors.containsKey("kaifeng"))
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(next, person.id, PalaceIds.CHUIGONG))

        val arrived = CharacterTravelSystem.tickArrivals(next.copy(turn = person.travelArrivalTurn!!)).first
        assertEquals("kaifeng", arrived.officers.first().currentCityId)
        assertEquals("zhao_ding", arrived.cityGovernors["kaifeng"])
    }

    @Test
    fun appointGarrisonStartsTravelAndOccupiesPostOnlyOnArrival() {
        val state = GameState(
            turn = 5,
            cities = listOf(
                city("yingtianfu", "应天府", 0, 0, true),
                city("kaifeng", "开封", 1800, 0)
            ),
            officers = listOf(officer("zong_ze", "yingtianfu"))
        )

        val result = AppointmentSystem.appointGarrison(state, "zong_ze", "kaifeng")
            as AppointmentSystem.AppointResult.Success
        val next = result.newState
        val person = next.officers.first()
        assertNull(next.cityGarrisons["kaifeng"])
        assertEquals("kaifeng", person.travelDestinationCityId)

        val arrived = CharacterTravelSystem.tickArrivals(next.copy(turn = person.travelArrivalTurn!!)).first
        assertEquals("zong_ze", arrived.cityGarrisons["kaifeng"])
        assertEquals(OfficerStatus.DEPLOYED, arrived.officers.first().status)
    }

    @Test
    fun recallVacatesOldPostImmediatelyAndRequiresTravel() {
        val zongZe = officer("zong_ze", "kaifeng", OfficerStatus.DEPLOYED)
        val state = GameState(
            turn = 7,
            cities = listOf(
                city("yingtianfu", "应天府", 0, 0, true),
                city("kaifeng", "开封", 1200, 0)
            ),
            officers = listOf(zongZe),
            cityGarrisons = mapOf("kaifeng" to "zong_ze")
        )

        val result = AppointmentSystem.recallToCourt(state, "zong_ze")
            as AppointmentSystem.AppointResult.Success
        val next = result.newState
        val person = next.officers.first()

        assertFalse(next.cityGarrisons.values.contains("zong_ze"))
        assertEquals("yingtianfu", person.travelDestinationCityId)
        assertNotNull(person.travelArrivalTurn)
        assertFalse(CharacterAppearanceSystem.canAppearInPalace(next, person.id, PalaceIds.CHUIGONG))

        val arrived = CharacterTravelSystem.tickArrivals(next.copy(turn = person.travelArrivalTurn!!)).first
        assertEquals(OfficerStatus.IN_COURT, arrived.officers.first().status)
        assertEquals("yingtianfu", arrived.officers.first().currentCityId)
    }
}

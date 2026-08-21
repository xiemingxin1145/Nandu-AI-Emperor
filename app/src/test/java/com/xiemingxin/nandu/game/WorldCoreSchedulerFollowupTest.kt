package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** WORLD-CORE-001 收尾回归：文/武职类型、离任腾缺、财政假警报。 */
class WorldCoreSchedulerFollowupTest {

    private fun city(
        id: String,
        name: String,
        grain: Int,
        troops: Int = 15_000,
        isCapital: Boolean = false
    ) = City(
        id = id,
        name = name,
        owner = "song",
        troops = troops,
        defense = 60,
        grain = grain,
        gold = 20_000,
        isCapital = isCapital
    )

    private fun officer(id: String, cityId: String = "yingtianfu") = Officer(
        id = id,
        name = id,
        faction = "宋廷",
        command = 75,
        force = 60,
        strategy = 80,
        politics = 90,
        loyalty = 90,
        currentCityId = cityId,
        status = OfficerStatus.IN_COURT
    )

    @Test
    fun crossCityGovernorArrivesAsGovernorNotGarrison() {
        val zhaoDing = officer("zhao_ding")
        val state = GameState(
            turn = 5,
            cities = listOf(
                city("yingtianfu", "应天府", 200_000, isCapital = true),
                city("kaifeng", "开封", 60_000)
            ),
            officers = listOf(zhaoDing),
            cityGovernors = mapOf("yingtianfu" to "zhao_ding")
        )

        val dispatched = OfficerDispatchSystem.dispatch(
            state = state,
            officerId = "zhao_ding",
            targetCityId = "kaifeng",
            arrivalStatus = OfficerStatus.DEPLOYED,
            postTitle = "开封府主官",
            garrisonPost = false
        ) as OfficerDispatchSystem.DispatchResult.Success

        // 奉旨离任即腾旧缺；途中不应继续挂着应天府主官。
        assertFalse(dispatched.newState.cityGovernors.values.contains("zhao_ding"))
        val traveler = dispatched.newState.officers.first()
        assertNotNull(traveler.travelArrivalTurn)

        val (arrived, reports) = CharacterTravelSystem.tickArrivals(
            dispatched.newState.copy(turn = traveler.travelArrivalTurn!!)
        )
        assertEquals("zhao_ding", arrived.cityGovernors["kaifeng"])
        assertFalse(arrived.cityGarrisons.values.contains("zhao_ding"))
        assertTrue(reports.any { it.contains("开封府主官") })
    }

    @Test
    fun governorPostKindSurvivesSaveRoundTripWhileTraveling() {
        val state = GameState(
            turn = 2,
            cities = listOf(
                city("yingtianfu", "应天府", 200_000, isCapital = true),
                city("kaifeng", "开封", 60_000)
            ),
            officers = listOf(officer("zhao_ding"))
        )
        val dispatched = OfficerDispatchSystem.dispatch(
            state,
            "zhao_ding",
            "kaifeng",
            OfficerStatus.DEPLOYED,
            postTitle = "开封府主官",
            garrisonPost = false
        ) as OfficerDispatchSystem.DispatchResult.Success

        val restored = GameSaveCodec.import(GameSaveCodec.export(dispatched.newState)).getOrThrow()
        val traveler = restored.officers.first { it.id == "zhao_ding" }
        val decoded = OfficerDispatchSystem.decodeTravelPost(traveler.travelArrivalPostTitle)
        assertEquals("开封府主官", decoded.title)
        assertEquals(false, decoded.garrisonPost)

        val (arrived, _) = CharacterTravelSystem.tickArrivals(restored.copy(turn = traveler.travelArrivalTurn!!))
        assertEquals("zhao_ding", arrived.cityGovernors["kaifeng"])
        assertFalse(arrived.cityGarrisons.values.contains("zhao_ding"))
    }

    @Test
    fun legacyUnmarkedTravelPostDefaultsToGarrisonForBackwardCompatibility() {
        val decoded = OfficerDispatchSystem.decodeTravelPost("东京留守")
        assertEquals("东京留守", decoded.title)
        assertEquals(true, decoded.garrisonPost)
    }

    @Test
    fun healthyTreasuryAndHealthyCitiesDoNotCreateFiscalTask() {
        val state = GameState(
            turn = 5,
            gold = 80_000,
            grain = 200_000,
            cities = listOf(
                city("yingtianfu", "应天府", 120_000, troops = 20_000, isCapital = true),
                city("kaifeng", "开封", 50_000, troops = 15_000)
            ),
            officers = emptyList(),
            jinThreat = 60,
            prestige = 50
        )
        val tasks = PalaceTaskSystem.generate(state)
        assertFalse("不能只因大宋还有城池就永久生成财政待办", tasks.any { it.signature == "fiscal" })
    }

    @Test
    fun criticallyLowLocalCityCreatesFiscalTaskEvenWhenCentralStoresHealthy() {
        val state = GameState(
            turn = 5,
            gold = 80_000,
            grain = 200_000,
            cities = listOf(
                city("yingtianfu", "应天府", 120_000, troops = 20_000, isCapital = true),
                city("kaifeng", "开封", 20_000, troops = 15_000)
            ),
            officers = emptyList(),
            jinThreat = 60,
            prestige = 50
        )
        val fiscal = PalaceTaskSystem.generate(state).firstOrNull { it.signature == "fiscal" }
        assertNotNull(fiscal)
        assertTrue(fiscal!!.relatedCityIds.contains("kaifeng"))
        assertTrue(fiscal.description.contains("开封"))
    }
}

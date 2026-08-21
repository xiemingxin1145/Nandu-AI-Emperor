package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EastAsiaGeographyTest {
    @Test
    fun geographicBoundsMatchTheActualBasemapExtent() {
        assertEquals(GeoMapPoint(0f, 0f), EastAsiaGeography.project(GeoLocation(87f, 49f)))
        assertEquals(GeoMapPoint(16000f, 10000f), EastAsiaGeography.project(GeoLocation(145f, 15f)))
    }

    @Test
    fun geographicProjectionRoundTripsWithoutMovingCities() {
        EastAsiaGeography.cityLocations.values.forEach { location ->
            val restored = EastAsiaGeography.unproject(EastAsiaGeography.project(location))
            assertEquals(location.longitude, restored.longitude, 0.0001f)
            assertEquals(location.latitude, restored.latitude, 0.0001f)
        }
    }

    @Test
    fun everyExistingMapNodeHasOneRealGeographicAnchor() {
        assertEquals(MapData.nodeMap.keys, EastAsiaGeography.cityLocations.keys)
        assertEquals(79, EastAsiaGeography.cityLocations.size)
    }

    @Test
    fun sampleContainsAllCoreHistoricStrategicCities() {
        listOf("linan", "jiankang", "kaifeng", "luoyang", "yanjing", "taiyuan", "chengdu",
            "xiangyang", "ezhou", "yangzhou", "guangzhou", "quanzhou", "xingqing", "dali",
            "yingtianfu", "xianren_pass").forEach { id ->
            assertTrue(id, EastAsiaGeography.cityLocations.containsKey(id))
        }
    }

    @Test
    fun northSouthCityOrderingMatchesRealChineseGeography() {
        val cities = EastAsiaGeography.cityLocations
        assertTrue(cities.getValue("yanjing").latitude > cities.getValue("kaifeng").latitude)
        assertTrue(cities.getValue("kaifeng").latitude > cities.getValue("jiankang").latitude)
        assertTrue(cities.getValue("jiankang").latitude > cities.getValue("linan").latitude)
        assertTrue(cities.getValue("linan").latitude > cities.getValue("guangzhou").latitude)
    }

    @Test
    fun westEastCityOrderingMatchesRealChineseGeography() {
        val cities = EastAsiaGeography.cityLocations
        assertTrue(cities.getValue("dali").longitude < cities.getValue("chengdu").longitude)
        assertTrue(cities.getValue("chengdu").longitude < cities.getValue("xiangyang").longitude)
        assertTrue(cities.getValue("xiangyang").longitude < cities.getValue("jiankang").longitude)
        assertTrue(cities.getValue("jiankang").longitude < cities.getValue("linan").longitude)
    }

    @Test
    fun coastalCitiesAndIslandsRemainAlongTheRealEasternAndSouthernSeas() {
        val cities = EastAsiaGeography.cityLocations
        assertTrue(cities.getValue("quanzhou").longitude > cities.getValue("guangzhou").longitude)
        assertTrue(cities.getValue("mingzhou").longitude > cities.getValue("linan").longitude)
        assertTrue(cities.getValue("qiongzhou").latitude < cities.getValue("guangzhou").latitude)
        assertTrue(cities.getValue("goryeo_route").longitude > cities.getValue("mingzhou").longitude)
    }

    @Test
    fun openingCapitalIsYingtianRatherThanTheFutureHangzhouCapital() {
        val marks = EastAsiaGeography.cityMarks(GameState()).associateBy { it.id }
        assertTrue(marks.getValue("yingtianfu").capital)
        assertFalse(marks.getValue("linan").capital)
        assertTrue(marks.getValue("kaifeng").capital)
    }

    @Test
    fun cityOwnershipAndFactionColorFollowLiveGameState() {
        val opening = GameState()
        val fallen = opening.copy(cities = opening.cities.map { city ->
            if (city.id == "yingtianfu") city.copy(owner = "jin", isCapital = false) else city
        })
        val mark = EastAsiaGeography.cityMarks(fallen).first { it.id == "yingtianfu" }

        assertEquals("jin", mark.owner)
        assertEquals("金国", mark.ownerName)
        assertEquals(fallen.factions.first { it.id == "jin" }.colorArgb, mark.colorArgb)
    }

    @Test
    fun relocatedCapitalMovesWithoutChangingGeographicCoordinates() {
        val opening = GameState()
        val moved = opening.copy(
            factions = opening.factions.map { faction ->
                if (faction.id == "song") faction.copy(capitalCityId = "linan") else faction
            },
            cities = opening.cities.map { city ->
                when (city.id) {
                    "yingtianfu" -> city.copy(isCapital = false)
                    "linan" -> city.copy(isCapital = true)
                    else -> city
                }
            }
        )
        val marks = EastAsiaGeography.cityMarks(moved).associateBy { it.id }

        assertTrue(marks.getValue("linan").capital)
        assertFalse(marks.getValue("yingtianfu").capital)
        assertEquals(EastAsiaGeography.cityLocations.getValue("linan"), marks.getValue("linan").location)
    }

    @Test
    fun strategicOnlyNodesDoNotInventCityState() {
        val marks = EastAsiaGeography.cityMarks(GameState()).associateBy { it.id }

        assertFalse(marks.getValue("xingqing").actualCity)
        assertEquals("西夏", marks.getValue("xingqing").ownerName)
        assertTrue(marks.getValue("yingtianfu").actualCity)
    }

    @Test
    fun fourPoliticalOverlaysUseExplicitClosedRealGeographicPolygons() {
        assertEquals(setOf("song", "jin", "xixia", "dali"), EastAsiaGeography.territories.map { it.factionId }.toSet())
        EastAsiaGeography.territories.forEach { territory ->
            assertTrue(territory.boundary.size >= 8)
            assertEquals(territory.boundary.first(), territory.boundary.last())
            territory.boundary.forEach { location ->
                assertTrue(location.longitude in EastAsiaGeography.WEST..EastAsiaGeography.EAST)
                assertTrue(location.latitude in EastAsiaGeography.SOUTH..EastAsiaGeography.NORTH)
            }
        }
    }

    @Test
    fun existingRoadsRemainConnectedToGeographicallyLocatedNodes() {
        MapData.roads.forEach { road ->
            assertTrue("${road.fromId} -> ${road.toId}", road.fromId in EastAsiaGeography.cityLocations)
            assertTrue("${road.fromId} -> ${road.toId}", road.toId in EastAsiaGeography.cityLocations)
        }
        assertEquals(106, MapData.roads.size)
    }

    @Test
    fun seaLaneMarkersRemainWaypointsRatherThanInventedCities() {
        val marks = EastAsiaGeography.cityMarks(GameState()).associateBy { it.id }
        listOf("jiaozhi_route", "south_sea", "goryeo_route").forEach { id ->
            assertFalse(id, marks.getValue(id).actualCity)
        }
    }

    @Test
    fun allVisibleNamesRemainPlayerReadableChinese() {
        EastAsiaGeography.cityMarks(GameState()).forEach { mark ->
            assertFalse(mark.id == mark.name)
            assertFalse(mark.owner == mark.ownerName)
        }
    }

    @Test
    fun projectionClampsOutsideCoordinatesWithoutBreakingWorldBounds() {
        assertEquals(GeoMapPoint(0f, 0f), EastAsiaGeography.project(GeoLocation(0f, 90f)))
        assertEquals(GeoMapPoint(16000f, 10000f), EastAsiaGeography.project(GeoLocation(180f, -30f)))
    }
}

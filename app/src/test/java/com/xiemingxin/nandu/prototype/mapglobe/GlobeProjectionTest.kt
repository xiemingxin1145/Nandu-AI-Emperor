package com.xiemingxin.nandu.prototype.mapglobe

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobeProjectionTest {
    @Test
    fun worldCoordinatesNormalizeAndClampToFormalMapBounds() {
        assertEquals(0f to 0f, GlobeProjection.worldToUv(-500f, -200f))
        assertEquals(0.5f to 0.5f, GlobeProjection.worldToUv(8000f, 5000f))
        assertEquals(1f to 1f, GlobeProjection.worldToUv(22000f, 13000f))
    }

    @Test
    fun fullyFlattenedPointUsesTheExistingStrategicCameraModel() {
        val point = project(worldX = 10000f, worldY = 4000f, flatten = 1f)

        assertEquals(500f + (10000f - 8000f) * 0.08f, point.screenX, 0.001f)
        assertEquals(400f + (4000f - 5000f) * 0.08f, point.screenY, 0.001f)
        assertTrue(point.visible)
    }

    @Test
    fun transitionInterpolatesContinuouslyBetweenGlobeAndStrategicMap() {
        val globe = project(worldX = 9400f, worldY = 3400f, flatten = 0f)
        val flat = project(worldX = 9400f, worldY = 3400f, flatten = 1f)
        val halfway = project(worldX = 9400f, worldY = 3400f, flatten = 0.5f)

        assertEquals((globe.screenX + flat.screenX) / 2f, halfway.screenX, 0.001f)
        assertEquals((globe.screenY + flat.screenY) / 2f, halfway.screenY, 0.001f)
    }

    @Test
    fun hiddenBacksideCitiesBecomeVisibleOnlyAfterMapUnfolds() {
        val hidden = project(worldX = 16000f, worldY = 5000f, flatten = 0f, yaw = 2f)
        val unfolded = project(worldX = 16000f, worldY = 5000f, flatten = 1f, yaw = 2f)

        assertFalse(hidden.visible)
        assertTrue(unfolded.visible)
    }

    @Test
    fun globeZoomChangesSphereRadiusWithoutChangingFlatCoordinates() {
        val normal = project(worldX = 12000f, worldY = 5000f, flatten = 0f, globeZoom = 1f)
        val enlarged = project(worldX = 12000f, worldY = 5000f, flatten = 0f, globeZoom = 1.3f)
        val flatNormal = project(worldX = 12000f, worldY = 5000f, flatten = 1f, globeZoom = 1f)
        val flatEnlarged = project(worldX = 12000f, worldY = 5000f, flatten = 1f, globeZoom = 1.3f)

        assertTrue(enlarged.screenX > normal.screenX)
        assertEquals(flatNormal.screenX, flatEnlarged.screenX, 0.001f)
        assertEquals(flatNormal.screenY, flatEnlarged.screenY, 0.001f)
    }

    @Test
    fun touchTargetsStayInsideAccessibleBoundaries() {
        assertEquals(12f, GlobeProjection.hitTestRadius(1f, 0.1f, 1f), 0.001f)
        assertEquals(48f, GlobeProjection.hitTestRadius(100f, 5f, 0f), 0.001f)
        assertTrue(GlobeProjection.hitTestRadius(16f, 1f, 0f) > 12f)
    }

    @Test
    fun previewUsesEveryFormalNodeOnceAndNeverInventsCities() {
        val marks = GlobeMapWorldState.cities(GameState())

        assertEquals(MapData.nodes.size, marks.size)
        assertEquals(MapData.nodeMap.keys, marks.map { it.id }.toSet())
        marks.forEach { mark ->
            val formal = MapData.nodeMap.getValue(mark.id)
            assertEquals(formal.worldX, mark.worldX, 0f)
            assertEquals(formal.worldY, mark.worldY, 0f)
        }
    }

    @Test
    fun actualCityOwnershipOverridesHistoricalMapHints() {
        val opening = GameState()
        val changed = opening.copy(cities = opening.cities.map { city ->
            if (city.id == "yingtianfu") city.copy(owner = "jin", isCapital = false) else city
        })

        val mark = GlobeMapWorldState.cities(changed).first { it.id == "yingtianfu" }
        assertEquals("jin", mark.faction)
        assertEquals("金国", mark.factionName)
        assertEquals(changed.factions.first { it.id == "jin" }.colorArgb, mark.factionColorArgb)
    }

    @Test
    fun openingCapitalIsYingtianAndHangzhouIsNotMisrepresented() {
        val marks = GlobeMapWorldState.cities(GameState()).associateBy { it.id }

        assertTrue(marks.getValue("yingtianfu").isCapital)
        assertFalse(marks.getValue("linan").isCapital)
        assertTrue(marks.getValue("kaifeng").isCapital)
    }

    @Test
    fun relocatedCapitalFollowsTheAuthoritativeFactionAndCityState() {
        val opening = GameState()
        val relocated = opening.copy(
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
        val marks = GlobeMapWorldState.cities(relocated).associateBy { it.id }

        assertFalse(marks.getValue("yingtianfu").isCapital)
        assertTrue(marks.getValue("linan").isCapital)
    }

    @Test
    fun strategicOnlyFactionsReceivePlayerReadableNamesWithoutFakeCityState() {
        val marks = GlobeMapWorldState.cities(GameState()).associateBy { it.id }
        val xixia = marks.getValue("xingqing")

        assertEquals("西夏", xixia.factionName)
        assertFalse(xixia.hasCityState)
        assertNotEquals(xixia.faction, xixia.factionName)
        assertTrue(marks.getValue("yingtianfu").hasCityState)
    }

    @Test
    fun everyRenderedRoadUsesOnlyFormalExistingWorldNodes() {
        val markIds = GlobeMapWorldState.cities(GameState()).map { it.id }.toSet()

        MapData.roads.forEach { road ->
            assertTrue("${road.fromId} -> ${road.toId}", road.fromId in markIds)
            assertTrue("${road.fromId} -> ${road.toId}", road.toId in markIds)
        }
    }

    private fun project(
        worldX: Float,
        worldY: Float,
        flatten: Float,
        yaw: Float = 0f,
        globeZoom: Float = 1f
    ): ProjectedPoint = GlobeProjection.project(
        worldX = worldX,
        worldY = worldY,
        canvasW = 1000f,
        canvasH = 800f,
        rotYaw = yaw,
        rotPitch = 0f,
        flatten = flatten,
        cameraWorldX = 8000f,
        cameraWorldY = 5000f,
        flatZoom = 0.08f,
        globeZoom = globeZoom
    )
}

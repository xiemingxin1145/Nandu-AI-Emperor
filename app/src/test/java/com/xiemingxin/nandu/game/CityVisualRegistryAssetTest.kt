package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CityVisualRegistryAssetTest {
    private val state = GameState()
    private val cities = state.cities.associateBy { it.id }
    private val registeredIconPaths = ArtResourceRegistry.mapIconImages.values.map { it.path }.toSet()

    @Test
    fun everyFormalMapNodeResolvesToARealRegisteredMapIcon() {
        MapData.nodes.forEach { node ->
            val visual = CityVisualRegistry.visualFor(cities[node.id], node)

            assertTrue("${node.id}: ${visual.mapIconPath}", visual.mapIconPath.startsWith("images/map/icons/"))
            assertTrue("${node.id}: ${visual.mapIconPath}", visual.mapIconPath in registeredIconPaths)
        }
    }

    @Test
    fun everyKnownMapIconAliasResolvesToOneOfTheSixteenExistingAssets() {
        assertEquals(24, ArtResourceRegistry.mapIconImages.size)
        assertEquals(16, registeredIconPaths.size)
        ArtResourceRegistry.mapIconImages.forEach { (key, asset) ->
            assertTrue("$key: ${asset.path}", asset.path.startsWith("images/map/icons/"))
            assertEquals(asset.path, ArtResourceRegistry.mapIcon(key))
        }
    }

    @Test
    fun openingCapitalUsesItsSongCapitalIconWhileHangzhouIsNotACapital() {
        val yingtian = MapData.nodeMap.getValue("yingtianfu")
        val hangzhou = MapData.nodeMap.getValue("linan")

        assertEquals("images/map/icons/city_capital_song.webp", CityVisualRegistry.visualFor(cities[yingtian.id], yingtian).mapIconPath)
        assertEquals("images/city/yingtianfu.webp", CityVisualRegistry.visualFor(cities[yingtian.id], yingtian).panelBackgroundPath)
        assertNotEquals(CityVisualTier.CAPITAL, CityVisualRegistry.visualFor(cities[hangzhou.id], hangzhou).tier)
    }

    @Test
    fun dynamicCapitalFallbackUsesItsActualFactionIcon() {
        listOf(
            "song" to "city_capital_song.webp",
            "jin" to "city_capital_jin.webp",
            "xixia" to "city_capital_xixia.webp",
            "dali" to "city_capital_dali.webp"
        ).forEach { (owner, filename) ->
            val node = MapNode("unregistered_${owner}_capital", "动态都城", 100f, 100f, isCapital = true, ownerHint = owner)

            assertEquals("images/map/icons/$filename", CityVisualRegistry.visualFor(null, node).mapIconPath)
        }
    }

    @Test
    fun specializedCityBackgroundsDoNotFallBackToImaginaryIdBasedFiles() {
        listOf(
            "ezhou" to "images/city/ezhou_river.webp",
            "yangzhou" to "images/city/yangzhou_canal.webp"
        ).forEach { (cityId, expected) ->
            val node = MapData.nodeMap.getValue(cityId)

            assertEquals(expected, CityVisualRegistry.visualFor(cities[cityId], node).panelBackgroundPath)
        }
    }

    @Test
    fun everyRegisteredCityBackgroundStaysInsideTheFormalSingularCityDirectory() {
        assertEquals(31, ArtResourceRegistry.cityBackgrounds.size)
        ArtResourceRegistry.cityBackgrounds.forEach { (cityId, asset) ->
            assertTrue("$cityId: ${asset.path}", asset.path.startsWith("images/city/"))
            assertFalse("$cityId: ${asset.path}", asset.path.startsWith("images/cities/"))
        }
    }

    @Test
    fun onlySafeExistingStateDrivenMapDecorationsAreEnabled() {
        assertEquals(5, MapDecorationRegistry.activeAssets.size)
        assertEquals(5, MapDecorationRegistry.activeAssets.toSet().size)
        assertTrue(MapDecorationRegistry.activeAssets.all { it.startsWith("images/map/decorations/") })
        assertEquals(MapDecorationRegistry.songArmyBanner, MapDecorationRegistry.armyBannerFor("song"))
        assertEquals(MapDecorationRegistry.jinArmyBanner, MapDecorationRegistry.armyBannerFor("jin"))
        assertNull(MapDecorationRegistry.armyBannerFor("xixia"))
    }
}

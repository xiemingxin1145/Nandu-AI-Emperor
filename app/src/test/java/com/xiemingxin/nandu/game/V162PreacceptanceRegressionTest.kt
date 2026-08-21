package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Integration-only invariants; this suite never edits story trigger or effect logic. */
class V162PreacceptanceRegressionTest {

    @Test
    fun courtArtRegistryExposesAll54DistinctAcceptedAssets() {
        val assets = buildList {
            addAll(ArtResourceRegistry.CourtNpc.officePortraits.values)
            addAll(ArtResourceRegistry.CourtNpc.officeHalfbodies.values)
            addAll(ArtResourceRegistry.CourtNpc.namedGenericIds.map(ArtResourceRegistry.CourtNpc::namedGenericPortrait))
            addAll(ArtResourceRegistry.CourtNpc.rankAndFilePoses)
            addAll(ArtResourceRegistry.CourtNpc.crowdScenes.values)
        }

        assertEquals(54, assets.size)
        assertEquals("court art registrations must not duplicate or hide accepted assets", 54, assets.toSet().size)
        assertEquals(12, ArtResourceRegistry.CourtNpc.officePortraits.size)
        assertEquals(12, ArtResourceRegistry.CourtNpc.officeHalfbodies.size)
        assertEquals(16, ArtResourceRegistry.CourtNpc.namedGenericIds.size)
        assertEquals(8, ArtResourceRegistry.CourtNpc.rankAndFilePoses.size)
        assertEquals(6, ArtResourceRegistry.CourtNpc.crowdScenes.size)
    }

    @Test
    fun genericCourtSpokespersonHasRealRegisteredPortraitAndHalfbody() {
        val virtualOfficerId = ArtResourceRegistry.CourtNpc.officialIdBySeed("stable-council-seat")

        assertTrue(virtualOfficerId.startsWith("npc_court_"))
        assertNotEquals(ArtResourceRegistry.Fallback.portrait, ArtResourceRegistry.portraitForOfficer(virtualOfficerId))
        assertNotEquals(ArtResourceRegistry.Fallback.halfbody, ArtResourceRegistry.halfbodyForOfficer(virtualOfficerId))
        assertTrue(ArtResourceRegistry.CourtNpc.officialLabelForId(virtualOfficerId).isNotBlank())
    }

    @Test
    fun genericCourtArtSelectionIsStableAcrossRecomposition() {
        val seed = "same-turn-same-council-seat"

        assertEquals(
            ArtResourceRegistry.CourtNpc.officialIdBySeed(seed),
            ArtResourceRegistry.CourtNpc.officialIdBySeed(seed)
        )
        assertEquals(
            ArtResourceRegistry.CourtNpc.namedGenericPortraitBySeed(seed),
            ArtResourceRegistry.CourtNpc.namedGenericPortraitBySeed(seed)
        )
    }

    @Test
    fun courtDecorationDoesNotSilentlyBecomeRealWorldOfficers() {
        val state = GameState()

        assertEquals("ROSTER-001 is documentation only in this integration", 12, state.officers.size)
        assertTrue(state.officers.none { it.id.startsWith("npc_court_") || it.id.startsWith("b_") })
    }

    @Test
    fun openingCapitalIsYingtianAcrossStateMapAndVisualRegistry() {
        val state = GameState()
        val city = state.cities.single { it.id == "yingtianfu" }
        val node = MapData.nodes.single { it.id == "yingtianfu" }
        val visual = CityVisualRegistry.visualFor(city, node)

        assertTrue(city.isCapital)
        assertTrue(node.isCapital)
        assertEquals(CityVisualTier.CAPITAL, visual.tier)
        assertEquals("images/city/yingtianfu.webp", visual.panelBackgroundPath)
        assertEquals("images/city/yingtianfu.webp", ArtResourceRegistry.cityBackground(city.id))
    }

    @Test
    fun hangzhouIsNotTheOpeningCapitalInMapOrVisualTier() {
        val state = GameState()
        val city = state.cities.single { it.id == "linan" }
        val node = MapData.nodes.single { it.id == "linan" }

        assertFalse(city.isCapital)
        assertFalse(node.isCapital)
        assertNotEquals(CityVisualTier.CAPITAL, CityVisualRegistry.visualFor(city, node).tier)
    }

    @Test
    fun openingStateCannotExposeHistoricalShunchangDemo() {
        assertFalse(HistoricalBattleAvailability.forShunchang(GameState()).available)
    }
}

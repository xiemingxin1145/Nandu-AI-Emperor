package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetV3Test {

    @Test
    fun `featured officers use restored art batch`() {
        assertTrue(VisualAssetV3.portraitForOfficer("yue_fei").contains("characters/batch1/"))
        assertTrue(VisualAssetV3.halfbodyForOfficer("han_shizhong").contains("characters/batch1/"))
        assertTrue(VisualAssetV3.portraitForOfficer("qin_hui").contains("characters/batch2/"))
        assertTrue(VisualAssetV3.halfbodyForOfficer("zhao_gou").contains("characters/batch2/"))
    }

    @Test
    fun `unfeatured officer falls back to legacy registry`() {
        assertEquals(
            ArtResourceRegistry.portraitForOfficer("li_gang"),
            VisualAssetV3.portraitForOfficer("li_gang")
        )
    }

    @Test
    fun `historical event text selects restored cg`() {
        val shunchang = VisualAssetV3.eventImageFor(
            eventId = "battle_shunchang",
            type = "random_military",
            title = "顺昌危急",
            description = "金军压境",
            artHint = "城头守军"
        )
        assertNotNull(shunchang)
        assertTrue(shunchang!!.contains("event_shunchang_prewar_batch1.webp"))

        val victory = VisualAssetV3.eventImageFor(
            eventId = "yancheng_victory",
            type = "history_event",
            title = "郾城大捷",
            description = "宋军告捷",
            artHint = ""
        )
        assertNotNull(victory)
        assertTrue(victory!!.contains("event_yancheng_victory_batch2.webp"))
    }

    @Test
    fun `military event can request non looping cg video asset`() {
        val video = CgResourceRegistry.videoFor(
            eventId = "jin_army_crosses_huai",
            type = "jin_event",
            title = "金军渡淮",
            description = "前锋压境",
            artHint = ""
        )
        assertNotNull(video)
        assertEquals("video/VID-CZ-001-PREWAR-V01.mp4", video!!.path)
    }
}

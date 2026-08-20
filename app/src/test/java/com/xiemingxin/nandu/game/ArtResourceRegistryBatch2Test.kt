package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtResourceRegistryBatch2Test {
    @Test
    fun `new historical characters use batch2 art`() {
        listOf("zhao_gou", "qin_hui", "zhao_ding", "liu_qi", "wu_jie", "wanyan_zongbi")
            .forEach { id ->
                assertTrue(ArtResourceRegistry.portraitForOfficer(id).contains("/batch2/"))
                assertTrue(ArtResourceRegistry.halfbodyForOfficer(id).contains("/batch2/"))
            }
    }

    @Test
    fun `shunchang location and yancheng cg are registered`() {
        assertEquals(
            "images/locations/batch2/bg_shunchang_wall_batch2.webp",
            ArtResourceRegistry.locationBackground("shunchang_wall")
        )
        assertEquals(
            "images/events/batch2/event_yancheng_victory_batch2.webp",
            ArtResourceRegistry.eventImage("yancheng_victory_batch2")
        )
    }
}

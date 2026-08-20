package com.xiemingxin.nandu.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CgResourceRegistryTest {
    @Test
    fun `exact story event resolves prewar cg`() {
        val video = CgResourceRegistry.videoFor(
            eventId = "jin_army_crosses_huai",
            type = "jin_event",
            title = "金兵渡淮",
            description = "军报急至",
            artHint = ""
        )

        assertEquals(CgResourceRegistry.prewar.path, video?.path)
    }

    @Test
    fun `unrelated court event does not show military cg`() {
        val video = CgResourceRegistry.videoFor(
            eventId = "court_tax_debate",
            type = "random_court",
            title = "榷税之议",
            description = "群臣争论商税",
            artHint = "朝堂"
        )

        assertNull(video)
    }

    @Test
    fun `military keyword fallback requires military event type`() {
        val video = CgResourceRegistry.videoFor(
            eventId = "generated_frontier_event",
            type = "random_military",
            title = "边城压境",
            description = "金军已到城外",
            artHint = "城头"
        )

        assertEquals(CgResourceRegistry.prewar.id, video?.id)
    }
}

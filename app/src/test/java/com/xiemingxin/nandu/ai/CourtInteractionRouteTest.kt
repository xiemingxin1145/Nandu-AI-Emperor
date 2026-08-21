package com.xiemingxin.nandu.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourtInteractionRouteTest {
    @Test
    fun routeIsConsumedOnlyOnce() {
        CourtInteractionRoute.select("consult")
        assertEquals("CONSULT", CourtInteractionRoute.consume())
        assertNull(CourtInteractionRoute.consume())
    }

    @Test
    fun invalidModeDoesNotLeak() {
        CourtInteractionRoute.select("whatever")
        assertNull(CourtInteractionRoute.consume())
    }
}

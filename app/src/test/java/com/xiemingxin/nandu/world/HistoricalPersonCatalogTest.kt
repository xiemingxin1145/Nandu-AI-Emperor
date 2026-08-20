package com.xiemingxin.nandu.world

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoricalPersonCatalogTest {
    @Test
    fun `historical seed ids remain compatible with current officer ids`() {
        val people = HistoricalPersonCatalog.asMap()
        assertTrue("yue_fei" in people)
        assertTrue("han_shizhong" in people)
        assertTrue("qin_hui" in people)
        assertTrue("zhao_ding" in people)
        assertTrue("wanyan_zongbi" in people)
        assertEquals(PersonOrigin.HISTORICAL, people.getValue("yue_fei").origin)
    }
}

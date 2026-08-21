package com.xiemingxin.nandu.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorldPresentationPolicyAiFailureTest {
    @Test
    fun rawDnsFailureBecomesInWorldReport() {
        val text = WorldPresentationPolicy.humanizeReport(
            GameState(),
            "【AI自动降级】Unable to resolve host xn--demo"
        )
        assertTrue(text.contains("驿报"))
        assertFalse(text.contains("Unable"))
        assertFalse(text.contains("AI自动降级"))
    }

    @Test
    fun implementationLabelsBecomeCourtLanguage() {
        val text = WorldPresentationPolicy.humanizeReport(
            GameState(),
            "【本地战略脑】金军暂缓南下"
        )
        assertTrue(text.startsWith("【枢密院议定】"))
        assertFalse(text.contains("战略脑"))
    }
}

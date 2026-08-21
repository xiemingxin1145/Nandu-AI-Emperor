package com.xiemingxin.nandu.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSettingsConfigTest {
    @Test
    fun placeholderEndpointCanNeverEnableRealAi() {
        val config = AiEngineConfig(
            providerType = AiProviderType.CUSTOM,
            apiKey = "sk-test",
            customModel = "https://你的中转站域名/v1|deepseek-chat"
        )
        assertFalse(config.isRealAiEnabled)
        assertEquals("|deepseek-chat", config.sanitized().customModel)
    }

    @Test
    fun exampleDomainIsUiHintOnly() {
        assertFalse(isUsableCustomBaseUrl("https://api.example.com/v1"))
    }

    @Test
    fun realHttpEndpointRemainsUntouched() {
        val config = AiEngineConfig(
            providerType = AiProviderType.CUSTOM,
            customModel = "https://llmtoken.io/v1|deepseek-v4-pro"
        )
        assertTrue(config.isRealAiEnabled)
        assertEquals("https://llmtoken.io/v1|deepseek-v4-pro", config.sanitized().customModel)
    }
}

package com.xiemingxin.nandu.ai

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiJsonRecoveryTest {

    @Test
    fun extractsFirstCompleteObjectAndIgnoresChineseExplanationAfterIt() {
        val raw = """
            {"summary":"朝堂问安","commands":[],"npcResponses":[]}
            圣旨是无实际命令的朝堂问安，所以commands应为空数组。
        """.trimIndent()

        assertEquals(
            "{\"summary\":\"朝堂问安\",\"commands\":[],\"npcResponses\":[]}",
            AiJsonRecovery.firstJsonObject(raw)
        )
    }

    @Test
    fun ignoresThinkBlockAndMarkdownFence() {
        val raw = """
            <think>我们需要回答用户，并确保格式正确。</think>
            ```json
            {"interactionType":"CHAT","summary":"天下多艰","commands":[]}
            ```
            以上是结果。
        """.trimIndent()

        val recovered = AiJsonRecovery.firstJsonObject(raw)
        assertEquals(
            "{\"interactionType\":\"CHAT\",\"summary\":\"天下多艰\",\"commands\":[]}",
            recovered
        )
    }

    @Test
    fun bracesInsideStringsDoNotBreakDepthTracking() {
        val raw = "前言 {\"summary\":\"臣言{不可轻进}\",\"commands\":[]} 后记 {\"junk\":true}"
        assertEquals(
            "{\"summary\":\"臣言{不可轻进}\",\"commands\":[]}",
            AiJsonRecovery.firstJsonObject(raw)
        )
    }

    @Test
    fun partialEdictResultUsesSafeDefaults() {
        val parsed = Json { ignoreUnknownKeys = true }.decodeFromString(
            EdictResult.serializer(),
            "{\"summary\":\"天下多艰\",\"interactionType\":\"CHAT\"}"
        )

        assertEquals("天下多艰", parsed.summary)
        assertTrue(parsed.commands.isEmpty())
        assertTrue(parsed.npcResponses.isEmpty())
        assertEquals("CHAT", parsed.interactionType)
    }

    @Test
    fun returnsNullWhenModelNeverProducedJson() {
        assertEquals(null, AiJsonRecovery.firstJsonObject("我们需要回答用户，先分析当前局势。"))
    }
}

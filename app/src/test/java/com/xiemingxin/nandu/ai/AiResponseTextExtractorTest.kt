package com.xiemingxin.nandu.ai

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiResponseTextExtractorTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun obj(raw: String): JsonObject = json.parseToJsonElement(raw) as JsonObject

    @Test
    fun readsStandardChatCompletionsContent() {
        val root = obj("""{"choices":[{"message":{"content":"{\"summary\":\"ok\"}"}}]}""")
        assertEquals("{\"summary\":\"ok\"}", AiResponseTextExtractor.extract(root))
    }

    @Test
    fun readsNestedDataWrapper() {
        val root = obj("""{"data":{"choices":[{"message":{"content":"hello"}}]}}""")
        assertEquals("hello", AiResponseTextExtractor.extract(root))
    }

    @Test
    fun readsResponsesApiOutputArray() {
        val root = obj("""{"output":[{"content":[{"type":"output_text","text":"court reply"}]}]}""")
        assertEquals("court reply", AiResponseTextExtractor.extract(root))
    }

    @Test
    fun reasoningIsOnlyLastResortButJsonCanBeRecovered() {
        val root = obj("""{"choices":[{"message":{"content":"","reasoning_content":"thinking... {\"interactionType\":\"CONSULT\",\"summary\":\"x\"} tail"}}]}""")
        val text = AiResponseTextExtractor.extract(root).orEmpty()
        assertTrue(text.startsWith("{"))
        assertTrue(text.contains("\"interactionType\":\"CONSULT\""))
    }
}

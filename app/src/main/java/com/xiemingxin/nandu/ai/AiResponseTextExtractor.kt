package com.xiemingxin.nandu.ai

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * OpenAI-compatible 中转并不总把最终正文放在 choices[0].message.content。
 * 这里集中兼容常见 wrapper / content array / output array，同时把 reasoning 只当最后兜底。
 * reasoning 不直接展示给玩家；它只会回到结构恢复层，尝试从中找 JSON 或触发一次修复请求。
 */
object AiResponseTextExtractor {

    fun extract(root: JsonObject): String? {
        val choice = (root["choices"] as? JsonArray)?.firstOrNull() as? JsonObject
        val message = choice?.get("message") as? JsonObject

        listOfNotNull(
            textOf(message?.get("content")),
            textOf(choice?.get("text")),
            textOf(root["output_text"]),
            textOf(root["content"]),
            textOf(root["answer"])
        ).firstOrNull { it.isNotBlank() }?.let { return it }

        // Responses API / 部分网关：output:[{content:[{text:"..."}]}]
        textOf(root["output"])?.takeIf { it.isNotBlank() }?.let { return it }

        // 常见中转二次包装：data / result / response 中再包一层 OpenAI 响应。
        listOf("data", "result", "response").forEach { key ->
            val nested = root[key]
            when (nested) {
                is JsonObject -> extract(nested)?.takeIf { it.isNotBlank() }?.let { return it }
                is JsonArray -> nested.forEach { item ->
                    if (item is JsonObject) extract(item)?.takeIf { it.isNotBlank() }?.let { return it }
                }
                else -> Unit
            }
        }

        // 最后才看 reasoning。某些 DeepSeek 中转会把整个输出（甚至 JSON）放这里，
        // 但 reasoning 不是玩家正文，因此只交给后续 JSON recovery / repair，不直接显示。
        val reasoning = listOfNotNull(
            textOf(message?.get("reasoning_content")),
            textOf(message?.get("reasoning")),
            textOf(choice?.get("reasoning_content")),
            textOf(root["reasoning_content"])
        ).firstOrNull { it.isNotBlank() }
        if (!reasoning.isNullOrBlank()) {
            return AiJsonRecovery.firstJsonObject(reasoning) ?: reasoning
        }

        // 极少数代理会自定义字段名，但仍使用 text/content/answer/message/output_text。
        return deepFind(root, depth = 0)
    }

    private fun textOf(element: JsonElement?): String? = when (element) {
        is JsonPrimitive -> element.contentOrNull
        is JsonArray -> element.mapNotNull { part ->
            when (part) {
                is JsonPrimitive -> part.contentOrNull
                is JsonObject -> {
                    listOf("text", "content", "output_text", "answer")
                        .firstNotNullOfOrNull { key -> textOf(part[key])?.takeIf { it.isNotBlank() } }
                }
                else -> null
            }
        }.joinToString("").takeIf { it.isNotBlank() }
        is JsonObject -> {
            listOf("text", "content", "output_text", "answer", "message")
                .firstNotNullOfOrNull { key -> textOf(element[key])?.takeIf { it.isNotBlank() } }
        }
        else -> null
    }

    private fun deepFind(element: JsonElement, depth: Int): String? {
        if (depth > 4) return null
        return when (element) {
            is JsonObject -> {
                val preferred = listOf("content", "text", "answer", "output_text")
                    .firstNotNullOfOrNull { key -> textOf(element[key])?.takeIf { it.isNotBlank() } }
                preferred ?: element.entries.firstNotNullOfOrNull { (key, value) ->
                    if (key.contains("reason", ignoreCase = true)) null else deepFind(value, depth + 1)
                }
            }
            is JsonArray -> element.firstNotNullOfOrNull { deepFind(it, depth + 1) }
            else -> null
        }
    }
}

package com.xiemingxin.nandu.ai

/**
 * 小模型/中转站经常会在 JSON 前后附加解释、Markdown 或思考内容。
 * 游戏协议只需要第一个完整 JSON 对象，因此这里做确定性的本地恢复，
 * 不依赖模型百分之百听话。
 */
internal object AiJsonRecovery {

    fun firstJsonObject(raw: String): String? {
        val text = stripThinkBlocks(raw)
        var start = -1
        var depth = 0
        var inString = false
        var escaped = false

        for (index in text.indices) {
            val ch = text[index]

            if (start < 0) {
                if (ch == '{') {
                    start = index
                    depth = 1
                }
                continue
            }

            if (inString) {
                if (escaped) {
                    escaped = false
                } else {
                    when (ch) {
                        '\\' -> escaped = true
                        '"' -> inString = false
                    }
                }
                continue
            }

            when (ch) {
                '"' -> inString = true
                '{' -> depth += 1
                '}' -> {
                    depth -= 1
                    if (depth == 0) return text.substring(start, index + 1)
                }
            }
        }

        return null
    }

    fun stripThinkBlocks(raw: String): String {
        var text = raw
        val thinkRegex = Regex("(?is)<think>.*?</think>")
        text = text.replace(thinkRegex, " ")
        return text
            .replace("```json", " ", ignoreCase = true)
            .replace("```", " ")
            .trim()
    }

    fun compactDiagnostic(raw: String, maxChars: Int = 180): String =
        stripThinkBlocks(raw)
            .replace(Regex("\\s+"), " ")
            .take(maxChars)
}

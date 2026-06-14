package com.xiemingxin.nandu.ai

/**
 * OpenAI Provider 骨架
 * 接口已定，后期填充实现即可
 */
class OpenAiProvider(private val apiKey: String, private val model: String = "gpt-4o") : AiProvider {

    override val providerType = AiProviderType.OPENAI
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        // TODO: 实现OpenAI Function Calling
        // POST https://api.openai.com/v1/chat/completions
        // 使用 response_format: { type: "json_schema" } 强制JSON输出
        return Result.failure(NotImplementedError("OpenAI Provider开发中"))
    }
}

/**
 * Gemini Provider 骨架
 */
class GeminiProvider(private val apiKey: String) : AiProvider {

    override val providerType = AiProviderType.GEMINI
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        // TODO: 实现Gemini API
        // POST https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent
        // 使用 response_mime_type: "application/json"
        return Result.failure(NotImplementedError("Gemini Provider开发中"))
    }
}

/**
 * OpenRouter Provider 骨架
 * 可路由到任意模型：claude/gpt/gemini/llama/mistral等
 */
class OpenRouterProvider(
    private val apiKey: String,
    private val model: String = "anthropic/claude-sonnet-4-6"
) : AiProvider {

    override val providerType = AiProviderType.OPENROUTER
    override val isConfigured get() = apiKey.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        // TODO: OpenRouter兼容OpenAI格式
        // POST https://openrouter.ai/api/v1/chat/completions
        // Header: Authorization: Bearer $apiKey
        // 支持 response_format: json_schema
        return Result.failure(NotImplementedError("OpenRouter Provider开发中"))
    }
}

/**
 * 自定义API Provider骨架
 * 支持任意兼容OpenAI格式的自建服务
 */
class CustomApiProvider(
    private val baseUrl: String,
    private val apiKey: String,
    private val model: String
) : AiProvider {

    override val providerType = AiProviderType.CUSTOM
    override val isConfigured get() = baseUrl.isNotBlank() && model.isNotBlank()

    override suspend fun parseEdict(edictText: String, gameContext: GameContext): Result<EdictResult> {
        // TODO: POST $baseUrl/chat/completions，兼容OpenAI格式
        return Result.failure(NotImplementedError("自定义API Provider开发中"))
    }
}

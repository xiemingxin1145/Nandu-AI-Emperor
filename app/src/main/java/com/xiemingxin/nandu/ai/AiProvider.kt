package com.xiemingxin.nandu.ai

import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════
//  统一JSON协议 — 所有模型必须返回此结构
// ══════════════════════════════════════════════

@Serializable
data class EdictResult(
    val summary: String,                          // AI对圣旨的理解摘要
    val commands: List<EdictCommand>,             // 解析出的命令列表
    val npcResponses: List<NpcResponse>,          // 群臣反应
    val riskTags: List<String> = emptyList(),     // 风险标签
    val confidence: Float = 1.0f,                 // 解析置信度
    val clarificationNeeded: Boolean = false,     // 是否需要澄清
    val clarificationHint: String = ""            // 澄清提示
)

@Serializable
data class EdictCommand(
    val type: String,           // 命令类型（白名单见下）
    val officerId: String = "", // 武将ID
    val fromCityId: String = "",
    val toCityId: String = "",
    val cityId: String = "",
    val troops: Int = 0,
    val role: String = "",
    val severity: String = "",  // suppress_officer用：light/medium/severe
    val resourceFocus: String = "",
    val amount: Int = 0,
    val deadlineTurns: Int = 0
) {
    companion object {
        // ⚠️ 命令白名单 — 不在此列的一律丢弃
        val ALLOWED_TYPES = setOf(
            "dispatch_army",      // 调兵
            "assign_officer",     // 任命
            "repair_city",        // 修城
            "raise_grain",        // 筹粮
            "suppress_officer",   // 压制大臣
            "reward_officer",     // 赏赐
            "punish_officer",     // 惩处
            "move_capital"        // 迁都（后期开放）
        )

        fun isValid(type: String) = type in ALLOWED_TYPES
    }
}

@Serializable
data class NpcResponse(
    val officerId: String,
    val attitude: String,   // support / oppose / neutral / concerned
    val text: String
)

// ══════════════════════════════════════════════
//  AiProvider 抽象接口 — 所有模型实现此接口
// ══════════════════════════════════════════════

interface AiProvider {
    val providerType: AiProviderType
    val isConfigured: Boolean

    suspend fun parseEdict(
        edictText: String,
        gameContext: GameContext
    ): Result<EdictResult>
}

// 传给AI的游戏上下文（让AI知道当前局势）
data class GameContext(
    val currentTurn: Int,
    val era: String,            // e.g. "靖康元年"
    val gold: Int,
    val grain: Int,
    val troopMorale: Int,
    val courtStability: Int,
    val jinThreat: Int,
    val activeCities: List<CityContext>,
    val availableOfficers: List<OfficerContext>
)

data class CityContext(
    val id: String,
    val name: String,
    val owner: String,
    val troops: Int,
    val defense: Int
)

data class OfficerContext(
    val id: String,
    val name: String,
    val faction: String,
    val currentCityId: String,
    val status: String
)

enum class AiProviderType(val displayName: String) {
    CLAUDE("Claude (Anthropic)"),
    OPENAI("OpenAI GPT"),
    GEMINI("Google Gemini"),
    OPENROUTER("OpenRouter"),
    CUSTOM("自定义API"),
    MOCK("本地离线模拟")
}

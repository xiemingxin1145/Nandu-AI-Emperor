package com.xiemingxin.nandu.ai

import kotlinx.serialization.Serializable

// ══════════════════════════════════════════════
//  统一JSON协议 — 所有模型必须返回此结构
// ══════════════════════════════════════════════

@Serializable
data class EdictResult(
    val summary: String = "",                       // AI对玩家话语/圣旨的理解摘要
    val commands: List<EdictCommand> = emptyList(), // 解析出的命令列表；闲谈/问策可以为空
    val npcResponses: List<NpcResponse> = emptyList(), // 群臣反应；不再强迫每轮必须有人发言
    val riskTags: List<String> = emptyList(),        // 风险标签
    val confidence: Float = 1.0f,                    // 解析置信度
    val clarificationNeeded: Boolean = false,        // 是否需要澄清
    val clarificationHint: String = "",             // 澄清提示
    val interactionType: String = ""                 // CHAT / CONSULT / ORDER / CLARIFICATION；缺省时由本地推断
)

@Serializable
data class EdictCommand(
    // WORLD-CORE 合流兼容：小模型仍可能把“赴某城任某职”吐成旧 assign_officer。
    // type 改为 var 仅用于 init 中一次性归一化；之后游戏仍按普通命令字段读取。
    var type: String,           // 命令类型（白名单见下）
    val officerId: String = "", // 武将/官员ID
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
    init {
        type = normalizeLegacyAssignment(type, cityId, role)
    }

    companion object {
        // ⚠️ 命令白名单 — 不在此列的一律丢弃
        val ALLOWED_TYPES = setOf(
            "dispatch_army",      // 调兵
            "assign_officer",     // 旧兼容：泛化任命；明确城池+职务会在本地自动归一
            "repair_city",        // 修城
            "raise_grain",        // 筹粮
            "suppress_officer",   // 压制大臣
            "reward_officer",     // 赏赐
            "punish_officer",     // 惩处
            // Stage 5 战争命令
            "attack_city",        // 进攻目标城池
            "retreat_army",       // 撤退
            // Stage 4 军团命令
            "form_army",          // 组建军团
            "move_army",          // 移动/改道军团
            "disband_army",       // 解散军团
            "change_army_commander", // 更换主帅
            "resupply_army",      // 主动补给
            // Stage 3 人事命令
            "appoint_governor",  // Stage3 任命城池主官
            "appoint_garrison",  // Stage3 任命驻城守将
            "dismiss_officer",   // Stage3 免职
            "transfer_officer",  // Stage3 调任
            "recruit_officer",   // Stage3 征辟人才（需先有talentLead）
            "move_capital"       // 迁都（后期开放）
        )

        fun isValid(type: String) = type in ALLOWED_TYPES

        /**
         * 兼容便宜模型/旧 Prompt：
         * - “赵鼎赴开封任主官/知府/通判/转运”等 → appoint_governor
         * - “宗泽赴东京任留守/守将/都统/统制/镇守”等 → appoint_garrison
         * 没有明确城池或职务时保持 assign_officer，让上层继续澄清，绝不猜地点。
         */
        private fun normalizeLegacyAssignment(rawType: String, cityId: String, role: String): String {
            if (rawType != "assign_officer" || cityId.isBlank() || role.isBlank()) return rawType
            val civilKeywords = listOf("主官", "知府", "知州", "知县", "通判", "转运", "漕运", "府尹", "太守")
            val militaryKeywords = listOf("守将", "留守", "都统", "统制", "都督", "制置", "经略", "镇守", "防御", "兵马", "军")
            return when {
                civilKeywords.any(role::contains) -> "appoint_governor"
                militaryKeywords.any(role::contains) -> "appoint_garrison"
                else -> rawType
            }
        }
    }
}

@Serializable
data class NpcResponse(
    val officerId: String = "",
    val attitude: String = "neutral",   // support / oppose / neutral / concerned
    val text: String = ""
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
data class ArmyContext(
    val id: String,
    val name: String,
    val commanderName: String,
    val commanderId: String,
    val currentCityId: String,
    val troops: Int,
    val morale: Int,
    val supplyLevel: Int,
    val statusLabel: String,
    val targetCityId: String = ""
)

data class GameContext(
    val currentTurn: Int,
    val era: String,
    val gold: Int,
    val grain: Int,
    val troopMorale: Int,
    val courtStability: Int,
    val jinThreat: Int,
    val activeCities: List<CityContext>,
    val availableOfficers: List<OfficerContext>,
    // Stage 3 扩展
    val pendingRecruitLeads: List<String> = emptyList(),
    // Stage 4 扩展：让AI知道已有军团，避免重复创建
    val songArmies: List<ArmyContext> = emptyList()
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
    val status: String,
    // Stage 3 扩展：让AI知道人物职务和基本能力
    val currentRole: String = "",           // 御前待命/某城主官/某城守将/待征辟
    val commandSummary: String = "",        // 简短能力摘要供AI选将
    val loyaltyLabel: String = "",          // 忠诚等级（忠直/可信/观望/不稳）
    val isRecruitLead: Boolean = false      // 是否已获得talentLead（待征辟状态）
)

enum class AiProviderType(val displayName: String) {
    CLAUDE("Claude (Anthropic)"),
    OPENAI("OpenAI"),
    GEMINI("Google Gemini"),
    GROK("xAI Grok"),
    DEEPSEEK("DeepSeek"),
    OPENROUTER("OpenRouter"),
    CUSTOM("自定义中转"),
    MOCK("离线 Mock")
}

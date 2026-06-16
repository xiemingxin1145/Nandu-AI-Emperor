package com.xiemingxin.nandu.agent

/**
 * Agent suggestion shown to the player before any game command is executed.
 * suggestedEdict can be copied into the imperial edict input box.
 */
data class AgentSuggestion(
    val id: String,
    val title: String,
    val reason: String,
    val suggestedEdict: String,
    val commandTypes: List<String>,
    val riskTags: List<AgentRiskTag>,
    val priority: AgentPriority = AgentPriority.NORMAL
)

enum class AgentPriority {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

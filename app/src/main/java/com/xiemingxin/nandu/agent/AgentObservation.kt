package com.xiemingxin.nandu.agent

/**
 * Agent 观察层：把当前局势压缩成一段稳定输入。
 *
 * 第一阶段故意不直接依赖 GameState，避免 Agent 层和游戏数据模型强耦合。
 * 等 UI 接线时，再由 ViewModel 把 GameState 映射成这个对象。
 */
data class AgentObservation(
    val yearLabel: String = "建炎年间",
    val courtStability: Int = 50,
    val jinThreat: Int = 70,
    val grain: Int = 100_000,
    val treasury: Int = 50_000,
    val songCityCount: Int = 0,
    val jinCityCount: Int = 0,
    val frontlineCities: List<String> = emptyList(),
    val recentEvents: List<String> = emptyList()
) {
    fun riskTags(): List<AgentRiskTag> = buildList {
        if (jinThreat >= 80) add(AgentRiskTag.JIN_PRESSURE)
        if (grain < 120_000) add(AgentRiskTag.GRAIN_PRESSURE)
        if (treasury < 40_000) add(AgentRiskTag.TREASURY_PRESSURE)
        if (courtStability < 45) add(AgentRiskTag.COURT_INSTABILITY)
        if (frontlineCities.isNotEmpty()) add(AgentRiskTag.FRONTLINE_ALERT)
    }
}

enum class AgentRiskTag {
    JIN_PRESSURE,
    GRAIN_PRESSURE,
    TREASURY_PRESSURE,
    COURT_INSTABILITY,
    FRONTLINE_ALERT
}

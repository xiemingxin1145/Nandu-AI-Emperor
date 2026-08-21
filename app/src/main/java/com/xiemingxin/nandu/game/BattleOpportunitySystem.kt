package com.xiemingxin.nandu.game

/**
 * V1.6.2 STAB-001：历史战役候选可用性 —— 最小可扩展入口。
 *
 * 背景（见 docs/PROJECT_MASTER_PLAN.md「历史锚点，不是历史铁轨」）：
 * 顺昌之战之前被做成了一个"美术资产实装测试场景"，皇宫主页永远显示入口，
 * 不检查年份、战局、顺昌归属、金军威胁、刘锜状态，玩家哪怕已经灭了金国
 * 也能强行点进去。这违反了 AGENTS.md 明确列出的第一条设计原则。
 *
 * 这里不重做整个历史事件系统（那是 V1.7 HIST-001~004 的范畴），只建立一个
 * 干净、可测试的"候选战役"判断入口：给定当前 GameState，回答"这场历史战役
 * 现在算不算一个合理候选"，以及不合格时的具体原因（用于调试/未来的军报文案）。
 *
 * 后续 HIST-004 把战役从"世界态势里长出来"时，应当扩展这里的 BattleId 和判断
 * 函数，而不是重新发明一套入口。
 */
enum class BattleId { SHUNCHANG }

data class BattleAvailability(
    val battleId: BattleId,
    val available: Boolean,
    /** 人类可读的原因；available=true 时是"满足条件"的正向说明，false 时是具体卡在哪一条。 */
    val reason: String
)

object HistoricalBattleAvailability {

    private const val JIN_FACTION_ID = "jin"

    // 顺昌之战历史上发生在绍兴十年（约公历1140年，五月）。
    // 注意：当前 GameCalendar.advance() 尚未实现"建炎→绍兴"年号切换（这是已知的
    // 独立缺陷，不在本任务范围内），calendar.year 是从开局(建炎元年=1)起累计的
    // 游戏内年数，不是公历数字，也不能拿"绍兴"字符串去匹配。这里按累计年数给一个
    // 合理宽度的窗口（对应大致公历1136~1146），而不是卡死单一年份——世界允许提前
    // 或推迟，窗口只是候选前提之一，不是唯一条件。
    private const val SHUNCHANG_WINDOW_START_GAME_YEAR = 10
    private const val SHUNCHANG_WINDOW_END_GAME_YEAR = 20

    // 顺昌本身在当前地图数据里不是独立城池实体；历史上顺昌（今安徽阜阳）属淮南西路，
    // 游戏用该路重镇"寿春"(shouchun) 代表这一带防线——刘锜的初始 currentCityId
    // 正是 shouchun。若未来地图拆出独立的顺昌城池，这里应改指向那个新 id。
    private const val SHUNCHANG_PROXY_CITY_ID = "shouchun"
    private const val LIU_QI_ID = "liu_qi"

    // jinThreat 低于这个值视为"金国已无实质南侵压力"（灭国/被打残/主动罢兵均可能压低它）。
    private const val MIN_JIN_THREAT_FOR_SOUTHERN_PRESSURE = 25

    fun forShunchang(state: GameState): BattleAvailability {
        val gameYear = state.calendar.year
        if (gameYear < SHUNCHANG_WINDOW_START_GAME_YEAR || gameYear > SHUNCHANG_WINDOW_END_GAME_YEAR) {
            return unavailable("尚未进入合理历史窗口（当前 ${state.calendar.eraName}，第 $gameYear 年）")
        }

        val jinFaction = state.factions.firstOrNull { it.id == JIN_FACTION_ID }
        if (jinFaction == null || jinFaction.isDestroyed) {
            return unavailable("金国已灭亡或未在当前世界中登场，无力南侵")
        }
        if (state.controlledCityCount(JIN_FACTION_ID) == 0) {
            return unavailable("金国已无任何城池，无力南侵")
        }
        if (state.jinThreat < MIN_JIN_THREAT_FOR_SOUTHERN_PRESSURE) {
            return unavailable("金军威胁值过低（${state.jinThreat}），不具备真实南侵压力")
        }

        val songFaction = state.factions.firstOrNull { it.isPlayable }
        if (songFaction != null && jinFaction.relationWith(songFaction.id) > -20) {
            return unavailable("宋金关系（${jinFaction.relationWith(songFaction.id)}）未达高敌对/战争状态")
        }

        val city = state.cities.firstOrNull { it.id == SHUNCHANG_PROXY_CITY_ID }
            ?: return unavailable("找不到顺昌一带（$SHUNCHANG_PROXY_CITY_ID）的城池数据")
        if (city.owner != "song") {
            return unavailable("顺昌一带（${city.name}）已非宋土，此役已失去战略意义")
        }

        val liuQi = state.officers.firstOrNull { it.id == LIU_QI_ID }
            ?: return unavailable("找不到刘锜的人物数据")
        if (!CharacterStateSource.isAlive(liuQi)) {
            return unavailable("刘锜已不在人世")
        }
        if (liuQi.status !in setOf(OfficerStatus.DEPLOYED, OfficerStatus.IN_CAPITAL, OfficerStatus.IN_COURT)) {
            return unavailable("刘锜当前状态（${liuQi.status}）无法领军参与此役")
        }
        if (liuQi.currentCityId != SHUNCHANG_PROXY_CITY_ID) {
            return unavailable("刘锜此刻不在顺昌一带（现在：${liuQi.currentCityId}）")
        }
        if (CharacterStateSource.isTraveling(liuQi)) {
            return unavailable("刘锜正在赶路途中，尚未真正驻防顺昌")
        }

        return BattleAvailability(BattleId.SHUNCHANG, true, "年份、金军威胁、顺昌归属、刘锜状态均满足，可作为候选战役出现")
    }

    private fun unavailable(reason: String): BattleAvailability =
        BattleAvailability(BattleId.SHUNCHANG, false, reason)
}

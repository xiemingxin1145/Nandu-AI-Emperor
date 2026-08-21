package com.xiemingxin.nandu.game

import org.junit.Test
import org.junit.Assert.*

/**
 * V1.6.2 STAB-001：顺昌战役候选可用性测试。
 *
 * 覆盖任务要求的四项：
 * 1. 1127 开局不出现顺昌战役；
 * 2. 候选条件不足（如刘锜不在/不在顺昌一带）不出现；
 * 3. 金国已无南侵能力时不出现；
 * 4. 满足候选条件时才可出现。
 */
class BattleOpportunitySystemTest {

    private fun songFaction(relationToJin: Int = -80) = Faction(
        "song", "大宋", "宋", "赵构", "yingtianfu", "行在草创，主战主和并立",
        isPlayable = true, isAI = false, relations = mapOf("jin" to relationToJin)
    )

    private fun jinFaction(
        destroyed: Boolean = false,
        relationToSong: Int = -80
    ) = Faction(
        "jin", "金国", "金", "完颜宗弼", "kaifeng", "兵锋正盛，窥伺中原",
        relations = mapOf("song" to relationToSong), isDestroyed = destroyed
    )

    private fun jinCity() = City(
        "kaifeng", "开封", "jin", troops = 50000, defense = 90, grain = 200000, gold = 100000,
        controlState = "FALLEN"
    )

    private fun shouchun(owner: String = "song") = City(
        "shouchun", "寿春", owner, troops = 10000, defense = 66, grain = 45000, gold = 10000,
        controlState = "FRONTLINE", route = "淮南西路"
    )

    private fun liuQi(
        status: OfficerStatus = OfficerStatus.DEPLOYED,
        currentCityId: String = "shouchun",
        travelArrivalTurn: Int? = null,
        travelDestinationCityId: String? = null
    ) = Officer(
        id = "liu_qi", name = "刘锜", faction = "宋廷", command = 88, force = 88, strategy = 80,
        politics = 55, loyalty = 92, currentCityId = currentCityId, status = status,
        travelArrivalTurn = travelArrivalTurn, travelDestinationCityId = travelDestinationCityId
    )

    /** 满足全部候选条件的“基准世界”，各测试在此基础上改动单一变量。 */
    private fun readyState(gameYear: Int = 14, jinThreat: Int = 60) = GameState(
        calendar = GameCalendar(eraName = "建炎十四年", year = gameYear, month = 5, tenDay = 1),
        jinThreat = jinThreat,
        factions = listOf(songFaction(), jinFaction()),
        cities = listOf(shouchun(), jinCity()),
        officers = listOf(liuQi())
    )

    // 1. 1127 开局（真实默认 GameState）不出现顺昌战役
    @Test
    fun openingGameDoesNotOfferShunchang() {
        val state = GameState() // 真实默认开局：建炎元年，calendar.year = 1
        val result = HistoricalBattleAvailability.forShunchang(state)
        assertFalse("1127开局不应出现顺昌战役候选", result.available)
    }

    // 2. 候选条件不足（刘锜不在顺昌一带）不出现
    @Test
    fun missingLiuQiAtShouchunMakesItUnavailable() {
        val state = readyState().copy(officers = listOf(liuQi(currentCityId = "xiangyang")))
        val result = HistoricalBattleAvailability.forShunchang(state)
        assertFalse("刘锜不在顺昌一带时不应出现候选", result.available)
    }

    @Test
    fun deceasedLiuQiMakesItUnavailable() {
        val state = readyState().copy(officers = listOf(liuQi(status = OfficerStatus.DECEASED)))
        assertFalse(HistoricalBattleAvailability.forShunchang(state).available)
    }

    @Test
    fun shouchunNotOwnedBySongMakesItUnavailable() {
        val state = readyState().copy(cities = listOf(shouchun(owner = "jin"), jinCity()))
        assertFalse("顺昌一带已失守时不应出现候选", HistoricalBattleAvailability.forShunchang(state).available)
    }

    @Test
    fun liuQiStillTravelingMakesItUnavailable() {
        val state = readyState().copy(
            officers = listOf(liuQi(travelDestinationCityId = "shouchun", travelArrivalTurn = 99))
        )
        assertFalse("刘锜尚未抵达顺昌时不应出现候选", HistoricalBattleAvailability.forShunchang(state).available)
    }

    // 3. 金国已无南侵能力时不出现（灭亡 / 无城池 / 威胁值过低，三种情形分别验证）
    @Test
    fun destroyedJinMakesItUnavailable() {
        val state = readyState().copy(factions = listOf(songFaction(), jinFaction(destroyed = true)))
        assertFalse("金国已灭亡时不应出现候选", HistoricalBattleAvailability.forShunchang(state).available)
    }

    @Test
    fun jinWithNoCitiesMakesItUnavailable() {
        val state = readyState().copy(cities = listOf(shouchun())) // 去掉金国唯一城池
        assertFalse("金国已无城池时不应出现候选", HistoricalBattleAvailability.forShunchang(state).available)
    }

    @Test
    fun lowJinThreatMakesItUnavailable() {
        val state = readyState(jinThreat = 10)
        assertFalse("金军威胁值过低时不应出现候选", HistoricalBattleAvailability.forShunchang(state).available)
    }

    @Test
    fun peaceRelationMakesItUnavailable() {
        val state = readyState().copy(factions = listOf(songFaction(relationToJin = 10), jinFaction(relationToSong = 10)))
        assertFalse("宋金关系未达高敌对/战争状态时不应出现候选", HistoricalBattleAvailability.forShunchang(state).available)
    }

    @Test
    fun outsideHistoricalWindowMakesItUnavailable() {
        val tooEarly = readyState(gameYear = 3)
        assertFalse(HistoricalBattleAvailability.forShunchang(tooEarly).available)
        val tooLate = readyState(gameYear = 30)
        assertFalse(HistoricalBattleAvailability.forShunchang(tooLate).available)
    }

    // 4. 满足候选条件时才可出现
    @Test
    fun allConditionsMetOffersShunchang() {
        val state = readyState()
        val result = HistoricalBattleAvailability.forShunchang(state)
        assertTrue("条件齐备时应出现候选：${result.reason}", result.available)
        assertEquals(BattleId.SHUNCHANG, result.battleId)
    }
}

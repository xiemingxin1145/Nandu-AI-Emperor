package com.xiemingxin.nandu.game

import kotlin.random.Random

/**
 * V0.9 战役夺城系统：计算攻城胜负，推进城池控制状态。
 *
 * 控制状态推进链：
 *   攻方胜：FALLEN→CONTESTED→FRONTLINE→STABLE（金占城被宋收复方向）
 *   守方胜：STABLE→FRONTLINE→CONTESTED→FALLEN（宋城被金攻陷方向）
 *   BESIEGED围困：胜则解围转FRONTLINE，败则直接FALLEN
 */
data class BattleOutcome(
    val attackerWins: Boolean,
    val successRate: Int,        // 0-100
    val newControlState: String,
    val attackerLosses: Int,
    val defenderLosses: Int,
    val report: String
)

object BattleResolver {

    private val songProgress = listOf("FALLEN", "CONTESTED", "FRONTLINE", "STABLE")

    /** 攻方推进一级控制状态（朝STABLE方向） */
    private fun advanceForAttacker(current: String): String {
        val idx = songProgress.indexOf(current)
        return if (idx in 0 until songProgress.size - 1) songProgress[idx + 1] else current
    }

    /** 守方失利，控制状态后退一级（朝FALLEN方向） */
    private fun retreatForDefender(current: String): String {
        val idx = songProgress.indexOf(current)
        return if (idx > 0) songProgress[idx - 1] else current
    }

    /**
     * 计算一场攻城战。
     * @param attackerTroops 攻方总兵力
     * @param attackerMorale 攻方士气
     * @param commanderCommand 主将统率(影响发挥)
     * @param city 目标城池
     * @param season 季节(秋利攻、冬不利)
     * @param weather 天气
     */
    fun resolveSiege(
        attackerTroops: Int,
        attackerMorale: Int,
        commanderCommand: Int,
        city: City,
        season: Season,
        weather: WeatherType
    ): BattleOutcome {
        // 攻方战力
        var attackPower = attackerTroops * (0.7 + attackerMorale / 100.0 * 0.5)
        attackPower *= (0.8 + commanderCommand / 100.0 * 0.7)

        // 守方战力 = 守军 + 城防加成
        val defenseBonus = 1.0 + city.defense / 100.0 * 0.8
        var defendPower = city.troops * defenseBonus

        // 民心影响守方(高民心死守)
        defendPower *= (0.85 + city.popularSupport / 100.0 * 0.3)

        // 季节修正
        when (season) {
            Season.AUTUMN -> attackPower *= 1.15   // 秋高马肥，利于进攻
            Season.WINTER -> attackPower *= 0.8    // 冬季苦寒，攻城艰难
            else -> {}
        }

        // 天气修正
        when (weather) {
            WeatherType.RAIN -> attackPower *= 0.9
            WeatherType.SNOW -> attackPower *= 0.82
            WeatherType.FOG -> attackPower *= 1.1   // 雾掩攻城
            else -> {}
        }

        // 地形：关隘/山地大幅利守
        when (city.terrain) {
            "pass" -> defendPower *= 1.25
            "mountain" -> defendPower *= 1.15
            "river" -> defendPower *= 1.1
            else -> {}
        }

        val ratio = attackPower / (attackPower + defendPower)
        val successRate = (ratio * 100).toInt().coerceIn(5, 95)
        val roll = Random.nextInt(100)
        val attackerWins = roll < successRate

        // 伤亡
        val intensity = 1.0 - kotlin.math.abs(ratio - 0.5) // 势均力敌则伤亡大
        val attackerLosses = (attackerTroops * (0.1 + intensity * 0.25) * (if (attackerWins) 0.7 else 1.2)).toInt()
        val defenderLosses = (city.troops * (0.1 + intensity * 0.3) * (if (attackerWins) 1.3 else 0.6)).toInt()

        val newState = if (city.controlState == "BESIEGED") {
            if (attackerWins) "FRONTLINE" else "FALLEN"
        } else {
            if (attackerWins) advanceForAttacker(city.controlState)
            else retreatForDefender(city.controlState)
        }

        val report = buildReport(city, attackerWins, successRate, attackerLosses, defenderLosses, newState)

        return BattleOutcome(attackerWins, successRate, newState, attackerLosses, defenderLosses, report)
    }

    private fun buildReport(
        city: City, win: Boolean, rate: Int,
        aLoss: Int, dLoss: Int, newState: String
    ): String {
        val head = if (win) "捷报：${city.name}之战，王师奏凯！" else "战报：${city.name}久攻不下，将士折损。"
        val stateText = when (newState) {
            "STABLE" -> "${city.name}已克复，城池归宋，可安抚百姓、征收赋税。"
            "FRONTLINE" -> "${city.name}已成前线，敌我反复，仍需增兵固守。"
            "CONTESTED" -> "${city.name}陷入争夺，城头变幻，胜负未分。"
            "FALLEN" -> "${city.name}仍在敌手，我军暂退，徐图后举。"
            else -> ""
        }
        return "$head\n胜率${rate}% · 我军折损${aLoss / 1000}千 · 敌军折损${dLoss / 1000}千\n$stateText"
    }
}

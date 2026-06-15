package com.xiemingxin.nandu.game

import kotlin.random.Random

/**
 * V0.7 金军战略AI：每旬反扑，制造战略压力。
 * 行为：增兵、反攻宋占前线城、把争夺中的城往金占方向推。
 */
data class JinTurnResult(
    val newState: GameState,
    val reports: List<String>
)

object JinAI {

    private val songProgress = listOf("FALLEN", "CONTESTED", "FRONTLINE", "STABLE")

    /** 守方(宋)失利，城池朝FALLEN后退一级 */
    private fun pushTowardFallen(current: String): String {
        val idx = songProgress.indexOf(current)
        return if (idx > 0) songProgress[idx - 1] else current
    }

    /**
     * 执行金军一旬的战略行动。
     * @param jinThreat 当前金国威胁值，越高金军越活跃
     */
    fun executeTurn(state: GameState, jinThreat: Int): JinTurnResult {
        val reports = mutableListOf<String>()
        var cities = state.cities

        // 金军活跃度：威胁值越高，行动越多
        val aggression = jinThreat / 100.0

        // 1. 金军增兵：金占城每旬恢复一定兵力
        cities = cities.map { c ->
            if (c.owner == "jin" && c.controlState == "FALLEN") {
                val reinforced = (c.troops * (1.0 + 0.05 * aggression)).toInt()
                c.copy(troops = reinforced.coerceAtMost(c.troops + 8000))
            } else c
        }

        // 2. 金军反攻：挑宋占的前线/争夺城下手
        val songFrontline = cities.filter {
            it.owner == "song" && (it.controlState == "FRONTLINE" || it.controlState == "CONTESTED")
        }

        // 反攻概率与威胁值挂钩
        if (songFrontline.isNotEmpty() && Random.nextInt(100) < (30 + jinThreat / 2)) {
            // 选一个最薄弱的宋前线城（防御+兵力最低）
            val targetCity = songFrontline.minByOrNull { it.defense + it.troops / 1000 }
            if (targetCity != null) {
                // 金军攻击力（受威胁值加成）
                val jinPower = 20000 * (0.8 + aggression * 0.6)
                val songDefend = targetCity.troops * (1.0 + targetCity.defense / 100.0)
                val jinWins = jinPower > songDefend * (0.7 + Random.nextDouble() * 0.6)

                cities = cities.map { c ->
                    if (c.id == targetCity.id) {
                        if (jinWins) {
                            val newState2 = pushTowardFallen(c.controlState)
                            val lostCity = newState2 == "FALLEN"
                            c.copy(
                                controlState = newState2,
                                owner = if (lostCity) "jin" else "song",
                                troops = (c.troops * 0.7).toInt(),
                                popularSupport = (c.popularSupport - 8).coerceAtLeast(0)
                            )
                        } else {
                            c.copy(troops = (c.troops * 0.92).toInt())
                        }
                    } else c
                }

                reports += if (jinWins) {
                    if (pushTowardFallen(targetCity.controlState) == "FALLEN")
                        "急报：金军大举南犯，${targetCity.name}失守！城池沦陷，军民南奔。"
                    else
                        "急报：金军猛攻${targetCity.name}，我军苦战不支，城池告急，已成拉锯之势。"
                } else {
                    "军报：金军进犯${targetCity.name}，守军奋勇拒敌，金兵暂退，然其势未消。"
                }
            }
        }

        // 3. 金军骚扰：低概率削弱某个宋占边境城的粮草
        if (Random.nextInt(100) < (15 + jinThreat / 4)) {
            val raidTarget = cities.filter { it.owner == "song" && it.controlState == "FRONTLINE" }.randomOrNull()
            if (raidTarget != null && raidTarget.grain > 10000) {
                cities = cities.map { c ->
                    if (c.id == raidTarget.id) c.copy(grain = (c.grain * 0.85).toInt()) else c
                }
                reports += "军报：金军游骑袭扰${raidTarget.name}粮道，焚毁粮秣若干。"
            }
        }

        return JinTurnResult(state.copy(cities = cities), reports)
    }
}

package com.xiemingxin.nandu.game

/**
 * V1.2 成就系统：里程碑达成不结束游戏，记录功业。
 * 成就只增不减，达成时弹庆祝，玩家继续游戏。
 */
data class Achievement(
    val id: String,
    val title: String,
    val desc: String,
    val tier: Int                  // 1普通 2显赫 3不世之功
)

object AchievementSystem {

    val all: List<Achievement> = listOf(
        Achievement("recover_yingtian", "克复应天", "收复应天府，宋室在中原重立根基。", 1),
        Achievement("hold_huai", "淮防稳固", "守住淮河防线，江淮屏障得保。", 1),
        Achievement("recover_kaifeng", "光复东京", "收复开封！靖康之耻得雪，二圣陵寝重归宋土。", 3),
        Achievement("recover_luoyang", "西京归宋", "收复洛阳，中原腹地连成一片。", 2),
        Achievement("cities_20", "半壁江山", "控制二十城，南宋疆土大复。", 2),
        Achievement("cities_28", "再造乾坤", "控制二十八城，几复祖宗故疆。", 3),
        Achievement("recover_all_jin", "混一寰宇", "收复全部金占之地，金虏遁归塞北，天下重归一统！", 3),
        Achievement("jin_threat_low", "金虏丧胆", "将金虏威胁压至三十以下，攻守之势逆转。", 2),
        Achievement("strong_army", "带甲百万", "全国驻军逾五十万，兵威极盛。", 2),
        Achievement("rich_state", "府库充盈", "国库存银逾三十万贯，财政丰裕。", 1)
    )

    fun byId(id: String): Achievement? = all.firstOrNull { it.id == id }

    /**
     * 检测当前局势新达成的成就（排除已获得的）。
     * @return 本次新达成的成就id列表
     */
    fun checkNewAchievements(state: GameState, earned: Set<String>): List<String> {
        val newly = mutableListOf<String>()
        val songCities = state.cities.count { it.owner == "song" }
        val jinCities = state.cities.count { it.owner == "jin" }
        val totalTroops = state.cities.filter { it.owner == "song" }.sumOf { it.troops } +
                          state.armies.filter { it.ownerFactionId == "song" }.sumOf { it.troops }
        val totalGold = state.cities.filter { it.owner == "song" }.sumOf { it.gold }

        fun owns(cityId: String) = state.cities.firstOrNull { it.id == cityId }?.owner == "song"

        fun tryAdd(id: String, cond: Boolean) {
            if (cond && id !in earned && id !in newly) newly.add(id)
        }

        tryAdd("recover_yingtian", owns("yingtianfu"))
        tryAdd("recover_kaifeng", owns("kaifeng"))
        tryAdd("recover_luoyang", owns("luoyang"))
        tryAdd("cities_20", songCities >= 20)
        tryAdd("cities_28", songCities >= 28)
        tryAdd("recover_all_jin", jinCities == 0)
        tryAdd("jin_threat_low", state.jinThreat < 30)
        tryAdd("strong_army", totalTroops >= 500000)
        tryAdd("rich_state", totalGold >= 300000)
        tryAdd("hold_huai", owns("chuzhou") && owns("shouchun") && owns("hefei"))

        return newly
    }
}

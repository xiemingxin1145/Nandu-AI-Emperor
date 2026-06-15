package com.xiemingxin.nandu.game

/**
 * V1.1 结局判定系统：给游戏一个目标和终点。
 *
 * 胜负条件（建炎元年开局，赵构视角）：
 *  - 中兴大宋（大胜）：收复开封 + 控城≥28
 *  - 北望中原（胜）：控城≥24 且 金虏威胁<40
 *  - 偏安江南（小胜）：撑到建炎设定年限，控城≥18
 *  - 划江而治（平）：撑到年限，控城12-17
 *  - 社稷倾覆（败）：临安失守 或 控城<8
 */
enum class GameEnding(val title: String, val rank: String, val desc: String) {
    GREAT_REVIVAL("中兴大宋", "S", "王师北定中原日，收复东京，迎还二圣之志得偿。陛下之功，比肩光武，史称中兴之主。"),
    NORTH_HOPE("北望中原", "A", "宋室重振，疆土大复，金虏不敢南顾。虽未尽复故疆，已为后世北伐奠定根基。"),
    SOUTH_PEACE("偏安江南", "B", "守住了半壁江山，江南繁华依旧。然中原父老，犹望王师，此憾绵绵无绝期。"),
    DIVIDE_RIVER("划江而治", "C", "宋金划江对峙，南北分治。社稷得保，然偏安之局已成，恢复无望。"),
    COLLAPSE("社稷倾覆", "D", "临安陷落，宋室南奔无地。靖康之耻未雪，又添崖山之痛。大宋三百年江山，至此而终。"),
    ONGOING("", "", "")
}

object VictoryJudge {

    /** 时限：建炎共约4年，这里设到第8年（约绍兴初）为强制结算 */
    private const val FINAL_YEAR = 8

    /**
     * 判定当前局势是否触发结局。
     * @return GameEnding，ONGOING表示游戏继续
     */
    fun judge(state: GameState): GameEnding {
        val songCities = state.cities.count { it.owner == "song" }
        val linan = state.cities.firstOrNull { it.id == "linan" }
        val kaifeng = state.cities.firstOrNull { it.id == "kaifeng" }
        val kaifengReclaimed = kaifeng?.owner == "song"
        val year = state.calendar.year

        // 即时失败：临安失守
        if (linan != null && linan.owner == "jin") return GameEnding.COLLAPSE
        // 即时失败：控城太少（被打崩）
        if (songCities < 8) return GameEnding.COLLAPSE

        // 即时大胜：收复开封 + 控城达标
        if (kaifengReclaimed && songCities >= 28) return GameEnding.GREAT_REVIVAL

        // 时限结算（到达终局年份）
        if (year >= FINAL_YEAR) {
            return when {
                kaifengReclaimed && songCities >= 28 -> GameEnding.GREAT_REVIVAL
                songCities >= 24 && state.jinThreat < 40 -> GameEnding.NORTH_HOPE
                songCities >= 18 -> GameEnding.SOUTH_PEACE
                songCities >= 12 -> GameEnding.DIVIDE_RIVER
                else -> GameEnding.COLLAPSE
            }
        }

        return GameEnding.ONGOING
    }

    /** 进度提示：让玩家随时知道离胜利还差多少 */
    fun progressHint(state: GameState): String {
        val songCities = state.cities.count { it.owner == "song" }
        val kaifeng = state.cities.firstOrNull { it.id == "kaifeng" }
        val year = state.calendar.year
        val yearsLeft = (FINAL_YEAR - year).coerceAtLeast(0)
        val kfText = if (kaifeng?.owner == "song") "开封已复" else "开封待复"
        return "控城 $songCities/36 · $kfText · 距结算 ${yearsLeft}年"
    }
}

package com.xiemingxin.nandu.game

/**
 * V1.2 结局判定：只有"亡国"才强制结束游戏。
 * 成就(收复开封/控城达标)由 AchievementSystem 处理，达成不结束。
 * 玩家也可主动"禅位归隐"体面收场。
 */
enum class GameEnding(val title: String, val rank: String, val posthumous: String, val desc: String) {
    // 真结局——亡国（强制结束 + 恶号）
    CAPITAL_LOST("社稷倾覆", "亡", "亡国之君",
        "临安陷落，宋室南奔无地。靖康之耻未雪，又添崖山之痛。后世史官记你为亡国之君，徽钦之后，再失半壁。"),
    LAND_LOST("神州陆沉", "亡", "失土之主",
        "城池尽丧，王师溃散，宋祚名存实亡。你坐视祖宗基业沦于异族，史称失土之主，遗臭千年。"),

    // 主动收场——禅位归隐（体面，按功业给庙号）
    ABDICATE_GLORY("功成身退", "禅", "中兴之主",
        "你于功业巅峰禅位归隐，将一个重振的大宋交予后人。史称中兴之主，配享太庙，万世敬仰。"),
    ABDICATE_PEACE("急流勇退", "禅", "守成之君",
        "你守住半壁江山后退位让贤。虽未尽复中原，亦不失为守成之君，社稷得以延续。"),
    ABDICATE_SHAME("仓皇逊位", "禅", "偏安之主",
        "你在内外交困中退位。偏安一隅，恢复无望，史评毁誉参半。"),

    ONGOING("", "", "", "")
}

object VictoryJudge {

    /**
     * 判定是否亡国（强制结束）。成就不在这里判，由AchievementSystem处理。
     * @return GameEnding，ONGOING表示游戏继续
     */
    fun judgeDefeat(state: GameState): GameEnding {
        val linan = state.cities.firstOrNull { it.id == "linan" }
        val songCities = state.cities.count { it.owner == "song" }

        // 临安失守 → 亡国
        if (linan != null && linan.owner == "jin") return GameEnding.CAPITAL_LOST
        // 控城跌破6 → 神州陆沉
        if (songCities < 6) return GameEnding.LAND_LOST

        return GameEnding.ONGOING
    }

    /**
     * 玩家主动禅位时，按当前功业评定庙号结局。
     */
    fun judgeAbdication(state: GameState): GameEnding {
        val songCities = state.cities.count { it.owner == "song" }
        val kaifengReclaimed = state.cities.firstOrNull { it.id == "kaifeng" }?.owner == "song"
        return when {
            kaifengReclaimed && songCities >= 24 -> GameEnding.ABDICATE_GLORY
            songCities >= 18 -> GameEnding.ABDICATE_PEACE
            else -> GameEnding.ABDICATE_SHAME
        }
    }

    /** 进度提示 */
    fun progressHint(state: GameState): String {
        val songCities = state.cities.count { it.owner == "song" }
        val kaifeng = state.cities.firstOrNull { it.id == "kaifeng" }
        val kfText = if (kaifeng?.owner == "song") "东京已复" else "东京待复"
        return "控城 $songCities/36 · $kfText · 第${state.calendar.year}年"
    }
}

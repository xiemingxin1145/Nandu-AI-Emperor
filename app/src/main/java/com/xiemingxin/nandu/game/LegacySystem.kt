package com.xiemingxin.nandu.game

import android.content.Context

/**
 * V1.2 传承系统：跨局持久化。一局结束后记录功业，下一局开局给加成。
 * 用 SharedPreferences 存储，跨游戏会话保留。
 */
data class LegacyData(
    val totalAchievements: Int = 0,     // 历代累计成就数
    val bestCities: Int = 0,            // 历代最高控城
    val reignsCompleted: Int = 0,       // 完成的局数
    val everRecoveredKaifeng: Boolean = false,  // 是否曾收复开封
    val everUnified: Boolean = false    // 是否曾一统
)

object LegacySystem {
    private const val PREFS = "nandu_legacy"
    private const val KEY_ACH = "total_achievements"
    private const val KEY_BEST = "best_cities"
    private const val KEY_REIGNS = "reigns_completed"
    private const val KEY_KAIFENG = "ever_kaifeng"
    private const val KEY_UNIFIED = "ever_unified"

    fun load(context: Context): LegacyData {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return LegacyData(
            totalAchievements = p.getInt(KEY_ACH, 0),
            bestCities = p.getInt(KEY_BEST, 0),
            reignsCompleted = p.getInt(KEY_REIGNS, 0),
            everRecoveredKaifeng = p.getBoolean(KEY_KAIFENG, false),
            everUnified = p.getBoolean(KEY_UNIFIED, false)
        )
    }

    /**
     * 一局结束时调用，累加功业到传承存档。
     * @param achievementsThisRun 本局达成的成就id集合
     * @param citiesThisRun 本局最终控城
     */
    fun recordReign(
        context: Context,
        achievementsThisRun: Set<String>,
        citiesThisRun: Int
    ) {
        val old = load(context)
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        p.edit().apply {
            putInt(KEY_ACH, old.totalAchievements + achievementsThisRun.size)
            putInt(KEY_BEST, maxOf(old.bestCities, citiesThisRun))
            putInt(KEY_REIGNS, old.reignsCompleted + 1)
            putBoolean(KEY_KAIFENG, old.everRecoveredKaifeng || "recover_kaifeng" in achievementsThisRun)
            putBoolean(KEY_UNIFIED, old.everUnified || "recover_all_jin" in achievementsThisRun)
            apply()
        }
    }

    /**
     * 根据传承数据，给新局的初始GameState加成。
     * 加成温和，体现"先辈余荫"而非碾压。
     */
    fun applyLegacyBonus(base: GameState, legacy: LegacyData): GameState {
        if (legacy.reignsCompleted == 0) return base  // 首局无加成

        // 成就越多，初始国力越强（每个成就+少量）
        val goldBonus = legacy.totalAchievements * 3000
        val grainBonus = legacy.totalAchievements * 8000
        val moraleBonus = (legacy.totalAchievements).coerceAtMost(15)
        // 曾收复开封：初始民心+5（百姓传颂先帝功业）
        val supportBonus = if (legacy.everRecoveredKaifeng) 5 else 0

        val buffedCities = base.cities.map { c ->
            if (c.owner == "song") {
                c.copy(
                    gold = c.gold + goldBonus / base.cities.count { it.owner == "song" }.coerceAtLeast(1),
                    grain = c.grain + grainBonus / base.cities.count { it.owner == "song" }.coerceAtLeast(1),
                    popularSupport = (c.popularSupport + supportBonus).coerceAtMost(100)
                )
            } else c
        }

        return base.copy(
            cities = buffedCities,
            troopMorale = (base.troopMorale + moraleBonus).coerceAtMost(100)
        )
    }

    /** 传承摘要文案，开局展示先辈余荫 */
    fun legacyBrief(legacy: LegacyData): String {
        if (legacy.reignsCompleted == 0) return "开国之君，基业草创，未有先帝荫泽。"
        val parts = mutableListOf<String>()
        parts.add("历经 ${legacy.reignsCompleted} 代")
        parts.add("累积功业 ${legacy.totalAchievements} 桩")
        if (legacy.everRecoveredKaifeng) parts.add("先帝曾光复东京")
        if (legacy.everUnified) parts.add("祖宗曾混一寰宇")
        return parts.joinToString("，") + "。先辈余荫，泽被后世。"
    }
}

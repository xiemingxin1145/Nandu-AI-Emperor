package com.xiemingxin.nandu.game

/**
 * V0.9 兵种系统：定义兵种属性与克制关系。
 * 兵种图来自 assets/images/units/。
 * 克制关系参考史实：神臂弩/背嵬军克金军骑兵，水军克步骑于江河。
 */
data class BattleUnitDef(
    val id: String,
    val name: String,
    val imagePath: String,
    val faction: String,           // song / jin
    val category: String,          // infantry步 / cavalry骑 / archer射 / navy水 / elite精锐
    val attack: Int,
    val defense: Int,
    val recruitGold: Int,
    val recruitGrain: Int,
    val counters: List<String>,    // 克制的兵种category
    val terrainBonus: String,      // 擅长地形
    val desc: String
)

object BattleUnitCatalog {
    private const val BASE = "images/units"

    val all: List<BattleUnitDef> = listOf(
        // 南宋兵种
        BattleUnitDef("song_infantry", "步人甲", "$BASE/unit_song_infantry_01.webp", "song", "infantry",
            70, 80, 1500, 1000, listOf("archer"), "plain", "重装步兵，披甲厚重，正面抗线之主力"),
        BattleUnitDef("song_archer", "弓手", "$BASE/unit_song_archer_01.webp", "song", "archer",
            65, 45, 1200, 800, listOf("infantry"), "plain", "远程压制，野战消耗敌阵"),
        BattleUnitDef("song_crossbowman", "弩手", "$BASE/unit_song_crossbowman_01.webp", "song", "archer",
            72, 48, 1400, 900, listOf("cavalry"), "pass", "强弩破甲，守关拒骑之利器"),
        BattleUnitDef("song_divine_arm_crossbow", "神臂弓手", "$BASE/unit_song_divine_arm_crossbow_01.webp", "song", "archer",
            88, 50, 3000, 1500, listOf("cavalry", "elite"), "pass", "神臂弓射程远、破甲强，专克金军重骑"),
        BattleUnitDef("song_cavalry", "骑兵", "$BASE/unit_song_cavalry_01.webp", "song", "cavalry",
            78, 60, 2500, 1800, listOf("archer"), "plain", "机动突击，追击溃敌"),
        BattleUnitDef("song_navy", "水军", "$BASE/unit_song_navy_01.webp", "song", "navy",
            75, 65, 2000, 1200, listOf("infantry", "cavalry"), "river", "楼船水战，长江淮河之屏障"),
        BattleUnitDef("song_beiwei_elite", "背嵬军", "$BASE/unit_song_beiwei_elite_01.webp", "song", "elite",
            95, 88, 5000, 3000, listOf("cavalry", "infantry"), "plain", "岳家军精锐，步骑皆能，撼山易撼此军难"),
        BattleUnitDef("song_shengjie_army", "胜捷军", "$BASE/unit_song_shengjie_army_01.webp", "song", "elite",
            85, 80, 4000, 2500, listOf("infantry"), "plain", "西军精锐，悍勇敢战"),
        BattleUnitDef("song_scout_cavalry", "踏白军", "$BASE/unit_song_scout_cavalry_01.webp", "song", "cavalry",
            70, 50, 2000, 1500, listOf("archer"), "plain", "轻骑斥候，侦察袭扰"),
        BattleUnitDef("song_tabai_scout", "探马", "$BASE/unit_song_tabai_scout_01.webp", "song", "cavalry",
            55, 40, 1000, 800, emptyList(), "plain", "哨探斥候，刺探军情"),
        // 金国兵种
        BattleUnitDef("jin_infantry", "金军步卒", "$BASE/unit_jin_infantry_01.webp", "jin", "infantry",
            72, 70, 0, 0, listOf("archer"), "plain", "金国步兵，攻城拔寨"),
        BattleUnitDef("jin_horse_archer", "马弓手", "$BASE/unit_jin_horse_archer_01.webp", "jin", "archer",
            80, 50, 0, 0, listOf("infantry"), "plain", "骑射骚扰，机动远程"),
        BattleUnitDef("jin_heavy_cavalry", "重骑", "$BASE/unit_jin_heavy_cavalry_01.webp", "jin", "cavalry",
            88, 75, 0, 0, listOf("infantry", "archer"), "plain", "金军重甲骑兵，平原冲阵无敌"),
        BattleUnitDef("jin_guai_zi_ma", "拐子马", "$BASE/unit_jin_guai_zi_ma_01.webp", "jin", "cavalry",
            85, 72, 0, 0, listOf("infantry"), "plain", "三马相连之骑阵，左右包抄"),
        BattleUnitDef("jin_iron_pagoda", "铁浮屠", "$BASE/unit_jin_iron_pagoda_01.webp", "jin", "elite",
            92, 90, 0, 0, listOf("infantry", "cavalry"), "plain", "金军重装具装骑兵，号铁浮屠，正面难当")
    )

    fun byId(id: String): BattleUnitDef? = all.firstOrNull { it.id == id }
    fun recruitable(faction: String): List<BattleUnitDef> =
        all.filter { it.faction == faction && it.recruitGold > 0 }

    /** 计算A对B的克制系数：克制+25%，被克-15% */
    fun counterFactor(attacker: BattleUnitDef, defender: BattleUnitDef): Double = when {
        defender.category in attacker.counters -> 1.25
        attacker.category in defender.counters -> 0.85
        else -> 1.0
    }

    fun categoryLabel(category: String): String = when (category) {
        "infantry" -> "步兵"
        "cavalry" -> "骑兵"
        "archer" -> "弓弩"
        "navy" -> "水军"
        "elite" -> "精锐"
        else -> "杂兵"
    }
}

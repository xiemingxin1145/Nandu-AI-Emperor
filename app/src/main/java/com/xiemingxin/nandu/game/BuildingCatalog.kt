package com.xiemingxin.nandu.game

/**
 * V0.9 城池建筑系统：定义可建造的建筑及其效果。
 * 建筑图来自 assets/images/buildings/。
 */
data class BuildingDef(
    val id: String,
    val name: String,
    val imagePath: String,
    val category: String,          // military军事 / economy民政 / defense防御 / special特殊
    val maxLevel: Int,
    val baseGoldCost: Int,
    val baseGrainCost: Int,
    val effectDesc: String,
    val requireWaterNode: Boolean = false
)

object BuildingCatalog {
    private const val BASE = "images/buildings"

    val all: List<BuildingDef> = listOf(
        BuildingDef("barracks", "军营", "$BASE/building_barracks_01.webp", "military", 3, 3000, 5000,
            "提升驻军上限与练兵效率，每级+5000募兵容量"),
        BuildingDef("drill_ground", "校场", "$BASE/building_drill_ground_01.webp", "military", 3, 2500, 4000,
            "提升军队士气与训练度，每级士气+5"),
        BuildingDef("armory", "武库", "$BASE/building_armory_01.webp", "military", 3, 4000, 2000,
            "提升军备质量，每级攻防+8%"),
        BuildingDef("city_wall", "城墙", "$BASE/building_city_wall_01.webp", "defense", 3, 5000, 3000,
            "提升城防，每级城防+15"),
        BuildingDef("granary", "粮仓", "$BASE/building_granary_01.webp", "economy", 3, 2000, 1000,
            "提升存粮上限与屯田产出，每级农业+10"),
        BuildingDef("market", "市集", "$BASE/building_market_01.webp", "economy", 3, 3000, 1000,
            "提升商业税收，每级商业+10"),
        BuildingDef("dock", "码头", "$BASE/building_dock_01.webp", "economy", 3, 4000, 2000,
            "提升水运与海贸收入，每级金收入+15%", requireWaterNode = true),
        BuildingDef("workshop", "作坊", "$BASE/building_workshop_01.webp", "economy", 3, 3500, 1500,
            "提升手工业产出，每级金收入+10%"),
        BuildingDef("academy", "书院", "$BASE/building_academy_01.webp", "special", 3, 4000, 2000,
            "提升人才寻访概率与文教，每级民心+5"),
        BuildingDef("post_station", "驿站", "$BASE/building_post_station_01.webp", "special", 2, 2500, 1500,
            "提升行军速度与情报传递，每级行军+10%"),
        BuildingDef("temple", "寺庙", "$BASE/building_temple_01.webp", "special", 2, 2000, 1000,
            "安抚民心，每级民心+8"),
        BuildingDef("taoist_temple", "道观", "$BASE/building_taoist_temple_01.webp", "special", 2, 2000, 1000,
            "提升民心与祥瑞概率，每级民心+6"),
        BuildingDef("county_office", "县衙", "$BASE/building_county_office_01.webp", "economy", 2, 1500, 1000,
            "提升行政效率，每级税收+8%"),
        BuildingDef("prefecture_office", "府衙", "$BASE/building_prefecture_office_01.webp", "economy", 3, 5000, 3000,
            "提升一城治理上限，每级综合产出+10%")
    )

    fun byId(id: String): BuildingDef? = all.firstOrNull { it.id == id }

    /** 当前等级升到下一级的造价（随等级递增） */
    fun upgradeCost(def: BuildingDef, currentLevel: Int): Pair<Int, Int> {
        val factor = 1.0 + currentLevel * 0.6
        return Pair((def.baseGoldCost * factor).toInt(), (def.baseGrainCost * factor).toInt())
    }

    fun categoryLabel(category: String): String = when (category) {
        "military" -> "军事"
        "economy" -> "民政"
        "defense" -> "防御"
        "special" -> "文教"
        else -> "其他"
    }
}

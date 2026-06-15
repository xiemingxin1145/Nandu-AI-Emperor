package com.xiemingxin.nandu.game

/**
 * V2.1 城池视觉注册表。
 *
 * 参考传统 SLG 地图结构：先有底图，再叠城池、道路、势力与面板。
 * 这里先登记视觉身份和资源槽位，不强依赖真实图片文件。
 */
enum class CityVisualTier(val label: String, val mapScale: Float) {
    CAPITAL("都城", 1.55f),
    METROPOLIS("巨城", 1.35f),
    FORTRESS("重镇", 1.22f),
    PORT("港市", 1.18f),
    PASS("关隘", 1.14f),
    COUNTY("州县", 1.0f),
    STRATEGIC("战略节点", 0.88f)
}

data class CityVisualInfo(
    val cityId: String,
    val displayName: String,
    val tier: CityVisualTier,
    val iconKey: String,
    val panelBackgroundPath: String,
    val mapIconPath: String,
    val tags: List<String>,
    val visualHint: String
)

object MapBackgroundRegistry {
    const val parchment = "images/map/song_world_parchment.webp"
    const val political = "images/map/song_world_political.webp"
    const val trade = "images/map/song_world_trade.webp"
    const val military = "images/map/song_world_military.webp"

    fun forLayer(layer: MapLayerMode): String = when (layer) {
        MapLayerMode.MILITARY -> military
        MapLayerMode.ECONOMY -> parchment
        MapLayerMode.DIPLOMACY -> political
        MapLayerMode.TRADE -> trade
    }
}

object CityVisualRegistry {
    private val important = listOf(
        CityVisualInfo("linan", "临安", CityVisualTier.CAPITAL, "capital_song", "images/city/linan_palace.webp", "images/map/city_capital_song.webp", listOf("皇都", "行在", "繁华"), "南宋行在，皇宫、政事堂、市舶财政的中心。"),
        CityVisualInfo("jiankang", "建康", CityVisualTier.FORTRESS, "river_fortress", "images/city/jiankang_river_fortress.webp", "images/map/city_fortress_river.webp", listOf("江防", "重镇"), "江淮门户，守江必守建康。"),
        CityVisualInfo("xiangyang", "襄阳", CityVisualTier.FORTRESS, "north_fortress", "images/city/xiangyang_fortress.webp", "images/map/city_fortress.webp", listOf("北伐", "山河要冲"), "荆襄北伐跳板，兵家必争。"),
        CityVisualInfo("kaifeng", "汴京", CityVisualTier.METROPOLIS, "old_capital", "images/city/kaifeng_old_capital.webp", "images/map/city_old_capital.webp", listOf("旧都", "中原"), "旧日东京，收复此城意味着中兴大业入史。"),
        CityVisualInfo("luoyang", "洛阳", CityVisualTier.METROPOLIS, "old_capital_west", "images/city/luoyang_old_capital.webp", "images/map/city_old_capital.webp", listOf("旧都", "西京"), "西京旧地，政治象征极重。"),
        CityVisualInfo("yanjing", "燕京", CityVisualTier.FORTRESS, "yanjing", "images/city/yanjing_north.webp", "images/map/city_fortress_jin.webp", listOf("燕云", "北方重镇"), "燕云门户，后期北望关键。"),
        CityVisualInfo("quanzhou", "泉州", CityVisualTier.PORT, "sea_port", "images/city/quanzhou_port.webp", "images/map/city_port.webp", listOf("市舶司", "香料海贸"), "东南大港，海贸与商税的爆点。"),
        CityVisualInfo("mingzhou", "明州", CityVisualTier.PORT, "ship_port", "images/city/mingzhou_shipyard.webp", "images/map/city_port.webp", listOf("船材", "高丽海路"), "船材与海东贸易节点。"),
        CityVisualInfo("guangzhou", "广州", CityVisualTier.PORT, "south_port", "images/city/guangzhou_southsea.webp", "images/map/city_port.webp", listOf("南海", "香药"), "南海商路门户，利润厚、风险也高。"),
        CityVisualInfo("chengdu", "成都", CityVisualTier.METROPOLIS, "west_metropolis", "images/city/chengdu_west.webp", "images/map/city_metropolis.webp", listOf("西南", "财赋"), "西南财赋与大理路线的根。"),
        CityVisualInfo("xingqing", "兴庆府", CityVisualTier.CAPITAL, "xixia_capital", "images/city/xingqing_xixia.webp", "images/map/city_capital_xixia.webp", listOf("西夏", "马市"), "西夏都城，外交、马市与河陇事件中心。"),
        CityVisualInfo("dali", "大理", CityVisualTier.CAPITAL, "dali_capital", "images/city/dali_southwest.webp", "images/map/city_capital_dali.webp", listOf("大理", "西南商路"), "西南国都，药材、马匹、铜矿的入口。"),
        CityVisualInfo("hefei", "合肥", CityVisualTier.FORTRESS, "front_fortress", "images/city/hefei_front.webp", "images/map/city_fortress.webp", listOf("江淮", "前线"), "江淮前线城池，防线稳定器。"),
        CityVisualInfo("shouchun", "寿春", CityVisualTier.FORTRESS, "front_fortress", "images/city/shouchun_front.webp", "images/map/city_fortress.webp", listOf("江淮", "前线"), "淮上要地，失则江南震动。")
    ).associateBy { it.cityId }

    fun visualFor(city: City?, node: MapNode): CityVisualInfo {
        important[city?.id ?: node.id]?.let { return it }
        val tier = when {
            node.isCapital -> CityVisualTier.CAPITAL
            city?.terrain == "coast" || city?.isWaterNode == true || node.nodeType == "trade" -> CityVisualTier.PORT
            city?.terrain == "pass" || node.nodeType == "pass" -> CityVisualTier.PASS
            city?.controlState == "FRONTLINE" || city?.controlState == "CONTESTED" -> CityVisualTier.FORTRESS
            city == null -> CityVisualTier.STRATEGIC
            city.cityLevel.contains("府") || city.population >= 700000 -> CityVisualTier.METROPOLIS
            else -> CityVisualTier.COUNTY
        }
        val id = city?.id ?: node.id
        val name = city?.name ?: node.name
        return CityVisualInfo(
            cityId = id,
            displayName = name,
            tier = tier,
            iconKey = tier.name.lowercase(),
            panelBackgroundPath = "images/city/${id}.webp",
            mapIconPath = "images/map/city_${tier.name.lowercase()}.webp",
            tags = defaultTags(city, node, tier),
            visualHint = defaultHint(tier)
        )
    }

    fun mapScale(city: City?, node: MapNode): Float = visualFor(city, node).tier.mapScale

    private fun defaultTags(city: City?, node: MapNode, tier: CityVisualTier): List<String> = buildList {
        add(tier.label)
        if (city?.owner == "jin" || node.ownerHint == "jin") add("金占")
        if (city?.owner == "song" || node.ownerHint == "song") add("宋控")
        if (node.ownerHint == "xixia") add("西夏")
        if (node.ownerHint == "dali") add("大理")
        if (city?.terrain == "coast" || city?.isWaterNode == true) add("水路")
        if (city?.controlState == "FRONTLINE" || city?.controlState == "CONTESTED") add("前线")
    }

    private fun defaultHint(tier: CityVisualTier): String = when (tier) {
        CityVisualTier.CAPITAL -> "都城级节点，应使用更大的城池图标与专属面板背景。"
        CityVisualTier.METROPOLIS -> "大城节点，适合展示商业、人口、府库与官署。"
        CityVisualTier.FORTRESS -> "军事重镇，适合展示城防、驻军、粮道和战报。"
        CityVisualTier.PORT -> "港市节点，适合展示市舶、海商、走私与海防。"
        CityVisualTier.PASS -> "关隘节点，适合展示山道、卡口、守军和补给。"
        CityVisualTier.COUNTY -> "普通州县，使用统一州城图标即可。"
        CityVisualTier.STRATEGIC -> "战略节点，暂未升级为完整城池，可先用于外交、贸易或剧情。"
    }
}

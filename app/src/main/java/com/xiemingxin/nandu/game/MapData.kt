package com.xiemingxin.nandu.game

// ══ 世界坐标系 16000 × 10000 ══
// 左上=西北  右下=东南
// 手机屏幕是摄像机窗口，通过 cameraOffset + zoom 转换

data class MapNode(
    val id: String,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val isCapital: Boolean = false
)

enum class RoadType {
    LAND,      // 陆路 — 黄土色
    RIVER,     // 水路/河流 — 蓝色
    CANAL,     // 漕运 — 浅蓝虚线
    MOUNTAIN,  // 山道 — 棕色虚线
    SEA,       // 海路 — 深蓝虚线
    PASS       // 关隘 — 红棕
}

data class CityRoad(
    val fromId: String,
    val toId: String,
    val type: RoadType
)

object MapData {

    // 第一版 12 城 — 艺术化战略压缩坐标
    // 数据结构按 120 城预留，后续直接追加
    val nodes: List<MapNode> = listOf(
        MapNode("linan", "临安", 11000f, 6800f, isCapital = true),
        MapNode("shaoxing", "绍兴", 11600f, 7000f),
        MapNode("suzhou", "平江", 10800f, 6300f),
        MapNode("mingzhou", "明州", 12200f, 7100f),
        MapNode("wenzhou", "温州", 12000f, 7800f),
        MapNode("jiankang", "建康", 10200f, 5600f),
        MapNode("ningguo", "宣州", 10000f, 6300f),
        MapNode("hongzhou", "洪州", 9200f, 7200f),
        MapNode("ganzhou", "虔州", 9000f, 8000f),
        MapNode("ezhou", "鄂州", 8400f, 6400f),
        MapNode("jiangling", "江陵", 7800f, 6000f),
        MapNode("tanzhou", "潭州", 8000f, 7400f),
        MapNode("xiangyang", "襄阳", 8200f, 5000f),
        MapNode("dengzhou", "邓州", 8400f, 4400f),
        MapNode("yangzhou", "扬州", 10600f, 5100f),
        MapNode("chuzhou", "楚州", 10800f, 4600f),
        MapNode("hefei", "庐州", 9800f, 5000f),
        MapNode("shouchun", "寿春", 9600f, 4600f),
        MapNode("xinyang", "信阳", 9000f, 4400f),
        MapNode("xinguan", "兴元府", 6600f, 4200f),
        MapNode("chengdu", "成都", 5200f, 5400f),
        MapNode("zizhou", "潼川", 5800f, 5800f),
        MapNode("kuizhou", "夔州", 7000f, 6000f),
        MapNode("xianren_pass", "仙人关", 6200f, 3800f),
        MapNode("fuzhou", "福州", 11000f, 8400f),
        MapNode("quanzhou", "泉州", 11200f, 8900f),
        MapNode("guangzhou", "广州", 8800f, 9200f),
        MapNode("yingtianfu", "应天府", 10000f, 4000f),
        MapNode("kaifeng", "开封", 9400f, 3400f),
        MapNode("taiyuan", "太原", 8200f, 2400f),
        MapNode("daming", "大名府", 9600f, 2800f),
        MapNode("zhending", "真定", 9000f, 2200f),
        MapNode("hejian", "河间", 9800f, 2200f),
        MapNode("luoyang", "洛阳", 8600f, 3600f),
        MapNode("jingzhao", "京兆府", 6800f, 3200f),
        MapNode("zhongshan", "中山", 9400f, 1800f)
    )

    // 道路网络 — 第一版只显示，不强制拦截调兵（36城版启用规则）
    // V0.8 完整路网：36城全连通，51条路（陆/水/运河/山道/海/关隘）
    val roads: List<CityRoad> = listOf(
        CityRoad("linan", "suzhou", RoadType.CANAL),
        CityRoad("suzhou", "jiankang", RoadType.CANAL),
        CityRoad("linan", "shaoxing", RoadType.CANAL),
        CityRoad("shaoxing", "mingzhou", RoadType.CANAL),
        CityRoad("shaoxing", "wenzhou", RoadType.SEA),
        CityRoad("linan", "ningguo", RoadType.LAND),
        CityRoad("suzhou", "yangzhou", RoadType.RIVER),
        CityRoad("jiankang", "yangzhou", RoadType.RIVER),
        CityRoad("jiankang", "ezhou", RoadType.RIVER),
        CityRoad("ezhou", "jiangling", RoadType.RIVER),
        CityRoad("jiangling", "kuizhou", RoadType.RIVER),
        CityRoad("ezhou", "tanzhou", RoadType.RIVER),
        CityRoad("jiankang", "ningguo", RoadType.LAND),
        CityRoad("ningguo", "hongzhou", RoadType.LAND),
        CityRoad("hongzhou", "tanzhou", RoadType.RIVER),
        CityRoad("hongzhou", "ganzhou", RoadType.MOUNTAIN),
        CityRoad("tanzhou", "ganzhou", RoadType.MOUNTAIN),
        CityRoad("wenzhou", "fuzhou", RoadType.SEA),
        CityRoad("fuzhou", "quanzhou", RoadType.SEA),
        CityRoad("ganzhou", "guangzhou", RoadType.MOUNTAIN),
        CityRoad("quanzhou", "guangzhou", RoadType.SEA),
        CityRoad("chengdu", "zizhou", RoadType.RIVER),
        CityRoad("zizhou", "kuizhou", RoadType.RIVER),
        CityRoad("chengdu", "xinguan", RoadType.MOUNTAIN),
        CityRoad("xinguan", "xianren_pass", RoadType.PASS),
        CityRoad("xianren_pass", "jingzhao", RoadType.PASS),
        CityRoad("xinguan", "xiangyang", RoadType.MOUNTAIN),
        CityRoad("jiangling", "xiangyang", RoadType.LAND),
        CityRoad("xiangyang", "dengzhou", RoadType.LAND),
        CityRoad("xiangyang", "xinyang", RoadType.PASS),
        CityRoad("yangzhou", "chuzhou", RoadType.RIVER),
        CityRoad("chuzhou", "shouchun", RoadType.RIVER),
        CityRoad("hefei", "shouchun", RoadType.LAND),
        CityRoad("hefei", "yangzhou", RoadType.LAND),
        CityRoad("shouchun", "xinyang", RoadType.LAND),
        CityRoad("hefei", "jiankang", RoadType.RIVER),
        CityRoad("chuzhou", "yingtianfu", RoadType.LAND),
        CityRoad("yingtianfu", "kaifeng", RoadType.LAND),
        CityRoad("xinyang", "kaifeng", RoadType.LAND),
        CityRoad("dengzhou", "luoyang", RoadType.LAND),
        CityRoad("xiangyang", "luoyang", RoadType.MOUNTAIN),
        CityRoad("kaifeng", "luoyang", RoadType.RIVER),
        CityRoad("kaifeng", "daming", RoadType.LAND),
        CityRoad("kaifeng", "yingtianfu", RoadType.LAND),
        CityRoad("luoyang", "jingzhao", RoadType.PASS),
        CityRoad("daming", "zhending", RoadType.LAND),
        CityRoad("daming", "hejian", RoadType.LAND),
        CityRoad("zhending", "taiyuan", RoadType.MOUNTAIN),
        CityRoad("zhending", "zhongshan", RoadType.MOUNTAIN),
        CityRoad("hejian", "zhongshan", RoadType.LAND),
        CityRoad("taiyuan", "jingzhao", RoadType.MOUNTAIN)
    )

    val nodeMap: Map<String, MapNode> = nodes.associateBy { it.id }

    // V0.8 邻接表：城市id → 直接相连的城市id集合（双向）
    val adjacency: Map<String, Set<String>> = buildMap {
        val tmp = HashMap<String, MutableSet<String>>()
        roads.forEach { road ->
            tmp.getOrPut(road.fromId) { mutableSetOf() }.add(road.toId)
            tmp.getOrPut(road.toId) { mutableSetOf() }.add(road.fromId)
        }
        tmp.forEach { (k, v) -> put(k, v) }
    }

    /** 两城是否直接相邻（有路相连） */
    fun isAdjacent(a: String, b: String): Boolean = adjacency[a]?.contains(b) == true

    /** 取某城所有相邻城市 */
    fun neighborsOf(cityId: String): Set<String> = adjacency[cityId] ?: emptySet()

    /** 取两城之间的路型（用于地形加成/补给风险），无直连返回null */
    fun roadType(a: String, b: String): RoadType? =
        roads.firstOrNull { (it.fromId == a && it.toId == b) || (it.fromId == b && it.toId == a) }?.type
}

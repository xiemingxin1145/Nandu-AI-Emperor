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
        MapNode("taiyuan",   "太原",     7600f, 2200f),
        MapNode("daming",    "大名府",   9800f, 2600f),
        MapNode("kaifeng",   "开封",     9000f, 3900f, isCapital = true),
        MapNode("xiangyang", "襄阳",     7200f, 5200f),
        MapNode("huaihe",    "淮河防线", 9600f, 5400f),
        MapNode("yangzhou",  "扬州",     10800f, 5600f),
        MapNode("xinguan",   "兴元府",   5000f, 5200f),
        MapNode("ezhou",     "鄂州",     7600f, 6600f),
        MapNode("jiankang",  "建康",     10400f, 6600f),
        MapNode("chengdu",   "成都",     3600f, 7200f),
        MapNode("linan",     "临安",     11600f, 7600f),
        MapNode("fuzhou",    "福州",     12800f, 8600f)
    )

    // 道路网络 — 第一版只显示，不强制拦截调兵（36城版启用规则）
    val roads: List<CityRoad> = listOf(
        CityRoad("taiyuan",   "kaifeng",   RoadType.LAND),
        CityRoad("daming",    "kaifeng",   RoadType.LAND),
        CityRoad("kaifeng",   "huaihe",    RoadType.LAND),
        CityRoad("huaihe",    "yangzhou",  RoadType.RIVER),
        CityRoad("yangzhou",  "jiankang",  RoadType.RIVER),
        CityRoad("huaihe",    "jiankang",  RoadType.RIVER),
        CityRoad("jiankang",  "linan",     RoadType.CANAL),
        CityRoad("linan",     "fuzhou",    RoadType.SEA),
        CityRoad("xiangyang", "kaifeng",   RoadType.LAND),
        CityRoad("xiangyang", "ezhou",     RoadType.RIVER),
        CityRoad("ezhou",     "jiankang",  RoadType.RIVER),
        CityRoad("xinguan",   "xiangyang", RoadType.MOUNTAIN),
        CityRoad("xinguan",   "chengdu",   RoadType.MOUNTAIN),
        CityRoad("chengdu",   "ezhou",     RoadType.RIVER)
    )

    val nodeMap: Map<String, MapNode> = nodes.associateBy { it.id }
}

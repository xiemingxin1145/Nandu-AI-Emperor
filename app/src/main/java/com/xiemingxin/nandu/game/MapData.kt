package com.xiemingxin.nandu.game

// ══ 世界坐标系 16000 × 10000 ══
// 左上=西北  右下=东南
// 手机屏幕是摄像机窗口，通过 cameraOffset + zoom 转换

data class MapNode(
    val id: String,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val isCapital: Boolean = false,
    val ownerHint: String = "",
    val nodeType: String = "city"
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

    // V1.6 天下战略第一版：从 36 节点扩到 68 节点。
    // 其中 GameState 已接入的城池仍负责数值结算；新增节点先作为战略密度、外交、外贸与后续城池扩展槽位。
    val nodes: List<MapNode> = listOf(
        // ===== 南宋核心区·两浙与江南 =====
        MapNode("linan", "临安", 11000f, 6800f, isCapital = true, ownerHint = "song"),
        MapNode("shaoxing", "绍兴", 11600f, 7000f, ownerHint = "song"),
        MapNode("suzhou", "平江", 10800f, 6300f, ownerHint = "song"),
        MapNode("mingzhou", "明州", 12200f, 7100f, ownerHint = "song"),
        MapNode("wenzhou", "温州", 12000f, 7800f, ownerHint = "song"),
        MapNode("huzhou", "湖州", 10600f, 6600f, ownerHint = "song"),
        MapNode("taizhou", "台州", 12350f, 7450f, ownerHint = "song"),
        MapNode("jiankang", "建康", 10200f, 5600f, ownerHint = "song"),
        MapNode("ningguo", "宣州", 10000f, 6300f, ownerHint = "song"),
        MapNode("chizhou", "池州", 9600f, 6100f, ownerHint = "song"),
        MapNode("rao_zhou", "饶州", 9400f, 6750f, ownerHint = "song"),
        MapNode("hongzhou", "洪州", 9200f, 7200f, ownerHint = "song"),
        MapNode("ganzhou", "虔州", 9000f, 8000f, ownerHint = "song"),
        MapNode("jizhou", "吉州", 8700f, 7650f, ownerHint = "song"),
        MapNode("jiangzhou", "江州", 9000f, 6500f, ownerHint = "song"),

        // ===== 荆湖与川陕 =====
        MapNode("ezhou", "鄂州", 8400f, 6400f, ownerHint = "song"),
        MapNode("jiangling", "江陵", 7800f, 6000f, ownerHint = "song"),
        MapNode("tanzhou", "潭州", 8000f, 7400f, ownerHint = "song"),
        MapNode("yuezhou", "岳州", 8150f, 6900f, ownerHint = "song"),
        MapNode("changde", "鼎州", 7550f, 7050f, ownerHint = "song"),
        MapNode("xiangyang", "襄阳", 8200f, 5000f, ownerHint = "song"),
        MapNode("dengzhou", "邓州", 8400f, 4400f, ownerHint = "song"),
        MapNode("xinguan", "兴元府", 6600f, 4200f, ownerHint = "song"),
        MapNode("chengdu", "成都", 5200f, 5400f, ownerHint = "song"),
        MapNode("zizhou", "潼川", 5800f, 5800f, ownerHint = "song"),
        MapNode("kuizhou", "夔州", 7000f, 6000f, ownerHint = "song"),
        MapNode("xianren_pass", "仙人关", 6200f, 3800f, ownerHint = "song", nodeType = "pass"),
        MapNode("lizhou", "利州", 6400f, 4550f, ownerHint = "song"),
        MapNode("langzhou", "阆州", 6000f, 5050f, ownerHint = "song"),
        MapNode("luzhou", "泸州", 5050f, 6500f, ownerHint = "song"),
        MapNode("xuzhou_shu", "叙州", 4800f, 7000f, ownerHint = "song"),
        MapNode("yazhou", "雅州", 4550f, 5900f, ownerHint = "song"),
        MapNode("lizhou_south", "黎州", 4200f, 6300f, ownerHint = "song"),

        // ===== 福建广南与海贸 =====
        MapNode("fuzhou", "福州", 11000f, 8400f, ownerHint = "song"),
        MapNode("quanzhou", "泉州", 11200f, 8900f, ownerHint = "song"),
        MapNode("guangzhou", "广州", 8800f, 9200f, ownerHint = "song"),
        MapNode("chaozhou", "潮州", 10100f, 9300f, ownerHint = "song"),
        MapNode("lianzhou_port", "廉州", 7600f, 9550f, ownerHint = "song"),
        MapNode("qiongzhou", "琼州", 8000f, 9850f, ownerHint = "song"),
        MapNode("jiaozhi_route", "交趾海路", 6500f, 9700f, ownerHint = "sea_trade", nodeType = "trade"),
        MapNode("south_sea", "南海诸国", 11800f, 9800f, ownerHint = "sea_trade", nodeType = "trade"),
        MapNode("goryeo_route", "高丽海路", 13800f, 5200f, ownerHint = "goryeo", nodeType = "trade"),

        // ===== 宋金交界与中原 =====
        MapNode("yangzhou", "扬州", 10600f, 5100f, ownerHint = "song"),
        MapNode("chuzhou", "楚州", 10800f, 4600f, ownerHint = "song"),
        MapNode("hefei", "庐州", 9800f, 5000f, ownerHint = "song"),
        MapNode("shouchun", "寿春", 9600f, 4600f, ownerHint = "song"),
        MapNode("xinyang", "信阳", 9000f, 4400f, ownerHint = "song"),
        MapNode("yingtianfu", "应天府", 10000f, 4000f, ownerHint = "song"),
        MapNode("haozhou", "濠州", 10200f, 4400f, ownerHint = "song"),
        MapNode("sizhou", "泗州", 10650f, 4250f, ownerHint = "song"),
        MapNode("kaifeng", "开封", 9400f, 3400f, ownerHint = "jin", isCapital = true),
        MapNode("luoyang", "洛阳", 8600f, 3600f, ownerHint = "jin"),
        MapNode("jingzhao", "京兆府", 6800f, 3200f, ownerHint = "jin"),
        MapNode("hezhong", "河中府", 7400f, 3150f, ownerHint = "jin"),
        MapNode("fengxiang", "凤翔府", 6200f, 3250f, ownerHint = "jin"),
        MapNode("qinzhou", "秦州", 5600f, 3450f, ownerHint = "jin"),
        MapNode("daming", "大名府", 9600f, 2800f, ownerHint = "jin"),
        MapNode("xiangzhou", "相州", 9300f, 3000f, ownerHint = "jin"),
        MapNode("weizhou", "卫州", 9000f, 3100f, ownerHint = "jin"),
        MapNode("taiyuan", "太原", 8200f, 2400f, ownerHint = "jin"),
        MapNode("zhending", "真定", 9000f, 2200f, ownerHint = "jin"),
        MapNode("hejian", "河间", 9800f, 2200f, ownerHint = "jin"),
        MapNode("zhongshan", "中山", 9400f, 1800f, ownerHint = "jin"),
        MapNode("yanjing", "燕京", 10200f, 1200f, ownerHint = "jin", isCapital = true),
        MapNode("yunzhong", "云中", 8000f, 1400f, ownerHint = "jin"),

        // ===== 西夏河陇 =====
        MapNode("xingqing", "兴庆府", 2600f, 2100f, ownerHint = "xixia", isCapital = true),
        MapNode("lingzhou", "灵州", 3150f, 2250f, ownerHint = "xixia"),
        MapNode("xiazhou", "夏州", 3600f, 1800f, ownerHint = "xixia"),
        MapNode("liangzhou", "凉州", 2100f, 2750f, ownerHint = "xixia"),
        MapNode("lanzhou", "兰州", 3800f, 3000f, ownerHint = "xixia"),
        MapNode("xizhou", "熙州", 4300f, 3400f, ownerHint = "xixia"),
        MapNode("hezhou", "河州", 4000f, 3650f, ownerHint = "xixia"),
        MapNode("qingyang", "庆阳府", 5000f, 3000f, ownerHint = "jin"),
        MapNode("yanan", "延安府", 4700f, 2500f, ownerHint = "jin"),

        // ===== 大理西南 =====
        MapNode("dali", "大理", 2700f, 8200f, ownerHint = "dali", isCapital = true),
        MapNode("shanchan", "善阐", 3150f, 7850f, ownerHint = "dali"),
        MapNode("yongchang", "永昌", 2100f, 8250f, ownerHint = "dali"),
        MapNode("tengchong", "腾冲", 1800f, 8500f, ownerHint = "dali"),
        MapNode("nanning", "邕州", 6600f, 9100f, ownerHint = "song")
    )

    // 道路网络 — V1.6 扩充西夏、大理、海贸通道；后续再接贸易收益与补给规则。
    val roads: List<CityRoad> = listOf(
        CityRoad("linan", "suzhou", RoadType.CANAL),
        CityRoad("suzhou", "jiankang", RoadType.CANAL),
        CityRoad("linan", "shaoxing", RoadType.CANAL),
        CityRoad("shaoxing", "mingzhou", RoadType.CANAL),
        CityRoad("shaoxing", "wenzhou", RoadType.SEA),
        CityRoad("linan", "huzhou", RoadType.CANAL),
        CityRoad("huzhou", "suzhou", RoadType.CANAL),
        CityRoad("wenzhou", "taizhou", RoadType.SEA),
        CityRoad("taizhou", "mingzhou", RoadType.SEA),
        CityRoad("linan", "ningguo", RoadType.LAND),
        CityRoad("jiankang", "chizhou", RoadType.RIVER),
        CityRoad("chizhou", "jiangzhou", RoadType.RIVER),
        CityRoad("jiangzhou", "ezhou", RoadType.RIVER),
        CityRoad("ningguo", "rao_zhou", RoadType.LAND),
        CityRoad("rao_zhou", "hongzhou", RoadType.RIVER),
        CityRoad("hongzhou", "jizhou", RoadType.LAND),
        CityRoad("jizhou", "ganzhou", RoadType.MOUNTAIN),
        CityRoad("suzhou", "yangzhou", RoadType.RIVER),
        CityRoad("jiankang", "yangzhou", RoadType.RIVER),
        CityRoad("jiankang", "ezhou", RoadType.RIVER),
        CityRoad("ezhou", "jiangling", RoadType.RIVER),
        CityRoad("jiangling", "kuizhou", RoadType.RIVER),
        CityRoad("ezhou", "yuezhou", RoadType.RIVER),
        CityRoad("yuezhou", "tanzhou", RoadType.RIVER),
        CityRoad("yuezhou", "changde", RoadType.RIVER),
        CityRoad("changde", "jiangling", RoadType.LAND),
        CityRoad("hongzhou", "tanzhou", RoadType.RIVER),
        CityRoad("hongzhou", "ganzhou", RoadType.MOUNTAIN),
        CityRoad("tanzhou", "ganzhou", RoadType.MOUNTAIN),
        CityRoad("wenzhou", "fuzhou", RoadType.SEA),
        CityRoad("fuzhou", "quanzhou", RoadType.SEA),
        CityRoad("quanzhou", "chaozhou", RoadType.SEA),
        CityRoad("chaozhou", "guangzhou", RoadType.SEA),
        CityRoad("guangzhou", "lianzhou_port", RoadType.SEA),
        CityRoad("lianzhou_port", "qiongzhou", RoadType.SEA),
        CityRoad("lianzhou_port", "jiaozhi_route", RoadType.SEA),
        CityRoad("quanzhou", "south_sea", RoadType.SEA),
        CityRoad("mingzhou", "goryeo_route", RoadType.SEA),
        CityRoad("ganzhou", "guangzhou", RoadType.MOUNTAIN),
        CityRoad("chengdu", "zizhou", RoadType.RIVER),
        CityRoad("zizhou", "kuizhou", RoadType.RIVER),
        CityRoad("chengdu", "xinguan", RoadType.MOUNTAIN),
        CityRoad("xinguan", "xianren_pass", RoadType.PASS),
        CityRoad("xinguan", "lizhou", RoadType.LAND),
        CityRoad("lizhou", "langzhou", RoadType.MOUNTAIN),
        CityRoad("langzhou", "chengdu", RoadType.RIVER),
        CityRoad("chengdu", "luzhou", RoadType.RIVER),
        CityRoad("luzhou", "xuzhou_shu", RoadType.RIVER),
        CityRoad("chengdu", "yazhou", RoadType.MOUNTAIN),
        CityRoad("yazhou", "lizhou_south", RoadType.MOUNTAIN),
        CityRoad("lizhou_south", "dali", RoadType.MOUNTAIN),
        CityRoad("xuzhou_shu", "shanchan", RoadType.MOUNTAIN),
        CityRoad("dali", "shanchan", RoadType.LAND),
        CityRoad("dali", "yongchang", RoadType.MOUNTAIN),
        CityRoad("yongchang", "tengchong", RoadType.MOUNTAIN),
        CityRoad("guangzhou", "nanning", RoadType.LAND),
        CityRoad("nanning", "shanchan", RoadType.MOUNTAIN),
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
        CityRoad("hefei", "haozhou", RoadType.LAND),
        CityRoad("haozhou", "sizhou", RoadType.LAND),
        CityRoad("sizhou", "yingtianfu", RoadType.LAND),
        CityRoad("chuzhou", "yingtianfu", RoadType.LAND),
        CityRoad("yingtianfu", "kaifeng", RoadType.LAND),
        CityRoad("xinyang", "kaifeng", RoadType.LAND),
        CityRoad("dengzhou", "luoyang", RoadType.LAND),
        CityRoad("xiangyang", "luoyang", RoadType.MOUNTAIN),
        CityRoad("kaifeng", "luoyang", RoadType.RIVER),
        CityRoad("kaifeng", "daming", RoadType.LAND),
        CityRoad("kaifeng", "yingtianfu", RoadType.LAND),
        CityRoad("kaifeng", "xiangzhou", RoadType.LAND),
        CityRoad("xiangzhou", "weizhou", RoadType.LAND),
        CityRoad("weizhou", "daming", RoadType.LAND),
        CityRoad("luoyang", "hezhong", RoadType.RIVER),
        CityRoad("hezhong", "jingzhao", RoadType.LAND),
        CityRoad("jingzhao", "fengxiang", RoadType.PASS),
        CityRoad("fengxiang", "qinzhou", RoadType.MOUNTAIN),
        CityRoad("qinzhou", "xizhou", RoadType.PASS),
        CityRoad("jingzhao", "qingyang", RoadType.LAND),
        CityRoad("qingyang", "yanan", RoadType.LAND),
        CityRoad("yanan", "xiazhou", RoadType.MOUNTAIN),
        CityRoad("xizhou", "lanzhou", RoadType.MOUNTAIN),
        CityRoad("lanzhou", "hezhou", RoadType.MOUNTAIN),
        CityRoad("lanzhou", "liangzhou", RoadType.MOUNTAIN),
        CityRoad("lanzhou", "lingzhou", RoadType.LAND),
        CityRoad("lingzhou", "xingqing", RoadType.LAND),
        CityRoad("xingqing", "xiazhou", RoadType.LAND),
        CityRoad("xingqing", "liangzhou", RoadType.MOUNTAIN),
        CityRoad("daming", "zhending", RoadType.LAND),
        CityRoad("daming", "hejian", RoadType.LAND),
        CityRoad("zhending", "taiyuan", RoadType.MOUNTAIN),
        CityRoad("zhending", "zhongshan", RoadType.MOUNTAIN),
        CityRoad("hejian", "zhongshan", RoadType.LAND),
        CityRoad("zhongshan", "yanjing", RoadType.LAND),
        CityRoad("taiyuan", "yunzhong", RoadType.MOUNTAIN),
        CityRoad("yunzhong", "yanjing", RoadType.MOUNTAIN),
        CityRoad("taiyuan", "jingzhao", RoadType.MOUNTAIN)
    )

    val nodeMap: Map<String, MapNode> = nodes.associateBy { it.id }

    // V1.6 邻接表：城市/战略点 id → 直接相连节点集合（双向）
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

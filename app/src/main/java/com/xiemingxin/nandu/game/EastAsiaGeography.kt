package com.xiemingxin.nandu.game

/** WGS84 sample locations projected onto the same 16,000 × 10,000 strategic canvas. */
data class GeoLocation(val longitude: Float, val latitude: Float)

data class GeoMapPoint(val x: Float, val y: Float)

data class GeoTerritory(
    val factionId: String,
    val label: String,
    val labelLocation: GeoLocation,
    val boundary: List<GeoLocation>
)

data class GeoCityMark(
    val id: String,
    val name: String,
    val location: GeoLocation,
    val position: GeoMapPoint,
    val owner: String,
    val ownerName: String,
    val colorArgb: Long,
    val capital: Boolean,
    val actualCity: Boolean,
    val important: Boolean
)

object EastAsiaGeography {
    const val WEST = 87f
    const val EAST = 145f
    const val SOUTH = 15f
    const val NORTH = 49f
    const val WORLD_WIDTH = 16000f
    const val WORLD_HEIGHT = 10000f
    const val BASEMAP_ASSET = "images/map_v2/east_asia_relief.webp"

    // Modern WGS84 approximations identify historic prefectural seats, not city
    // boundaries. Trade waypoints are geographic sea-lane anchors, not cities.
    val cityLocations: Map<String, GeoLocation> = mapOf(
        "linan" to GeoLocation(120.155f, 30.275f),
        "shaoxing" to GeoLocation(120.581f, 30.030f),
        "suzhou" to GeoLocation(120.585f, 31.299f),
        "mingzhou" to GeoLocation(121.550f, 29.868f),
        "wenzhou" to GeoLocation(120.699f, 27.994f),
        "huzhou" to GeoLocation(120.087f, 30.895f),
        "taizhou" to GeoLocation(121.421f, 28.657f),
        "jiankang" to GeoLocation(118.796f, 32.060f),
        "ningguo" to GeoLocation(118.759f, 30.946f),
        "chizhou" to GeoLocation(117.491f, 30.665f),
        "rao_zhou" to GeoLocation(116.675f, 29.005f),
        "hongzhou" to GeoLocation(115.858f, 28.683f),
        "ganzhou" to GeoLocation(114.940f, 25.831f),
        "jizhou" to GeoLocation(114.986f, 27.112f),
        "jiangzhou" to GeoLocation(115.992f, 29.713f),
        "ezhou" to GeoLocation(114.316f, 30.593f),
        "jiangling" to GeoLocation(112.239f, 30.335f),
        "tanzhou" to GeoLocation(112.938f, 28.228f),
        "yuezhou" to GeoLocation(113.128f, 29.357f),
        "changde" to GeoLocation(111.699f, 29.031f),
        "xiangyang" to GeoLocation(112.122f, 32.009f),
        "dengzhou" to GeoLocation(112.088f, 32.687f),
        "xinguan" to GeoLocation(107.024f, 33.067f),
        "chengdu" to GeoLocation(104.066f, 30.573f),
        "zizhou" to GeoLocation(105.090f, 30.995f),
        "kuizhou" to GeoLocation(109.466f, 31.019f),
        "xianren_pass" to GeoLocation(106.183f, 33.524f),
        "lizhou" to GeoLocation(105.844f, 32.435f),
        "langzhou" to GeoLocation(106.004f, 31.558f),
        "luzhou" to GeoLocation(105.442f, 28.871f),
        "xuzhou_shu" to GeoLocation(104.630f, 28.760f),
        "yazhou" to GeoLocation(103.001f, 29.987f),
        "lizhou_south" to GeoLocation(102.357f, 29.232f),
        "fuzhou" to GeoLocation(119.296f, 26.074f),
        "quanzhou" to GeoLocation(118.675f, 24.874f),
        "guangzhou" to GeoLocation(113.264f, 23.129f),
        "chaozhou" to GeoLocation(116.622f, 23.657f),
        "lianzhou_port" to GeoLocation(109.120f, 21.482f),
        "qiongzhou" to GeoLocation(110.331f, 20.032f),
        "jiaozhi_route" to GeoLocation(107.100f, 20.900f),
        "south_sea" to GeoLocation(119.500f, 18.000f),
        "goryeo_route" to GeoLocation(125.000f, 35.500f),
        "yangzhou" to GeoLocation(119.412f, 32.394f),
        "chuzhou" to GeoLocation(119.020f, 33.610f),
        "hefei" to GeoLocation(117.227f, 31.820f),
        "shouchun" to GeoLocation(116.790f, 32.573f),
        "xinyang" to GeoLocation(114.075f, 32.123f),
        "yingtianfu" to GeoLocation(115.650f, 34.415f),
        "haozhou" to GeoLocation(117.559f, 32.867f),
        "sizhou" to GeoLocation(118.216f, 33.480f),
        "kaifeng" to GeoLocation(114.308f, 34.797f),
        "luoyang" to GeoLocation(112.454f, 34.619f),
        "jingzhao" to GeoLocation(108.939f, 34.341f),
        "hezhong" to GeoLocation(110.450f, 34.867f),
        "fengxiang" to GeoLocation(107.395f, 34.523f),
        "qinzhou" to GeoLocation(105.724f, 34.580f),
        "daming" to GeoLocation(115.148f, 36.286f),
        "xiangzhou" to GeoLocation(114.392f, 36.097f),
        "weizhou" to GeoLocation(114.064f, 35.398f),
        "taiyuan" to GeoLocation(112.549f, 37.870f),
        "zhending" to GeoLocation(114.572f, 38.144f),
        "hejian" to GeoLocation(116.084f, 38.446f),
        "zhongshan" to GeoLocation(114.995f, 38.517f),
        "yanjing" to GeoLocation(116.407f, 39.904f),
        "yunzhong" to GeoLocation(113.300f, 40.077f),
        "xingqing" to GeoLocation(106.230f, 38.487f),
        "lingzhou" to GeoLocation(106.198f, 37.986f),
        "xiazhou" to GeoLocation(108.943f, 37.595f),
        "liangzhou" to GeoLocation(102.638f, 37.928f),
        "lanzhou" to GeoLocation(103.834f, 36.061f),
        "xizhou" to GeoLocation(104.618f, 35.581f),
        "hezhou" to GeoLocation(103.210f, 35.601f),
        "qingyang" to GeoLocation(107.638f, 35.734f),
        "yanan" to GeoLocation(109.489f, 36.585f),
        "dali" to GeoLocation(100.226f, 25.592f),
        "shanchan" to GeoLocation(102.713f, 25.040f),
        "yongchang" to GeoLocation(99.168f, 25.112f),
        "tengchong" to GeoLocation(98.490f, 25.018f),
        "nanning" to GeoLocation(108.366f, 22.817f)
    )

    private fun line(vararg positions: Pair<Float, Float>): List<GeoLocation> =
        positions.map { GeoLocation(it.first, it.second) }

    // Political polygons are intentionally documented historical approximations;
    // actual city colors always come from live GameState, never these outlines.
    val territories: List<GeoTerritory> = listOf(
        GeoTerritory(
            "song", "大宋", GeoLocation(112.0f, 28.2f),
            line(101.3f to 33.2f, 105.4f to 33.8f, 108.0f to 33.6f, 111.1f to 33.8f,
                114.2f to 34.0f, 116.1f to 34.5f, 118.1f to 34.0f, 120.0f to 32.4f,
                121.4f to 30.9f, 121.5f to 28.3f, 120.3f to 26.1f, 118.7f to 24.3f,
                116.3f to 23.1f, 113.2f to 22.5f, 110.0f to 21.7f, 107.5f to 22.0f,
                106.0f to 24.2f, 103.7f to 26.5f, 102.0f to 29.0f, 101.3f to 33.2f)
        ),
        GeoTerritory(
            "jin", "金国", GeoLocation(114.8f, 38.2f),
            line(103.0f to 41.5f, 108.0f to 42.0f, 114.0f to 43.5f, 119.0f to 45.3f,
                125.0f to 44.8f, 127.1f to 42.0f, 123.5f to 40.1f, 122.0f to 39.2f,
                120.0f to 37.4f, 121.1f to 36.9f, 120.1f to 35.2f, 118.1f to 34.0f,
                116.1f to 34.5f, 114.2f to 34.0f, 111.1f to 33.8f, 108.0f to 33.6f,
                105.4f to 33.8f, 105.0f to 36.0f, 106.5f to 38.0f, 103.0f to 41.5f)
        ),
        GeoTerritory(
            "xixia", "西夏", GeoLocation(103.9f, 38.1f),
            line(98.5f to 41.7f, 102.4f to 42.2f, 106.7f to 41.5f, 109.0f to 39.5f,
                108.2f to 37.0f, 105.9f to 35.6f, 102.4f to 35.1f, 99.2f to 36.9f,
                97.5f to 39.3f, 98.5f to 41.7f)
        ),
        GeoTerritory(
            "dali", "大理", GeoLocation(101.0f, 25.3f),
            line(98.0f to 28.4f, 100.0f to 28.8f, 102.7f to 27.8f, 104.0f to 25.8f,
                103.0f to 23.0f, 100.8f to 22.0f, 98.5f to 23.2f, 97.0f to 25.7f,
                98.0f to 28.4f)
        )
    )

    private val fallbackNames = mapOf(
        "song" to "大宋", "jin" to "金国", "xixia" to "西夏", "dali" to "大理",
        "goryeo" to "高丽", "sea_trade" to "海贸", "rebel" to "义军"
    )

    private val fallbackColors = mapOf(
        "song" to 0xFF4C89B3L, "jin" to 0xFFB85E52L, "xixia" to 0xFFC0A069L,
        "dali" to 0xFF689868L, "goryeo" to 0xFF9690B9L, "sea_trade" to 0xFF74AAB2L,
        "rebel" to 0xFFAD9062L
    )

    private val importantIds = setOf(
        "yingtianfu", "linan", "jiankang", "kaifeng", "luoyang", "yanjing", "taiyuan",
        "chengdu", "xiangyang", "ezhou", "yangzhou", "guangzhou", "quanzhou", "xingqing",
        "dali", "jingzhao", "jiangling", "fuzhou", "qiongzhou", "lanzhou", "xianren_pass"
    )

    fun project(location: GeoLocation): GeoMapPoint = GeoMapPoint(
        x = ((location.longitude - WEST) / (EAST - WEST) * WORLD_WIDTH).coerceIn(0f, WORLD_WIDTH),
        y = ((NORTH - location.latitude) / (NORTH - SOUTH) * WORLD_HEIGHT).coerceIn(0f, WORLD_HEIGHT)
    )

    fun unproject(point: GeoMapPoint): GeoLocation = GeoLocation(
        longitude = WEST + point.x.coerceIn(0f, WORLD_WIDTH) / WORLD_WIDTH * (EAST - WEST),
        latitude = NORTH - point.y.coerceIn(0f, WORLD_HEIGHT) / WORLD_HEIGHT * (NORTH - SOUTH)
    )

    fun cityMarks(state: GameState): List<GeoCityMark> {
        val liveCities = state.cities.associateBy { it.id }
        val factions = state.factions.associateBy { it.id }
        return MapData.nodes.mapNotNull { node ->
            val location = cityLocations[node.id] ?: return@mapNotNull null
            val city = liveCities[node.id]
            val factionId = city?.owner ?: node.ownerHint
            val faction = factions[factionId]
            GeoCityMark(
                id = node.id,
                name = city?.name ?: node.name,
                location = location,
                position = project(location),
                owner = factionId,
                ownerName = faction?.name ?: fallbackNames[factionId] ?: "边地诸部",
                colorArgb = faction?.colorArgb ?: fallbackColors[factionId] ?: 0xFF918D80L,
                capital = city?.let { it.isCapital || faction?.capitalCityId == it.id }
                    ?: node.isCapital,
                actualCity = city != null,
                important = node.id in importantIds || node.isCapital
            )
        }
    }

    fun territoryColor(state: GameState, factionId: String): Long =
        state.factions.firstOrNull { it.id == factionId }?.colorArgb
            ?: fallbackColors[factionId]
            ?: 0xFF918D80L
}

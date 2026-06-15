package com.xiemingxin.nandu.game

/**
 * V0.8 视觉地形层：大江大河大山的地理标志线。
 * 坐标为世界坐标系（与MapNode一致），供MapScreen的Canvas绘制。
 * 这些是"地理标志"，让玩家一眼识别长江防线、太行阻隔、黄河天险。
 */
data class TerrainLine(
    val name: String,
    val points: List<Pair<Float, Float>>,
    val kind: String,        // river水系 / mountain山脉
    val labelX: Float,       // 名称标注位置
    val labelY: Float
)

object TerrainFeatures {

    val rivers: List<TerrainLine> = listOf(
        TerrainLine(
            "黄河",
            listOf(5500f to 3000f, 6800f to 3100f, 8000f to 3000f, 8600f to 3400f,
                   9400f to 3300f, 10400f to 3200f, 11200f to 3400f),
            "river", 7000f, 2950f
        ),
        TerrainLine(
            "长江",
            listOf(5000f to 6400f, 6200f to 6200f, 7000f to 6000f, 7800f to 6100f,
                   8400f to 6400f, 9400f to 6200f, 10200f to 5700f, 11000f to 5800f, 12000f to 6000f),
            "river", 6000f, 6150f
        ),
        TerrainLine(
            "淮河",
            listOf(8400f to 4500f, 9000f to 4400f, 9600f to 4600f, 10400f to 4700f, 11200f to 4800f),
            "river", 9000f, 4350f
        )
    )

    val mountains: List<TerrainLine> = listOf(
        TerrainLine(
            "太行山",
            listOf(7800f to 2000f, 8000f to 2600f, 8100f to 3200f, 8300f to 3800f, 8500f to 4200f),
            "mountain", 7600f, 3000f
        ),
        TerrainLine(
            "秦岭",
            listOf(5200f to 4400f, 6000f to 4200f, 6600f to 4000f, 7200f to 4200f, 7800f to 4400f),
            "mountain", 6400f, 3850f
        )
    )

    val all: List<TerrainLine> = rivers + mountains
}

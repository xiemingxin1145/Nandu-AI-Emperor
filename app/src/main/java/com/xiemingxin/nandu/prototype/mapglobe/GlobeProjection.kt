package com.xiemingxin.nandu.prototype.mapglobe

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * MAP-PROTOTYPE-001 独立投影工具。
 *
 * 不修改正式 MapScreen / MapData。
 * 将现有 16000×10000 世界坐标近似映射到 2.5D 伪球面，再可逆地展开回平面。
 */
object GlobeProjection {
    const val WORLD_W = 16000f
    const val WORLD_H = 10000f

    fun worldToUv(worldX: Float, worldY: Float): Pair<Float, Float> {
        val u = (worldX / WORLD_W).coerceIn(0f, 1f)
        val v = (worldY / WORLD_H).coerceIn(0f, 1f)
        return u to v
    }

    fun project(
        worldX: Float,
        worldY: Float,
        canvasW: Float,
        canvasH: Float,
        rotYaw: Float,
        rotPitch: Float,
        flatten: Float,
        cameraWorldX: Float,
        cameraWorldY: Float,
        flatZoom: Float
    ): ProjectedPoint {
        val (u, v) = worldToUv(worldX, worldY)
        val lon = (u - 0.5f) * PI.toFloat() * 0.95f + rotYaw
        val lat = (0.5f - v) * PI.toFloat() * 0.55f + rotPitch

        val cosLat = cos(lat)
        val x3 = cosLat * sin(lon)
        val y3 = sin(lat)
        val z3 = cosLat * cos(lon)

        val radius = minOf(canvasW, canvasH) * 0.42f
        val perspective = 1f / (1.35f - z3 * 0.35f)
        val sphereX = canvasW * 0.5f + x3 * radius * perspective
        val sphereY = canvasH * 0.5f - y3 * radius * perspective
        val depth = (z3 + 1f) * 0.5f
        val visibleOnSphere = z3 > -0.15f

        val flatX = canvasW * 0.5f + (worldX - cameraWorldX) * flatZoom
        val flatY = canvasH * 0.5f + (worldY - cameraWorldY) * flatZoom

        val t = flatten.coerceIn(0f, 1f)
        val sx = sphereX + (flatX - sphereX) * t
        val sy = sphereY + (flatY - sphereY) * t
        val scale = (0.55f + depth * 0.7f) * (1f - t) + 1f * t
        val alpha = if (t > 0.55f) 1f else if (visibleOnSphere) (0.35f + depth * 0.65f) else 0.08f

        return ProjectedPoint(
            screenX = sx,
            screenY = sy,
            scale = scale,
            alpha = alpha,
            depth = depth,
            visible = t > 0.55f || visibleOnSphere
        )
    }

    fun hitTestRadius(base: Float, scale: Float, flatten: Float): Float {
        return (base * scale * (1.1f - flatten * 0.2f)).coerceIn(12f, 48f)
    }
}

data class ProjectedPoint(
    val screenX: Float,
    val screenY: Float,
    val scale: Float,
    val alpha: Float,
    val depth: Float,
    val visible: Boolean
)

data class PrototypeCityMark(
    val id: String,
    val name: String,
    val worldX: Float,
    val worldY: Float,
    val faction: String,
    val isCapital: Boolean = false
)

object PrototypeMapSampleData {
    val cities: List<PrototypeCityMark> = listOf(
        PrototypeCityMark("yingtianfu", "应天府", 10000f, 4000f, "song", isCapital = true),
        PrototypeCityMark("linan", "杭州", 11000f, 6800f, "song"),
        PrototypeCityMark("jiankang", "建康", 10200f, 5600f, "song"),
        PrototypeCityMark("xiangyang", "襄阳", 8200f, 5000f, "song"),
        PrototypeCityMark("kaifeng", "开封", 9400f, 3400f, "jin", isCapital = true),
        PrototypeCityMark("yanjing", "燕京", 10200f, 1200f, "jin", isCapital = true),
        PrototypeCityMark("taiyuan", "太原", 8200f, 2400f, "jin"),
        PrototypeCityMark("xingqing", "兴庆府", 2600f, 2100f, "xixia", isCapital = true),
        PrototypeCityMark("dali", "大理", 2400f, 8200f, "dali", isCapital = true),
        PrototypeCityMark("quanzhou", "泉州", 11200f, 8900f, "song"),
        PrototypeCityMark("guangzhou", "广州", 8800f, 9200f, "song"),
        PrototypeCityMark("chengdu", "成都", 5200f, 5400f, "song")
    )

    val factionColors: Map<String, Long> = mapOf(
        "song" to 0xFF2E86C1,
        "jin" to 0xFFB22222,
        "xixia" to 0xFFB38A48,
        "dali" to 0xFF3F7A4D
    )
}

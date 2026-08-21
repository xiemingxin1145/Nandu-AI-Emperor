package com.xiemingxin.nandu.prototype.mapglobe

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        flatZoom: Float,
        globeZoom: Float = 1f
    ): ProjectedPoint {
        val (u, v) = worldToUv(worldX, worldY)
        val lon = (u - 0.5f) * PI.toFloat() * 0.95f + rotYaw
        val lat = (0.5f - v) * PI.toFloat() * 0.55f + rotPitch

        val cosLat = cos(lat)
        val x3 = cosLat * sin(lon)
        val y3 = sin(lat)
        val z3 = cosLat * cos(lon)

        val radius = minOf(canvasW, canvasH) * 0.42f * globeZoom.coerceIn(0.78f, 1.42f)
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
    val factionName: String,
    val factionColorArgb: Long,
    val isCapital: Boolean = false,
    val hasCityState: Boolean = false
)

/** The prototype owns no cities: every mark is derived from the formal world graph. */
object GlobeMapWorldState {
    private val strategicFactionNames = mapOf(
        "song" to "大宋",
        "jin" to "金国",
        "xixia" to "西夏",
        "dali" to "大理",
        "goryeo" to "高丽",
        "sea_trade" to "海贸诸商",
        "rebel" to "地方义军"
    )

    private val strategicFactionColors = mapOf(
        "song" to 0xFF2E86C1L,
        "jin" to 0xFFB22222L,
        "xixia" to 0xFFB38A48L,
        "dali" to 0xFF3F7A4DL,
        "goryeo" to 0xFF798CC4L,
        "sea_trade" to 0xFF48A3B1L,
        "rebel" to 0xFF8A6D3BL
    )

    fun cities(state: GameState): List<PrototypeCityMark> {
        val actualCities = state.cities.associateBy { it.id }
        val factions = state.factions.associateBy { it.id }
        return MapData.nodes.map { node ->
            val actualCity = actualCities[node.id]
            val factionId = actualCity?.owner ?: node.ownerHint
            val faction = factions[factionId]
            PrototypeCityMark(
                id = node.id,
                name = actualCity?.name ?: node.name,
                worldX = node.worldX,
                worldY = node.worldY,
                faction = factionId,
                factionName = faction?.name ?: strategicFactionNames[factionId] ?: "边地诸部",
                factionColorArgb = faction?.colorArgb ?: strategicFactionColors[factionId] ?: 0xFF8C8C8CL,
                isCapital = actualCity?.let { it.isCapital || faction?.capitalCityId == it.id }
                    ?: node.isCapital,
                hasCityState = actualCity != null
            )
        }
    }
}

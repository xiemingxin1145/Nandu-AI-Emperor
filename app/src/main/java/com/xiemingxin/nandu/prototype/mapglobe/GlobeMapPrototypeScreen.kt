package com.xiemingxin.nandu.prototype.mapglobe

import android.graphics.Paint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.MapLayerMode
import com.xiemingxin.nandu.game.RoadType
import com.xiemingxin.nandu.ui.components.AssetImage
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

/** Device-only preview: MapData and GameState remain the only world sources. */
@Composable
fun GlobeMapPrototypeScreen(
    gameState: GameState,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onExit)

    val scope = rememberCoroutineScope()
    val flatten = remember { Animatable(0f) }
    val marks = remember(gameState.cities, gameState.factions) { GlobeMapWorldState.cities(gameState) }
    val markById = remember(marks) { marks.associateBy { it.id } }
    val actualCities = remember(gameState.cities) { gameState.cities.associateBy { it.id } }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
    }

    var rotYaw by remember { mutableFloatStateOf(-0.24f) }
    var rotPitch by remember { mutableFloatStateOf(0.06f) }
    var globeZoom by remember { mutableFloatStateOf(1f) }
    var cameraX by remember { mutableFloatStateOf(8400f) }
    var cameraY by remember { mutableFloatStateOf(5000f) }
    var flatZoom by remember { mutableFloatStateOf(0.065f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    val isFlat = flatten.value > 0.92f

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF070B12))) {
        AssetImage(
            path = ArtResourceRegistry.mapBackground(MapLayerMode.DIPLOMACY),
            fallbackPath = "images/map/song_world_parchment.webp",
            contentDescription = "天下疆域底图",
            contentScale = ContentScale.Crop,
            placeholderText = "图",
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlat) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        if (isFlat) {
                            flatZoom = (flatZoom * gestureZoom).coerceIn(0.032f, 0.16f)
                            cameraX = (cameraX - pan.x / flatZoom).coerceIn(0f, GlobeProjection.WORLD_W)
                            cameraY = (cameraY - pan.y / flatZoom).coerceIn(0f, GlobeProjection.WORLD_H)
                        } else {
                            globeZoom = (globeZoom * gestureZoom).coerceIn(0.78f, 1.42f)
                            rotYaw += pan.x * 0.0042f
                            rotPitch = (rotPitch - pan.y * 0.0022f).coerceIn(-0.42f, 0.48f)
                        }
                    }
                }
                .pointerInput(marks, flatten.value, rotYaw, rotPitch, cameraX, cameraY, flatZoom, globeZoom) {
                    detectTapGestures { tap ->
                        val hits = marks.mapNotNull { city ->
                            val point = GlobeProjection.project(
                                city.worldX, city.worldY,
                                size.width.toFloat(), size.height.toFloat(),
                                rotYaw, rotPitch, flatten.value,
                                cameraX, cameraY, flatZoom, globeZoom
                            )
                            if (!point.visible) return@mapNotNull null
                            val radius = GlobeProjection.hitTestRadius(
                                if (city.isCapital) 19f else 13f,
                                point.scale,
                                flatten.value
                            ).coerceAtLeast(22f)
                            val distance = sqrt((tap.x - point.screenX).pow(2) + (tap.y - point.screenY).pow(2))
                            if (distance <= radius) city to distance else null
                        }
                        selectedId = hits.minByOrNull { it.second }?.first?.id
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val transition = flatten.value

            drawRect(Color(0xD8070B12).copy(alpha = 0.84f - transition * 0.20f))

            if (transition < 0.90f) {
                val radius = minOf(width, height) * 0.46f * globeZoom * (1f - transition * 0.15f)
                val center = Offset(width * 0.5f, height * 0.50f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xB52B4261), Color(0xAE17253A), Color(0xCB090F18)),
                        center = center,
                        radius = radius * 1.10f
                    ),
                    radius = radius,
                    center = center
                )
                drawCircle(
                    color = Color(0xFFC9A227).copy(alpha = 0.42f * (1f - transition)),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2.5f)
                )
            }

            val projected = marks.associateWith { city ->
                GlobeProjection.project(
                    city.worldX, city.worldY, width, height,
                    rotYaw, rotPitch, transition, cameraX, cameraY, flatZoom, globeZoom
                )
            }

            if (transition < 0.82f) {
                marks.filter { it.faction.isNotBlank() }
                    .groupBy { it.faction }
                    .values
                    .filter { it.size >= 3 }
                    .forEach { factionCities ->
                        val centerX = factionCities.map { it.worldX }.average().toFloat()
                        val centerY = factionCities.map { it.worldY }.average().toFloat()
                        val center = GlobeProjection.project(
                            centerX, centerY, width, height,
                            rotYaw, rotPitch, transition, cameraX, cameraY, flatZoom, globeZoom
                        )
                        if (center.visible) {
                            val radius = (35f + factionCities.size * 2.7f).coerceAtMost(145f) * center.scale
                            drawCircle(
                                color = Color(factionCities.first().factionColorArgb)
                                    .copy(alpha = 0.32f * center.alpha * (1f - transition)),
                                radius = radius,
                                center = Offset(center.screenX, center.screenY)
                            )
                        }
                    }
            }

            MapData.roads.forEach { road ->
                val startMark = markById[road.fromId] ?: return@forEach
                val endMark = markById[road.toId] ?: return@forEach
                val start = projected[startMark] ?: return@forEach
                val end = projected[endMark] ?: return@forEach
                if (!start.visible || !end.visible) return@forEach
                val color = when (road.type) {
                    RoadType.RIVER, RoadType.CANAL, RoadType.SEA -> Color(0xFF70A9C9)
                    RoadType.PASS, RoadType.MOUNTAIN -> Color(0xFFB49565)
                    RoadType.LAND -> Color(0xFFC5AF88)
                }
                drawLine(
                    color = color.copy(alpha = (0.14f + transition * 0.29f) * minOf(start.alpha, end.alpha)),
                    start = Offset(start.screenX, start.screenY),
                    end = Offset(end.screenX, end.screenY),
                    strokeWidth = if (transition > 0.55f) 2f else 1.3f
                )
            }

            val occupiedLabels = mutableListOf<Offset>()
            projected.entries.sortedBy { it.value.depth }.forEach { (city, point) ->
                if (!point.visible && transition < 0.55f) return@forEach
                val center = Offset(point.screenX, point.screenY)
                val radius = GlobeProjection.hitTestRadius(
                    if (city.isCapital) 14f else 8f,
                    point.scale,
                    transition
                ).coerceAtMost(if (city.isCapital) 19f else 13f)
                if (selectedId == city.id) {
                    drawCircle(Color(0xFFC9A227).copy(alpha = 0.47f), radius * 1.9f, center)
                }
                drawCircle(Color(city.factionColorArgb).copy(alpha = point.alpha), radius, center)
                drawCircle(Color.White.copy(alpha = 0.30f * point.alpha), radius, center, style = Stroke(1.3f))

                val important = city.isCapital || selectedId == city.id
                val zoomAllowsLabel = transition > 0.7f && flatZoom >= 0.076f && city.hasCityState
                if (point.alpha > 0.38f && (important || zoomAllowsLabel)) {
                    drawCityLabel(
                        city = city,
                        center = center,
                        radius = radius,
                        alpha = point.alpha,
                        selected = selectedId == city.id,
                        occupiedLabels = occupiedLabels,
                        paint = labelPaint
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xD90A0E16))
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Text("寰宇试览 · 万里山河", color = Color(0xFFC9A227), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(
                if (isFlat) "双指缩放 · 拖拽巡视 · 点选城池" else "拖拽转动天下 · 双指远近 · 点选城池",
                color = Color(0xFFB6A27B),
                fontSize = 11.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 5.dp)) {
                marks.distinctBy { it.faction }
                    .filter { it.faction in setOf("song", "jin", "xixia", "dali") }
                    .take(4)
                    .forEach { city ->
                        Text("● ${city.factionName}", color = Color(city.factionColorArgb), fontSize = 10.sp)
                    }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xE00A0E16))
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedId?.let { id ->
                val selected = markById[id] ?: return@let
                val actual = actualCities[id]
                val details = when {
                    selected.isCapital -> "都城行在"
                    actual != null -> "守军 ${actual.troops} · 城防 ${actual.defense}"
                    else -> "疆域战略节点"
                }
                Text("${selected.name} · ${selected.factionName} · $details", color = Color(0xFFE8DCC0), fontSize = 12.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { scope.launch { flatten.animateTo(1f, tween(820, easing = FastOutSlowInEasing)) } },
                    enabled = flatten.value < 0.95f,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4E16)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("展开山河", color = Color(0xFFE8DCC0), fontSize = 13.sp) }
                Button(
                    onClick = {
                        scope.launch {
                            flatten.animateTo(0f, tween(820, easing = FastOutSlowInEasing))
                            selectedId = null
                        }
                    },
                    enabled = flatten.value > 0.05f,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("收回寰宇", color = Color(0xFFE8DCC0), fontSize = 13.sp) }
            }

            Button(
                onClick = onExit,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34322E)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("返回正式山河图", color = Color(0xFFD3C6AB), fontSize = 12.sp) }
        }
    }
}

private fun DrawScope.drawCityLabel(
    city: PrototypeCityMark,
    center: Offset,
    radius: Float,
    alpha: Float,
    selected: Boolean,
    occupiedLabels: MutableList<Offset>,
    paint: Paint
) {
    if (!selected && occupiedLabels.any { (it - center).getDistance() < 48f }) return
    occupiedLabels += center
    paint.color = android.graphics.Color.argb((alpha * 225).toInt().coerceIn(0, 255), 232, 220, 192)
    paint.textSize = if (city.isCapital || selected) 28f else 24f
    drawContext.canvas.nativeCanvas.drawText(city.name, center.x, center.y + radius + 24f, paint)
}

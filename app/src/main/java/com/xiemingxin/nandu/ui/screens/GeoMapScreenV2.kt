package com.xiemingxin.nandu.ui.screens

import android.graphics.BitmapFactory
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.EastAsiaGeography
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.GeoCityMark
import com.xiemingxin.nandu.game.GeoLocation
import com.xiemingxin.nandu.game.GeoMapPoint
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.MapLayerMode
import com.xiemingxin.nandu.game.RoadType
import com.xiemingxin.nandu.game.WorldTurnAction
import com.xiemingxin.nandu.game.WorldTurnReplay
import com.xiemingxin.nandu.ui.components.SeasonalTransitionOverlay
import com.xiemingxin.nandu.ui.components.WorldTurnReplayOverlay
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val GeoGold = Color(0xFFE0C486)
private val GeoPaper = Color(0xFFF0E8D3)
private val GeoPanel = Color(0xEE151B1B)

/** The near strategic map and the far "寰宇" view are the same geographic camera. */
@Composable
fun GeoMapScreenV2(
    gameState: GameState,
    replay: WorldTurnReplay? = null,
    lastReplay: WorldTurnReplay? = null,
    onDismissReplay: () -> Unit = {},
    onReopenReplay: () -> Unit = {},
    onCitySelected: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val relief = remember(context) {
        runCatching {
            context.assets.open(EastAsiaGeography.BASEMAP_ASSET).use { input ->
                BitmapFactory.decodeStream(input)?.asImageBitmap()
            }
        }.getOrNull()
    }
    val marks = remember(gameState.cities, gameState.factions) { EastAsiaGeography.cityMarks(gameState) }
    val marksById = remember(marks) { marks.associateBy { it.id } }
    val liveCities = remember(gameState.cities) { gameState.cities.associateBy { it.id } }
    val labelPaint = remember {
        Paint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.BLACK)
        }
    }
    val initialCenter = EastAsiaGeography.project(GeoLocation(111.4f, 33.0f))

    var cameraX by remember { mutableFloatStateOf(initialCenter.x) }
    var cameraY by remember { mutableFloatStateOf(initialCenter.y) }
    var zoom by remember { mutableFloatStateOf(0.095f) }
    var showCities by remember { mutableStateOf(true) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var activeLayer by remember { mutableStateOf(MapLayerMode.MILITARY) }
    var replayIndex by remember(replay?.turn) { mutableIntStateOf(-1) }
    var showSeasonTransition by remember(replay?.turn) {
        mutableStateOf(replay?.seasonalTransition != null)
    }

    LaunchedEffect(replay?.turn) {
        val current = replay ?: return@LaunchedEffect
        if (current.seasonalTransition != null) {
            delay(3400)
            showSeasonTransition = false
        }
        current.actions.indices.forEach { index ->
            replayIndex = index
            delay(1350)
        }
        replayIndex = current.actions.size
    }

    val activeReplayAction = replay?.actions?.getOrNull(replayIndex)
    LaunchedEffect(replay?.turn, replayIndex, showSeasonTransition) {
        if (showSeasonTransition) return@LaunchedEffect
        val action = activeReplayAction ?: return@LaunchedEffect
        val mark = marksById[action.targetCityId.ifBlank { action.originCityId }] ?: return@LaunchedEffect
        cameraX = mark.position.x
        cameraY = mark.position.y
        zoom = 0.14f
        selectedId = mark.id
        showCities = true
        activeLayer = MapLayerMode.MILITARY
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0D1F2C))) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.052f, 0.26f)
                        cameraX = (cameraX - pan.x / zoom).coerceIn(0f, EastAsiaGeography.WORLD_WIDTH)
                        cameraY = (cameraY - pan.y / zoom).coerceIn(0f, EastAsiaGeography.WORLD_HEIGHT)
                    }
                }
                .pointerInput(marks, showCities, zoom, cameraX, cameraY) {
                    detectTapGestures { tap ->
                        if (!showCities) return@detectTapGestures
                        val nearest = marks
                            .filter { it.actualCity || it.important || zoom >= 0.12f }
                            .map { mark ->
                                val screen = screenPoint(mark.position, cameraX, cameraY, zoom, size.width.toFloat(), size.height.toFloat())
                                mark to (screen - tap).getDistance()
                            }
                            .minByOrNull { it.second }
                        selectedId = if (nearest != null && nearest.second <= 38f) nearest.first.id else null
                    }
                }
        ) {
            val left = (size.width / 2f - cameraX * zoom).roundToInt()
            val top = (size.height / 2f - cameraY * zoom).roundToInt()
            relief?.let { image ->
                drawImage(
                    image = image,
                    dstOffset = IntOffset(left, top),
                    dstSize = IntSize(
                        (EastAsiaGeography.WORLD_WIDTH * zoom).roundToInt().coerceAtLeast(1),
                        (EastAsiaGeography.WORLD_HEIGHT * zoom).roundToInt().coerceAtLeast(1)
                    )
                )
            }

            drawTerritoryOverlays(gameState, cameraX, cameraY, zoom)
            drawGeographicLabels(cameraX, cameraY, zoom, labelPaint)

            if (zoom >= 0.105f && activeLayer != MapLayerMode.DIPLOMACY) {
                MapData.roads.forEach { road ->
                    val start = marksById[road.fromId] ?: return@forEach
                    val end = marksById[road.toId] ?: return@forEach
                    if (activeLayer == MapLayerMode.TRADE && road.type !in setOf(RoadType.SEA, RoadType.RIVER, RoadType.CANAL)) return@forEach
                    val color = when (road.type) {
                        RoadType.SEA, RoadType.RIVER, RoadType.CANAL -> Color(0xFF86BAC4)
                        RoadType.PASS, RoadType.MOUNTAIN -> Color(0xFFBFAA7A)
                        RoadType.LAND -> Color(0xFFD0BE99)
                    }
                    drawLine(
                        color = color.copy(alpha = if (activeLayer == MapLayerMode.TRADE) 0.69f else 0.38f),
                        start = geoScreen(start.position, cameraX, cameraY, zoom),
                        end = geoScreen(end.position, cameraX, cameraY, zoom),
                        strokeWidth = if (activeLayer == MapLayerMode.TRADE) 2.6f else 1.6f,
                        pathEffect = if (road.type == RoadType.SEA) PathEffect.dashPathEffect(floatArrayOf(12f, 9f)) else null,
                        cap = StrokeCap.Round
                    )
                }
            }

            activeReplayAction?.let { action ->
                drawActualWorldAction(action, marksById, cameraX, cameraY, zoom)
            }

            if (showCities) {
                val occupiedLabels = mutableListOf<Offset>()
                marks.sortedWith(compareBy<GeoCityMark> { !it.capital }.thenBy { !it.important }).forEach { mark ->
                    val visibleAtScale = when {
                        zoom < 0.079f -> mark.capital || mark.id in setOf("chengdu", "xiangyang", "guangzhou", "xingqing", "dali")
                        zoom < 0.12f -> mark.important || mark.actualCity
                        else -> true
                    }
                    if (!visibleAtScale) return@forEach
                    val point = geoScreen(mark.position, cameraX, cameraY, zoom)
                    if (point.x < -40f || point.y < -40f || point.x > size.width + 40f || point.y > size.height + 40f) return@forEach
                    val radius = if (mark.capital) 9.2f else if (mark.important) 6.7f else 4.8f
                    if (selectedId == mark.id) {
                        drawCircle(GeoGold.copy(alpha = 0.45f), radius + 10f, point)
                        drawCircle(GeoGold, radius + 9f, point, style = Stroke(1.7f))
                    }
                    drawCircle(Color(0xCC111313), radius + 2f, point)
                    drawCircle(Color(mark.colorArgb), radius, point)
                    if (mark.capital) drawCircle(GeoPaper, radius + 2.5f, point, style = Stroke(1.2f))

                    val showLabel = mark.capital || selectedId == mark.id ||
                        (zoom >= 0.08f && mark.important) || (zoom >= 0.15f && mark.actualCity)
                    if (showLabel && (selectedId == mark.id || occupiedLabels.none { (it - point).getDistance() < 54f })) {
                        occupiedLabels += point
                        labelPaint.color = if (mark.capital) 0xFFF0D28C.toInt() else 0xFFF0E8D3.toInt()
                        labelPaint.textSize = if (mark.capital) 30f else 25f
                        drawContext.canvas.nativeCanvas.drawText(mark.name, point.x, point.y + radius + 26f, labelPaint)
                    }
                }
            }

            if (activeLayer == MapLayerMode.MILITARY && zoom >= 0.105f) {
                gameState.armies.filter { it.troops > 0 }.forEach { army ->
                    val mark = marksById[army.currentCityId] ?: return@forEach
                    val center = geoScreen(mark.position, cameraX, cameraY, zoom) + Offset(14f, -15f)
                    val ownerColor = Color(EastAsiaGeography.territoryColor(gameState, army.ownerFactionId))
                    val pennant = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(center.x + 15f, center.y + 4f)
                        lineTo(center.x, center.y + 9f)
                        close()
                    }
                    drawPath(pennant, ownerColor)
                    drawLine(GeoPaper, center, center + Offset(0f, 16f), strokeWidth = 1.4f)
                }
            }
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .background(GeoPanel.copy(alpha = 0.95f)).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("万里山河", color = GeoGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("${gameState.era} · ${gameState.cities.count { it.owner == "song" }}座城池在宋", color = Color(0xFFC5BAA4), fontSize = 10.sp)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    GeoControl("寰宇", zoom < 0.079f) {
                        val center = EastAsiaGeography.project(GeoLocation(113f, 33.0f))
                        cameraX = center.x; cameraY = center.y; zoom = 0.062f; selectedId = null
                    }
                    GeoControl("山河", zoom in 0.079f..0.125f) {
                        cameraX = initialCenter.x; cameraY = initialCenter.y; zoom = 0.10f; selectedId = null
                    }
                    GeoControl("近览", zoom > 0.125f) {
                        val focus = selectedId?.let { marksById[it]?.position } ?: marksById["yingtianfu"]?.position ?: initialCenter
                        cameraX = focus.x; cameraY = focus.y; zoom = 0.17f
                    }
                }
            }

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MapLayerMode.values().forEach { layer ->
                    GeoControl(layer.label, activeLayer == layer) { activeLayer = layer }
                }
                GeoControl(if (showCities) "隐藏城点" else "显示城点", !showCities) {
                    showCities = !showCities
                    if (!showCities) selectedId = null
                }
            }
        }

        if (replay == null && selectedId == null) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(GeoPanel.copy(alpha = 0.86f)).padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("song" to "大宋", "jin" to "金国", "xixia" to "西夏", "dali" to "大理").forEach { (id, label) ->
                    Text("● $label", color = Color(EastAsiaGeography.territoryColor(gameState, id)), fontSize = 11.sp)
                }
                if (lastReplay != null) {
                    GeoControl("天下纪要", false) { onReopenReplay() }
                }
            }
        }

        selectedId?.let { id ->
            val mark = marksById[id] ?: return@let
            val city = liveCities[id]
            Card(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(9.dp),
                colors = CardDefaults.cardColors(containerColor = GeoPanel),
                border = BorderStroke(1.dp, GeoGold.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(11.dp)
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(mark.name, color = GeoGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(mark.ownerName, color = Color(mark.colorArgb), fontSize = 13.sp)
                    }
                    if (city != null) {
                        Text("守军 ${city.troops} · 城防 ${city.defense} · 人口 ${city.population / 10000}万", color = GeoPaper, fontSize = 11.sp)
                        Text("${city.route} · ${city.cityLevel}${if (mark.capital) " · 当前行在" else ""}", color = Color(0xFFB8AE98), fontSize = 11.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onCitySelected("${mark.id}|enter") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF665231)),
                                modifier = Modifier.weight(1f)
                            ) { Text("进入城池", fontSize = 11.sp) }
                            OutlinedButton(
                                onClick = { onCitySelected("${mark.id}|auto") },
                                border = BorderStroke(1.dp, GeoGold.copy(alpha = 0.75f)),
                                modifier = Modifier.weight(1f)
                            ) { Text("拟定圣旨", color = GeoGold, fontSize = 11.sp) }
                        }
                    } else {
                        Text("现有天下地图战略节点，尚未接入独立城池数值。", color = GeoPaper, fontSize = 11.sp)
                    }
                    GeoControl("收起", false) { selectedId = null }
                }
            }
        }

        if (replay != null && !showSeasonTransition) {
            WorldTurnReplayOverlay(
                replay = replay,
                currentAction = activeReplayAction,
                currentActionNumber = (replayIndex + 1).coerceAtLeast(0),
                onSkip = { selectedId = null; onDismissReplay() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 10.dp, vertical = 10.dp)
            )
        }
        if (showSeasonTransition) {
            replay?.seasonalTransition?.let { transition ->
                SeasonalTransitionOverlay(transition = transition, onDismiss = { showSeasonTransition = false })
            }
        }
    }
}

@Composable
private fun GeoControl(label: String, active: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        border = BorderStroke(1.dp, if (active) GeoGold else Color(0xFF61584A)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) Color(0xFF4A402D) else Color.Transparent
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 9.dp, vertical = 3.dp)
    ) {
        Text(label, color = if (active) GeoGold else Color(0xFFD3C8AF), fontSize = 10.sp)
    }
}

private fun screenPoint(point: GeoMapPoint, cameraX: Float, cameraY: Float, zoom: Float, width: Float, height: Float): Offset =
    Offset(width / 2f + (point.x - cameraX) * zoom, height / 2f + (point.y - cameraY) * zoom)

private fun DrawScope.geoScreen(point: GeoMapPoint, cameraX: Float, cameraY: Float, zoom: Float): Offset =
    screenPoint(point, cameraX, cameraY, zoom, size.width, size.height)

private fun DrawScope.drawTerritoryOverlays(state: GameState, cameraX: Float, cameraY: Float, zoom: Float) {
    EastAsiaGeography.territories.forEach { territory ->
        val path = Path()
        territory.boundary.forEachIndexed { index, location ->
            val point = geoScreen(EastAsiaGeography.project(location), cameraX, cameraY, zoom)
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        val color = Color(EastAsiaGeography.territoryColor(state, territory.factionId))
        drawPath(path, color.copy(alpha = 0.20f))
        drawPath(path, color.copy(alpha = 0.54f), style = Stroke(width = 1.7f))
    }
}

private fun DrawScope.drawGeographicLabels(cameraX: Float, cameraY: Float, zoom: Float, paint: Paint) {
    val features = listOf(
        Triple("黄 海", GeoLocation(123.3f, 35.4f), 0xA8B8D9DDL.toInt()),
        Triple("东 海", GeoLocation(127.1f, 28.4f), 0xA8B8D9DDL.toInt()),
        Triple("南 海", GeoLocation(116.3f, 19.7f), 0xA8B8D9DDL.toInt()),
        Triple("秦 岭", GeoLocation(108.2f, 33.55f), 0xA9E7D6A9L.toInt()),
        Triple("太 行", GeoLocation(113.5f, 37.4f), 0xA9E7D6A9L.toInt()),
        Triple("燕 山", GeoLocation(117.2f, 40.9f), 0xA9E7D6A9L.toInt()),
        Triple("长 江", GeoLocation(115.0f, 30.4f), 0xC196D1DEL.toInt()),
        Triple("黄 河", GeoLocation(110.5f, 35.2f), 0xC196D1DEL.toInt())
    )
    features.forEach { (name, location, color) ->
        val point = geoScreen(EastAsiaGeography.project(location), cameraX, cameraY, zoom)
        if (point.x in 0f..size.width && point.y in 0f..size.height) {
            paint.color = color
            paint.textSize = if (name.contains("海")) 32f else 22f
            drawContext.canvas.nativeCanvas.drawText(name, point.x, point.y, paint)
        }
    }
    EastAsiaGeography.territories.forEach { territory ->
        val point = geoScreen(EastAsiaGeography.project(territory.labelLocation), cameraX, cameraY, zoom)
        paint.color = 0xDFF4E7C2.toInt()
        paint.textSize = if (zoom < 0.08f) 39f else 45f
        drawContext.canvas.nativeCanvas.drawText(territory.label, point.x, point.y, paint)
    }
}

private fun DrawScope.drawActualWorldAction(
    action: WorldTurnAction,
    marksById: Map<String, GeoCityMark>,
    cameraX: Float,
    cameraY: Float,
    zoom: Float
) {
    val color = if (action.factionId == "jin") Color(0xFFD77C6A) else Color(0xFFF0C878)
    action.routeNodeIds.zipWithNext().forEach { (fromId, toId) ->
        val from = marksById[fromId] ?: return@forEach
        val to = marksById[toId] ?: return@forEach
        drawLine(color, geoScreen(from.position, cameraX, cameraY, zoom), geoScreen(to.position, cameraX, cameraY, zoom), strokeWidth = 5f, cap = StrokeCap.Round)
    }
    listOf(action.originCityId, action.targetCityId).distinct().forEach { id ->
        val mark = marksById[id] ?: return@forEach
        val center = geoScreen(mark.position, cameraX, cameraY, zoom)
        drawCircle(color.copy(alpha = 0.30f), 20f, center)
        drawCircle(color, 16f, center, style = Stroke(2.2f))
    }
}

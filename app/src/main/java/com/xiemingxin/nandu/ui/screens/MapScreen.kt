package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.xiemingxin.nandu.game.Army
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.MapNode
import com.xiemingxin.nandu.game.RoadType
import com.xiemingxin.nandu.game.TerrainFeatures
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.CityManagePanel
import com.xiemingxin.nandu.ui.components.RecruitPanel
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.JinRed
import com.xiemingxin.nandu.ui.theme.MapBg
import com.xiemingxin.nandu.ui.theme.SongBright
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

private data class MapFocusRegion(
    val id: String,
    val name: String,
    val camX: Float,
    val camY: Float,
    val zoom: Float,
    val color: Color
)

private data class MapWorldRegion(
    val name: String,
    val faction: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val labelX: Float,
    val labelY: Float,
    val color: Color
)

private val FocusRegions = listOf(
    MapFocusRegion("all", "天下", 0f, 0f, 0.052f, ImperialGold),
    MapFocusRegion("song", "宋境", 2800f, 4850f, 0.078f, SongBright),
    MapFocusRegion("front", "江淮", 6200f, 4200f, 0.090f, ImperialGold),
    MapFocusRegion("jingxiang", "荆襄", 3500f, 4300f, 0.090f, Color(0xFFD0A66A)),
    MapFocusRegion("central", "中原", 6000f, 1300f, 0.086f, JinRed),
    MapFocusRegion("hebei", "河北", 8000f, 200f, 0.078f, JinRed),
    MapFocusRegion("xixia", "西夏", 1200f, 1800f, 0.070f, Color(0xFFB38A48))
)

private val WorldRegions = listOf(
    MapWorldRegion("江南行在", "南宋核心", 3000f, 6100f, 11800f, 9300f, 7200f, 8050f, SongBright.copy(alpha = 0.16f)),
    MapWorldRegion("江淮防线", "宋金争夺", 6200f, 4550f, 11200f, 6500f, 8600f, 5550f, ImperialGold.copy(alpha = 0.14f)),
    MapWorldRegion("荆襄山河", "北伐跳板", 3500f, 3900f, 6900f, 6500f, 5200f, 5250f, Color(0xFFD0A66A).copy(alpha = 0.15f)),
    MapWorldRegion("中原旧都", "金国占领", 6100f, 1750f, 10700f, 4650f, 8400f, 3200f, JinRed.copy(alpha = 0.18f)),
    MapWorldRegion("河北燕云", "金国纵深", 7700f, 250f, 12600f, 2200f, 10100f, 1250f, JinRed.copy(alpha = 0.11f)),
    MapWorldRegion("西夏河陇", "后续势力", 800f, 1300f, 3900f, 3600f, 2350f, 2450f, Color(0xFFB38A48).copy(alpha = 0.16f)),
    MapWorldRegion("大理西南", "远期外交", 1450f, 6900f, 3600f, 9550f, 2550f, 8200f, Color(0xFF3F7A4D).copy(alpha = 0.13f)),
    MapWorldRegion("东海海贸", "远期海路", 10800f, 6500f, 15400f, 9650f, 13200f, 7900f, Color(0xFF2376C9).copy(alpha = 0.14f))
)

@Composable
fun MapScreen(gameState: GameState, onCitySelected: (String) -> Unit = {}) {
    var cameraX by remember { mutableStateOf(986f) }
    var cameraY by remember { mutableStateOf(2500f) }
    var zoom by remember { mutableStateOf(0.07f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var manageCityId by remember { mutableStateOf<String?>(null) }
    var recruitCityId by remember { mutableStateOf<String?>(null) }
    val cityMap = gameState.cities.associateBy { it.id }
    val armiesByCity = gameState.armies.groupBy { it.currentCityId }
    val officerNames = gameState.officers.associate { it.id to it.name }

    fun focus(region: MapFocusRegion) {
        cameraX = region.camX
        cameraY = region.camY
        zoom = region.zoom
        selectedId = null
    }

    Box(modifier = Modifier.fillMaxSize().background(MapBg)) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.018f, 0.14f)
                        cameraX = (cameraX - pan.x / zoom).coerceIn(0f, 14000f)
                        cameraY = (cameraY - pan.y / zoom).coerceIn(0f, 8000f)
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { tap ->
                        val wx = tap.x / zoom + cameraX
                        val wy = tap.y / zoom + cameraY
                        val nearest = MapData.nodes.minByOrNull { n -> (n.worldX - wx).pow(2) + (n.worldY - wy).pow(2) }
                        nearest?.let { n ->
                            val sx = (n.worldX - cameraX) * zoom
                            val sy = (n.worldY - cameraY) * zoom
                            val dist = sqrt((tap.x - sx).pow(2) + (tap.y - sy).pow(2))
                            selectedId = if (dist < 62f) {
                                if (selectedId == n.id) null else n.id
                            } else null
                        }
                    }
                }
        ) {
            drawRect(Color(0xFF10190F))
            drawWorldBase(cameraX, cameraY, zoom)
            drawWorldRegions(cameraX, cameraY, zoom)
            drawFactionRegions(cameraX, cameraY, zoom)
            drawMajorRivers(cameraX, cameraY, zoom)
            drawMountainRanges(cameraX, cameraY, zoom)
            drawMapGrid(cameraX, cameraY, zoom)
            drawRoads(cameraX, cameraY, zoom)
            drawArmyRoutes(gameState, cameraX, cameraY, zoom)
            drawRegionLabels(cameraX, cameraY, zoom)
            drawTerrainLabels(cameraX, cameraY, zoom)

            MapData.nodes.forEach { node ->
                val city = cityMap[node.id]
                val screen = w2s(node.worldX, node.worldY, cameraX, cameraY, zoom)
                val isJin = city?.owner == "jin"
                val isFrontline = city?.controlState == "FRONTLINE" || city?.controlState == "CONTESTED"
                val isSel = selectedId == node.id
                val r = ((if (node.isCapital) 15f else 10f) * zoom * 35f).coerceIn(7f, 25f)
                val mainColor = if (isJin) Color(0xFFE24A4A) else if (node.isCapital) ImperialGold else Color(0xFF4DA3E6)
                if (isFrontline) drawFrontlineGlow(screen, r, isJin)
                if (isSel) {
                    drawCircle(ImperialGold.copy(alpha = 0.34f), r * 2.9f, screen)
                    drawCircle(ImperialGold.copy(alpha = 0.55f), r * 2.1f, screen, style = Stroke(width = 2f))
                }
                city?.troops?.let { t ->
                    if (t > 5000) drawCircle((if (isJin) JinRed else SongBright).copy(alpha = 0.22f), r + (t / 60000f * 12f).coerceIn(3f, 13f), screen)
                }
                drawCityIcon(screen, r, mainColor, node.isCapital, isJin, isFrontline)
                drawCityName(node.name, screen, r, zoom)
            }

            drawArmyFlags(gameState, cameraX, cameraY, zoom)
        }

        MapStatusChip(gameState, Modifier.align(Alignment.TopStart).padding(12.dp))
        RegionQuickBar(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp, start = 8.dp, end = 8.dp),
            onFocus = ::focus
        )
        MapLegend(Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 80.dp))

        selectedId?.let { id ->
            MapData.nodeMap[id]?.let { node ->
                CityDetailPanel(
                    node = node,
                    city = cityMap[id],
                    armies = armiesByCity[id].orEmpty().sortedByDescending { it.troops },
                    officerNames = officerNames,
                    onDismiss = { selectedId = null },
                    onDraft = { action ->
                        if (action == "manage") manageCityId = id
                        else if (action == "recruit") recruitCityId = id
                        else onCitySelected("$id|$action")
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        manageCityId?.let { mid ->
            cityMap[mid]?.let { mCity ->
                Dialog(onDismissRequest = { manageCityId = null }) {
                    CityManagePanel(
                        city = mCity,
                        onBuild = { buildingId -> onCitySelected("$mid|build:$buildingId") },
                        onDismiss = { manageCityId = null }
                    )
                }
            }
        }

        recruitCityId?.let { rid ->
            cityMap[rid]?.let { rCity ->
                Dialog(onDismissRequest = { recruitCityId = null }) {
                    RecruitPanel(
                        city = rCity,
                        onRecruit = { unitId -> onCitySelected("$rid|recruit:$unitId") },
                        onDismiss = { recruitCityId = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionQuickBar(modifier: Modifier = Modifier, onFocus: (MapFocusRegion) -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        FocusRegions.forEach { region ->
            OutlinedButton(
                onClick = { onFocus(region) },
                border = BorderStroke(1.dp, region.color.copy(alpha = 0.70f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xCC0A0D0F),
                    contentColor = region.color
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(region.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MapStatusChip(gameState: GameState, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xD9090806)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(gameState.calendar.displayText(), color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${gameState.season.label} · ${gameState.weather.label} · V1.3.3", color = Color(0xFFC6A96C), fontSize = 10.sp)
        }
    }
}

@Composable
private fun MapLegend(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.background(Color(0xDD0A0D0F), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        LegendRow(SongBright, "■", "宋控")
        LegendRow(JinRed, "■", "金占")
        LegendRow(Color(0xFFB38A48), "■", "西夏占坑")
        LegendRow(Color(0xFF68B7E8), "━", "水路")
        LegendRow(Color(0xFFD0A66A), "━", "陆路")
        LegendRow(ImperialGold, "⚑", "军团")
    }
}

private fun DrawScope.drawWorldBase(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(0f, 0f, 16000f, 10000f, Color(0xFF152417), camX, camY, zoom)
    drawWorldRect(0f, 0f, 16000f, 10000f, Color.Black.copy(alpha = 0.18f), camX, camY, zoom)
}

private fun DrawScope.drawWorldRegions(camX: Float, camY: Float, zoom: Float) {
    WorldRegions.forEach { r ->
        drawWorldRect(r.left, r.top, r.right, r.bottom, r.color, camX, camY, zoom)
        drawWorldRect(r.left, r.top, r.right, r.bottom, Color.Black.copy(alpha = 0.08f), camX, camY, zoom)
    }
}

private fun DrawScope.drawRegionLabels(camX: Float, camY: Float, zoom: Float) {
    WorldRegions.forEach { region ->
        val s = w2s(region.labelX, region.labelY, camX, camY, zoom)
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(230, 225, 202, 130)
                textSize = (18f * zoom * 33f).coerceIn(15f, 36f)
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(240, 0, 0, 0))
            }
            canvas.nativeCanvas.drawText(region.name, s.x, s.y, p)
            p.textSize = (9f * zoom * 33f).coerceIn(10f, 18f)
            p.color = android.graphics.Color.argb(210, 190, 172, 116)
            canvas.nativeCanvas.drawText(region.faction, s.x, s.y + p.textSize + 6f, p)
        }
    }
}

private fun DrawScope.drawFactionRegions(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(6900f, 1850f, 10500f, 4450f, JinRed.copy(alpha = 0.12f), camX, camY, zoom)
    drawWorldRect(3300f, 5100f, 13400f, 9050f, SongBright.copy(alpha = 0.10f), camX, camY, zoom)
    drawWorldRect(7100f, 5000f, 11100f, 6200f, Color(0xFF7A3B3B).copy(alpha = 0.10f), camX, camY, zoom)
}

private fun DrawScope.drawMajorRivers(camX: Float, camY: Float, zoom: Float) {
    TerrainFeatures.rivers.forEach { river ->
        val pts = river.points.map { Offset(it.first, it.second) }
        val color = when (river.name) {
            "黄河" -> Color(0xFFB8995A).copy(alpha = 0.75f)
            "长江" -> Color(0xFF2D9CDB).copy(alpha = 0.78f)
            else -> Color(0xFF5BC0EB).copy(alpha = 0.62f)
        }
        val width = if (river.name == "长江" || river.name == "黄河") 9f else 5.5f
        drawWorldPath(pts, color, width, camX, camY, zoom)
    }
}

private fun DrawScope.drawMountainRanges(camX: Float, camY: Float, zoom: Float) {
    TerrainFeatures.mountains.forEach { mtn ->
        val pts = mtn.points.map { Offset(it.first, it.second) }
        drawWorldPath(pts, Color(0xFF7A5C38).copy(alpha = 0.68f), 7f, camX, camY, zoom)
        pts.forEach { p ->
            val s = w2s(p.x, p.y, camX, camY, zoom)
            val sz = (10f * zoom * 35f).coerceIn(4f, 12f)
            val peak = Path().apply {
                moveTo(s.x, s.y - sz)
                lineTo(s.x - sz * 0.7f, s.y + sz * 0.5f)
                lineTo(s.x + sz * 0.7f, s.y + sz * 0.5f)
                close()
            }
            drawPath(peak, Color(0xFF8B6B43).copy(alpha = 0.7f))
            drawPath(peak, Color(0xFF5A4329).copy(alpha = 0.8f), style = Stroke(width = 1f))
        }
    }
}

private fun DrawScope.drawTerrainLabels(camX: Float, camY: Float, zoom: Float) {
    if (zoom * 38f <= 0.7f) return
    TerrainFeatures.all.forEach { feature ->
        val s = w2s(feature.labelX, feature.labelY, camX, camY, zoom)
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint().apply {
                color = if (feature.kind == "river") android.graphics.Color.argb(220, 150, 200, 235) else android.graphics.Color.argb(220, 200, 170, 130)
                textSize = (11f * zoom * 38f).coerceIn(13f, 30f)
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(5f, 0f, 2f, android.graphics.Color.argb(230, 0, 0, 0))
                isFakeBoldText = true
            }
            canvas.nativeCanvas.drawText(feature.name, s.x, s.y, p)
        }
    }
}

private fun DrawScope.drawRoads(camX: Float, camY: Float, zoom: Float) {
    MapData.roads.forEach { road ->
        val from = MapData.nodeMap[road.fromId] ?: return@forEach
        val to = MapData.nodeMap[road.toId] ?: return@forEach
        val fs = w2s(from.worldX, from.worldY, camX, camY, zoom)
        val ts = w2s(to.worldX, to.worldY, camX, camY, zoom)
        val sw = (2.7f * zoom * 38f).coerceIn(1.2f, 7f)
        val pair = when (road.type) {
            RoadType.RIVER -> Color(0xFF68B7E8) to null
            RoadType.CANAL -> Color(0xFF9DD7EA) to PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
            RoadType.SEA -> Color(0xFF2376C9) to PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
            RoadType.MOUNTAIN -> Color(0xFFB08A54) to PathEffect.dashPathEffect(floatArrayOf(5f, 9f))
            RoadType.LAND -> Color(0xFFD0A66A) to null
            RoadType.PASS -> Color(0xFF8B2500) to null
        }
        drawLine(Color.Black.copy(alpha = 0.35f), fs, ts, sw + 2f, pathEffect = pair.second, cap = StrokeCap.Round)
        drawLine(pair.first, fs, ts, sw, pathEffect = pair.second, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawArmyRoutes(gameState: GameState, camX: Float, camY: Float, zoom: Float) {
    gameState.armies.filter { it.troops > 0 && it.currentCityId.isNotBlank() && it.targetCityId.isNotBlank() }.forEach { army ->
        val from = MapData.nodeMap[army.currentCityId] ?: return@forEach
        val to = MapData.nodeMap[army.targetCityId] ?: return@forEach
        val start = w2s(from.worldX, from.worldY, camX, camY, zoom)
        val end = w2s(to.worldX, to.worldY, camX, camY, zoom)
        drawLine(Color.Black.copy(alpha = 0.45f), start, end, 5f, cap = StrokeCap.Round)
        drawLine((if (army.ownerFactionId == "jin") JinRed else ImperialGold).copy(alpha = 0.72f), start, end, 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f)), cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawArmyFlags(gameState: GameState, camX: Float, camY: Float, zoom: Float) {
    val slots = mutableMapOf<String, Int>()
    gameState.armies.filter { it.troops > 0 }.forEach { army ->
        val node = MapData.nodeMap[army.currentCityId] ?: return@forEach
        val base = w2s(node.worldX, node.worldY, camX, camY, zoom)
        val slotKey = "${(base.x / 44f).toInt()}_${(base.y / 44f).toInt()}"
        val slot = slots.getOrDefault(slotKey, 0)
        slots[slotKey] = slot + 1
        val color = if (army.ownerFactionId == "jin") JinRed else SongBright
        val offset = Offset(base.x + 22f + slot * 12f, base.y - 36f)
        drawArmyFlag(offset, if (army.ownerFactionId == "jin") "金" else army.name.take(1), color, "${army.troops / 1000}k")
    }
}

private fun DrawScope.drawArmyFlag(origin: Offset, label: String, color: Color, subLabel: String) {
    drawLine(Color(0xFF1A1208), origin, Offset(origin.x, origin.y + 28f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    val banner = Path().apply {
        moveTo(origin.x, origin.y)
        lineTo(origin.x + 26f, origin.y + 5f)
        lineTo(origin.x + 19f, origin.y + 16f)
        lineTo(origin.x, origin.y + 13f)
        close()
    }
    drawPath(banner, color)
    drawPath(banner, Color.Black.copy(alpha = 0.52f), style = Stroke(width = 1.1f))
    drawTextOnCanvas(label, Offset(origin.x + 13f, origin.y + 12f), 10.5f, android.graphics.Color.WHITE, true)
    drawTextOnCanvas(subLabel, Offset(origin.x + 14f, origin.y + 38f), 8f, android.graphics.Color.WHITE, true)
}

private fun DrawScope.drawCityIcon(center: Offset, r: Float, color: Color, isCapital: Boolean, isJin: Boolean, isFrontline: Boolean) {
    drawRect(Color.Black.copy(alpha = 0.45f), Offset(center.x - r * 1.18f + 2f, center.y - r * 0.52f + 3f), Size(r * 2.36f, r * 1.22f))
    drawRect(color.copy(alpha = 0.95f), Offset(center.x - r * 1.18f, center.y - r * 0.52f), Size(r * 2.36f, r * 1.22f))
    drawRect(Color(0xFF0A0907), Offset(center.x - r * 0.30f, center.y + r * 0.05f), Size(r * 0.60f, r * 0.65f))
    val roof = Path().apply {
        moveTo(center.x - r * 1.35f, center.y - r * 0.52f)
        lineTo(center.x - r * 0.72f, center.y - r * 1.08f)
        lineTo(center.x, center.y - r * 0.62f)
        lineTo(center.x + r * 0.72f, center.y - r * 1.08f)
        lineTo(center.x + r * 1.35f, center.y - r * 0.52f)
    }
    drawPath(roof, if (isJin) Color(0xFF5A1111) else Color(0xFF092B44), style = Stroke(width = 2.0f, cap = StrokeCap.Round))
    drawRect(Color(0xFF050504), Offset(center.x - r * 1.28f, center.y - r * 0.62f), Size(r * 2.56f, r * 1.42f), style = Stroke(width = 1.4f))
    if (isCapital) {
        drawCircle(ImperialGold.copy(alpha = 0.35f), r * 2.05f, center)
        drawCircle(ImperialGold.copy(alpha = 0.80f), r * 1.42f, center, style = Stroke(width = 1.5f))
    }
    if (isFrontline) drawLine(ImperialGold.copy(alpha = 0.75f), Offset(center.x - r * 1.12f, center.y + r * 0.92f), Offset(center.x + r * 1.12f, center.y + r * 0.92f), 1.7f)
}

private fun DrawScope.drawCityName(name: String, screen: Offset, r: Float, zoom: Float) {
    val textScale = zoom * 38f
    if (textScale <= 0.55f) return
    drawTextOnCanvas(name, Offset(screen.x, screen.y + r + (9.5f * textScale).coerceIn(10f, 26f) + 5f), (9.5f * textScale).coerceIn(10f, 26f), android.graphics.Color.WHITE, false)
}

private fun DrawScope.drawMapGrid(camX: Float, camY: Float, zoom: Float) {
    val gridWorld = 2000f
    val gridColor = Color(0xFF233029).copy(alpha = 0.35f)
    val startX = (camX / gridWorld).toInt() * gridWorld
    val startY = (camY / gridWorld).toInt() * gridWorld
    var wx = startX
    while (wx < camX + size.width / zoom + gridWorld) {
        val sx = (wx - camX) * zoom
        drawLine(gridColor, Offset(sx, 0f), Offset(sx, size.height), 0.5f)
        wx += gridWorld
    }
    var wy = startY
    while (wy < camY + size.height / zoom + gridWorld) {
        val sy = (wy - camY) * zoom
        drawLine(gridColor, Offset(0f, sy), Offset(size.width, sy), 0.5f)
        wy += gridWorld
    }
}

private fun DrawScope.drawWorldRect(left: Float, top: Float, right: Float, bottom: Float, color: Color, camX: Float, camY: Float, zoom: Float) {
    val a = w2s(left, top, camX, camY, zoom)
    val b = w2s(right, bottom, camX, camY, zoom)
    drawRect(color, topLeft = a, size = Size(b.x - a.x, b.y - a.y))
}

private fun DrawScope.drawWorldPath(points: List<Offset>, color: Color, baseWidth: Float, camX: Float, camY: Float, zoom: Float) {
    if (points.size < 2) return
    val path = Path()
    val first = w2s(points.first().x, points.first().y, camX, camY, zoom)
    path.moveTo(first.x, first.y)
    points.drop(1).forEach { p ->
        val sp = w2s(p.x, p.y, camX, camY, zoom)
        path.lineTo(sp.x, sp.y)
    }
    val width = (baseWidth * zoom * 35f).coerceIn(2f, 14f)
    drawPath(path, Color.Black.copy(alpha = 0.30f), style = Stroke(width = width + 3f, cap = StrokeCap.Round))
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
}

private fun DrawScope.drawFrontlineGlow(center: Offset, r: Float, isJin: Boolean) {
    val glow = if (isJin) JinRed else ImperialGold
    drawCircle(glow.copy(alpha = 0.20f), r * 2.4f, center)
    drawCircle(glow.copy(alpha = 0.42f), r * 1.8f, center, style = Stroke(width = 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))))
}

private fun DrawScope.drawTextOnCanvas(text: String, center: Offset, textSize: Float, color: Int, bold: Boolean) {
    drawIntoCanvas { canvas ->
        val p = android.graphics.Paint().apply {
            this.color = color
            this.textSize = textSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = bold
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(210, 0, 0, 0))
        }
        canvas.nativeCanvas.drawText(text, center.x, center.y, p)
    }
}

private fun w2s(wx: Float, wy: Float, camX: Float, camY: Float, zoom: Float) = Offset((wx - camX) * zoom, (wy - camY) * zoom)

@Composable
private fun LegendRow(color: Color, symbol: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(symbol, color = color, fontSize = 11.sp)
        Text(label, color = Color(0xFFB9AA82), fontSize = 10.sp)
    }
}

@Composable
fun CityDetailPanel(
    node: MapNode,
    city: City?,
    armies: List<Army>,
    officerNames: Map<String, String>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onDraft: (String) -> Unit = {}
) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xF01A1208)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.60f))) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (city != null) {
                AssetImage(
                    path = ArtResourceRegistry.cityBackground(city.id),
                    fallbackPath = ArtResourceRegistry.Fallback.city,
                    contentDescription = node.name,
                    contentScale = ContentScale.Crop,
                    placeholderText = "城",
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.height(10.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(node.name, color = ImperialGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            city == null -> "--"
                            city.owner == "jin" -> "金国占领 · ${city.controlState}"
                            node.isCapital -> "大宋京城 · ${city.controlState}"
                            else -> "大宋控制 · ${city.controlState}"
                        },
                        color = Color(0xFFB9AA82), fontSize = 12.sp
                    )
                    if (city != null && city.route.isNotBlank()) {
                        Text("${city.route} · ${city.cityLevel} · ${terrainLabel(city.terrain)} · 口${city.population / 10000}万", color = Color(0xFF8C7A60), fontSize = 10.sp)
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Text("X", color = Color(0xFF8B7355), fontSize = 14.sp) }
            }
            if (city != null) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CityStatItem("兵", "兵力", "${city.troops / 1000}k")
                    CityStatItem("防", "城防", "${city.defense}")
                    CityStatItem("粮", "粮草", "${city.grain / 1000}k")
                    CityStatItem("财", "金库", "${city.gold / 1000}k")
                    CityStatItem("民", "民心", "${city.popularSupport}")
                }
                Spacer(Modifier.height(10.dp))
                Text("驻防军团", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (armies.isEmpty()) Text("此城暂无正式军团驻防。", color = Color(0xFF8C7A60), fontSize = 11.sp)
                else armies.take(4).forEach { army -> CityArmyRow(army, officerNames[army.commanderId] ?: army.status) }
                Spacer(Modifier.height(12.dp))
                Text("拟旨操作", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    CityActionButton("修城", Modifier.weight(1f)) { onDraft("repair") }
                    CityActionButton("调兵", Modifier.weight(1f)) { onDraft("dispatch") }
                    CityActionButton("筹粮", Modifier.weight(1f)) { onDraft("grain") }
                    CityActionButton(if (city.owner == "jin") "进取" else "备战", Modifier.weight(1f)) { onDraft(if (city.owner == "jin") "siege" else "attack") }
                }
                Spacer(Modifier.height(7.dp))
                CityActionButton("◈ 进 入 城 池 ◈", Modifier.fillMaxWidth()) { onDraft("enter") }
                if (city.owner == "song") {
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        CityActionButton("营建城池", Modifier.weight(1f)) { onDraft("manage") }
                        CityActionButton("募兵练军", Modifier.weight(1f)) { onDraft("recruit") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityArmyRow(army: Army, commander: String) {
    val color = if (army.ownerFactionId == "jin") JinRed else SongBright
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1D150B), RoundedCornerShape(7.dp)).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(army.name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${army.troops / 1000}k", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val movement = if (army.status.contains("进军") && army.targetCityId.isNotBlank()) " · 目标${MapData.nodeMap[army.targetCityId]?.name ?: army.targetCityId} · 余${army.marchDaysRemaining}天" else ""
        Text("统帅：$commander · 士气${army.morale} · ${army.status}$movement", color = Color(0xFFB9AA82), fontSize = 10.sp)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CityActionButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
    ) { Text(text, fontSize = 11.sp) }
}

private fun terrainLabel(terrain: String): String = when (terrain) {
    "river" -> "水乡"
    "mountain" -> "山地"
    "pass" -> "关隘"
    "coast" -> "沿海"
    else -> "平原"
}

@Composable
private fun CityStatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 14.sp, color = ImperialGold)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8C7A60), fontSize = 9.sp)
    }
}

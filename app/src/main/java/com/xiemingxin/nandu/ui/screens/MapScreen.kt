package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
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
import com.xiemingxin.nandu.game.Army
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.MapNode
import com.xiemingxin.nandu.game.RoadType
import com.xiemingxin.nandu.game.WeatherType
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.CityManagePanel
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.JinRed
import com.xiemingxin.nandu.ui.theme.MapBg
import com.xiemingxin.nandu.ui.theme.SongBright
import kotlinx.coroutines.delay
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun MapScreen(gameState: GameState, onCitySelected: (String) -> Unit = {}) {
    var cameraX by remember { mutableStateOf(2500f) }
    var cameraY by remember { mutableStateOf(1200f) }
    var zoom by remember { mutableStateOf(0.027f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var manageCityId by remember { mutableStateOf<String?>(null) }
    var weatherPhase by remember { mutableStateOf(0f) }
    val cityMap = gameState.cities.associateBy { it.id }
    val armiesByCity = gameState.armies.groupBy { it.currentCityId }
    val officerNames = gameState.officers.associate { it.id to it.name }

    LaunchedEffect(gameState.weather) {
        while (true) {
            weatherPhase = (weatherPhase + 0.0125f) % 1f
            delay(33L)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MapBg)) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.015f, 0.14f)
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
                            selectedId = if (dist < 58f) {
                                if (selectedId == n.id) null else n.id
                            } else null
                        }
                    }
                }
        ) {
            drawRect(Color(0xFF10190F))
            drawTerrainRegions(cameraX, cameraY, zoom)
            drawFactionRegions(cameraX, cameraY, zoom)
            drawMajorRivers(cameraX, cameraY, zoom)
            drawMapGrid(cameraX, cameraY, zoom)
            drawRoads(cameraX, cameraY, zoom)
            drawArmyRoutes(gameState, cameraX, cameraY, zoom, weatherPhase)
            drawWeatherWash(gameState.weather, weatherPhase)

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

            drawArmyCorpsFlags(gameState, cameraX, cameraY, zoom)
            drawWeatherParticles(gameState.weather, weatherPhase)
        }

        MapStatusChip(gameState, Modifier.align(Alignment.TopStart).padding(12.dp))
        Column(
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 80.dp)
                .background(Color(0xDD0A0D0F), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            LegendRow(SongBright, "■", "宋控")
            LegendRow(JinRed, "■", "金占")
            LegendRow(Color(0xFF68B7E8), "━", "水路")
            LegendRow(Color(0xFFD0A66A), "━", "陆路")
            LegendRow(ImperialGold, "➤", "行军/粮道")
            LegendRow(ImperialGold, "⚑", "军团")
        }
        selectedId?.let { id ->
            MapData.nodeMap[id]?.let { node ->
                CityDetailPanel(
                    node = node,
                    city = cityMap[id],
                    armies = armiesByCity[id].orEmpty().sortedByDescending { it.troops },
                    officerNames = officerNames,
                    onDismiss = { selectedId = null },
                    onDraft = { action ->
                        if (action == "manage") {
                            manageCityId = id
                        } else {
                            onCitySelected("$id|$action")
                        }
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // V0.9 城池经营弹窗
        manageCityId?.let { mid ->
            cityMap[mid]?.let { mCity ->
                Dialog(onDismissRequest = { manageCityId = null }) {
                    CityManagePanel(
                        city = mCity,
                        onBuild = { buildingId ->
                            onCitySelected("$mid|build:$buildingId")
                        },
                        onDismiss = { manageCityId = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapStatusChip(gameState: GameState, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xD9090806)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(gameState.calendar.displayText(), color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${gameState.season.label} · ${gameState.weather.label}", color = Color(0xFFC6A96C), fontSize = 10.sp)
        }
    }
}

private fun DrawScope.drawArmyRoutes(gameState: GameState, camX: Float, camY: Float, zoom: Float, phase: Float) {
    gameState.armies.filter { army ->
        army.troops > 0 && army.currentCityId.isNotBlank() &&
            (army.routeFromCityId.isNotBlank() || army.supplyCityId != army.currentCityId || army.targetCityId.isNotBlank() || army.status.contains("进军"))
    }.forEach { army ->
        val fromId = army.routeFromCityId.ifBlank { army.supplyCityId.ifBlank { army.currentCityId } }
        val toId = army.targetCityId.ifBlank { army.currentCityId }
        val fromNode = MapData.nodeMap[fromId] ?: MapData.nodeMap[army.homeCityId] ?: return@forEach
        val toNode = MapData.nodeMap[toId] ?: return@forEach
        if (fromNode.id == toNode.id) return@forEach
        val start = w2s(fromNode.worldX, fromNode.worldY, camX, camY, zoom)
        val end = w2s(toNode.worldX, toNode.worldY, camX, camY, zoom)
        val color = if (army.ownerFactionId == "jin") JinRed else ImperialGold
        drawMarchRoute(start, end, color, phase, army.status)
    }
}

private fun DrawScope.drawMarchRoute(start: Offset, end: Offset, color: Color, phase: Float, status: String) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val length = sqrt(dx * dx + dy * dy)
    if (length < 12f) return
    val angle = atan2(dy, dx)
    val routePath = Path().apply {
        moveTo(start.x, start.y)
        lineTo(end.x, end.y)
    }
    drawPath(routePath, Color.Black.copy(alpha = 0.45f), style = Stroke(width = 5f, cap = StrokeCap.Round))
    drawPath(
        routePath,
        color.copy(alpha = if (status.contains("进军")) 0.82f else 0.48f),
        style = Stroke(
            width = if (status.contains("进军")) 3.2f else 2.2f,
            cap = StrokeCap.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), phase * 28f)
        )
    )
    val arrows = (length / 95f).toInt().coerceIn(1, 4)
    for (i in 1..arrows) {
        val t = ((i.toFloat() / (arrows + 1)) + phase * 0.18f) % 1f
        val point = Offset(start.x + dx * t, start.y + dy * t)
        drawArrowHead(point, angle, color.copy(alpha = 0.86f))
    }
}

private fun DrawScope.drawArrowHead(center: Offset, angle: Float, color: Color) {
    val size = 9f
    val left = Offset(center.x - cos(angle - 0.58f) * size, center.y - sin(angle - 0.58f) * size)
    val right = Offset(center.x - cos(angle + 0.58f) * size, center.y - sin(angle + 0.58f) * size)
    val path = Path().apply {
        moveTo(center.x + cos(angle) * size * 0.75f, center.y + sin(angle) * size * 0.75f)
        lineTo(left.x, left.y)
        lineTo(right.x, right.y)
        close()
    }
    drawPath(path, color)
    drawPath(path, Color.Black.copy(alpha = 0.45f), style = Stroke(width = 1f))
}

private fun DrawScope.drawArmyCorpsFlags(gameState: GameState, camX: Float, camY: Float, zoom: Float) {
    val officerMap = gameState.officers.associateBy { it.id }
    val slots = mutableMapOf<String, Int>()
    gameState.armies
        .filter { it.troops > 0 }
        .sortedWith(compareBy<Army> { it.ownerFactionId }.thenByDescending { it.troops })
        .forEach { army ->
            val base = armyScreenPosition(army, camX, camY, zoom) ?: return@forEach
            val slotKey = "${(base.x / 44f).toInt()}_${(base.y / 44f).toInt()}"
            val slot = slots.getOrDefault(slotKey, 0)
            slots[slotKey] = slot + 1
            val color = when (army.ownerFactionId) {
                "jin" -> JinRed
                "song" -> SongBright
                else -> ImperialGold
            }
            val isMoving = army.status.contains("进军") && army.targetCityId.isNotBlank()
            val offsetX = if (army.ownerFactionId == "jin") -34f - (slot % 2) * 25f else 24f + (slot % 2) * 27f
            val offsetY = -38f - (slot / 2) * 25f
            val commander = officerMap[army.commanderId]?.name ?: army.status
            val subLabel = if (isMoving) "余${army.marchDaysRemaining}天" else "${army.troops / 1000}k"
            val lineLabel = if (isMoving) "赴${MapData.nodeMap[army.targetCityId]?.name ?: army.targetCityId}" else commander
            val scale = if (isMoving) 1.02f else if (army.troops >= 20000) 1.0f else 0.86f
            drawArmyFlag(
                origin = Offset(base.x + offsetX, base.y + offsetY),
                label = armyFlagLabel(army.name, army.ownerFactionId),
                color = if (isMoving) color.copy(alpha = 1f) else color,
                scale = scale,
                subLabel = subLabel,
                commander = lineLabel
            )
            if (isMoving) {
                drawCircle(ImperialGold.copy(alpha = 0.18f), 24f, base)
                drawCircle(ImperialGold.copy(alpha = 0.58f), 9f, base, style = Stroke(width = 1.6f))
            }
        }
}

private fun armyScreenPosition(army: Army, camX: Float, camY: Float, zoom: Float): Offset? {
    val isMoving = army.status.contains("进军") && army.targetCityId.isNotBlank()
    if (!isMoving) {
        val node = MapData.nodeMap[army.currentCityId] ?: return null
        return w2s(node.worldX, node.worldY, camX, camY, zoom)
    }
    val fromId = army.routeFromCityId.ifBlank { army.supplyCityId.ifBlank { army.currentCityId } }
    val from = MapData.nodeMap[fromId] ?: MapData.nodeMap[army.currentCityId] ?: return null
    val to = MapData.nodeMap[army.targetCityId] ?: return w2s(from.worldX, from.worldY, camX, camY, zoom)
    val total = army.marchDaysTotal.coerceAtLeast(1)
    val passed = (total - army.marchDaysRemaining).coerceIn(0, total)
    val progress = (passed.toFloat() / total.toFloat()).coerceIn(0.05f, 0.95f)
    val wx = from.worldX + (to.worldX - from.worldX) * progress
    val wy = from.worldY + (to.worldY - from.worldY) * progress
    return w2s(wx, wy, camX, camY, zoom)
}

private fun armyFlagLabel(name: String, factionId: String): String {
    if (factionId == "jin") return "金"
    return when {
        name.contains("岳") -> "岳"
        name.contains("韩") -> "韩"
        name.contains("吴") -> "吴"
        name.contains("刘") -> "刘"
        else -> name.take(1)
    }
}

private fun DrawScope.drawArmyFlag(origin: Offset, label: String, color: Color, scale: Float, subLabel: String, commander: String) {
    val h = 32f * scale
    val w = 28f * scale
    drawCircle(color.copy(alpha = 0.18f), 20f * scale, Offset(origin.x + 6f, origin.y + h * 0.5f))
    drawLine(Color(0xFF1A1208), origin, Offset(origin.x, origin.y + h), strokeWidth = 2.6f * scale, cap = StrokeCap.Round)
    val banner = Path().apply {
        moveTo(origin.x, origin.y)
        lineTo(origin.x + w, origin.y + 5f * scale)
        lineTo(origin.x + w * 0.72f, origin.y + 16f * scale)
        lineTo(origin.x, origin.y + 13f * scale)
        close()
    }
    drawPath(banner, color.copy(alpha = 0.98f))
    drawPath(banner, Color.Black.copy(alpha = 0.52f), style = Stroke(width = 1.15f * scale))
    drawFlagLabel(label, Offset(origin.x + w * 0.48f, origin.y + 11.5f * scale), 10.5f * scale)
    drawFlagLabel(subLabel, Offset(origin.x + w * 0.55f, origin.y + h + 9f * scale), 7.2f * scale)
    drawCommanderName(commander, Offset(origin.x + w * 0.54f, origin.y + h + 20f * scale), 7.2f * scale)
}

private fun DrawScope.drawFlagLabel(label: String, center: Offset, textSize: Float) {
    drawIntoCanvas { canvas ->
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            this.textSize = textSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
            setShadowLayer(2f, 0f, 1f, android.graphics.Color.argb(190, 0, 0, 0))
        }
        canvas.nativeCanvas.drawText(label, center.x, center.y, p)
    }
}

private fun DrawScope.drawCommanderName(label: String, center: Offset, textSize: Float) {
    drawIntoCanvas { canvas ->
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.argb(230, 235, 220, 180)
            this.textSize = textSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(220, 0, 0, 0))
        }
        canvas.nativeCanvas.drawText(label.take(4), center.x, center.y, p)
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

private fun DrawScope.drawTerrainRegions(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(0f, 0f, 16000f, 10000f, Color(0xFF152417), camX, camY, zoom)
    drawWorldRect(6100f, 1600f, 11300f, 4700f, Color(0xFF4A4326).copy(alpha = 0.50f), camX, camY, zoom)
    drawWorldRect(6700f, 4900f, 12300f, 7200f, Color(0xFF123E35).copy(alpha = 0.56f), camX, camY, zoom)
    drawWorldRect(3000f, 5550f, 7000f, 8500f, Color(0xFF244322).copy(alpha = 0.58f), camX, camY, zoom)
    drawWorldRect(4400f, 4200f, 7700f, 6100f, Color(0xFF5B4527).copy(alpha = 0.48f), camX, camY, zoom)
    drawWorldRect(10200f, 6900f, 13700f, 9400f, Color(0xFF16382B).copy(alpha = 0.50f), camX, camY, zoom)
}

private fun DrawScope.drawFactionRegions(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(6900f, 1850f, 10500f, 4450f, JinRed.copy(alpha = 0.17f), camX, camY, zoom)
    drawWorldRect(3300f, 5100f, 13400f, 9050f, SongBright.copy(alpha = 0.12f), camX, camY, zoom)
    drawWorldRect(7100f, 5000f, 11100f, 6200f, Color(0xFF7A3B3B).copy(alpha = 0.12f), camX, camY, zoom)
}

private fun DrawScope.drawMajorRivers(camX: Float, camY: Float, zoom: Float) {
    drawWorldPath(listOf(Offset(2500f, 6900f), Offset(4200f, 6750f), Offset(6000f, 6650f), Offset(7600f, 6600f), Offset(9200f, 6550f), Offset(10800f, 6650f), Offset(13000f, 7050f)), Color(0xFF2D9CDB).copy(alpha = 0.70f), 9f, camX, camY, zoom)
    drawWorldPath(listOf(Offset(7200f, 5200f), Offset(7900f, 5700f), Offset(7600f, 6600f), Offset(7000f, 7000f)), Color(0xFF49B6E8).copy(alpha = 0.60f), 5.5f, camX, camY, zoom)
    drawWorldPath(listOf(Offset(8500f, 5250f), Offset(9600f, 5400f), Offset(10800f, 5600f), Offset(11600f, 6000f)), Color(0xFF5BC0EB).copy(alpha = 0.60f), 6f, camX, camY, zoom)
    drawWorldPath(listOf(Offset(6500f, 3800f), Offset(8200f, 3900f), Offset(9000f, 3900f), Offset(10400f, 3600f)), Color(0xFF496F9A).copy(alpha = 0.40f), 5f, camX, camY, zoom)
}

private fun DrawScope.drawWeatherWash(weather: WeatherType, phase: Float) {
    when (weather) {
        WeatherType.CLEAR -> drawRect(Color(0x11E8C66A))
        WeatherType.RAIN -> drawRect(Color(0x220A1A24))
        WeatherType.STORM -> drawRect(Color(0x66040608))
        WeatherType.FOG -> drawRect(Color.White.copy(alpha = 0.10f + phase * 0.04f))
        WeatherType.SNOW -> drawRect(Color(0x22303A42))
        WeatherType.WIND -> drawRect(Color(0x22A98A55))
    }
}

private fun DrawScope.drawWeatherParticles(weather: WeatherType, phase: Float) {
    when (weather) {
        WeatherType.RAIN -> drawRain(70, 0.28f, 18f, phase, 1.45f)
        WeatherType.STORM -> drawRain(150, 0.48f, 24f, phase, 2.2f)
        WeatherType.SNOW -> drawSnow(85, phase)
        WeatherType.WIND -> drawWind(38, phase)
        WeatherType.FOG -> drawFogLines(phase)
        WeatherType.CLEAR -> Unit
    }
}

private fun DrawScope.drawRain(count: Int, alpha: Float, length: Float, phase: Float, speed: Float) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    for (i in 0 until count) {
        val x = wrap(i * 73f - phase * w * 0.45f, w)
        val y = wrap(i * 137f + phase * h * speed, h)
        drawLine(Color(0xFF9DC8E8).copy(alpha = alpha), Offset(x, y), Offset(x - length * 0.45f, y + length), 1.1f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawSnow(count: Int, phase: Float) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    for (i in 0 until count) {
        val drift = if (i % 2 == 0) phase * 48f else -phase * 32f
        val x = wrap(i * 91f + drift, w)
        val y = wrap(i * 157f + phase * h * 0.38f, h)
        drawCircle(Color.White.copy(alpha = 0.45f), if (i % 3 == 0) 2.1f else 1.25f, Offset(x, y))
    }
}

private fun DrawScope.drawWind(count: Int, phase: Float) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    for (i in 0 until count) {
        val x = wrap(i * 113f + phase * w * 1.35f, w)
        val y = wrap(i * 83f + phase * 38f, h)
        drawLine(Color(0xFFD1B06A).copy(alpha = 0.26f), Offset(x, y), Offset(x + 46f, y - 10f), 1.4f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawFogLines(phase: Float) {
    for (i in 0 until 7) {
        val y = size.height * (i + 1) / 8f
        val xShift = wrap(phase * size.width * 0.35f + i * 57f, size.width.coerceAtLeast(1f)) - size.width * 0.2f
        drawLine(Color.White.copy(alpha = 0.13f), Offset(xShift - size.width * 0.4f, y), Offset(xShift + size.width * 1.1f, y - 18f), 18f, cap = StrokeCap.Round)
    }
}

private fun wrap(value: Float, max: Float): Float {
    val m = max.coerceAtLeast(1f)
    val r = value % m
    return if (r < 0f) r + m else r
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
    drawRect(Color.White.copy(alpha = 0.22f), Offset(center.x - r * 0.90f, center.y - r * 0.35f), Size(r * 0.30f, r * 0.24f))
    drawRect(Color.White.copy(alpha = 0.22f), Offset(center.x + r * 0.60f, center.y - r * 0.35f), Size(r * 0.30f, r * 0.24f))
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
    drawIntoCanvas { canvas ->
        val p = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = (9.5f * textScale).coerceIn(10f, 26f)
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(210, 0, 0, 0))
        }
        canvas.nativeCanvas.drawText(name, screen.x, screen.y + r + p.textSize + 5f, p)
    }
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
            // V0.8 城池背景CG（缺图自动回退占位）
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
                    // V0.8 地理信息行
                    if (city != null && city.route.isNotBlank()) {
                        Text(
                            "${city.route} · ${city.cityLevel} · ${terrainLabel(city.terrain)} · 口${city.population / 10000}万",
                            color = Color(0xFF8C7A60), fontSize = 10.sp
                        )
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
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CityStatItem("商", "商业", "${city.commerce}")
                    CityStatItem("农", "农业", "${city.agriculture}")
                    if (city.isWaterNode) CityStatItem("漕", "水运", "通")
                    if (city.isPass) CityStatItem("关", "关隘", "险")
                }
                Spacer(Modifier.height(12.dp))
                Text("驻防军团", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (armies.isEmpty()) {
                    Text("此城暂无正式军团驻防。", color = Color(0xFF8C7A60), fontSize = 11.sp)
                } else {
                    armies.take(4).forEach { army -> CityArmyRow(army, officerNames[army.commanderId] ?: army.status) }
                }
                Spacer(Modifier.height(12.dp))
                Text("拟旨操作", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    CityActionButton("修城", Modifier.weight(1f)) { onDraft("repair") }
                    CityActionButton("调兵", Modifier.weight(1f)) { onDraft("dispatch") }
                    CityActionButton("筹粮", Modifier.weight(1f)) { onDraft("grain") }
                    CityActionButton(if (city.owner == "jin") "进取" else "备战", Modifier.weight(1f)) { onDraft("attack") }
                }
                if (city.owner == "song") {
                    Spacer(Modifier.height(7.dp))
                    CityActionButton("营 建 城 池", Modifier.fillMaxWidth()) { onDraft("manage") }
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

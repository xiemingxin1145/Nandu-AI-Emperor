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
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.MapNode
import com.xiemingxin.nandu.game.RoadType
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.MapBg
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun MapScreen(
    gameState: GameState,
    onCitySelected: (String) -> Unit = {}
) {
    var cameraX by remember { mutableStateOf(2500f) }
    var cameraY by remember { mutableStateOf(1200f) }
    var zoom by remember { mutableStateOf(0.027f) }

    var selectedId by remember { mutableStateOf<String?>(null) }
    val cityMap = gameState.cities.associateBy { it.id }

    Box(modifier = Modifier.fillMaxSize().background(MapBg)) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
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
                        val nearest = MapData.nodes.minByOrNull { n ->
                            (n.worldX - wx).pow(2) + (n.worldY - wy).pow(2)
                        }
                        nearest?.let { n ->
                            val sx = (n.worldX - cameraX) * zoom
                            val sy = (n.worldY - cameraY) * zoom
                            val dist = sqrt((tap.x - sx).pow(2) + (tap.y - sy).pow(2))
                            if (dist < 55f) {
                                selectedId = if (selectedId == n.id) null else n.id
                                onCitySelected(n.id)
                            } else {
                                selectedId = null
                            }
                        }
                    }
                }
        ) {
            drawRect(Color(0xFF122016))
            drawTerrainRegions(cameraX, cameraY, zoom)
            drawFactionRegions(cameraX, cameraY, zoom)
            drawMajorRivers(cameraX, cameraY, zoom)
            drawMapGrid(cameraX, cameraY, zoom)

            MapData.roads.forEach { road ->
                val from = MapData.nodeMap[road.fromId] ?: return@forEach
                val to = MapData.nodeMap[road.toId] ?: return@forEach
                val fs = w2s(from.worldX, from.worldY, cameraX, cameraY, zoom)
                val ts = w2s(to.worldX, to.worldY, cameraX, cameraY, zoom)
                val sw = (2.5f * zoom * 38f).coerceIn(1f, 6f)

                val (color, pathFx) = when (road.type) {
                    RoadType.RIVER -> Color(0xFF68B7E8) to null
                    RoadType.CANAL -> Color(0xFF9DD7EA) to PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                    RoadType.SEA -> Color(0xFF2376C9) to PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                    RoadType.MOUNTAIN -> Color(0xFFB08A54) to PathEffect.dashPathEffect(floatArrayOf(5f, 9f))
                    RoadType.LAND -> Color(0xFFD0A66A) to null
                    RoadType.PASS -> Color(0xFF8B2500) to null
                }
                drawLine(color, fs, ts, strokeWidth = sw, pathEffect = pathFx, cap = StrokeCap.Round)
            }

            MapData.nodes.forEach { node ->
                val city = cityMap[node.id]
                val screen = w2s(node.worldX, node.worldY, cameraX, cameraY, zoom)
                val isJin = city?.owner == "jin"
                val isSel = selectedId == node.id
                val r = ((if (node.isCapital) 14f else 9f) * zoom * 35f).coerceIn(6f, 24f)

                if (isSel) {
                    drawCircle(ImperialGold.copy(alpha = 0.30f), r * 2.7f, screen)
                }
                city?.troops?.let { t ->
                    if (t > 5000) {
                        val haloR = r + (t / 60000f * 10f).coerceIn(2f, 10f)
                        val haloColor = if (isJin) Color(0xFFB22222) else Color(0xFF2E86C1)
                        drawCircle(haloColor.copy(alpha = 0.20f), haloR, screen)
                    }
                }

                val borderColor = if (isJin) Color(0xFFE24A4A) else if (node.isCapital) ImperialGold else Color(0xFF4DA3E6)
                drawCityIcon(screen, r, borderColor, node.isCapital)

                val textScale = zoom * 38f
                if (textScale > 0.55f) {
                    drawIntoCanvas { canvas ->
                        val p = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = (9.5f * textScale).coerceIn(10f, 26f)
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            setShadowLayer(3f, 0f, 1f, android.graphics.Color.argb(180, 0, 0, 0))
                        }
                        canvas.nativeCanvas.drawText(node.name, screen.x, screen.y + r + p.textSize + 3f, p)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp)
                .background(Color(0xCC0A0D0F), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            LegendRow(Color(0xFF2E86C1), "■", "宋控")
            LegendRow(Color(0xFFB22222), "■", "金占")
            LegendRow(Color(0xFF68B7E8), "━", "河流/水路")
            LegendRow(Color(0xFFD0A66A), "━", "陆路")
            LegendRow(Color(0xFFB08A54), "┅", "山道")
        }

        selectedId?.let { id ->
            MapData.nodeMap[id]?.let { node ->
                CityDetailPanel(
                    node = node,
                    city = cityMap[id],
                    onDismiss = { selectedId = null },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

private fun DrawScope.drawTerrainRegions(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(0f, 0f, 16000f, 10000f, Color(0xFF152417), camX, camY, zoom)
    drawWorldRect(6400f, 1700f, 11200f, 4700f, Color(0xFF3A3A20).copy(alpha = 0.45f), camX, camY, zoom) // 北方平原
    drawWorldRect(6700f, 5000f, 12100f, 7100f, Color(0xFF163C31).copy(alpha = 0.50f), camX, camY, zoom) // 江淮水网
    drawWorldRect(3100f, 5600f, 6900f, 8400f, Color(0xFF244322).copy(alpha = 0.55f), camX, camY, zoom) // 川蜀盆地
    drawWorldRect(4500f, 4300f, 7600f, 6000f, Color(0xFF4A3822).copy(alpha = 0.45f), camX, camY, zoom) // 秦岭山道
    drawWorldRect(10300f, 7000f, 13600f, 9300f, Color(0xFF183B2D).copy(alpha = 0.45f), camX, camY, zoom) // 两浙闽地
}

private fun DrawScope.drawFactionRegions(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(7000f, 1900f, 10400f, 4400f, Color(0xFFB22222).copy(alpha = 0.15f), camX, camY, zoom)
    drawWorldRect(3300f, 5100f, 13300f, 9000f, Color(0xFF2E86C1).copy(alpha = 0.10f), camX, camY, zoom)
}

private fun DrawScope.drawMajorRivers(camX: Float, camY: Float, zoom: Float) {
    drawWorldPath(
        listOf(
            Offset(2500f, 6900f), Offset(4200f, 6750f), Offset(6000f, 6650f),
            Offset(7600f, 6600f), Offset(9200f, 6550f), Offset(10800f, 6650f), Offset(13000f, 7050f)
        ),
        Color(0xFF2D9CDB).copy(alpha = 0.65f),
        8f,
        camX,
        camY,
        zoom
    )
    drawWorldPath(
        listOf(
            Offset(7200f, 5200f), Offset(7900f, 5700f), Offset(7600f, 6600f), Offset(7000f, 7000f)
        ),
        Color(0xFF49B6E8).copy(alpha = 0.55f),
        5f,
        camX,
        camY,
        zoom
    )
    drawWorldPath(
        listOf(
            Offset(8500f, 5250f), Offset(9600f, 5400f), Offset(10800f, 5600f), Offset(11600f, 6000f)
        ),
        Color(0xFF5BC0EB).copy(alpha = 0.55f),
        6f,
        camX,
        camY,
        zoom
    )
    drawWorldPath(
        listOf(
            Offset(6500f, 3800f), Offset(8200f, 3900f), Offset(9000f, 3900f), Offset(10400f, 3600f)
        ),
        Color(0xFF496F9A).copy(alpha = 0.35f),
        5f,
        camX,
        camY,
        zoom
    )
}

private fun DrawScope.drawWorldRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    color: Color,
    camX: Float,
    camY: Float,
    zoom: Float
) {
    val topLeft = w2s(left, top, camX, camY, zoom)
    val bottomRight = w2s(right, bottom, camX, camY, zoom)
    drawRect(
        color = color,
        topLeft = topLeft,
        size = Size(bottomRight.x - topLeft.x, bottomRight.y - topLeft.y)
    )
}

private fun DrawScope.drawWorldPath(
    points: List<Offset>,
    color: Color,
    baseWidth: Float,
    camX: Float,
    camY: Float,
    zoom: Float
) {
    if (points.size < 2) return
    val path = Path()
    val first = w2s(points.first().x, points.first().y, camX, camY, zoom)
    path.moveTo(first.x, first.y)
    points.drop(1).forEach { p ->
        val sp = w2s(p.x, p.y, camX, camY, zoom)
        path.lineTo(sp.x, sp.y)
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = (baseWidth * zoom * 35f).coerceIn(2f, 14f), cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawCityIcon(center: Offset, r: Float, color: Color, isCapital: Boolean) {
    drawRect(
        color = Color(0xFF050504),
        topLeft = Offset(center.x - r * 1.25f, center.y - r * 0.95f),
        size = Size(r * 2.5f, r * 1.75f)
    )
    drawRect(
        color = color,
        topLeft = Offset(center.x - r, center.y - r * 0.75f),
        size = Size(r * 2f, r * 1.35f)
    )
    drawRect(
        color = Color(0xFF11100C),
        topLeft = Offset(center.x - r * 0.36f, center.y - r * 0.18f),
        size = Size(r * 0.72f, r * 0.78f)
    )
    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(center.x - r, center.y - r * 0.75f), end = Offset(center.x, center.y - r * 1.25f), strokeWidth = 1.4f)
    drawLine(color = Color.White.copy(alpha = 0.5f), start = Offset(center.x, center.y - r * 1.25f), end = Offset(center.x + r, center.y - r * 0.75f), strokeWidth = 1.4f)
    if (isCapital) {
        drawCircle(ImperialGold.copy(alpha = 0.35f), r * 1.9f, center)
    }
}

private fun DrawScope.drawMapGrid(camX: Float, camY: Float, zoom: Float) {
    val gridWorld = 2000f
    val gridColor = Color(0xFF233029).copy(alpha = 0.55f)
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

private fun w2s(wx: Float, wy: Float, camX: Float, camY: Float, zoom: Float) =
    Offset((wx - camX) * zoom, (wy - camY) * zoom)

@Composable
private fun LegendRow(color: Color, symbol: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(symbol, color = color, fontSize = 11.sp)
        Text(label, color = Color(0xFF9A9078), fontSize = 10.sp)
    }
}

@Composable
fun CityDetailPanel(
    node: MapNode,
    city: City?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF01A1208)),
        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        node.name,
                        color = ImperialGold,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when {
                            city == null -> "—"
                            city.owner == "jin" -> "🔴 金国占领"
                            node.isCapital -> "🟡 大宋京城"
                            else -> "🔵 大宋控制"
                        },
                        color = Color(0xFF8B7355),
                        fontSize = 12.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Text("✕", color = Color(0xFF5A5A5A), fontSize = 14.sp)
                }
            }

            if (city != null) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CityStatItem("⚔", "兵力", "${city.troops / 1000}k")
                    CityStatItem("🏯", "城防", "${city.defense}")
                    CityStatItem("🌾", "粮草", "${city.grain / 1000}k")
                    CityStatItem("💰", "金库", "${city.gold / 1000}k")
                    CityStatItem("❤", "民心", "${city.popularSupport}")
                }
            }
        }
    }
}

@Composable
private fun CityStatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 14.sp)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF6A6060), fontSize = 9.sp)
    }
}

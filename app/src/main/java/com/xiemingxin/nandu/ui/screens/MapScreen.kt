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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
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
    // ── 摄像机状态 ──
    // 初始视角：让地图大致居中可见
    var cameraX by remember { mutableStateOf(2500f) }
    var cameraY by remember { mutableStateOf(1200f) }
    var zoom   by remember { mutableStateOf(0.027f) }

    var selectedId by remember { mutableStateOf<String?>(null) }
    val cityMap = gameState.cities.associateBy { it.id }

    Box(modifier = Modifier.fillMaxSize().background(MapBg)) {

        // ── 主地图 Canvas ──
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                // 缩放 + 平移
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.015f, 0.14f)
                        cameraX = (cameraX - pan.x / zoom).coerceIn(0f, 14000f)
                        cameraY = (cameraY - pan.y / zoom).coerceIn(0f, 8000f)
                    }
                }
                // 点击城池
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
            // 1. 画背景网格（淡淡的，体现地图感）
            drawMapGrid(cameraX, cameraY, zoom)

            // 2. 画道路
            MapData.roads.forEach { road ->
                val from = MapData.nodeMap[road.fromId] ?: return@forEach
                val to   = MapData.nodeMap[road.toId]   ?: return@forEach
                val fs = w2s(from.worldX, from.worldY, cameraX, cameraY, zoom)
                val ts = w2s(to.worldX,   to.worldY,   cameraX, cameraY, zoom)
                val sw = (2.5f * zoom * 38f).coerceIn(1f, 6f)

                val (color, pathFx) = when (road.type) {
                    RoadType.RIVER    -> Color(0xFF4A90D9) to null
                    RoadType.CANAL    -> Color(0xFF87CEEB) to PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
                    RoadType.SEA      -> Color(0xFF1B6FBA) to PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
                    RoadType.MOUNTAIN -> Color(0xFF7A5C3A) to PathEffect.dashPathEffect(floatArrayOf(5f, 9f))
                    RoadType.LAND     -> Color(0xFF9B8260) to null
                    RoadType.PASS     -> Color(0xFF8B2500) to null
                }
                drawLine(color, fs, ts, strokeWidth = sw, pathEffect = pathFx, cap = StrokeCap.Round)
            }

            // 3. 画城池节点
            MapData.nodes.forEach { node ->
                val city   = cityMap[node.id]
                val screen = w2s(node.worldX, node.worldY, cameraX, cameraY, zoom)
                val isJin  = city?.owner == "jin"
                val isSel  = selectedId == node.id
                val r      = ((if (node.isCapital) 14f else 9f) * zoom * 35f).coerceIn(5f, 22f)

                // 选中光晕
                if (isSel) {
                    drawCircle(ImperialGold.copy(alpha = 0.25f), r * 2.6f, screen)
                }
                // 兵力光环
                city?.troops?.let { t ->
                    if (t > 5000) {
                        val haloR = r + (t / 60000f * 10f).coerceIn(2f, 10f)
                        val haloColor = if (isJin) Color(0xFFB22222) else Color(0xFF2E86C1)
                        drawCircle(haloColor.copy(alpha = 0.18f), haloR, screen)
                    }
                }
                // 外圈边框
                val borderColor = if (isJin) Color(0xFFB22222) else if (node.isCapital) ImperialGold else Color(0xFF2E86C1)
                drawCircle(Color(0xFF050504), r + 2.5f, screen)
                drawCircle(borderColor, r, screen)

                // 首都内部十字
                if (node.isCapital) {
                    val cr = r * 0.45f
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(screen.x - cr, screen.y), Offset(screen.x + cr, screen.y), 1.5f)
                    drawLine(Color.White.copy(alpha = 0.9f), Offset(screen.x, screen.y - cr), Offset(screen.x, screen.y + cr), 1.5f)
                }

                // 城池名称（缩放够大才显示）
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
                        canvas.nativeCanvas.drawText(node.name, screen.x, screen.y + r + p.textSize + 1f, p)
                    }
                }
            }
        }

        // ── 右下角图例 ──
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 80.dp)
                .background(Color(0xCC0A0D0F), RoundedCornerShape(6.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            LegendRow(Color(0xFF4A90D9), "━", "水路")
            LegendRow(Color(0xFF87CEEB), "┅", "漕运")
            LegendRow(Color(0xFF1B6FBA), "┅", "海路")
            LegendRow(Color(0xFF7A5C3A), "┅", "山道")
            LegendRow(Color(0xFF9B8260), "━", "陆路")
        }

        // ── 选中城池详情卡片 ──
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

// ── 背景网格 ──
private fun DrawScope.drawMapGrid(camX: Float, camY: Float, zoom: Float) {
    val gridWorld = 2000f
    val gridColor = Color(0xFF1A2020)
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

// 世界坐标 → 屏幕坐标
private fun w2s(wx: Float, wy: Float, camX: Float, camY: Float, zoom: Float) =
    Offset((wx - camX) * zoom, (wy - camY) * zoom)

@Composable
private fun LegendRow(color: Color, symbol: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(symbol, color = color, fontSize = 11.sp)
        Text(label,  color = Color(0xFF6A6A6A), fontSize = 10.sp)
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

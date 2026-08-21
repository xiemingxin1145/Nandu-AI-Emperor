package com.xiemingxin.nandu.prototype.mapglobe

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * MAP-PROTOTYPE-001 independent dual-layer map prototype.
 * Does NOT replace MapScreen. Does NOT touch court/AI executor.
 */
@Composable
fun GlobeMapPrototypeScreen(
    onExit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val flatten = remember { Animatable(0f) }
    var rotYaw by remember { mutableFloatStateOf(-0.35f) }
    var rotPitch by remember { mutableFloatStateOf(0.08f) }
    var cameraX by remember { mutableFloatStateOf(9000f) }
    var cameraY by remember { mutableFloatStateOf(4800f) }
    var flatZoom by remember { mutableFloatStateOf(0.055f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var modeLabel by remember { mutableStateOf("寰宇图") }
    val isFlat = flatten.value > 0.92f

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF070B12))) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isFlat) {
                    if (isFlat) {
                        detectTransformGestures { _, pan, zoomChange, _ ->
                            flatZoom = (flatZoom * zoomChange).coerceIn(0.03f, 0.14f)
                            cameraX = (cameraX - pan.x / flatZoom).coerceIn(0f, GlobeProjection.WORLD_W)
                            cameraY = (cameraY - pan.y / flatZoom).coerceIn(0f, GlobeProjection.WORLD_H)
                        }
                    } else {
                        detectDragGestures { change, drag ->
                            change.consume()
                            rotYaw += drag.x * 0.0045f
                            rotPitch = (rotPitch - drag.y * 0.0025f).coerceIn(-0.35f, 0.45f)
                        }
                    }
                }
                .pointerInput(flatten.value, rotYaw, rotPitch, cameraX, cameraY, flatZoom) {
                    detectTapGestures { tap ->
                        val hits = PrototypeMapSampleData.cities.mapNotNull { city ->
                            val p = GlobeProjection.project(
                                city.worldX, city.worldY,
                                size.width.toFloat(), size.height.toFloat(),
                                rotYaw, rotPitch, flatten.value,
                                cameraX, cameraY, flatZoom
                            )
                            if (!p.visible) return@mapNotNull null
                            val r = GlobeProjection.hitTestRadius(if (city.isCapital) 18f else 12f, p.scale, flatten.value)
                            val d = sqrt((tap.x - p.screenX).pow(2) + (tap.y - p.screenY).pow(2))
                            if (d <= r) city to d else null
                        }
                        selectedId = hits.minByOrNull { it.second }?.first?.id
                    }
                }
        ) {
            val w = size.width
            val h = size.height
            val t = flatten.value

            if (t < 0.85f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1A2740), Color(0xFF0A101C), Color(0xFF05070C)),
                        center = Offset(w * 0.5f, h * 0.48f),
                        radius = minOf(w, h) * 0.55f
                    ),
                    radius = minOf(w, h) * 0.46f * (1f - t * 0.15f),
                    center = Offset(w * 0.5f, h * 0.48f)
                )
                drawCircle(
                    color = Color(0xFFC9A227).copy(alpha = 0.35f * (1f - t)),
                    radius = minOf(w, h) * 0.46f * (1f - t * 0.15f),
                    center = Offset(w * 0.5f, h * 0.48f),
                    style = Stroke(width = 2f)
                )
            } else {
                drawRect(Color(0xFF1A140C))
                drawRect(Color(0x22C9A227))
            }

            if (t < 0.7f) {
                drawFactionWash(Color(0x66B22222), 9000f, 2400f, 2200f, w, h, rotYaw, rotPitch, t, cameraX, cameraY, flatZoom)
                drawFactionWash(Color(0x662E86C1), 10000f, 6200f, 2600f, w, h, rotYaw, rotPitch, t, cameraX, cameraY, flatZoom)
                drawFactionWash(Color(0x66B38A48), 3000f, 2400f, 1400f, w, h, rotYaw, rotPitch, t, cameraX, cameraY, flatZoom)
            }

            val projected = PrototypeMapSampleData.cities.map { city ->
                city to GlobeProjection.project(
                    city.worldX, city.worldY, w, h,
                    rotYaw, rotPitch, t, cameraX, cameraY, flatZoom
                )
            }.sortedBy { it.second.depth }

            projected.forEach { (city, p) ->
                if (!p.visible && t < 0.55f) return@forEach
                val base = PrototypeMapSampleData.factionColors[city.faction] ?: 0xFF888888
                val color = Color(base).copy(alpha = p.alpha)
                val r = GlobeProjection.hitTestRadius(if (city.isCapital) 16f else 10f, p.scale, t)
                if (selectedId == city.id) {
                    drawCircle(Color(0xFFC9A227).copy(alpha = 0.55f), r * 1.8f, Offset(p.screenX, p.screenY))
                }
                drawCircle(color, r, Offset(p.screenX, p.screenY))
                drawCircle(Color.White.copy(alpha = 0.35f * p.alpha), r, Offset(p.screenX, p.screenY), style = Stroke(1.5f))
                if (p.alpha > 0.35f && (city.isCapital || t > 0.5f || selectedId == city.id)) {
                    drawContext.canvas.nativeCanvas.drawText(
                        city.name,
                        p.screenX,
                        p.screenY + r + 16f,
                        android.graphics.Paint().apply {
                            this.color = android.graphics.Color.argb((p.alpha * 220).toInt().coerceIn(0, 255), 232, 220, 192)
                            textSize = 28f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .background(Color(0xCC0A0E16))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("MAP-PROTOTYPE-001 · 双层天下图", color = Color(0xFFC9A227), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text("当前：$modeLabel  ·  伪球面 ⇄ 平铺战略（不替换正式山河页）", color = Color(0xFF9A8862), fontSize = 11.sp)
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xCC0A0E16))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectedId?.let { id ->
                val city = PrototypeMapSampleData.cities.find { it.id == id }
                if (city != null) {
                    Text(
                        "选中：${city.name}（${city.faction}）  world=(${city.worldX.toInt()}, ${city.worldY.toInt()})",
                        color = Color(0xFFE8DCC0),
                        fontSize = 12.sp
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            flatten.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
                            modeLabel = "平铺战略图"
                        }
                    },
                    enabled = flatten.value < 0.95f,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4E16)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("展开山河", color = Color(0xFFE8DCC0), fontSize = 13.sp) }
                Button(
                    onClick = {
                        scope.launch {
                            flatten.animateTo(0f, tween(900, easing = FastOutSlowInEasing))
                            modeLabel = "寰宇图"
                            selectedId = null
                        }
                    },
                    enabled = flatten.value > 0.05f,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E3A5F)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) { Text("收回寰宇", color = Color(0xFFE8DCC0), fontSize = 13.sp) }
            }
            if (onExit != null) {
                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF333333)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("离开原型", color = Color(0xFFB0B0B0), fontSize = 12.sp) }
            } else {
                Text("挂载方式见 docs/demo/MAP_GLOBE_PROTOTYPE.md", color = Color(0xFF6A6A6A), fontSize = 10.sp)
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFactionWash(
    color: Color,
    worldX: Float,
    worldY: Float,
    spread: Float,
    w: Float,
    h: Float,
    rotYaw: Float,
    rotPitch: Float,
    flatten: Float,
    cameraX: Float,
    cameraY: Float,
    flatZoom: Float
) {
    val p = GlobeProjection.project(worldX, worldY, w, h, rotYaw, rotPitch, flatten, cameraX, cameraY, flatZoom)
    if (!p.visible) return
    val radius = spread * (0.02f + (1f - flatten) * 0.04f) * p.scale
    drawCircle(color.copy(alpha = color.alpha * p.alpha * 0.8f), radius, Offset(p.screenX, p.screenY))
}

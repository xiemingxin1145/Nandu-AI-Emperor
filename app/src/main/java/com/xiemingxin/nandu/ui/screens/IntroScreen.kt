package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.ui.components.AssetImage
import kotlinx.coroutines.delay

private val IntroGold = Color(0xFFC9A227)
private val IntroCream = Color(0xFFE8DCC0)
private val IntroInk = Color(0xFF0E0A05)
private val IntroRed = Color(0xFF7D1D16)

private val IntroNarration = listOf(
    "靖康之后，山河破碎。",
    "汴京陷落，宗室南渡。",
    "金兵压境，江淮震动。",
    "朕承大统于危亡之间。",
    "山河未定，朕当中兴。"
)

/**
 * V1.3.4 动态开场：皇宫背景 + 赵构立绘 + 雪火粒子 + 旁白序章。
 * 先用 Compose 轻动画做电影感，不引入真视频和重引擎。
 */
@Composable
fun IntroScreen(onStart: () -> Unit) {
    var phase by remember { mutableFloatStateOf(0f) }
    var lineCount by remember { mutableIntStateOf(1) }

    LaunchedEffect(Unit) {
        while (true) {
            phase = (phase + 0.012f) % 1f
            delay(33L)
        }
    }

    LaunchedEffect(Unit) {
        IntroNarration.indices.forEach { i ->
            delay(if (i == 0) 600L else 1500L)
            lineCount = (i + 1).coerceAtMost(IntroNarration.size)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(IntroInk)) {
        AssetImage(
            path = "images/buildings/building_imperial_palace_01.webp",
            contentDescription = "临安行在",
            contentScale = ContentScale.Crop,
            placeholderText = "宫",
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        IntroInk.copy(alpha = 0.44f),
                        IntroInk.copy(alpha = 0.22f),
                        IntroInk.copy(alpha = 0.92f)
                    )
                )
            )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawSnowAndEmbers(phase)
            drawWarSmoke(phase)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(34.dp))
            Text("南 渡 无 悔", color = IntroGold, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("山河未定，朕当中兴", color = IntroCream, fontSize = 14.sp)
            Spacer(Modifier.height(18.dp))
            Text("V1.3.4 · 序章", color = Color(0xFF8C7A60), fontSize = 10.sp)

            Spacer(Modifier.height(30.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AssetImage(
                    path = "images/characters/halfbody_zhao_gou.webp",
                    contentDescription = "康王赵构",
                    contentScale = ContentScale.Crop,
                    placeholderText = "构",
                    modifier = Modifier.size(138.dp).clip(RoundedCornerShape(16.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text("康王 · 赵构", color = IntroGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("北望汴梁，南守江淮。你不是在选择一座城，而是在选择大宋还能不能活下去。", color = IntroCream, fontSize = 12.sp, lineHeight = 19.sp)
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                    .background(IntroInk.copy(alpha = 0.66f))
                    .padding(16.dp)
            ) {
                Text(
                    IntroNarration.take(lineCount).joinToString("\n"),
                    color = IntroCream,
                    fontSize = 15.sp,
                    lineHeight = 26.sp,
                    textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IntroGold),
                border = BorderStroke(1.dp, IntroCream.copy(alpha = 0.5f))
            ) {
                Text("即 位 · 中 兴 大 宋", color = IntroInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                border = BorderStroke(1.dp, IntroGold.copy(alpha = 0.45f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = IntroGold)
            ) {
                Text("跳过序章", fontSize = 12.sp)
            }
        }
    }
}

private fun DrawScope.drawSnowAndEmbers(phase: Float) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    for (i in 0 until 72) {
        val x = ((i * 89f + phase * w * 0.22f) % w)
        val y = ((i * 131f + phase * h * 0.56f) % h)
        val r = if (i % 3 == 0) 2.0f else 1.15f
        drawCircle(Color.White.copy(alpha = 0.30f), r, Offset(x, y))
    }
    for (i in 0 until 34) {
        val x = ((i * 113f + phase * w * 0.38f) % w)
        val y = h - ((i * 97f + phase * h * 0.72f) % h)
        drawCircle(IntroGold.copy(alpha = 0.28f), if (i % 4 == 0) 2.8f else 1.5f, Offset(x, y))
    }
}

private fun DrawScope.drawWarSmoke(phase: Float) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    val y = h * 0.42f
    val x1 = w * (0.15f + 0.05f * phase)
    val x2 = w * (0.76f - 0.04f * phase)
    drawCircle(Color.Black.copy(alpha = 0.16f), w * 0.22f, Offset(x1, y))
    drawCircle(IntroRed.copy(alpha = 0.10f), w * 0.16f, Offset(x2, y + 35f))
}

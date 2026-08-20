package com.xiemingxin.nandu.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.audio.GameAudioPlayer
import com.xiemingxin.nandu.ui.components.AssetImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 历史序章场景（V1.0）
 * 四幕结构：山河将倾 → 靖康之变 → 宋室南渡 → 历史开始偏转
 * 包含：旁白语音 + 同步字幕 + BGM + 环境声 + 静态CG动态演出 + 跳过
 */
private enum class PrologueAct { ACT_1, ACT_2, ACT_3, ACT_4, DONE }

private data class ActConfig(
    val cgPath: String,
    val cgFallback: String,
    val narratorPath: String,
    val subtitle: String,
    val actTitle: String,
    val bgmPath: String?,
    val ambiencePath: String?,
    val durationMs: Long,
    val kenBurnsZoom: Float = 1.15f,
    val kenBurnsOffsetX: Float = 0f,
    val kenBurnsOffsetY: Float = 0f
)

private val acts = listOf(
    ActConfig(
        actTitle = "第一幕 · 山河将倾",
        cgPath = "images/events/event_jin_army_crosses_huai.webp",
        cgFallback = "images/battles/battle_field.webp",
        narratorPath = "audio/voice/narrator/narrator_act1_shanhejiangqing.wav",
        subtitle = "大宋靖康年间，金军铁骑再度南下。\n汴京以北，烽火连天，山河将倾。",
        bgmPath = "audio/bgm/bgm_crisis.ogg",
        ambiencePath = "audio/ambience/amb_frontier_wind.ogg",
        durationMs = 18000,
        kenBurnsZoom = 1.2f,
        kenBurnsOffsetX = 0.05f
    ),
    ActConfig(
        actTitle = "第二幕 · 靖康之变",
        cgPath = "images/events/event_jingkang_siege_01.webp",
        cgFallback = "images/battles/battle_siege.webp",
        narratorPath = "audio/voice/narrator/narrator_act2_jingkang.wav",
        subtitle = "汴京陷落，二帝北狩。\n百余年东京繁华，一夕倾覆。\n宗室百官，尽被掳北去。",
        bgmPath = "audio/bgm/bgm_event_sad.ogg",
        ambiencePath = "audio/ambience/amb_storm.ogg",
        durationMs = 22000,
        kenBurnsZoom = 1.18f,
        kenBurnsOffsetY = -0.03f
    ),
    ActConfig(
        actTitle = "第三幕 · 宋室南渡",
        cgPath = "images/events/event_jianyan_south_crossing_01.webp",
        cgFallback = "images/locations/yangzhou_river.webp",
        narratorPath = "audio/voice/narrator/narrator_act3_nandu.wav",
        subtitle = "康王赵构即位于南京应天府，改元建炎。\n然而江山未稳，金兵已经渡河而来。",
        bgmPath = "audio/bgm/bgm_map.ogg",
        ambiencePath = "audio/ambience/amb_river.ogg",
        durationMs = 18000,
        kenBurnsZoom = 1.12f,
        kenBurnsOffsetX = -0.04f
    ),
    ActConfig(
        actTitle = "第四幕 · 历史开始偏转",
        cgPath = "images/events/event_imperial_war_council_01.webp",
        cgFallback = "images/palace/chuigongdian.webp",
        narratorPath = "audio/voice/narrator/narrator_act4_lishipianzhuan.wav",
        subtitle = "旧史已成。\n余下山河，由你执笔。",
        bgmPath = "audio/bgm/bgm_main_menu.ogg",
        ambiencePath = "audio/ambience/amb_palace_murmur.ogg",
        durationMs = 14000,
        kenBurnsZoom = 1.25f,
        kenBurnsOffsetY = 0.02f
    )
)

@Composable
fun PrologueScreen(
    audioPlayer: GameAudioPlayer?,
    onPrologueComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentAct by remember { mutableStateOf(PrologueAct.ACT_1) }
    var actAlpha by remember { mutableStateOf(0f) }
    var subtitleAlpha by remember { mutableStateOf(0f) }
    var isTransitioning by remember { mutableStateOf(false) }
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    val actIndex = currentAct.ordinal.coerceAtMost(acts.size - 1)
    val config = acts[actIndex]

    // 进入一幕：淡入 + 播放旁白 + Ken Burns 效果
    LaunchedEffect(currentAct) {
        if (currentAct == PrologueAct.DONE) return@LaunchedEffect
        isTransitioning = true
        actAlpha = 0f
        subtitleAlpha = 0f
        scaleAnim.snapTo(1f)
        offsetXAnim.snapTo(0f)
        offsetYAnim.snapTo(0f)

        // 播放 BGM 和环境声
        config.bgmPath?.let { audioPlayer?.playBgm(it, volume = 0.5f) }
        config.ambiencePath?.let { audioPlayer?.playAmbience(it, volume = 0.4f) }

        // 淡入
        actAlpha = 1f
        delay(800)
        subtitleAlpha = 1f

        // Ken Burns 缓慢推镜/平移
        launch {
            scaleAnim.animateTo(
                targetValue = config.kenBurnsZoom,
                animationSpec = tween(durationMillis = config.durationMs.toInt(), easing = LinearEasing)
            )
        }
        launch {
            offsetXAnim.animateTo(
                targetValue = config.kenBurnsOffsetX,
                animationSpec = tween(durationMillis = config.durationMs.toInt(), easing = LinearEasing)
            )
        }
        launch {
            offsetYAnim.animateTo(
                targetValue = config.kenBurnsOffsetY,
                animationSpec = tween(durationMillis = config.durationMs.toInt(), easing = LinearEasing)
            )
        }

        // 播放旁白
        isTransitioning = false
        audioPlayer?.playVoice(config.narratorPath) {
            // 旁白播放完成，进入下一幕
            advanceToNextAct(
                currentAct = currentAct,
                onComplete = {
                    if (it == PrologueAct.DONE) {
                        audioPlayer?.stopAmbience()
                        onPrologueComplete()
                    } else {
                        currentAct = it
                    }
                }
            )
        }
    }

    // 清理
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.stopVoice()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
        // CG 背景（带 Ken Burns 效果）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .scale(scaleAnim.value)
                .offset(
                    x = (offsetXAnim.value * 200).dp,
                    y = (offsetYAnim.value * 200).dp
                )
                .alpha(actAlpha)
        ) {
            AssetImage(
                path = config.cgPath,
                fallbackPath = config.cgFallback,
                contentDescription = config.actTitle,
                contentScale = ContentScale.Crop,
                placeholderText = config.actTitle.take(2),
                modifier = Modifier.fillMaxSize()
            )
        }

        // 暗角遮罩
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(actAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)),
                        radius = 800f
                    )
                )
        )

        // 底部渐变（保证字幕可读）
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(actAlpha)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.85f)
                        )
                    )
                )
        )

        // 氛围粒子（烟尘/火星/飘雪）
        Canvas(modifier = Modifier.fillMaxSize().alpha(actAlpha * 0.6f)) {
            drawPrologueParticles(currentAct)
        }

        // 幕标题（顶部）
        Text(
            text = config.actTitle,
            color = Color(0xFFC9A227).copy(alpha = subtitleAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 48.dp)
                .alpha(subtitleAlpha)
        )

        // 字幕（底部）
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 32.dp, vertical = 60.dp)
                .alpha(subtitleAlpha),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.55f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFC9A227).copy(alpha = 0.3f))
        ) {
            Text(
                text = config.subtitle,
                color = Color(0xFFE8DCC0),
                fontSize = 15.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
            )
        }

        // 跳过按钮（右上角）
        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable {
                    audioPlayer?.stopVoice()
                    audioPlayer?.stopAmbience()
                    onPrologueComplete()
                },
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFC9A227).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("跳过", color = Color(0xFFC9A227), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.size(6.dp))
                Text("»", color = Color(0xFFC9A227), fontSize = 14.sp)
            }
        }

        // 幕进度指示（底部小点）
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            acts.indices.forEach { i ->
                val isActive = i == actIndex
                val isPast = i < actIndex
                Box(
                    modifier = Modifier
                        .size(if (isActive) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isActive -> Color(0xFFC9A227)
                                isPast -> Color(0xFFC9A227).copy(alpha = 0.4f)
                                else -> Color.White.copy(alpha = 0.2f)
                            }
                        )
                )
            }
        }
    }
}

private fun advanceToNextAct(
    currentAct: PrologueAct,
    onComplete: (PrologueAct) -> Unit
) {
    val next = when (currentAct) {
        PrologueAct.ACT_1 -> PrologueAct.ACT_2
        PrologueAct.ACT_2 -> PrologueAct.ACT_3
        PrologueAct.ACT_3 -> PrologueAct.ACT_4
        PrologueAct.ACT_4 -> PrologueAct.DONE
        PrologueAct.DONE -> PrologueAct.DONE
    }
    onComplete(next)
}

private fun DrawScope.drawPrologueParticles(act: PrologueAct) {
    val particleCount = 12
    val color = when (act) {
        PrologueAct.ACT_1 -> Color(0xFF8B4513).copy(alpha = 0.3f) // 风尘
        PrologueAct.ACT_2 -> Color(0xFFDC143C).copy(alpha = 0.25f) // 火星
        PrologueAct.ACT_3 -> Color(0xFFB0C4DE).copy(alpha = 0.3f) // 水雾
        PrologueAct.ACT_4 -> Color(0xFFC9A227).copy(alpha = 0.35f) // 金光
        PrologueAct.DONE -> Color.Transparent
    }
    val random = kotlin.random.Random(act.ordinal * 42L)
    repeat(particleCount) {
        val x = size.width * random.nextFloat()
        val y = size.height * (0.2f + random.nextFloat() * 0.6f)
        val radius = 1f + random.nextFloat() * 2.5f
        drawCircle(color = color, radius = radius, center = Offset(x, y))
    }
    // 飘落的线条（雨/雪）
    if (act == PrologueAct.ACT_2 || act == PrologueAct.ACT_3) {
        val lineColor = Color.White.copy(alpha = 0.15f)
        repeat(8) {
            val x = size.width * random.nextFloat()
            val y = size.height * random.nextFloat()
            drawLine(
                color = lineColor,
                start = Offset(x, y),
                end = Offset(x - 10f, y + 30f),
                strokeWidth = 1f
            )
        }
    }
}

package com.xiemingxin.nandu.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
 * 正式历史序章。
 *
 * 关键规则：
 * 1. 画面时间轴永远独立于音频。旁白文件缺失、解码失败或播放报错，都不能导致秒跳。
 * 2. 新游戏明确告诉玩家：玩家意识穿越进入赵构身份，而不是默认扮演历史原版赵构。
 * 3. 只有玩家主动点击“跳过”才允许提前结束序章。
 * 4. 当前测试版强制使用设备中文 TTS，保证真机一定有可听见的旁白；正式配音文件验收后再切回资产音频。
 */
private enum class PrologueAct { ACT_1, ACT_2, ACT_3, ACT_4, ACT_5, DONE }

private data class ActConfig(
    val cgPath: String,
    val cgFallback: String,
    val narratorPath: String?,
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
        durationMs = 16000,
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
        durationMs = 18000,
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
        durationMs = 16000,
        kenBurnsZoom = 1.12f,
        kenBurnsOffsetX = -0.04f
    ),
    ActConfig(
        actTitle = "第四幕 · 旧史已成",
        cgPath = "images/events/event_imperial_war_council_01.webp",
        cgFallback = "images/palace/chuigongdian.webp",
        narratorPath = "audio/voice/narrator/narrator_act4_lishipianzhuan.wav",
        subtitle = "靖康已成旧史，山河却仍在烽火之中。\n从这一刻起，未来不再只有一条路。",
        bgmPath = "audio/bgm/bgm_main_menu.ogg",
        ambiencePath = "audio/ambience/amb_palace_murmur.ogg",
        durationMs = 15000,
        kenBurnsZoom = 1.2f,
        kenBurnsOffsetY = 0.02f
    ),
    ActConfig(
        actTitle = "第五幕 · 我成了赵构",
        cgPath = "images/events/cg_prologue_identity_reflection.webp",
        cgFallback = "images/palace/chuigongdian.webp",
        narratorPath = null,
        subtitle = "再睁开眼时，我已不在原来的世界。\n铜镜里的那张脸，属于大宋官家——赵构。\n\n殿外传来内侍的声音：\n“官家，百官已经候在垂拱殿。”\n\n旧史已成。余下山河，由我执笔。",
        bgmPath = "audio/bgm/bgm_main_menu.ogg",
        ambiencePath = "audio/ambience/amb_palace_murmur.ogg",
        durationMs = 18000,
        kenBurnsZoom = 1.12f,
        kenBurnsOffsetX = 0.02f
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
    val scaleAnim = remember { Animatable(1f) }
    val offsetXAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(0f) }

    val actIndex = currentAct.ordinal.coerceAtMost(acts.size - 1)
    val config = acts[actIndex]

    LaunchedEffect(currentAct) {
        if (currentAct == PrologueAct.DONE) return@LaunchedEffect

        audioPlayer?.stopVoice()
        actAlpha = 0f
        subtitleAlpha = 0f
        scaleAnim.snapTo(1f)
        offsetXAnim.snapTo(0f)
        offsetYAnim.snapTo(0f)

        config.bgmPath?.let { audioPlayer?.playBgm(it, volume = 0.46f) }
        config.ambiencePath?.let { audioPlayer?.playAmbience(it, volume = 0.32f) }

        actAlpha = 1f
        delay(700)
        subtitleAlpha = 1f

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

        // 真机反馈现有 WAV 无声，因此本测试分支先强制中文 TTS。
        // 时间轴仍然独立：TTS 成功/失败都不会影响画面时长。
        audioPlayer?.speakNarration(config.subtitle.replace("\n", " "))

        delay(config.durationMs)
        audioPlayer?.stopVoice()

        val next = nextAct(currentAct)
        if (next == PrologueAct.DONE) {
            audioPlayer?.stopAmbience()
            audioPlayer?.stopBgm()
            onPrologueComplete()
        } else {
            currentAct = next
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            audioPlayer?.stopVoice()
            audioPlayer?.stopAmbience()
        }
    }

    Box(modifier = modifier.fillMaxSize().background(Color.Black)) {
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

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(actAlpha)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.28f),
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.88f)
                        )
                    )
                )
        )

        Canvas(modifier = Modifier.fillMaxSize().alpha(actAlpha * 0.6f)) {
            drawPrologueParticles(currentAct)
        }

        Text(
            text = config.actTitle,
            color = Color(0xFFC9A227).copy(alpha = subtitleAlpha),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 52.dp)
                .alpha(subtitleAlpha)
        )

        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 28.dp, vertical = 64.dp)
                .alpha(subtitleAlpha),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.58f)),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0xFFC9A227).copy(alpha = 0.32f))
        ) {
            Text(
                text = config.subtitle,
                color = Color(0xFFE8DCC0),
                fontSize = if (currentAct == PrologueAct.ACT_5) 16.sp else 15.sp,
                lineHeight = 26.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 18.dp)
            )
        }

        Card(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .clickable {
                    audioPlayer?.stopVoice()
                    audioPlayer?.stopAmbience()
                    audioPlayer?.stopBgm()
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

private fun nextAct(currentAct: PrologueAct): PrologueAct = when (currentAct) {
    PrologueAct.ACT_1 -> PrologueAct.ACT_2
    PrologueAct.ACT_2 -> PrologueAct.ACT_3
    PrologueAct.ACT_3 -> PrologueAct.ACT_4
    PrologueAct.ACT_4 -> PrologueAct.ACT_5
    PrologueAct.ACT_5 -> PrologueAct.DONE
    PrologueAct.DONE -> PrologueAct.DONE
}

private fun DrawScope.drawPrologueParticles(act: PrologueAct) {
    val particleCount = 12
    val color = when (act) {
        PrologueAct.ACT_1 -> Color(0xFF8B4513).copy(alpha = 0.3f)
        PrologueAct.ACT_2 -> Color(0xFFDC143C).copy(alpha = 0.25f)
        PrologueAct.ACT_3 -> Color(0xFFB0C4DE).copy(alpha = 0.3f)
        PrologueAct.ACT_4 -> Color(0xFFC9A227).copy(alpha = 0.32f)
        PrologueAct.ACT_5 -> Color(0xFFC9A227).copy(alpha = 0.22f)
        PrologueAct.DONE -> Color.Transparent
    }
    val random = kotlin.random.Random(act.ordinal * 42L)
    repeat(particleCount) {
        val x = size.width * random.nextFloat()
        val y = size.height * (0.2f + random.nextFloat() * 0.6f)
        val radius = 1f + random.nextFloat() * 2.5f
        drawCircle(color = color, radius = radius, center = Offset(x, y))
    }
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

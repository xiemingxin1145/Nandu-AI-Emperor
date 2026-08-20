package com.xiemingxin.nandu.ui.screens

import android.net.Uri
import android.widget.VideoView
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
import androidx.compose.ui.viewinterop.AndroidView
import com.xiemingxin.nandu.audio.GameAudioPlayer
import com.xiemingxin.nandu.ui.components.AssetImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 正式历史序章。
 *
 * 音频硬规则：
 * 1. 画面时间轴绝不依赖音频回调，音频失败不能让序章秒跳。
 * 2. 正式旁白使用已打包的 M4A；播放器失败时也只影响该条声音。
 * 3. 序章 BGM 只能从已人工验收的白名单进入，旧曲不得自动复用。
 * 4. 所有生成视频的内嵌音轨一律静音；配乐、环境声、旁白只由游戏音频层负责。
 * 5. 每幕切换前先停掉上一幕 BGM/环境声/人声，防止叠音和残留。
 */
private enum class PrologueAct { ACT_1, ACT_2, ACT_3, ACT_4, ACT_5, ACT_6, DONE }

private data class ActConfig(
    val cgPath: String,
    val cgFallback: String,
    val videoPath: String? = null,
    val narratorPath: String,
    val subtitle: String,
    val actTitle: String,
    val bgmPath: String? = null,
    val ambiencePath: String? = null,
    val ambienceVolume: Float = 0.12f,
    val durationMs: Long,
    val kenBurnsZoom: Float = 1.15f,
    val kenBurnsOffsetX: Float = 0f,
    val kenBurnsOffsetY: Float = 0f
)

/**
 * 只有真机试听通过、确认无歌词/人声/爆音/提示语的曲目才允许加入。
 * 当前先清空，彻底隔离旧的 crisis/event_sad/map/main_menu/court BGM。
 * 新的南宋场景音乐入库后再逐条加入白名单。
 */
private val CERTIFIED_PROLOGUE_BGM: Set<String> = emptySet()

private val acts = listOf(
    ActConfig(
        actTitle = "第一幕 · 山河将倾",
        cgPath = "images/events/event_jin_army_crosses_huai.webp",
        cgFallback = "images/battles/battle_field.webp",
        narratorPath = "audio/voice/prologue/prologue_act1_shanhejiangqing.m4a",
        subtitle = "大宋靖康年间，金军铁骑再度南下。\n汴京以北，烽火连天，山河将倾。",
        ambiencePath = "audio/ambience/amb_frontier_wind.ogg",
        ambienceVolume = 0.10f,
        durationMs = 14000,
        kenBurnsZoom = 1.2f,
        kenBurnsOffsetX = 0.05f
    ),
    ActConfig(
        actTitle = "第二幕 · 靖康之变",
        cgPath = "images/events/event_jingkang_siege_01.webp",
        cgFallback = "images/battles/battle_siege.webp",
        videoPath = "videos/intro/V03_intro_cinematic.mp4",
        narratorPath = "audio/voice/prologue/prologue_act2_jingkang.m4a",
        subtitle = "汴京陷落，二帝北狩。\n百余年东京繁华，一夕倾覆。\n宗室百官，尽被掳北去。",
        // 本幕暂不叠加 storm：先把生成视频内嵌噪音与旧 BGM 完全隔离。
        ambiencePath = null,
        durationMs = 20000,
        kenBurnsZoom = 1.18f,
        kenBurnsOffsetY = -0.03f
    ),
    ActConfig(
        actTitle = "第三幕 · 宋室南渡",
        cgPath = "images/events/event_jianyan_south_crossing_01.webp",
        cgFallback = "images/locations/yangzhou_river.webp",
        narratorPath = "audio/voice/prologue/prologue_act3_nandu.m4a",
        subtitle = "康王赵构即位于南京应天府，改元建炎。\n然而江山未稳，金兵已经渡河而来。",
        ambiencePath = "audio/ambience/amb_river.ogg",
        ambienceVolume = 0.10f,
        durationMs = 15000,
        kenBurnsZoom = 1.12f,
        kenBurnsOffsetX = -0.04f
    ),
    ActConfig(
        actTitle = "第四幕 · 旧史已成",
        cgPath = "images/events/event_imperial_war_council_01.webp",
        cgFallback = "images/palace/chuigongdian.webp",
        narratorPath = "audio/voice/prologue/prologue_act4_lishipianzhuan.m4a",
        subtitle = "靖康已成旧史，山河却仍在烽火之中。\n从这一刻起，未来不再只有一条路。",
        ambiencePath = "audio/ambience/amb_palace_murmur.ogg",
        ambienceVolume = 0.08f,
        durationMs = 16000,
        kenBurnsZoom = 1.2f,
        kenBurnsOffsetY = 0.02f
    ),
    ActConfig(
        actTitle = "第五幕 · 我成了赵构",
        cgPath = "images/events/cg_prologue_identity_reflection.webp",
        cgFallback = "images/palace/chuigongdian.webp",
        narratorPath = "audio/voice/prologue/prologue_act5_player_inner.m4a",
        subtitle = "再睁开眼时，我已不在原来的世界。\n铜镜里的那张脸，属于大宋官家——赵构。\n\n旧史已成。余下山河，由我执笔。",
        ambiencePath = "audio/ambience/amb_palace_murmur.ogg",
        ambienceVolume = 0.07f,
        durationMs = 17000,
        kenBurnsZoom = 1.12f,
        kenBurnsOffsetX = 0.02f
    ),
    ActConfig(
        actTitle = "第六幕 · 百官候朝",
        cgPath = "images/palace/chuigongdian.webp",
        cgFallback = "images/events/event_imperial_war_council_01.webp",
        narratorPath = "audio/voice/prologue/prologue_act6_neishi.m4a",
        subtitle = "殿外脚步停住。\n\n“官家，百官已经候在垂拱殿。”",
        ambiencePath = "audio/ambience/amb_palace_murmur.ogg",
        ambienceVolume = 0.07f,
        durationMs = 8000,
        kenBurnsZoom = 1.08f
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

        // 每幕都从干净音场开始，避免主菜单/上一幕残留继续响。
        audioPlayer?.stopVoice()
        audioPlayer?.stopBgm()
        audioPlayer?.stopAmbience()

        actAlpha = 0f
        subtitleAlpha = 0f
        scaleAnim.snapTo(1f)
        offsetXAnim.snapTo(0f)
        offsetYAnim.snapTo(0f)

        config.bgmPath
            ?.takeIf { it in CERTIFIED_PROLOGUE_BGM }
            ?.let { audioPlayer?.playBgm(it, volume = 0.30f) }
        config.ambiencePath?.let {
            audioPlayer?.playAmbience(it, volume = config.ambienceVolume)
        }

        actAlpha = 1f
        delay(850)
        subtitleAlpha = 1f

        if (config.videoPath == null) {
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
        }

        audioPlayer?.playVoice(config.narratorPath, voiceVolume = 1f)

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
            audioPlayer?.stopBgm()
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
            if (config.videoPath != null) {
                PrologueAssetVideo(
                    path = config.videoPath,
                    fallbackPath = config.cgPath,
                    secondFallbackPath = config.cgFallback,
                    contentDescription = config.actTitle,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AssetImage(
                    path = config.cgPath,
                    fallbackPath = config.cgFallback,
                    contentDescription = config.actTitle,
                    contentScale = ContentScale.Crop,
                    placeholderText = config.actTitle.take(2),
                    modifier = Modifier.fillMaxSize()
                )
            }
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
            border = androidx.compose.foundation.BorderStroke(
                0.5.dp,
                Color(0xFFC9A227).copy(alpha = 0.32f)
            )
        ) {
            Text(
                text = config.subtitle,
                color = Color(0xFFE8DCC0),
                fontSize = if (currentAct == PrologueAct.ACT_5 || currentAct == PrologueAct.ACT_6) 16.sp else 15.sp,
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
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                Color(0xFFC9A227).copy(alpha = 0.5f)
            )
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

@Composable
private fun PrologueAssetVideo(
    path: String,
    fallbackPath: String,
    secondFallbackPath: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var failed by remember(path) { mutableStateOf(false) }
    var videoView by remember(path) { mutableStateOf<VideoView?>(null) }

    DisposableEffect(path) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }

    if (failed) {
        AssetImage(
            path = fallbackPath,
            fallbackPath = secondFallbackPath,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            placeholderText = contentDescription.take(2),
            modifier = modifier
        )
    } else {
        AndroidView(
            factory = { context ->
                VideoView(context).also { view ->
                    videoView = view
                    view.setVideoURI(Uri.parse("file:///android_asset/$path"))
                    view.setOnPreparedListener { player ->
                        player.isLooping = false
                        // 生成视频的自带音轨永远不用。序章声音由 GameAudioPlayer 统一管理。
                        player.setVolume(0f, 0f)
                        view.start()
                    }
                    view.setOnErrorListener { _, _, _ ->
                        failed = true
                        true
                    }
                }
            },
            modifier = modifier.background(Color.Black)
        )
    }
}

private fun nextAct(currentAct: PrologueAct): PrologueAct = when (currentAct) {
    PrologueAct.ACT_1 -> PrologueAct.ACT_2
    PrologueAct.ACT_2 -> PrologueAct.ACT_3
    PrologueAct.ACT_3 -> PrologueAct.ACT_4
    PrologueAct.ACT_4 -> PrologueAct.ACT_5
    PrologueAct.ACT_5 -> PrologueAct.ACT_6
    PrologueAct.ACT_6 -> PrologueAct.DONE
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
        PrologueAct.ACT_6 -> Color(0xFFC9A227).copy(alpha = 0.16f)
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

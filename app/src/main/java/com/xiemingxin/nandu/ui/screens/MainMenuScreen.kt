package com.xiemingxin.nandu.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.InkBlack
import com.xiemingxin.nandu.ui.theme.XuanCream

// ── 主菜单局部色 ──────────────────────────────────────
private val MenuBg       = Color(0xFF0C0907)
private val GoldDim      = Color(0xFFAA8820)
private val CreamDim     = Color(0xFFB8A88A)
private val ItemBorder   = Color(0xFF8B6914)
private val ItemPressed  = Color(0xFF2A1E08)

/**
 * V1.0 主菜单。
 *
 * @param onNewGame   开辟新局 → 播序章 IntroScreen
 * @param onContinue  旧梦回溯 → 跳序章直接进游戏
 * @param onSettings  世事设置
 * @param onExit      退出
 */
@Composable
fun MainMenuScreen(
    onNewGame: () -> Unit,
    onContinue: () -> Unit,
    onSettings: () -> Unit,
    onExit: () -> Unit
) {
    // 标题字微浮动动画
    val pulse = rememberInfiniteTransition(label = "title_pulse")
    val titleAlpha by pulse.animateFloat(
        initialValue = 0.90f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = LinearEasing), RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize().background(MenuBg)) {

        // ── 全屏背景图 ──────────────────────────────────
        AssetImage(
            path = ArtResourceRegistry.menuBackground(),
            contentDescription = "南渡无悔主菜单",
            contentScale = ContentScale.Crop,
            placeholderText = "山河",
            modifier = Modifier.fillMaxSize()
        )

        // ── 渐变遮罩：上轻下重，给按钮区留暗底 ──────────
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.00f to MenuBg.copy(alpha = 0.15f),
                    0.38f to MenuBg.copy(alpha = 0.30f),
                    0.60f to MenuBg.copy(alpha = 0.72f),
                    1.00f to MenuBg.copy(alpha = 0.96f)
                )
            )
        )

        // ── 主内容列 ────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.weight(1f))

            // ── 标题区 ──────────────────────────────────
            Text(
                text = "南 渡 无 悔",
                color = ImperialGold.copy(alpha = titleAlpha),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "山河未定，朕当中兴",
                color = CreamDim,
                fontSize = 13.sp,
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            // 装饰分割线
            Box(
                Modifier
                    .fillMaxWidth(0.55f)
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, GoldDim, Color.Transparent)
                        )
                    )
            )

            Spacer(Modifier.height(40.dp))

            // ── 五项菜单 ─────────────────────────────────
            MenuEntry(
                label    = "开 辟 新 局",
                subtitle = "靖康板荡，天命未绝。",
                onClick  = onNewGame
            )
            Spacer(Modifier.height(14.dp))
            MenuEntry(
                label    = "旧 梦 回 溯",
                subtitle = "历史已然走过，记忆尚在。",
                onClick  = onContinue
            )
            Spacer(Modifier.height(14.dp))
            MenuEntry(
                label    = "天 命 绘 卷",
                subtitle = "已见之人，已历之事，皆有迹可寻。",
                onClick  = { /* 图鉴：后续版本实装 */ },
                enabled  = false
            )
            Spacer(Modifier.height(14.dp))
            MenuEntry(
                label    = "世 事 设 置",
                subtitle = "音律、文字、AI接引——皆可调适。",
                onClick  = onSettings
            )
            Spacer(Modifier.height(14.dp))
            MenuEntry(
                label    = "退 出",
                subtitle = "此去关山万里，山河待君归来。",
                onClick  = onExit,
                isPrimary = false
            )

            Spacer(Modifier.height(20.dp))

            // 版本号
            Text(
                text  = "建炎元年  Demo 版",
                color = Color(0xFF5A4A30),
                fontSize = 9.sp,
                letterSpacing = 1.sp
            )
        }
    }
}

// ── 单条菜单项 ───────────────────────────────────────────
@Composable
private fun MenuEntry(
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    isPrimary: Boolean = true,
    enabled: Boolean = true
) {
    val source   = remember { MutableInteractionSource() }
    val pressed  by source.collectIsPressedAsState()

    val bgColor  = when {
        !enabled -> Color(0xFF120E06).copy(alpha = 0.4f)
        pressed  -> ItemPressed
        else     -> Color(0xFF0F0B05).copy(alpha = 0.72f)
    }
    val textColor = when {
        !enabled -> GoldDim.copy(alpha = 0.38f)
        isPrimary -> ImperialGold
        else      -> CreamDim
    }
    val borderColor = when {
        !enabled -> ItemBorder.copy(alpha = 0.25f)
        pressed  -> ImperialGold.copy(alpha = 0.8f)
        else     -> ItemBorder.copy(alpha = 0.6f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable(
                interactionSource = source,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(vertical = 14.dp, horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = label,
                color      = textColor,
                fontSize   = if (isPrimary) 18.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp
            )
            if (subtitle.isNotEmpty()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text      = subtitle,
                    color     = CreamDim.copy(alpha = if (enabled) 0.55f else 0.25f),
                    fontSize  = 10.sp,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

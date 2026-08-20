package com.xiemingxin.nandu.ui.screens

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.theme.ImperialGold

/**
 * 顺昌战役·战前军议 / 战报界面
 * 美术资产实装能力测试场景
 *
 * 素材来源：
 * - 背景: images/palace/shumiyuan.webp (枢密院)
 * - 主将: images/characters/halfbody_liu_qi.webp (刘锜)
 * - 副将: images/characters/halfbody_yue_fei.webp (岳飞)
 * - 副将: images/characters/halfbody_han_shizhong.webp (韩世忠)
 * - 皇帝: images/characters/halfbody_zhao_gou.webp (赵构)
 * - CG: images/events/batch1/event_shunchang_prewar_batch1.webp
 * - 视频: video/VID-CZ-001-PREWAR-V01.mp4
 * - UI纹理: images/ui/edict_scroll.webp, images/ui/dialog_frame.webp
 */
private val SceneInk = Color(0xFF0A0704)
private val SceneGold = Color(0xFFC9A227)
private val SceneCream = Color(0xFFE8DCC0)
private val SceneSub = Color(0xFF9A8862)
private val SceneRed = Color(0xFF8B1A1A)
private val SceneJade = Color(0xFF2D6A4F)
private val ScenePaper = Color(0xFF2A1F12)
private val ScenePaperLight = Color(0xFF3A2D1A)

enum class DecisionChoice { NONE, DEFEND, REINFORCE, DELIBERATE }

@Composable
fun ShunchangBattleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCg by remember { mutableStateOf(false) }
    var showVideo by remember { mutableStateOf(false) }
    var decision by remember { mutableStateOf(DecisionChoice.NONE) }
    var decisionFeedback by remember { mutableStateOf<String?>(null) }
    var moraleBoost by remember { mutableStateOf(0) }
    var grainUsed by remember { mutableStateOf(0) }

    // 进入场景自动播放CG演出
    DisposableEffect(Unit) {
        showCg = true
        onDispose { }
    }

    Box(modifier = modifier.fillMaxSize().background(SceneInk)) {
        // ── 主视觉背景：枢密院 ──
        AssetImage(
            path = "images/palace/shumiyuan.webp",
            fallbackPath = "images/palace/military_camp.webp",
            contentDescription = "枢密院军议",
            contentScale = ContentScale.Crop,
            placeholderText = "枢",
            modifier = Modifier.fillMaxSize()
        )

        // 背景暗化遮罩（上浅下深，保证底部文字可读）
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        SceneInk.copy(alpha = 0.30f),
                        SceneInk.copy(alpha = 0.55f),
                        SceneInk.copy(alpha = 0.92f)
                    )
                )
            )
        )

        // 氛围粒子
        Canvas(modifier = Modifier.fillMaxSize()) { drawWarAtmosphere() }

        // ── 内容层 ──
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部标题栏
            SceneTopBar(onBack = onBack, onPlayVideo = { showVideo = true })

            Spacer(Modifier.height(8.dp))

            // ── 武将角色表现区 ──
            GeneralRow(
                mainGeneral = "liu_qi",
                leftGeneral = "yue_fei",
                rightGeneral = "han_shizhong",
                emperor = "zhao_gou"
            )

            Spacer(Modifier.height(10.dp))

            // ── 战报系统 ──
            BattleReportCard(
                moraleBoost = moraleBoost,
                grainUsed = grainUsed,
                decision = decision
            )

            Spacer(Modifier.height(10.dp))

            // ── 皇帝决策 ──
            DecisionPanel(
                decision = decision,
                onChoose = { choice ->
                    decision = choice
                    when (choice) {
                        DecisionChoice.DEFEND -> {
                            decisionFeedback = "【批红】准奏。着刘锜死守顺昌，城在人在，城亡人亡！赐御酒百坛，激励三军。"
                            moraleBoost = 15
                            grainUsed = 500
                        }
                        DecisionChoice.REINFORCE -> {
                            decisionFeedback = "【批红】着岳飞部即日驰援顺昌，韩世忠部扼守江淮要道，互为犄角。"
                            moraleBoost = 10
                            grainUsed = 2000
                        }
                        DecisionChoice.DELIBERATE -> {
                            decisionFeedback = "【批红】此事重大，着枢密院再议。赵鼎、秦桧速来见朕，详陈攻守之策。"
                            moraleBoost = -5
                            grainUsed = 0
                        }
                        DecisionChoice.NONE -> { decisionFeedback = null }
                    }
                }
            )

            // 决策反馈
            decisionFeedback?.let { feedback ->
                Spacer(Modifier.height(10.dp))
                DecisionFeedbackCard(text = feedback, decision = decision)
            }

            Spacer(Modifier.height(16.dp))
        }

        // ── CG 演出对话框 ──
        if (showCg) {
            ShunchangCgDialog(onDismiss = { showCg = false })
        }

        // ── 视频播放对话框 ──
        if (showVideo) {
            ShunchangVideoDialog(onDismiss = { showVideo = false })
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 顶部标题栏
// ═══════════════════════════════════════════════════════════
@Composable
private fun SceneTopBar(onBack: () -> Unit, onPlayVideo: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier
                .size(38.dp)
                .clickable { onBack() },
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = ScenePaper.copy(alpha = 0.85f)),
            border = BorderStroke(1.dp, SceneGold.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("←", color = SceneGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "顺 昌 战 役",
                color = SceneGold,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )
            Text(
                "建炎四年 · 战前军议",
                color = SceneSub,
                fontSize = 10.sp,
                letterSpacing = 2.sp
            )
        }

        Card(
            modifier = Modifier
                .size(38.dp)
                .clickable { onPlayVideo() },
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = SceneRed.copy(alpha = 0.75f)),
            border = BorderStroke(1.dp, SceneGold.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("▶", color = SceneCream, fontSize = 12.sp)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 武将角色表现区
// ═══════════════════════════════════════════════════════════
@Composable
private fun GeneralRow(
    mainGeneral: String,
    leftGeneral: String,
    rightGeneral: String,
    emperor: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
    ) {
        // 皇帝（上方居中，小尺寸，半透明，象征天子临朝）
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-4).dp)
        ) {
            EmperorBadge(emperor = emperor)
        }

        // 左侧副将（岳飞，偏后，略小，偏左）
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 8.dp, y = 10.dp)
                .alpha(0.82f)
        ) {
            GeneralCard(
                officerId = leftGeneral,
                name = "岳飞",
                title = "清远军节度使",
                size = 130.dp,
                alignment = Alignment.BottomStart
            )
        }

        // 右侧副将（韩世忠，偏后，略小，偏右）
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = 10.dp)
                .alpha(0.82f)
        ) {
            GeneralCard(
                officerId = rightGeneral,
                name = "韩世忠",
                title = "武胜军节度使",
                size = 130.dp,
                alignment = Alignment.BottomEnd
            )
        }

        // 中央主将（刘锜，最大，最前，视觉重点）
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 4.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp))
        ) {
            GeneralCard(
                officerId = mainGeneral,
                name = "刘锜",
                title = "顺昌守将 · 主将",
                size = 165.dp,
                alignment = Alignment.BottomCenter,
                isMain = true
            )
        }
    }
}

@Composable
private fun EmperorBadge(emperor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        AssetImage(
            path = "images/characters/halfbody_$emperor.webp",
            fallbackPath = "images/characters/portrait_$emperor.webp",
            contentDescription = "皇帝",
            contentScale = ContentScale.Crop,
            placeholderText = "帝",
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(ScenePaper)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            "天子 · 赵构",
            color = SceneGold,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun GeneralCard(
    officerId: String,
    name: String,
    title: String,
    size: androidx.compose.ui.unit.Dp,
    alignment: Alignment,
    isMain: Boolean = false
) {
    val borderColor = if (isMain) SceneGold else SceneGold.copy(alpha = 0.5f)
    val borderWidth = if (isMain) 2.dp else 1.dp

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Card(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = ScenePaper),
            border = BorderStroke(borderWidth, borderColor)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AssetImage(
                    path = "images/characters/halfbody_$officerId.webp",
                    fallbackPath = "images/characters/portrait_$officerId.webp",
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    placeholderText = name.take(1),
                    modifier = Modifier.fillMaxSize()
                )
                // 底部渐变遮罩，保证名字可读
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Transparent,
                                    SceneInk.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(
            name,
            color = if (isMain) SceneGold else SceneCream,
            fontSize = if (isMain) 13.sp else 11.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            title,
            color = SceneSub,
            fontSize = 8.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ═══════════════════════════════════════════════════════════
// 战报系统
// ═══════════════════════════════════════════════════════════
@Composable
private fun BattleReportCard(
    moraleBoost: Int,
    grainUsed: Int,
    decision: DecisionChoice
) {
    val baseMorale = 72
    val baseGrain = 18000
    val currentMorale = (baseMorale + moraleBoost).coerceIn(0, 100)
    val currentGrain = baseGrain - grainUsed

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, SceneGold.copy(alpha = 0.35f))
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // 卷轴背景纹理
            AssetImage(
                path = "images/ui/edict_scroll.webp",
                contentDescription = "战报卷轴",
                contentScale = ContentScale.Crop,
                placeholderText = "卷",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .alpha(0.18f)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {
                // 战报标题
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AssetImage(
                            path = "images/ui/war_badge.webp",
                            contentDescription = "军",
                            contentScale = ContentScale.Fit,
                            placeholderText = "军",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "前 线 军 报",
                            color = SceneGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp
                        )
                    }
                    Text(
                        "顺昌府 · 急报",
                        color = SceneRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 数据网格
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportStat(
                        label = "守军",
                        value = "18,000",
                        sub = "八字军 + 殿前司",
                        color = SceneCream
                    )
                    ReportStat(
                        label = "粮草",
                        value = "${currentGrain / 1000},${String.format("%03d", currentGrain % 1000)}",
                        sub = "可支 ${currentGrain / 3000} 月",
                        color = if (currentGrain < 10000) SceneRed else SceneJade
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportStat(
                        label = "城防",
                        value = "坚",
                        sub = "城墙完好 · 瓮城齐备",
                        color = SceneJade
                    )
                    ReportStat(
                        label = "士气",
                        value = "$currentMorale%",
                        sub = if (moraleBoost > 0) "↑ 御旨激励" else if (moraleBoost < 0) "↓ 迁延观望" else "军心尚稳",
                        color = when {
                            currentMorale >= 70 -> SceneJade
                            currentMorale >= 50 -> SceneGold
                            else -> SceneRed
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ReportStat(
                        label = "敌军",
                        value = "约 100,000",
                        sub = "完颜宗弼 · 铁浮图",
                        color = SceneRed
                    )
                    ReportStat(
                        label = "危局",
                        value = "极危",
                        sub = "众寡悬殊 · 孤城无援",
                        color = SceneRed
                    )
                }

                Spacer(Modifier.height(10.dp))

                // 战报正文
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = ScenePaperLight.copy(alpha = 0.6f))
                ) {
                    Text(
                        text = "  臣刘锜顿首：金兵号十万，已至顺昌城下。臣已凿船沉舟，示无退意。城中军民同仇敌忾，愿以死报陛下。惟粮草尚足，城防尚坚，然众寡悬殊，恳望朝廷早定大计，或坚守待变，或遣师驰援。臣不胜惶恐，待命而已。",
                        color = SceneCream,
                        fontSize = 10.sp,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportStat(
    label: String,
    value: String,
    sub: String,
    color: Color
) {
    Card(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = ScenePaper.copy(alpha = 0.55f)),
        border = BorderStroke(0.5.dp, SceneGold.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(label, color = SceneSub, fontSize = 9.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(2.dp))
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(1.dp))
            Text(sub, color = SceneSub, fontSize = 8.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 皇帝决策面板
// ═══════════════════════════════════════════════════════════
@Composable
private fun DecisionPanel(
    decision: DecisionChoice,
    onChoose: (DecisionChoice) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "天 子 决 断",
            color = SceneGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        DecisionButton(
            title = "下旨死守顺昌",
            desc = "着刘锜城在人在，赐御酒激励三军",
            icon = "🛡",
            isSelected = decision == DecisionChoice.DEFEND,
            onClick = { onChoose(DecisionChoice.DEFEND) }
        )
        Spacer(Modifier.height(6.dp))
        DecisionButton(
            title = "调岳飞部驰援",
            desc = "岳飞即日东进，韩世忠扼守江淮",
            icon = "⚔",
            isSelected = decision == DecisionChoice.REINFORCE,
            onClick = { onChoose(DecisionChoice.REINFORCE) }
        )
        Spacer(Modifier.height(6.dp))
        DecisionButton(
            title = "命枢密院再议",
            desc = "召赵鼎、秦桧详陈攻守之策，从长计议",
            icon = "📜",
            isSelected = decision == DecisionChoice.DELIBERATE,
            onClick = { onChoose(DecisionChoice.DELIBERATE) }
        )
    }
}

@Composable
private fun DecisionButton(
    title: String,
    desc: String,
    icon: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) SceneRed.copy(alpha = 0.35f) else ScenePaper.copy(alpha = 0.5f)
    val borderColor = if (isSelected) SceneGold else SceneGold.copy(alpha = 0.3f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                icon,
                fontSize = 18.sp,
                modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    color = if (isSelected) SceneGold else SceneCream,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    desc,
                    color = SceneSub,
                    fontSize = 9.sp,
                    lineHeight = 13.sp
                )
            }
            if (isSelected) {
                Text("✓", color = SceneGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 决策反馈卡片（批红/圣旨）
// ═══════════════════════════════════════════════════════════
@Composable
private fun DecisionFeedbackCard(text: String, decision: DecisionChoice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = ScenePaperLight.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, SceneRed.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 印章效果
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(SceneRed.copy(alpha = 0.8f), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("敕", color = SceneCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "圣 旨 已 下",
                    color = SceneRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text,
                color = SceneCream,
                fontSize = 11.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when (decision) {
                    DecisionChoice.DEFEND -> "—— 军报已更新：士气 ↑ 粮草 ↓"
                    DecisionChoice.REINFORCE -> "—— 军报已更新：士气 ↑ 粮草大耗"
                    DecisionChoice.DELIBERATE -> "—— 军报已更新：士气 ↓ 军心浮动"
                    DecisionChoice.NONE -> ""
                },
                color = SceneSub,
                fontSize = 9.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════
// CG 演出对话框
// ═══════════════════════════════════════════════════════════
@Composable
private fun ShunchangCgDialog(onDismiss: () -> Unit) {
    var alpha by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {
        // 模拟淡入
        alpha = 1f
        onDispose { }
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(SceneInk)
        ) {
            // CG 图片
            AssetImage(
                path = "images/events/batch1/event_shunchang_prewar_batch1.webp",
                fallbackPath = "images/battles/battle_siege.webp",
                contentDescription = "顺昌战前",
                contentScale = ContentScale.Crop,
                placeholderText = "CG",
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha)
            )

            // 暗角遮罩
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.Transparent, SceneInk.copy(alpha = 0.7f)),
                            radius = 600f
                        )
                    )
            )

            // 底部字幕
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, SceneInk.copy(alpha = 0.9f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column {
                    Text(
                        "建炎四年 · 顺昌",
                        color = SceneGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "完颜宗弼率十万铁骑南下，顺昌孤城告急。刘锜凿船沉舟，示无退意……",
                        color = SceneCream,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }

            // 点击继续提示
            Text(
                "点击任意处继续",
                color = SceneSub.copy(alpha = 0.7f),
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )
        }
    }

    // 点击对话框任意处关闭（通过透明覆盖层）
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onDismiss() }
    )
}

// ═══════════════════════════════════════════════════════════
// 视频播放对话框（带静态图片 fallback）
// ═══════════════════════════════════════════════════════════
@Composable
private fun ShunchangVideoDialog(onDismiss: () -> Unit) {
    var videoError by remember { mutableStateOf(false) }
    var videoView by remember { mutableStateOf<android.widget.VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = SceneInk),
            border = BorderStroke(1.dp, SceneGold.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    "战前过场 · 视频",
                    color = SceneGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    if (!videoError) {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).also { view ->
                                    videoView = view
                                    try {
                                        view.setVideoURI(
                                            Uri.parse("file:///android_asset/video/VID-CZ-001-PREWAR-V01.mp4")
                                        )
                                        view.setOnPreparedListener { player ->
                                            player.isLooping = false
                                            view.start()
                                        }
                                        view.setOnErrorListener { _, _, _ ->
                                            videoError = true
                                            true
                                        }
                                    } catch (e: Exception) {
                                        videoError = true
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Fallback：视频加载失败时显示静态 CG
                    if (videoError) {
                        AssetImage(
                            path = "images/events/batch1/event_shunchang_prewar_batch1.webp",
                            fallbackPath = "images/battles/battle_siege.webp",
                            contentDescription = "战前静态画面",
                            contentScale = ContentScale.Crop,
                            placeholderText = "视频",
                            modifier = Modifier.fillMaxSize()
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(SceneInk.copy(alpha = 0.5f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("⚠", color = SceneGold, fontSize = 24.sp)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "视频编码不兼容，已切换静态画面",
                                    color = SceneCream,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Card(
                        modifier = Modifier
                            .clickable { onDismiss() }
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(6.dp),
                        colors = CardDefaults.cardColors(containerColor = SceneGold)
                    ) {
                        Text("返回", color = SceneInk, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════
// 氛围绘制
// ═══════════════════════════════════════════════════════════
private fun DrawScope.drawWarAtmosphere() {
    // 顶部光晕
    drawCircle(
        color = SceneGold.copy(alpha = 0.04f),
        radius = size.minDimension * 0.6f,
        center = Offset(size.width * 0.5f, size.height * 0.15f)
    )
    // 飘散的粒子（模拟尘埃/火星）
    val particles = listOf(
        Offset(size.width * 0.2f, size.height * 0.3f),
        Offset(size.width * 0.7f, size.height * 0.25f),
        Offset(size.width * 0.5f, size.height * 0.4f),
        Offset(size.width * 0.85f, size.height * 0.35f),
        Offset(size.width * 0.15f, size.height * 0.45f)
    )
    particles.forEach { pos ->
        drawCircle(
            color = SceneGold.copy(alpha = 0.12f),
            radius = 2f,
            center = pos
        )
    }
}

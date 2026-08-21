package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xiemingxin.nandu.game.BattleSceneParticipant
import com.xiemingxin.nandu.game.BattleScenePresentation
import com.xiemingxin.nandu.game.BattleScenePresentationSystem
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.ui.components.AssetImage

private val BriefInk = Color(0xFF0A0704)
private val BriefGold = Color(0xFFC9A227)
private val BriefCream = Color(0xFFE8DCC0)
private val BriefSub = Color(0xFF9A8862)
private val BriefRed = Color(0xFF8B1A1A)
private val BriefGreen = Color(0xFF4F8D66)

/**
 * STAB-002 动态战役军情页。
 *
 * 所有日期、人物、当前职务、位置、兵力都来自 GameState；
 * 不再把后世官职或固定历史名将硬塞进当前年份。
 * STAB-003 完成前本页只展示真实军情，不伪装“点一下就已改变世界”的局部决策。
 */
@Composable
fun DynamicShunchangBriefingScreen(
    state: GameState,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val briefing = BattleScenePresentationSystem.shunchang(state)

    Box(modifier = modifier.fillMaxSize().background(BriefInk)) {
        AssetImage(
            path = "images/palace/shumiyuan.webp",
            fallbackPath = "images/palace/military_camp.webp",
            contentDescription = "枢密院军情",
            contentScale = ContentScale.Crop,
            placeholderText = "枢",
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        BriefInk.copy(alpha = 0.38f),
                        BriefInk.copy(alpha = 0.68f),
                        BriefInk.copy(alpha = 0.96f)
                    )
                )
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Header(briefing, onBack)
            ParticipantSection(briefing)
            SituationSection(briefing)
            ReportSection(briefing)
            PendingCommandNotice()
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun Header(briefing: BattleScenePresentation, onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xC5100A04)),
        border = BorderStroke(1.dp, BriefGold.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onBack, border = BorderStroke(1.dp, BriefGold.copy(alpha = 0.6f))) {
                Text("← 返回", color = BriefGold, fontSize = 11.sp)
            }
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    briefing.battleName,
                    color = BriefGold,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp
                )
                Text(
                    "${briefing.dateText} · ${briefing.theaterText}",
                    color = BriefSub,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
            Text("军情", color = BriefRed, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ParticipantSection(briefing: BattleScenePresentation) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("当前战区将领", color = BriefGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        if (briefing.mainParticipant == null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xC51A1208)),
                border = BorderStroke(1.dp, BriefRed.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "当前战区没有可核实的在任主帅。不会再用岳飞、刘锜、韩世忠等历史名将作为固定占位。",
                    color = BriefCream,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(14.dp)
                )
            }
            return
        }

        ParticipantCard(briefing.mainParticipant, isMain = true)
        if (briefing.supportingParticipants.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                briefing.supportingParticipants.forEach { participant ->
                    Box(modifier = Modifier.weight(1f)) {
                        ParticipantCard(participant, isMain = false)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParticipantCard(participant: BattleSceneParticipant, isMain: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xD3181008)),
        border = BorderStroke(if (isMain) 1.5.dp else 1.dp, BriefGold.copy(alpha = if (isMain) 0.8f else 0.4f)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssetImage(
                path = ArtResourceRegistry.halfbodyForOfficer(participant.officerId),
                fallbackPath = ArtResourceRegistry.portraitForOfficer(participant.officerId),
                contentDescription = participant.name,
                contentScale = ContentScale.Crop,
                placeholderText = participant.name.take(1),
                modifier = Modifier.size(if (isMain) 86.dp else 64.dp).clip(RoundedCornerShape(11.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(participant.name, color = BriefGold, fontSize = if (isMain) 16.sp else 13.sp, fontWeight = FontWeight.Bold)
                Text(participant.displayTitle.ifBlank { "当前无明确职务记录" }, color = BriefCream, fontSize = 10.sp, lineHeight = 14.sp)
                Text("所在：${participant.locationText}", color = BriefSub, fontSize = 9.sp)
                participant.armyName?.let { Text("军团：$it", color = BriefSub, fontSize = 9.sp) }
            }
        }
    }
}

@Composable
private fun SituationSection(briefing: BattleScenePresentation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xC5120D08)),
        border = BorderStroke(1.dp, BriefGold.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("战区实况", color = BriefGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("宋军", formatNumber(briefing.friendlyTroops), BriefCream, Modifier.weight(1f))
                Stat("军粮", formatNumber(briefing.friendlyGrain), BriefGreen, Modifier.weight(1f))
                Stat("士气", "${briefing.friendlyMorale}%", BriefGold, Modifier.weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat("金军", if (briefing.enemyTroops > 0) formatNumber(briefing.enemyTroops) else "未核实", BriefRed, Modifier.weight(1f))
                Stat("态势", briefing.situationText, if (briefing.enemyTroops > briefing.friendlyTroops) BriefRed else BriefGold, Modifier.weight(2f))
            }
            Text("敌情：${briefing.enemySummary}", color = BriefSub, fontSize = 10.sp, lineHeight = 15.sp)
        }
    }
}

@Composable
private fun Stat(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0x801A1208)),
        shape = RoundedCornerShape(9.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = BriefSub, fontSize = 9.sp)
            Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun ReportSection(briefing: BattleScenePresentation) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(13.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD21B1208)),
        border = BorderStroke(1.dp, BriefGold.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("枢密院核实军报", color = BriefGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(briefing.reportText, color = BriefCream, fontSize = 11.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun PendingCommandNotice() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xB8140E08)),
        border = BorderStroke(1.dp, BriefSub.copy(alpha = 0.35f))
    ) {
        Text(
            "军令按钮暂不在此页伪造即时结果。下一任务 STAB-003 会把‘固守 / 驰援 / 再议’接入正式 GameState、军团路线、粮草和战报后再开放。",
            color = BriefSub,
            fontSize = 10.sp,
            lineHeight = 16.sp,
            modifier = Modifier.padding(12.dp)
        )
    }
}

private fun formatNumber(value: Int): String = String.format("%,d", value.coerceAtLeast(0))

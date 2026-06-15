package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.PalaceIds
import com.xiemingxin.nandu.game.PalaceRegistry
import com.xiemingxin.nandu.game.PalaceTaskSystem
import com.xiemingxin.nandu.ui.components.AssetImage

private val HallGold = Color(0xFFC9A227)
private val HallCream = Color(0xFFE8DCC0)
private val HallInk = Color(0xFF0E0A05)
private val HallSub = Color(0xFF9A8862)
private val HallRed = Color(0xFF7D1D16)
private val HallBlue = Color(0xFF4DA3E6)

/**
 * V2.2 皇宫大厅：接入完整资源包的宫殿背景与人物卡。
 */
@Composable
fun PalaceHallScreen(
    state: GameState,
    aiStatus: String,
    isRealAiEnabled: Boolean,
    onOpenSettings: () -> Unit,
    onOpenPalace: (String) -> Unit,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val songCities = state.cities.count { it.owner == "song" }
    val jinCities = state.cities.count { it.owner == "jin" }
    val taskCounts = PalaceTaskSystem.countByPalace(state)
    val totalTasks = taskCounts.values.sum()
    val activeRumors = state.rumors.size
    val hiddenTalent = state.officers.count { it.status.name == "HIDDEN" || it.status.name == "WANDERING" }
    val hallWarning = when {
        totalTasks >= 5 -> "诸殿奏事纷至，宜先断火急之务。"
        state.jinThreat >= 85 -> "金军压境，江淮风急。今日宜整军、修城、筹粮。"
        state.grain < 120000 -> "府库粮储吃紧，若欲北伐，须先安民屯田。"
        state.courtStability < 45 -> "朝局摇动，主战主和争执不休，需速定国策。"
        else -> "行在暂安，山河未定。可巡视疆土，择机经营城池。"
    }

    Box(modifier = modifier.fillMaxSize().background(HallInk)) {
        AssetImage(
            path = "images/palace/chuigongdian.webp",
            fallbackPath = "images/palace/linan_street.webp",
            contentDescription = "临安行在",
            contentScale = ContentScale.Crop,
            placeholderText = "宫",
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        HallInk.copy(alpha = 0.35f),
                        HallInk.copy(alpha = 0.24f),
                        HallInk.copy(alpha = 0.94f)
                    )
                )
            )
        )
        Canvas(modifier = Modifier.fillMaxSize()) { drawPalaceAtmosphere() }

        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PalaceTopBar(state, songCities, jinCities, aiStatus, isRealAiEnabled, onOpenSettings)
            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x9A100A04)),
                border = BorderStroke(1.dp, HallGold.copy(alpha = 0.46f))
            ) {
                Column(modifier = Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("临 安 行 在", color = HallGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("建炎天子 · 南渡朝廷 · V2.2 美术资源接线", color = HallCream, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    AssetImage(
                        path = ArtResourceRegistry.halfbodyForOfficer("zhao_gou"),
                        fallbackPath = ArtResourceRegistry.portraitForOfficer("zhao_ding"),
                        contentDescription = "赵构",
                        contentScale = ContentScale.Crop,
                        placeholderText = "构",
                        modifier = Modifier.size(150.dp).clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("大宋皇帝 · 赵构", color = HallGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("诸殿待办 $totalTasks 件。今日从哪一殿开局？", color = HallSub, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            TodayDecreeCard(hallWarning)
            Spacer(Modifier.height(12.dp))
            StateSummaryGrid(state, songCities)
            Spacer(Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("宫 殿 待 办", color = HallCream, fontSize = 13.sp, letterSpacing = 4.sp)
                Text("本旬 $totalTasks 件", color = HallGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(9.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PalaceWing(PalaceIds.CHUIGONG, "写旨、群臣争论、准奏驳回", taskBadge(taskCounts, PalaceIds.CHUIGONG), Modifier.weight(1f), onOpenPalace)
                PalaceWing(PalaceIds.WENDE, "人才、官阶、在野线索", taskBadge(taskCounts, PalaceIds.WENDE, "线索 $hiddenTalent"), Modifier.weight(1f), onOpenPalace)
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PalaceWing(PalaceIds.SHUMI, "军团、将领、攻守筹划", taskBadge(taskCounts, PalaceIds.SHUMI, "金威 ${state.jinThreat}"), Modifier.weight(1f), onOpenPalace)
                PalaceWing(PalaceIds.ZHENGSHI, "府库、粮草、民生政务", taskBadge(taskCounts, PalaceIds.ZHENGSHI, "粮 ${state.grain / 1000}k"), Modifier.weight(1f), onOpenPalace)
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PalaceWing(PalaceIds.YUSHU, "传闻、记录、风险复盘", taskBadge(taskCounts, PalaceIds.YUSHU, "密折 $activeRumors"), Modifier.weight(1f), onOpenPalace)
                PalaceWing(PalaceIds.HUANGCHENG, "情报验证、朝臣动向", taskBadge(taskCounts, PalaceIds.HUANGCHENG, "巡江淮"), Modifier.weight(1f), onOpenPalace)
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PalaceWing(PalaceIds.HOUYUAN, "太后、皇后、宦官密奏", taskBadge(taskCounts, PalaceIds.HOUYUAN, "内廷"), Modifier.weight(1f), onOpenPalace)
                PalaceWing(PalaceIds.TAIMIAO, "祭祀、威望、功业传承", taskBadge(taskCounts, PalaceIds.TAIMIAO, "名望 ${state.prestige}"), Modifier.weight(1f), onOpenPalace)
            }
            Spacer(Modifier.height(12.dp))

            Text(
                "山河 / 国政 / 军务旧入口仍保留；宫殿卡优先进入专属待办页。",
                color = HallSub,
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clickable { onNavigate(0) }
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

private fun taskBadge(counts: Map<String, Int>, palaceId: String, fallback: String = "待议"): String {
    val count = counts[palaceId] ?: 0
    return if (count > 0) "待办 $count" else fallback
}

@Composable
private fun PalaceTopBar(
    state: GameState,
    songCities: Int,
    jinCities: Int,
    aiStatus: String,
    isRealAiEnabled: Boolean,
    onOpenSettings: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xD9090806)),
        border = BorderStroke(1.dp, HallGold.copy(alpha = 0.32f)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(11.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(state.calendar.displayText(), color = HallGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("${state.season.label} · ${state.weather.label} · 宋城 $songCities / 金城 $jinCities", color = HallSub, fontSize = 10.sp)
            }
            Card(
                modifier = Modifier.clickable { onOpenSettings() },
                colors = CardDefaults.cardColors(containerColor = if (isRealAiEnabled) Color(0xAA193A22) else Color(0xAA2A1A12)),
                border = BorderStroke(1.dp, if (isRealAiEnabled) Color(0xFF7FCF8A) else HallRed.copy(alpha = 0.65f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalAlignment = Alignment.End) {
                    Text(if (isRealAiEnabled) "AI圣旨已启用" else "离线推演", color = if (isRealAiEnabled) Color(0xFFB9F0BD) else Color(0xFFFFB08A), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(aiStatus.take(18), color = HallSub, fontSize = 8.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TodayDecreeCard(text: String) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xC51E1508)), border = BorderStroke(1.dp, HallRed.copy(alpha = 0.55f)), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text("【 今 日 圣 断 】", color = HallGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(text, color = HallCream, fontSize = 12.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun StateSummaryGrid(state: GameState, songCities: Int) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xB70E0A05)), border = BorderStroke(1.dp, HallGold.copy(alpha = 0.28f)), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StateChip("库银", "${state.gold / 1000}k")
                StateChip("存粮", "${state.grain / 1000}k")
                StateChip("军心", "${state.troopMorale}")
                StateChip("朝局", "${state.courtStability}")
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StateChip("金威", "${state.jinThreat}")
                StateChip("主战", "${state.warFactionPower}")
                StateChip("主和", "${state.peaceFactionPower}")
                StateChip("宋城", "$songCities")
            }
        }
    }
}

@Composable
private fun StateChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = HallGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = HallSub, fontSize = 9.sp)
    }
}

@Composable
private fun PalaceWing(palaceId: String, subtitle: String, badge: String, modifier: Modifier = Modifier, onOpenPalace: (String) -> Unit) {
    val palace = PalaceRegistry.byId(palaceId)
    val path = ArtResourceRegistry.palaceBackground(palaceId)
    Card(
        modifier = modifier.height(116.dp).clickable { onOpenPalace(palaceId) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xBD140E08)),
        border = BorderStroke(1.dp, HallGold.copy(alpha = 0.42f))
    ) {
        Box(Modifier.fillMaxSize()) {
            AssetImage(path = path, contentDescription = palace.name, contentScale = ContentScale.Crop, placeholderText = palace.name.take(1), modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Color(0x9A000000)))
            Column(modifier = Modifier.fillMaxSize().padding(11.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(palace.name, color = HallGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(subtitle, color = HallCream, fontSize = 9.sp, lineHeight = 13.sp)
                    }
                    Text(palace.icon, fontSize = 22.sp)
                }
                Text(badge, color = if (badge.startsWith("待办")) Color(0xFFFFD36A) else HallSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun DrawScope.drawPalaceAtmosphere() {
    val gold = HallGold.copy(alpha = 0.13f)
    repeat(9) { i ->
        val x = size.width * (i + 1) / 10f
        drawLine(gold, Offset(x, 0f), Offset(x - 90f, size.height), strokeWidth = 1.1f)
    }
    drawCircle(HallGold.copy(alpha = 0.09f), radius = size.minDimension * 0.62f, center = Offset(size.width * 0.74f, size.height * 0.18f))
}

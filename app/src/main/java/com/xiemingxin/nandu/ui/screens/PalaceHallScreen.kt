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
 * V1.5 皇宫大厅：八宫殿入口 + 每旬待办数量。
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
                        HallInk.copy(alpha = 0.45f),
                        HallInk.copy(alpha = 0.28f),
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
                    Text("建炎天子 · 南渡朝廷 · V1.5 待办骨架", color = HallCream, fontSize = 11.sp)
                    Spacer(Modifier.height(10.dp))
                    AssetImage(
                        path = "images/characters/halfbody_zhao_gou.webp",
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xC51E1508)),
        border = BorderStroke(1.dp, HallRed.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text("【 今 日 圣 断 】", color = HallGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(text, color = HallCream, fontSize = 12.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun StateSummaryGrid(state: GameState, songCities: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xB70E0A05)),
        border = BorderStroke(1.dp, HallGold.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StateChip("库银", "${state.gold / 1000}k")
                StateChip("存粮", "${state.grain / 1000}k")
                StateChip("军心", "${state.troopMorale}")
                StateChip("控城", "$songCities", Color(0xFF8FB573))
            }
            Spacer(Modifier.height(9.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StateChip("朝局", "${state.courtStability}")
                StateChip("金威", "${state.jinThreat}", Color(0xFFCC6655))
                StateChip("主战", "${state.warFactionPower}", Color(0xFFD4A437))
                StateChip("主和", "${state.peaceFactionPower}", Color(0xFF8A9BB5))
            }
        }
    }
}

@Composable
private fun StateChip(label: String, value: String, valueColor: Color = HallGold) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = HallSub, fontSize = 9.sp)
    }
}

@Composable
private fun PalaceWing(palaceId: String, desc: String, badge: String, modifier: Modifier, onOpenPalace: (String) -> Unit) {
    val palace = PalaceRegistry.byId(palaceId)
    val hasTask = badge.startsWith("待办")
    Card(
        modifier = modifier.height(112.dp).clickable { onOpenPalace(palaceId) },
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD31E1508)),
        border = BorderStroke(1.dp, (if (hasTask) HallRed else HallGold).copy(alpha = 0.58f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(9.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(palace.icon, fontSize = 24.sp)
            Spacer(Modifier.height(3.dp))
            Text(palace.name, color = HallGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(palace.subtitle.substringBefore(" / "), color = HallCream, fontSize = 10.sp)
            Spacer(Modifier.height(3.dp))
            Text(desc, color = HallSub, fontSize = 9.sp, textAlign = TextAlign.Center, lineHeight = 12.sp)
            Spacer(Modifier.height(3.dp))
            Text(badge, color = if (hasTask) Color(0xFFFFB08A) else HallBlue, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private fun DrawScope.drawPalaceAtmosphere() {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    drawCircle(HallGold.copy(alpha = 0.08f), w * 0.38f, Offset(w * 0.52f, h * 0.24f))
    drawCircle(HallRed.copy(alpha = 0.06f), w * 0.44f, Offset(w * 0.14f, h * 0.56f))
    drawCircle(HallBlue.copy(alpha = 0.04f), w * 0.50f, Offset(w * 0.86f, h * 0.58f))
}

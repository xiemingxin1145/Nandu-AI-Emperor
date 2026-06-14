package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.InkBlack
import com.xiemingxin.nandu.ui.theme.JinRed
import com.xiemingxin.nandu.ui.theme.SongBright
import com.xiemingxin.nandu.ui.theme.XuanCream

@Composable
fun StateScreen(gameState: GameState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(InkBlack).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { HeaderCard(gameState) }
        item { ResourceGrid(gameState) }
        item { CourtPowerCard(gameState) }
        item { FactionCard(gameState) }
        item { TalentHintCard(gameState) }
        item { CitySummaryCard(gameState) }
        item { OfficerSummaryCard(gameState) }
        item { ChroniclePreviewCard(gameState) }
    }
}

@Composable
private fun HeaderCard(state: GameState) {
    PanelCard {
        Text("国政总览 V0.5.6", color = ImperialGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("${state.calendar.displayText()}  ${state.season.label}  天气${state.weather.label}", color = XuanCream, fontSize = 13.sp)
        Text(state.weather.effectText, color = Color(0xFFB9AA82), fontSize = 11.sp)
    }
}

@Composable
private fun ResourceGrid(state: GameState) {
    PanelCard {
        SectionTitle("天下底盘")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigStat("国库", "${state.gold / 1000}k", "贯", Modifier.weight(1f))
            BigStat("粮草", "${state.grain / 1000}k", "石", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigStat("军心", state.troopMorale.toString(), "/100", Modifier.weight(1f))
            BigStat("金国威胁", state.jinThreat.toString(), "/100", Modifier.weight(1f), if (state.jinThreat >= 80) JinRed else ImperialGold)
        }
    }
}

@Composable
private fun CourtPowerCard(state: GameState) {
    PanelCard {
        SectionTitle("朝堂形势")
        GaugeLine("朝堂稳定", state.courtStability, ImperialGold)
        Spacer(Modifier.height(8.dp))
        GaugeLine("主战派", state.warFactionPower, SongBright)
        Spacer(Modifier.height(8.dp))
        GaugeLine("主和派", state.peaceFactionPower, JinRed)
    }
}

@Composable
private fun FactionCard(state: GameState) {
    val songCities = state.cities.count { it.owner == "song" }
    val jinCities = state.cities.count { it.owner == "jin" }
    val songTroops = state.cities.filter { it.owner == "song" }.sumOf { it.troops }
    val jinTroops = state.cities.filter { it.owner == "jin" }.sumOf { it.troops }
    PanelCard {
        SectionTitle("势力归属")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FactionMini("大宋", songCities, songTroops, SongBright, Modifier.weight(1f))
            FactionMini("金国", jinCities, jinTroops, JinRed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun TalentHintCard(state: GameState) {
    val hidden = state.officers.count { it.status == OfficerStatus.HIDDEN || it.status == OfficerStatus.SOLDIER || it.status == OfficerStatus.WANDERING }
    val registered = state.officers.count { it.status == OfficerStatus.IN_COURT || it.status == OfficerStatus.DEPLOYED }
    PanelCard {
        SectionTitle("人才名册")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BigStat("已登记", registered.toString(), "人", Modifier.weight(1f), SongBright)
            BigStat("未发现", hidden.toString(), "线索", Modifier.weight(1f), ImperialGold)
        }
        Spacer(Modifier.height(6.dp))
        Text("未来名将不会开局全亮。可下旨：寻访岳飞、访求襄阳人才、搜索建康军中勇士。", color = Color(0xFFB9AA82), fontSize = 11.sp)
    }
}

@Composable
private fun CitySummaryCard(state: GameState) {
    val frontline = state.cities.filter { it.controlState == "FRONTLINE" || it.controlState == "CONTESTED" }
    PanelCard {
        SectionTitle("前线城池")
        if (frontline.isEmpty()) {
            Text("当前无前线告急。", color = XuanCream, fontSize = 12.sp)
        } else {
            frontline.forEach { city ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(city.name, color = XuanCream, fontSize = 12.sp)
                    Text("兵${city.troops / 1000}k 防${city.defense} 民${city.popularSupport}", color = Color(0xFFB9AA82), fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun OfficerSummaryCard(state: GameState) {
    val active = state.officers.filter { it.status == OfficerStatus.IN_COURT || it.status == OfficerStatus.DEPLOYED }
    PanelCard {
        SectionTitle("朝廷已登记人才")
        if (active.isEmpty()) {
            Text("尚无可用人才。请下旨寻访。", color = Color(0xFF8B7355), fontSize = 12.sp)
        } else {
            active.take(8).forEach { officer ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${officer.name} · ${officer.faction}", color = XuanCream, fontSize = 12.sp)
                    Text("统${officer.command} 忠${officer.loyalty} @${officer.currentCityId}", color = Color(0xFFB9AA82), fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ChroniclePreviewCard(state: GameState) {
    PanelCard {
        SectionTitle("近旬起居注")
        val recent = state.chronicle.takeLast(3).reversed()
        if (recent.isEmpty()) {
            Text("尚无圣旨记录。", color = Color(0xFF8B7355), fontSize = 12.sp)
        } else {
            recent.forEach { entry ->
                Text(entry.era, color = ImperialGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(entry.summary, color = XuanCream, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PanelCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF120D07)),
        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.30f)),
        shape = RoundedCornerShape(12.dp)
    ) { Column(Modifier.padding(12.dp), content = content) }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = ImperialGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun BigStat(label: String, value: String, suffix: String, modifier: Modifier, valueColor: Color = ImperialGold) {
    Column(modifier.background(Color(0xFF1D150B), RoundedCornerShape(8.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFB9AA82), fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Text(suffix, color = Color(0xFF8B7355), fontSize = 10.sp)
    }
}

@Composable
private fun GaugeLine(label: String, value: Int, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = XuanCream, fontSize = 12.sp)
        Text(value.toString(), color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(
        progress = { value.coerceIn(0, 100) / 100f },
        modifier = Modifier.fillMaxWidth().height(6.dp),
        color = color,
        trackColor = Color(0xFF332414)
    )
}

@Composable
private fun FactionMini(name: String, cities: Int, troops: Int, color: Color, modifier: Modifier) {
    Column(modifier.background(Color(0xFF1D150B), RoundedCornerShape(8.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(name, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text("城 $cities", color = XuanCream, fontSize = 12.sp)
        Text("兵 ${troops / 1000}k", color = Color(0xFFB9AA82), fontSize = 11.sp)
    }
}

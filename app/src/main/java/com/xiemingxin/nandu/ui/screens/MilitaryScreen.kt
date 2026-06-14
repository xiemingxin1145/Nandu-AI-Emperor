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
import com.xiemingxin.nandu.game.Army
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.InkBlack
import com.xiemingxin.nandu.ui.theme.JinRed
import com.xiemingxin.nandu.ui.theme.SongBright
import com.xiemingxin.nandu.ui.theme.XuanCream

@Composable
fun MilitaryScreen(gameState: GameState) {
    val cityMap = gameState.cities.associateBy { it.id }
    val officerMap = gameState.officers.associateBy { it.id }
    val activeOfficers = gameState.officers.filter { it.status != OfficerStatus.DISMISSED && it.status != OfficerStatus.DECEASED }
    val deployed = activeOfficers.filter { it.status == OfficerStatus.DEPLOYED }
    val inCourt = activeOfficers.filter { it.status == OfficerStatus.IN_COURT }
    val songTroops = gameState.armies.filter { it.ownerFactionId == "song" }.sumOf { it.troops }
    val jinTroops = gameState.armies.filter { it.ownerFactionId == "jin" }.sumOf { it.troops }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(InkBlack).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PanelCard {
                Text("军务府 V0.5.4", color = ImperialGold, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("军团行军已按距离、兵种、季节、天气计算预计天数。每过一旬，行军天数自动扣减，归零后才抵达。", color = XuanCream, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
        item {
            PanelCard {
                SectionTitle("宋金军团兵势")
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TroopBlock("大宋军团", songTroops, SongBright, Modifier.weight(1f))
                    TroopBlock("金国军团", jinTroops, JinRed, Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                val total = (songTroops + jinTroops).coerceAtLeast(1)
                GaugeLine("大宋兵势", songTroops * 100 / total, SongBright)
                Spacer(Modifier.height(8.dp))
                GaugeLine("金国兵势", jinTroops * 100 / total, JinRed)
            }
        }
        item {
            PanelCard {
                SectionTitle("正式军团")
                gameState.armies.sortedWith(compareBy<Army> { it.ownerFactionId }.thenByDescending { it.troops }).forEach { army ->
                    ArmyRow(
                        army = army,
                        commanderName = officerMap[army.commanderId]?.name ?: "无名统帅",
                        cityName = cityMap[army.currentCityId]?.name ?: army.currentCityId,
                        targetCityName = cityMap[army.targetCityId]?.name ?: army.targetCityId
                    )
                }
            }
        }
        item {
            PanelCard {
                SectionTitle("在外统兵")
                if (deployed.isEmpty()) {
                    Text("暂未任命将领外出统兵。可在朝议页下旨任命或调兵。", color = Color(0xFFB9AA82), fontSize = 12.sp)
                } else {
                    deployed.forEach { officer -> OfficerRow(officer, cityMap[officer.currentCityId]?.name ?: officer.currentCityId) }
                }
            }
        }
        item {
            PanelCard {
                SectionTitle("御前可用")
                inCourt.forEach { officer -> OfficerRow(officer, cityMap[officer.currentCityId]?.name ?: officer.currentCityId) }
            }
        }
        item {
            PanelCard {
                SectionTitle("前线驻军")
                gameState.cities
                    .filter { it.controlState == "FRONTLINE" || it.controlState == "CONTESTED" || it.owner == "jin" }
                    .sortedByDescending { it.troops }
                    .forEach { city ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(city.name, color = if (city.owner == "jin") JinRed else XuanCream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(city.controlState, color = Color(0xFF8B7355), fontSize = 10.sp)
                            }
                            Text("城兵 ${city.troops / 1000}k  防 ${city.defense}", color = Color(0xFFB9AA82), fontSize = 12.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                    }
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
private fun TroopBlock(label: String, troops: Int, color: Color, modifier: Modifier) {
    Column(modifier.background(Color(0xFF1D150B), RoundedCornerShape(8.dp)).padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(0xFFB9AA82), fontSize = 11.sp)
        Text("${troops / 1000}k", color = color, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("兵", color = Color(0xFF8B7355), fontSize = 10.sp)
    }
}

@Composable
private fun GaugeLine(label: String, value: Int, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = XuanCream, fontSize = 12.sp)
        Text("$value%", color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
    LinearProgressIndicator(
        progress = { value.coerceIn(0, 100) / 100f },
        modifier = Modifier.fillMaxWidth().height(6.dp),
        color = color,
        trackColor = Color(0xFF332414)
    )
}

@Composable
private fun ArmyRow(army: Army, commanderName: String, cityName: String, targetCityName: String) {
    val color = if (army.ownerFactionId == "jin") JinRed else SongBright
    val moving = army.status.contains("进军") && army.targetCityId.isNotBlank()
    Column(Modifier.fillMaxWidth().background(Color(0xFF1A1208), RoundedCornerShape(8.dp)).padding(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(army.name, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${army.troops / 1000}k", color = ImperialGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(3.dp))
        Text("统帅：$commanderName · 驻地：$cityName · 状态：${army.status}", color = XuanCream, fontSize = 11.sp)
        if (moving) {
            Text("目标：$targetCityName · 总程${army.marchDaysTotal}天 · 剩余${army.marchDaysRemaining}天", color = ImperialGold, fontSize = 10.sp)
        }
        Text("类型：${army.armyType} · 士气：${army.morale} · 粮道：${army.supplyCityId}", color = Color(0xFF8B7355), fontSize = 10.sp)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun OfficerRow(officer: Officer, cityName: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text(officer.name, color = XuanCream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text("${officer.faction} · $cityName · ${officer.status.name}", color = Color(0xFF8B7355), fontSize = 10.sp)
        }
        Text("统${officer.command} 武${officer.force} 智${officer.strategy} 忠${officer.loyalty}", color = Color(0xFFB9AA82), fontSize = 11.sp)
    }
    Spacer(Modifier.height(8.dp))
}

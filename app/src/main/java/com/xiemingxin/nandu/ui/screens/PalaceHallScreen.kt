package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.ui.components.AssetImage

private val HallGold = Color(0xFFC9A227)
private val HallCream = Color(0xFFE8DCC0)
private val HallInk = Color(0xFF0E0A05)
private val HallSub = Color(0xFF9A8862)

/**
 * V1.0 皇宫大厅：开局后的主页。赵构坐殿，周围是可点功能入口。
 * 让玩家感觉"皇帝坐在宫里治国"，而非冷冰冰的功能Tab。
 * onNavigate(tabIndex) 跳转到对应功能页。
 */
@Composable
fun PalaceHallScreen(
    state: GameState,
    onNavigate: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(HallInk)) {
        // 皇宫背景
        AssetImage(
            path = "images/buildings/building_imperial_palace_01.webp",
            contentDescription = "皇宫",
            contentScale = ContentScale.Crop,
            placeholderText = "宫",
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(HallInk.copy(alpha = 0.55f), HallInk.copy(alpha = 0.4f), HallInk.copy(alpha = 0.9f))
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 顶部：年号 + 季节天气
            Text(state.calendar.displayText(), color = HallGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("${state.season.label} · ${state.weather.label}", color = HallCream, fontSize = 12.sp)

            Spacer(Modifier.height(14.dp))
            // 赵构坐殿
            AssetImage(
                path = "images/characters/halfbody_zhao_gou.webp",
                contentDescription = "赵构",
                contentScale = ContentScale.Crop,
                placeholderText = "构",
                modifier = Modifier.size(130.dp).clip(RoundedCornerShape(12.dp))
            )
            Text("大宋皇帝 · 赵构", color = HallGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(14.dp))
            // 国势摘要
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(HallInk.copy(alpha = 0.65f)).padding(12.dp)
            ) {
                Column {
                    Text("【 国 势 】", color = HallGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StateChip("库银", "${state.gold / 1000}k")
                        StateChip("存粮", "${state.grain / 1000}k")
                        StateChip("军心", "${state.troopMorale}")
                        StateChip("朝局", "${state.courtStability}")
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        StateChip("金虏威胁", "${state.jinThreat}", Color(0xFFCC6655))
                        StateChip("主战", "${state.warFactionPower}", Color(0xFFD4A437))
                        StateChip("主和", "${state.peaceFactionPower}", Color(0xFF8A9BB5))
                        val songCities = state.cities.count { it.owner == "song" }
                        StateChip("控城", "$songCities", Color(0xFF8FB573))
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            // 四大功能入口
            Text("殿 议 朝 政", color = HallCream, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HallEntry("朝议", "下诏理政", "📜", Modifier.weight(1f)) { onNavigate(0) }
                HallEntry("山河", "巡视疆土", "🗺", Modifier.weight(1f)) { onNavigate(1) }
            }
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                HallEntry("国政", "理财安民", "⚖", Modifier.weight(1f)) { onNavigate(2) }
                HallEntry("军务", "点兵遣将", "⚔", Modifier.weight(1f)) { onNavigate(3) }
            }
            Spacer(Modifier.height(16.dp))
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
private fun HallEntry(title: String, sub: String, icon: String, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(88.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1508)),
        border = BorderStroke(1.dp, HallGold.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(10.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(icon, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(title, color = HallGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = HallSub, fontSize = 10.sp)
        }
    }
}

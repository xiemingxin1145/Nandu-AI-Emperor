package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.game.OfficerIntel
import com.xiemingxin.nandu.game.SkillEffects
import com.xiemingxin.nandu.game.commandLimit
import com.xiemingxin.nandu.game.profile

private val PanelGold = Color(0xFFC9A227)
private val PanelCream = Color(0xFFE8DCC0)
private val PanelDark = Color(0xF21A1208)
private val PanelSub = Color(0xFF9A8862)

/**
 * V0.8 人物详情面板：点击武将弹出，显示头像、五维、技能、评估。
 * 数据统一走 OfficerProfile 系统（profile()）。
 */
@Composable
fun CharacterDetailPanel(
    officer: Officer,
    cityName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val p = officer.profile()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, PanelGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                AssetImage(
                    path = ArtResourceRegistry.portraitForOfficer(officer.id),
                    fallbackPath = ArtResourceRegistry.Fallback.portrait,
                    contentDescription = officer.name,
                    contentScale = ContentScale.Crop,
                    placeholderText = officer.name.take(1),
                    modifier = Modifier.size(92.dp).clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(officer.name, color = PanelGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                            Text("X", color = PanelSub, fontSize = 14.sp)
                        }
                    }
                    Text(p.rank + " · " + p.origin, color = PanelCream, fontSize = 13.sp)
                    Text("现驻 " + cityName, color = PanelSub, fontSize = 11.sp)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "忠：" + OfficerIntel.loyaltyLabel(officer.loyalty) + " · 志：" + OfficerIntel.ambitionLabel(p.ambition),
                        color = PanelSub, fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            StatBar("武", officer.force)
            StatBar("统", officer.command)
            StatBar("谋", officer.strategy)
            StatBar("政", officer.politics)
            StatBar("魅", p.charm)

            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MiniStat("名望", OfficerIntel.fameLabel(p.fame))
                MiniStat("资历", OfficerIntel.experienceLabel(p.experience))
                MiniStat("可统兵", (officer.commandLimit() / 1000).toString() + "k")
            }

            if (p.skills.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("专长", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SkillTagRow(p.skills)
                Spacer(Modifier.height(4.dp))
                Text(SkillEffects.shortSummary(p.skills), color = PanelSub, fontSize = 10.sp)
            }

            Spacer(Modifier.height(12.dp))
            Text("评估", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                OfficerIntel.trustBrief(officer.loyalty, p.ambition),
                color = Color(0xFF8FB573), fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    val frac = value.coerceIn(0, 100) / 100f
    val barColor = when {
        value >= 90 -> Color(0xFFD4AF37)
        value >= 75 -> Color(0xFF8FB573)
        value >= 55 -> Color(0xFF8A9BB5)
        else -> Color(0xFF8B7355)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = PanelSub, fontSize = 12.sp, modifier = Modifier.width(22.dp))
        Box(
            modifier = Modifier.weight(1f).height(14.dp)
                .clip(RoundedCornerShape(7.dp)).background(Color(0xFF2A2010))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(frac).fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp)).background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(value.toString(), color = PanelCream, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
    }
}

@Composable
private fun MiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = PanelGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = PanelSub, fontSize = 9.sp)
    }
}

@Composable
private fun SkillTagRow(skills: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        skills.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { skill ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF2E2210))
                            .border(BorderStroke(1.dp, PanelGold.copy(alpha = 0.4f)), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(skill, color = PanelGold, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

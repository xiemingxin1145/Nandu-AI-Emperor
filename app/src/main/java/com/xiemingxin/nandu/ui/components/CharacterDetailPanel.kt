package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
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

private val PanelGold = Color(0xFFC9A227)
private val PanelCream = Color(0xFFE8DCC0)
private val PanelDark = Color(0xF21A1208)
private val PanelSub = Color(0xFF9A8862)

/**
 * V0.8 人物面板：点击武将弹出，显示头像、五维、技能、出身、简介。
 * 隐藏维度（忠诚/野心）做模糊处理，保留信息不对称。
 */
@Composable
fun CharacterDetailPanel(
    officer: Officer,
    cityName: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PanelDark),
        border = BorderStroke(1.dp, PanelGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 顶部：头像 + 名字 + 出身
            Row(verticalAlignment = Alignment.Top) {
                AssetImage(
                    path = ArtResourceRegistry.portraitForOfficer(officer.id),
                    fallbackPath = ArtResourceRegistry.Fallback.portrait,
                    contentDescription = officer.name,
                    contentScale = ContentScale.Crop,
                    placeholderText = officer.name.take(1),
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(10.dp))
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
                    Text(rankLabel(officer.rankLevel), color = PanelCream, fontSize = 13.sp)
                    val originText = if (officer.origin.isNotBlank()) "出身：${officer.origin}" else officer.faction
                    Text("$originText · 现驻$cityName", color = PanelSub, fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    // 忠诚做模糊三档显示
                    Text("态度：${loyaltyLabel(officer.loyalty)}", color = loyaltyColor(officer.loyalty), fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(14.dp))
            // 五维能力条
            StatBar("武", officer.force)
            StatBar("统", officer.command)
            StatBar("谋", officer.strategy)
            StatBar("政", officer.politics)
            StatBar("魅", officer.charm)

            // 可统兵上限
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF241A0C), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("可统兵上限", color = PanelSub, fontSize = 12.sp)
                Text("${commandLimit(officer)} 人", color = PanelGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }

            // 技能标签
            if (officer.skills.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("专长", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                SkillTagRow(officer.skills)
            }

            // 简介
            if (officer.bio.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                Text("生平", color = PanelGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(officer.bio, color = PanelCream, fontSize = 12.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun StatBar(label: String, value: Int) {
    val frac = (value.coerceIn(0, 100)) / 100f
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
            modifier = Modifier
                .weight(1f)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color(0xFF2A2010))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(7.dp))
                    .background(barColor)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text("$value", color = PanelCream, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))
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

private fun rankLabel(level: Int): String = when (level) {
    0 -> "军中小卒"
    1 -> "百夫长 / 军校"
    2 -> "偏将"
    3 -> "统领 / 统制"
    4 -> "方面大将 / 重臣"
    5 -> "宣抚使 / 都督"
    else -> "未仕"
}

private fun loyaltyLabel(loyalty: Int): String = when {
    loyalty >= 85 -> "忠心耿耿"
    loyalty >= 60 -> "尚算可靠"
    loyalty >= 40 -> "心思难测"
    else -> "貌合神离"
}

private fun loyaltyColor(loyalty: Int): Color = when {
    loyalty >= 85 -> Color(0xFF8FB573)
    loyalty >= 60 -> Color(0xFFD4AF37)
    loyalty >= 40 -> Color(0xFFCC9955)
    else -> Color(0xFFCC6655)
}

/** 可统兵上限 = 官阶基数 × 统率系数 */
private fun commandLimit(o: Officer): Int {
    val base = when (o.rankLevel) {
        0 -> 1000
        1 -> 5000
        2 -> 12000
        3 -> 30000
        4 -> 60000
        else -> 90000
    }
    return (base * (0.6 + o.command / 100.0 * 0.6)).toInt()
}

package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.CityVisitAction
import com.xiemingxin.nandu.game.Rumor
import com.xiemingxin.nandu.game.TavernSystem

private val TvGold = Color(0xFFC9A227)
private val TvCream = Color(0xFFE8DCC0)
private val TvDark = Color(0xF21A1208)
private val TvSub = Color(0xFF9A8862)
private val TvRed = Color(0xFF7D1D16)

/**
 * V1.4.0 酒楼情报面板：城中走访的灵魂入口。
 * 打听消息 / 宴请宾客 / 结交游侠 / 探查传闻 —— 换取传闻、名望、在野人才线索。
 */
@Composable
fun TavernPanel(
    city: City,
    actionPoints: Int,
    prestige: Int,
    rumors: List<Rumor>,
    lastNarration: String?,
    onVisit: (CityVisitAction) -> Unit,
    onDismissNarration: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TvDark),
        border = BorderStroke(1.dp, TvGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("🏮 ${city.name} · 酒楼", color = TvGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("三教九流汇聚之地，消息、人脉、奇遇皆在杯盏之间", color = TvSub, fontSize = 10.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Text("X", color = TvSub, fontSize = 14.sp)
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill("行动力", "$actionPoints / ${TavernSystem.MAX_ACTION_POINTS}", if (actionPoints > 0) TvGold else TvRed, Modifier.weight(1f))
                StatPill("名望", "$prestige", TvGold, Modifier.weight(1f))
                StatPill("情报", "${rumors.size}", TvCream, Modifier.weight(1f))
            }

            Spacer(Modifier.height(10.dp))

            // 走访见闻（最近一次）
            if (lastNarration != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onDismissNarration() },
                    colors = CardDefaults.cardColors(containerColor = Color(0xC5241A0C)),
                    border = BorderStroke(1.dp, TvGold.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Column(modifier = Modifier.padding(11.dp)) {
                        Text("【 走 访 见 闻 】", color = TvGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(lastNarration, color = TvCream, fontSize = 12.sp, lineHeight = 18.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("（轻触收起）", color = TvSub, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(10.dp))
            }

            // 四个走访动作
            val disabled = actionPoints <= 0 || city.owner != "song"
            CityVisitAction.values().forEach { act ->
                VisitActionRow(act, city.gold >= act.goldCost && !disabled) { onVisit(act) }
                Spacer(Modifier.height(7.dp))
            }

            if (rumors.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("近 日 风 闻", color = TvCream, fontSize = 12.sp, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                Column(
                    modifier = Modifier.heightIn(max = 170.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rumors.takeLast(12).reversed().forEach { RumorRow(it) }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, valueColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xB30E0A05)),
        border = BorderStroke(1.dp, TvGold.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(9.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(label, color = TvSub, fontSize = 9.sp)
        }
    }
}

@Composable
private fun VisitActionRow(action: CityVisitAction, enabled: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (enabled) Color(0xD3241A0C) else Color(0x55140E06), RoundedCornerShape(11.dp))
            .clickable(enabled = enabled) { onClick() }
            .padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(action.label, color = if (enabled) TvGold else TvSub, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(action.desc, color = TvSub, fontSize = 10.sp, lineHeight = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Column(horizontalAlignment = Alignment.End) {
            Text("耗银 ${action.goldCost}", color = TvCream, fontSize = 10.sp)
            Text("行动力 -1", color = TvSub, fontSize = 9.sp)
        }
    }
}

@Composable
private fun RumorRow(rumor: Rumor) {
    val tagColor = when (rumor.category) {
        "military" -> Color(0xFFCC6655)
        "civil" -> Color(0xFF8FB573)
        "trade" -> Color(0xFFD4A437)
        "talent" -> Color(0xFF4DA3E6)
        else -> Color(0xFFB089D6)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0x991D150B), RoundedCornerShape(9.dp))
            .padding(9.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .background(tagColor.copy(alpha = 0.22f), RoundedCornerShape(5.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Text(TavernSystem.categoryLabel(rumor.category), color = tagColor, fontSize = 9.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(8.dp))
        Text(rumor.text, color = TvCream, fontSize = 11.sp, lineHeight = 16.sp, modifier = Modifier.weight(1f))
    }
}

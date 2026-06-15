package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import com.xiemingxin.nandu.game.BattleUnitCatalog
import com.xiemingxin.nandu.game.BattleUnitDef
import com.xiemingxin.nandu.game.City

private val RcGold = Color(0xFFC9A227)
private val RcCream = Color(0xFFE8DCC0)
private val RcDark = Color(0xF21A1208)
private val RcSub = Color(0xFF9A8862)

/**
 * V0.9 募兵面板：在城池招募各兵种，消耗金粮，增加该城驻军。
 * 每批招募1000人。onRecruit(unitId) 由上层执行扣费与增兵。
 */
@Composable
fun RecruitPanel(
    city: City,
    onRecruit: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = RcDark),
        border = BorderStroke(1.dp, RcGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(city.name + " · 募兵", color = RcGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("现有驻军 " + (city.troops / 1000) + "k · 库银 " + (city.gold / 1000) + "k · 存粮 " + (city.grain / 1000) + "k",
                        color = RcSub, fontSize = 11.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Text("X", color = RcSub, fontSize = 14.sp)
                }
            }
            Text("每批募兵 1000 人，兵种相克见专长说明", color = RcSub, fontSize = 10.sp)
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BattleUnitCatalog.recruitable("song").forEach { def ->
                    RecruitRow(def, city) { onRecruit(def.id) }
                }
            }
        }
    }
}

@Composable
private fun RecruitRow(def: BattleUnitDef, city: City, onRecruit: () -> Unit) {
    val affordable = city.gold >= def.recruitGold && city.grain >= def.recruitGrain
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1D150B), RoundedCornerShape(10.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AssetImage(
            path = def.imagePath,
            contentDescription = def.name,
            contentScale = ContentScale.Crop,
            placeholderText = def.name.take(1),
            modifier = Modifier.size(54.dp).clip(RoundedCornerShape(8.dp))
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(def.name, color = RcCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("[" + BattleUnitCatalog.categoryLabel(def.category) + "]", color = RcSub, fontSize = 10.sp)
            }
            Text("攻" + def.attack + " 防" + def.defense, color = Color(0xFFB9AA82), fontSize = 10.sp)
            Text(def.desc, color = RcSub, fontSize = 10.sp)
            Text("募兵：" + def.recruitGold + "金 " + def.recruitGrain + "粮", color = Color(0xFFB9AA82), fontSize = 10.sp)
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = onRecruit,
            enabled = affordable,
            border = BorderStroke(1.dp, if (affordable) RcGold.copy(alpha = 0.6f) else RcSub.copy(alpha = 0.3f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RcGold)
        ) {
            Text("募兵", fontSize = 11.sp)
        }
    }
}

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
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
import com.xiemingxin.nandu.game.BuildingCatalog
import com.xiemingxin.nandu.game.BuildingDef
import com.xiemingxin.nandu.game.City

private val MgGold = Color(0xFFC9A227)
private val MgCream = Color(0xFFE8DCC0)
private val MgDark = Color(0xF21A1208)
private val MgSub = Color(0xFF9A8862)

/**
 * V0.9 城池经营面板：列出可建造的建筑，显示图、等级、效果、造价。
 * onBuild(buildingId) 由上层接到圣旨/规则引擎执行实际建造。
 */
@Composable
fun CityManagePanel(
    city: City,
    onBuild: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MgDark),
        border = BorderStroke(1.dp, MgGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(city.name + " · 营建", color = MgGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("库银 " + (city.gold / 1000) + "k · 存粮 " + (city.grain / 1000) + "k", color = MgSub, fontSize = 11.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                    Text("X", color = MgSub, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BuildingCatalog.all.forEach { def ->
                    val level = city.buildings[def.id] ?: 0
                    val locked = def.requireWaterNode && !city.isWaterNode
                    BuildingRow(def, level, locked, city) { onBuild(def.id) }
                }
            }
        }
    }
}

@Composable
private fun BuildingRow(
    def: BuildingDef,
    level: Int,
    locked: Boolean,
    city: City,
    onBuild: () -> Unit
) {
    val (goldCost, grainCost) = BuildingCatalog.upgradeCost(def, level)
    val maxed = level >= def.maxLevel
    val affordable = city.gold >= goldCost && city.grain >= grainCost
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
                Text(def.name, color = MgCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("[" + BuildingCatalog.categoryLabel(def.category) + "]", color = MgSub, fontSize = 10.sp)
                Spacer(Modifier.width(6.dp))
                Text("Lv." + level + "/" + def.maxLevel, color = MgGold, fontSize = 11.sp)
            }
            Text(def.effectDesc, color = MgSub, fontSize = 10.sp)
            if (!maxed && !locked) {
                Text("造价：" + goldCost + "金 " + grainCost + "粮", color = Color(0xFFB9AA82), fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(8.dp))
        when {
            locked -> Text("需水运", color = Color(0xFF8B7355), fontSize = 11.sp)
            maxed -> Text("已满级", color = Color(0xFF8FB573), fontSize = 11.sp)
            else -> OutlinedButton(
                onClick = onBuild,
                enabled = affordable,
                border = BorderStroke(1.dp, if (affordable) MgGold.copy(alpha = 0.6f) else MgSub.copy(alpha = 0.3f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MgGold)
            ) {
                Text(if (level == 0) "建造" else "升级", fontSize = 11.sp)
            }
        }
    }
}

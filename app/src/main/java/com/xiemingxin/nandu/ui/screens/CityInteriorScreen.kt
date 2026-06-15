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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.BuildingCatalog
import com.xiemingxin.nandu.game.BuildingDef
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.RecruitPanel

private val CiGold = Color(0xFFC9A227)
private val CiCream = Color(0xFFE8DCC0)
private val CiInk = Color(0xFF0E0A05)
private val CiSub = Color(0xFF9A8862)

/**
 * V1.0 城池内景：城市背景图 + 固定建筑槽位。
 * 已建建筑亮显，未建灰暗，点击弹出升级面板。复用 buildInCity 逻辑。
 */
@Composable
fun CityInteriorScreen(
    city: City,
    onBuild: (String) -> Unit,
    onRecruit: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBuilding by remember { mutableStateOf<BuildingDef?>(null) }
    var showRecruit by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize().background(CiInk)) {
        // 城池背景
        AssetImage(
            path = ArtResourceRegistry.cityBackground(city.id),
            fallbackPath = ArtResourceRegistry.Fallback.city,
            contentDescription = city.name,
            contentScale = ContentScale.Crop,
            placeholderText = city.name,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(CiInk.copy(alpha = 0.4f), CiInk.copy(alpha = 0.25f), CiInk.copy(alpha = 0.92f))
                )
            )
        )

        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            // 顶部：城名 + 返回
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(city.name, color = CiGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    val ownerText = if (city.owner == "jin") "金占" else "大宋"
                    Text("$ownerText · ${city.route} · ${city.cityLevel}", color = CiCream, fontSize = 11.sp)
                }
                Card(
                    modifier = Modifier.clickable { onBack() },
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1508)),
                    border = BorderStroke(1.dp, CiGold.copy(alpha = 0.5f))
                ) {
                    Text("← 返回", color = CiGold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Spacer(Modifier.height(8.dp))
            // 城池数据条
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(CiInk.copy(alpha = 0.6f)).padding(10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CityStat("兵", "${city.troops / 1000}k")
                CityStat("防", "${city.defense}")
                CityStat("粮", "${city.grain / 1000}k")
                CityStat("银", "${city.gold / 1000}k")
                CityStat("民", "${city.popularSupport}")
                CityStat("商", "${city.commerce}")
                CityStat("农", "${city.agriculture}")
            }

            Spacer(Modifier.height(12.dp))
            Text("城 内 营 建", color = CiCream, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))

            // 建筑槽位网格
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(BuildingCatalog.all) { def ->
                    val level = city.buildings[def.id] ?: 0
                    BuildingSlot(def, level) { selectedBuilding = def }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (city.owner == "song") {
                Button(
                    onClick = { showRecruit = true },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = CiGold)
                ) { Text("募 兵 练 军", color = CiInk, fontWeight = FontWeight.Bold) }
            }
        }

        // 建筑详情/升级弹窗
        selectedBuilding?.let { def ->
            val level = city.buildings[def.id] ?: 0
            Dialog(onDismissRequest = { selectedBuilding = null }) {
                BuildingUpgradeCard(def, level, city,
                    onBuild = { onBuild(def.id); selectedBuilding = null },
                    onDismiss = { selectedBuilding = null })
            }
        }

        // V1.0 城内募兵弹窗（募完留在城内，数据即时刷新）
        if (showRecruit) {
            Dialog(onDismissRequest = { showRecruit = false }) {
                RecruitPanel(
                    city = city,
                    onRecruit = { unitId -> onRecruit(unitId) },
                    onDismiss = { showRecruit = false }
                )
            }
        }
    }
}

@Composable
private fun CityStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = CiGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CiSub, fontSize = 9.sp)
    }
}

@Composable
private fun BuildingSlot(def: BuildingDef, level: Int, onClick: () -> Unit) {
    val built = level > 0
    Card(
        modifier = Modifier.height(100.dp).clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = if (built) Color(0xCC241A0C) else Color(0x99140E06)),
        border = BorderStroke(1.dp, if (built) CiGold.copy(alpha = 0.65f) else CiSub.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AssetImage(
                path = def.imagePath,
                contentDescription = def.name,
                contentScale = ContentScale.Crop,
                placeholderText = def.name.take(1),
                modifier = Modifier.size(46.dp).clip(RoundedCornerShape(6.dp)).alpha(if (built) 1f else 0.4f)
            )
            Spacer(Modifier.height(4.dp))
            Text(def.name, color = if (built) CiCream else CiSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (built) "Lv.$level" else "未建", color = if (built) CiGold else CiSub, fontSize = 9.sp)
        }
    }
}

@Composable
private fun BuildingUpgradeCard(
    def: BuildingDef, level: Int, city: City,
    onBuild: () -> Unit, onDismiss: () -> Unit
) {
    val (goldCost, grainCost) = BuildingCatalog.upgradeCost(def, level)
    val maxed = level >= def.maxLevel
    val locked = def.requireWaterNode && !city.isWaterNode
    val affordable = city.gold >= goldCost && city.grain >= grainCost
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1208)),
        border = BorderStroke(1.dp, CiGold)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AssetImage(
                path = def.imagePath,
                contentDescription = def.name,
                contentScale = ContentScale.Crop,
                placeholderText = def.name.take(1),
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(def.name, color = CiGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("[${BuildingCatalog.categoryLabel(def.category)}] Lv.$level/${def.maxLevel}", color = CiSub, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text(def.effectDesc, color = CiCream, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            when {
                locked -> Text("此城无水运，无法建造", color = Color(0xFFCC6655), fontSize = 12.sp)
                maxed -> Text("已达最高等级", color = Color(0xFF8FB573), fontSize = 13.sp)
                else -> {
                    Text("造价：${goldCost}金 ${grainCost}粮", color = CiCream, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = onBuild,
                        enabled = affordable && city.owner == "song",
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = CiGold)
                    ) { Text(if (level == 0) "建 造" else "升 级", color = CiInk, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

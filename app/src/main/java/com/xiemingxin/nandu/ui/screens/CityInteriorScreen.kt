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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
private val CiRed = Color(0xFF7D1D16)
private val CiBlue = Color(0xFF4DA3E6)

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
    val builtCount = city.buildings.count { it.value > 0 }
    val advice = cityAdvice(city)

    Box(modifier = modifier.fillMaxSize().background(CiInk)) {
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
                    listOf(
                        CiInk.copy(alpha = 0.38f),
                        CiInk.copy(alpha = 0.18f),
                        CiInk.copy(alpha = 0.94f)
                    )
                )
            )
        )
        Canvas(modifier = Modifier.fillMaxSize()) { drawCityAtmosphere(city) }

        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            CityHeader(city, builtCount, onBack)
            Spacer(Modifier.height(10.dp))
            CityAdviceCard(advice)
            Spacer(Modifier.height(10.dp))
            CityStatsPanel(city)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("城 内 营 建", color = CiCream, fontSize = 13.sp, letterSpacing = 3.sp)
                Text("已建 $builtCount/${BuildingCatalog.all.size}", color = CiSub, fontSize = 10.sp)
            }
            Spacer(Modifier.height(8.dp))

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

            Spacer(Modifier.height(9.dp))
            CityBottomActions(city) { showRecruit = true }
        }

        selectedBuilding?.let { def ->
            val level = city.buildings[def.id] ?: 0
            Dialog(onDismissRequest = { selectedBuilding = null }) {
                BuildingUpgradeCard(def, level, city,
                    onBuild = { onBuild(def.id); selectedBuilding = null },
                    onDismiss = { selectedBuilding = null })
            }
        }

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
private fun CityHeader(city: City, builtCount: Int, onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xC70E0A05)),
        border = BorderStroke(1.dp, CiGold.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(city.name, color = CiGold, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                val ownerText = if (city.owner == "jin") "金国占领" else "大宋控制"
                Text("$ownerText · ${city.route} · ${city.cityLevel}", color = CiCream, fontSize = 11.sp)
                Text("${terrainName(city.terrain)} · 建设 $builtCount · 人口${city.population / 10000}万", color = CiSub, fontSize = 10.sp)
            }
            Card(
                modifier = Modifier.clickable { onBack() },
                shape = RoundedCornerShape(9.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1508)),
                border = BorderStroke(1.dp, CiGold.copy(alpha = 0.55f))
            ) {
                Text("返回山河", color = CiGold, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
            }
        }
    }
}

@Composable
private fun CityAdviceCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xC51E1508)),
        border = BorderStroke(1.dp, CiRed.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("【 城 务 提 醒 】", color = CiGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(text, color = CiCream, fontSize = 11.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun CityStatsPanel(city: City) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xB90E0A05)),
        border = BorderStroke(1.dp, CiGold.copy(alpha = 0.30f)),
        shape = RoundedCornerShape(13.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CityStat("兵", "${city.troops / 1000}k", if (city.troops < 10000) Color(0xFFFF8A70) else CiGold)
                CityStat("防", "${city.defense}")
                CityStat("粮", "${city.grain / 1000}k")
                CityStat("银", "${city.gold / 1000}k")
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CityStat("民", "${city.popularSupport}", if (city.popularSupport < 55) Color(0xFFFF8A70) else CiGold)
                CityStat("商", "${city.commerce}")
                CityStat("农", "${city.agriculture}")
                CityStat(if (city.isWaterNode) "漕" else "路", if (city.isWaterNode) "通" else "平", if (city.isWaterNode) CiBlue else CiSub)
            }
        }
    }
}

@Composable
private fun CityBottomActions(city: City, onRecruitClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (city.owner == "song") {
            Button(
                onClick = onRecruitClick,
                modifier = Modifier.weight(1f).height(44.dp),
                shape = RoundedCornerShape(11.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CiGold)
            ) { Text("募 兵 练 军", color = CiInk, fontWeight = FontWeight.Bold) }
            Card(
                modifier = Modifier.weight(1f).height(44.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1508)),
                border = BorderStroke(1.dp, CiGold.copy(alpha = 0.45f)),
                shape = RoundedCornerShape(11.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("营 建 城 务", color = CiGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1508)),
                border = BorderStroke(1.dp, CiRed.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(11.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("金占城池：需先从山河图发动进取", color = Color(0xFFFFC0A5), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun CityStat(label: String, value: String, valueColor: Color = CiGold) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CiSub, fontSize = 9.sp)
    }
}

@Composable
private fun BuildingSlot(def: BuildingDef, level: Int, onClick: () -> Unit) {
    val built = level > 0
    Card(
        modifier = Modifier.height(108.dp).clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (built) Color(0xD3241A0C) else Color(0xA3140E06)),
        border = BorderStroke(1.dp, if (built) CiGold.copy(alpha = 0.68f) else CiSub.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AssetImage(
                path = def.imagePath,
                contentDescription = def.name,
                contentScale = ContentScale.Crop,
                placeholderText = def.name.take(1),
                modifier = Modifier.size(50.dp).clip(RoundedCornerShape(7.dp)).alpha(if (built) 1f else 0.38f)
            )
            Spacer(Modifier.height(5.dp))
            Text(def.name, color = if (built) CiCream else CiSub, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(if (built) "Lv.$level · ${BuildingCatalog.categoryLabel(def.category)}" else "未建 · ${BuildingCatalog.categoryLabel(def.category)}", color = if (built) CiGold else CiSub, fontSize = 8.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun BuildingUpgradeCard(def: BuildingDef, level: Int, city: City, onBuild: () -> Unit, onDismiss: () -> Unit) {
    val (goldCost, grainCost) = BuildingCatalog.upgradeCost(def, level)
    val maxed = level >= def.maxLevel
    val locked = def.requireWaterNode && !city.isWaterNode
    val affordable = city.gold >= goldCost && city.grain >= grainCost
    Card(
        shape = RoundedCornerShape(15.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1208)),
        border = BorderStroke(1.dp, CiGold)
    ) {
        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            AssetImage(
                path = def.imagePath,
                contentDescription = def.name,
                contentScale = ContentScale.Crop,
                placeholderText = def.name.take(1),
                modifier = Modifier.size(82.dp).clip(RoundedCornerShape(11.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text(def.name, color = CiGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("[${BuildingCatalog.categoryLabel(def.category)}] Lv.$level/${def.maxLevel}", color = CiSub, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))
            Text(def.effectDesc, color = CiCream, fontSize = 12.sp, lineHeight = 18.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            when {
                locked -> Text("此城无水运，无法建造", color = Color(0xFFCC6655), fontSize = 12.sp)
                maxed -> Text("已达最高等级", color = Color(0xFF8FB573), fontSize = 13.sp)
                else -> {
                    Text("造价：${goldCost}金 ${grainCost}粮", color = CiCream, fontSize = 12.sp)
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier.weight(1f).height(42.dp).clickable { onDismiss() },
                            colors = CardDefaults.cardColors(containerColor = Color(0xCC1E1508)),
                            border = BorderStroke(1.dp, CiSub.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(10.dp)
                        ) { Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) { Text("取消", color = CiSub, fontSize = 12.sp) } }
                        Button(
                            onClick = onBuild,
                            enabled = affordable && city.owner == "song",
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CiGold)
                        ) { Text(if (level == 0) "建造" else "升级", color = CiInk, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

private fun cityAdvice(city: City): String = when {
    city.owner == "jin" -> "此城仍为金国所据。若欲经营，须先在山河图中组织进取，夺回城池后方可营建。"
    city.popularSupport < 55 -> "民心偏低，宜优先修寺观、书院，或减少抽丁募兵，免生怨气。"
    city.grain < 25000 -> "粮储不足，宜先修粮仓、兴农桑，否则大军难以久战。"
    city.defense < 60 -> "城防薄弱，宜先筑城墙、整军械，防止前线震动。"
    city.troops < 10000 -> "驻军不足，若处前线，宜募兵练军；若处后方，宜先安民富商。"
    city.commerce < 55 -> "商业未兴，可修市场、通漕运，增强钱粮周转。"
    else -> "此城根基尚稳，可按战略需要选择兴农、修城、募兵或发展商贸。"
}

private fun terrainName(terrain: String): String = when (terrain) {
    "river" -> "水乡"
    "mountain" -> "山地"
    "pass" -> "关隘"
    "coast" -> "沿海"
    else -> "平原"
}

private fun DrawScope.drawCityAtmosphere(city: City) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    drawCircle(CiGold.copy(alpha = 0.06f), w * 0.45f, Offset(w * 0.50f, h * 0.24f))
    if (city.owner == "jin") drawCircle(CiRed.copy(alpha = 0.10f), w * 0.45f, Offset(w * 0.20f, h * 0.55f))
    if (city.isWaterNode) drawCircle(CiBlue.copy(alpha = 0.07f), w * 0.52f, Offset(w * 0.80f, h * 0.62f))
}

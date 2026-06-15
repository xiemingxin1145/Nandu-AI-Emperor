package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import com.xiemingxin.nandu.game.AudioResourceRegistry
import com.xiemingxin.nandu.game.BuildingCatalog
import com.xiemingxin.nandu.game.BuildingDef
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.CityVisitAction
import com.xiemingxin.nandu.game.Rumor
import com.xiemingxin.nandu.game.TavernSystem
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.PlayAmbienceEffect
import com.xiemingxin.nandu.ui.components.PlaySfxEffect
import com.xiemingxin.nandu.ui.components.RecruitPanel
import com.xiemingxin.nandu.ui.components.TavernPanel

private val CiGold = Color(0xFFC9A227)
private val CiCream = Color(0xFFE8DCC0)
private val CiInk = Color(0xFF0E0A05)
private val CiSub = Color(0xFF9A8862)
private val CiRed = Color(0xFF7D1D16)
private val CiBlue = Color(0xFF4DA3E6)

/** 城内场景热点：错落分布在城图上的可点功能点位 */
private data class CityHotspot(
    val key: String,
    val label: String,
    val icon: String,
    val buildingId: String,   // 关联建筑（显示等级），酒楼等功能点为空
    val xr: Float,
    val yr: Float
)

private val CITY_HOTSPOTS = listOf(
    CityHotspot("build", "府衙", "\uD83C\uDFDB", "prefecture_office", 0.50f, 0.13f),
    CityHotspot("tavern", "酒楼", "\uD83C\uDFEE", "", 0.21f, 0.37f),
    CityHotspot("market", "市集", "\uD83C\uDFEA", "market", 0.79f, 0.35f),
    CityHotspot("barracks", "兵营", "\u2694", "barracks", 0.27f, 0.66f),
    CityHotspot("granary", "粮仓", "\uD83C\uDF3E", "granary", 0.73f, 0.67f),
    CityHotspot("wall", "城防", "\uD83D\uDEE1", "city_wall", 0.50f, 0.86f)
)

@Composable
fun CityInteriorScreen(
    city: City,
    actionPoints: Int,
    prestige: Int,
    rumors: List<Rumor>,
    lastVisitNarration: String?,
    onBuild: (String) -> Unit,
    onRecruit: (String) -> Unit,
    onVisit: (CityVisitAction) -> Unit,
    onDismissVisitNarration: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedBuilding by remember { mutableStateOf<BuildingDef?>(null) }
    var showRecruit by remember { mutableStateOf(false) }
    var showTavern by remember { mutableStateOf(false) }
    var showBuildOverview by remember { mutableStateOf(false) }
    val builtCount = city.buildings.count { it.value > 0 }
    val advice = cityAdvice(city)

    Box(modifier = modifier.fillMaxSize().background(CiInk)) {
        // V1.4.1 城内环境音：进酒楼切人声喧闹，否则市井白日声
        val ambiencePath = if (showTavern) AudioResourceRegistry.Ambience.tavern else AudioResourceRegistry.Ambience.cityDay
        PlayAmbienceEffect(path = ambiencePath, volume = 0.42f)
        // 走访见闻到达时一声"叮"
        PlaySfxEffect(path = AudioResourceRegistry.Sfx.reportArrive, triggerKey = lastVisitNarration, volume = 0.55f, variant = true)

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
                        CiInk.copy(alpha = 0.42f),
                        CiInk.copy(alpha = 0.16f),
                        CiInk.copy(alpha = 0.92f)
                    )
                )
            )
        )
        Canvas(modifier = Modifier.fillMaxSize()) { drawCityAtmosphere(city) }

        Column(modifier = Modifier.fillMaxSize().padding(14.dp)) {
            CityHeader(city, actionPoints, prestige, onBack)
            Spacer(Modifier.height(9.dp))
            CityStatsPanel(city)
            Spacer(Modifier.height(8.dp))
            CityAdviceCard(advice)
            Spacer(Modifier.height(6.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("城 内 走 访", color = CiCream, fontSize = 12.sp, letterSpacing = 3.sp)
                Text("点街景办事 · 已建 $builtCount/${BuildingCatalog.all.size}", color = CiSub, fontSize = 9.sp)
            }

            // 城内场景热点区
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                val bw = maxWidth
                val bh = maxHeight
                CITY_HOTSPOTS.forEach { hs ->
                    val level = if (hs.buildingId.isNotEmpty()) (city.buildings[hs.buildingId] ?: 0) else -1
                    HotspotChip(
                        hotspot = hs,
                        level = level,
                        modifier = Modifier.offset(x = bw * hs.xr - 40.dp, y = bh * hs.yr - 30.dp)
                    ) {
                        when (hs.key) {
                            "tavern" -> showTavern = true
                            "barracks" -> if (city.owner == "song") showRecruit = true else showBuildOverview = true
                            else -> showBuildOverview = true
                        }
                    }
                }
            }
        }

        // 营建总览（保留完整 14 建筑网格）
        if (showBuildOverview) {
            Dialog(onDismissRequest = { showBuildOverview = false }) {
                BuildOverviewPanel(
                    city = city,
                    onPick = { def -> selectedBuilding = def },
                    onDismiss = { showBuildOverview = false }
                )
            }
        }

        // 建筑升级卡
        selectedBuilding?.let { def ->
            val level = city.buildings[def.id] ?: 0
            Dialog(onDismissRequest = { selectedBuilding = null }) {
                BuildingUpgradeCard(def, level, city,
                    onBuild = { onBuild(def.id); selectedBuilding = null },
                    onDismiss = { selectedBuilding = null })
            }
        }

        // 酒楼情报
        if (showTavern) {
            Dialog(onDismissRequest = { showTavern = false }) {
                TavernPanel(
                    city = city,
                    actionPoints = actionPoints,
                    prestige = prestige,
                    rumors = rumors.filter { it.sourceCityId == city.id },
                    lastNarration = lastVisitNarration,
                    onVisit = onVisit,
                    onDismissNarration = onDismissVisitNarration,
                    onDismiss = { showTavern = false; onDismissVisitNarration() }
                )
            }
        }

        // 募兵
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
private fun HotspotChip(hotspot: CityHotspot, level: Int, modifier: Modifier, onClick: () -> Unit) {
    val built = level > 0
    val isFunc = level < 0   // 功能点（酒楼等）无建筑等级
    Column(
        modifier = modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xE61E1508), Color(0xE60E0A05))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(hotspot.icon, fontSize = 24.sp)
        }
        Spacer(Modifier.height(3.dp))
        Card(
            shape = RoundedCornerShape(7.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xCC120C05)),
            border = BorderStroke(1.dp, if (isFunc) CiGold.copy(alpha = 0.7f) else if (built) CiGold.copy(alpha = 0.6f) else CiSub.copy(alpha = 0.35f))
        ) {
            Text(
                text = if (isFunc) hotspot.label else if (built) "${hotspot.label}·Lv$level" else "${hotspot.label}·未建",
                color = if (isFunc || built) CiGold else CiSub,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
private fun CityHeader(city: City, actionPoints: Int, prestige: Int, onBack: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xC70E0A05)),
        border = BorderStroke(1.dp, CiGold.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(city.name, color = CiGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                val ownerText = if (city.owner == "jin") "金国占领" else "大宋控制"
                Text("$ownerText · ${city.route} · ${city.cityLevel}", color = CiCream, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("行动力 $actionPoints/${TavernSystem.MAX_ACTION_POINTS}", color = if (actionPoints > 0) CiGold else Color(0xFFFF8A70), fontSize = 10.sp)
                    Text("名望 $prestige", color = CiBlue, fontSize = 10.sp)
                }
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
        Column(modifier = Modifier.padding(11.dp)) {
            Text("【 城 务 提 醒 】", color = CiGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(text, color = CiCream, fontSize = 11.sp, lineHeight = 16.sp)
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
private fun CityStat(label: String, value: String, valueColor: Color = CiGold) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = CiSub, fontSize = 9.sp)
    }
}

@Composable
private fun BuildOverviewPanel(city: City, onPick: (BuildingDef) -> Unit, onDismiss: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xF21A1208)),
        border = BorderStroke(1.dp, CiGold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("\uD83C\uDFDB ${city.name} · 营建城务", color = CiGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("收起", color = CiSub, fontSize = 12.sp, modifier = Modifier.clickable { onDismiss() }.padding(4.dp))
            }
            Spacer(Modifier.height(9.dp))
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().height(380.dp)
            ) {
                items(BuildingCatalog.all) { def ->
                    val level = city.buildings[def.id] ?: 0
                    BuildingSlot(def, level) { onPick(def) }
                }
            }
        }
    }
}

@Composable
private fun BuildingSlot(def: BuildingDef, level: Int, onClick: () -> Unit) {
    val built = level > 0
    Card(
        modifier = Modifier.height(106.dp).clickable { onClick() },
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
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(7.dp)).alpha(if (built) 1f else 0.38f)
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
    else -> "此城根基尚稳。可入酒楼走访打探，或营建城务、募兵练军。"
}

private fun DrawScope.drawCityAtmosphere(city: City) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    drawCircle(CiGold.copy(alpha = 0.06f), w * 0.45f, Offset(w * 0.50f, h * 0.24f))
    if (city.owner == "jin") drawCircle(CiRed.copy(alpha = 0.10f), w * 0.45f, Offset(w * 0.20f, h * 0.55f))
    if (city.isWaterNode) drawCircle(CiBlue.copy(alpha = 0.07f), w * 0.52f, Offset(w * 0.80f, h * 0.62f))
}

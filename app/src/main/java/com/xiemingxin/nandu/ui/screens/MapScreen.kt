package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.game.Army
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.City
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.MapLayerMode
import com.xiemingxin.nandu.game.MapNode
import com.xiemingxin.nandu.game.RoadType
import com.xiemingxin.nandu.game.TerrainFeatures
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.CityManagePanel
import com.xiemingxin.nandu.ui.components.RecruitPanel
import com.xiemingxin.nandu.ui.theme.ImperialGold
import com.xiemingxin.nandu.ui.theme.JinRed
import com.xiemingxin.nandu.ui.theme.MapBg
import com.xiemingxin.nandu.ui.theme.SongBright
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.math.sqrt

private data class MapFocusRegion(
    val id: String,
    val name: String,
    val camX: Float,
    val camY: Float,
    val zoom: Float,
    val color: Color,
    val power: String,
    val summary: String,
    val strategy: String,
    val danger: String
)

private data class MapWorldRegion(
    val name: String,
    val faction: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val labelX: Float,
    val labelY: Float,
    val color: Color
)

private val FocusRegions = listOf(
    MapFocusRegion("all", "天下", 0f, 0f, 0.052f, ImperialGold, "天下诸势", "总览南宋、金国、西夏、大理与海贸诸区。", "看清全局后再决定经营、守城或进取。", "中"),
    MapFocusRegion("song", "宋境", 2800f, 4850f, 0.078f, SongBright, "南宋", "江南行在与东南财富根本。", "稳住钱粮、民心、海贸，才有北伐本钱。", "低"),
    MapFocusRegion("front", "江淮", 6200f, 4200f, 0.090f, ImperialGold, "宋金争夺", "淮河防线，是南宋生死缓冲带。", "优先修城、屯粮、布防，避免金军南下。", "高"),
    MapFocusRegion("jingxiang", "荆襄", 3500f, 4300f, 0.090f, Color(0xFFD0A66A), "南宋", "荆襄是北伐跳板，也是山河要冲。", "城防与步兵收益高，适合稳步推进。", "中"),
    MapFocusRegion("central", "中原", 6000f, 1300f, 0.086f, JinRed, "金国占领", "开封、洛阳旧都所在，政治象征极重。", "贸然进取损耗大，需先断粮道、集军心。", "高"),
    MapFocusRegion("hebei", "河北", 8000f, 200f, 0.078f, JinRed, "金国纵深", "河北燕云为金国后方屏障。", "后期扩展区，当前先作为北方压力来源。", "极高"),
    MapFocusRegion("xixia", "西夏", 1200f, 1800f, 0.070f, Color(0xFFB38A48), "西夏", "河陇与河西走廊，未来第三势力。", "先做边贸、买马、外交事件，不急参战。", "未知")
)

private val WorldRegions = listOf(
    MapWorldRegion("江南行在", "南宋核心", 3000f, 6100f, 11800f, 9300f, 7200f, 8050f, SongBright.copy(alpha = 0.16f)),
    MapWorldRegion("江淮防线", "宋金争夺", 6200f, 4550f, 11200f, 6500f, 8600f, 5550f, ImperialGold.copy(alpha = 0.14f)),
    MapWorldRegion("荆襄山河", "北伐跳板", 3500f, 3900f, 6900f, 6500f, 5200f, 5250f, Color(0xFFD0A66A).copy(alpha = 0.15f)),
    MapWorldRegion("中原旧都", "金国占领", 6100f, 1750f, 10700f, 4650f, 8400f, 3200f, JinRed.copy(alpha = 0.18f)),
    MapWorldRegion("河北燕云", "金国纵深", 7700f, 250f, 12600f, 2200f, 10100f, 1250f, JinRed.copy(alpha = 0.11f)),
    MapWorldRegion("西夏河陇", "西夏", 800f, 1300f, 3900f, 3600f, 2350f, 2450f, Color(0xFFB38A48).copy(alpha = 0.16f)),
    MapWorldRegion("大理西南", "大理", 1450f, 6900f, 3600f, 9550f, 2550f, 8200f, Color(0xFF3F7A4D).copy(alpha = 0.13f)),
    MapWorldRegion("东海海贸", "海贸诸商", 10800f, 6500f, 15400f, 9650f, 13200f, 7900f, Color(0xFF2376C9).copy(alpha = 0.14f))
)

private val TradeRouteIds = setOf(
    "quanzhou", "mingzhou", "guangzhou", "chaozhou", "lianzhou_port", "qiongzhou", "jiaozhi_route", "south_sea", "goryeo_route",
    "xingqing", "lingzhou", "lanzhou", "dali", "shanchan", "chengdu"
)

@Composable
fun MapScreen(gameState: GameState, onCitySelected: (String) -> Unit = {}) {
    var cameraX by remember { mutableStateOf(986f) }
    var cameraY by remember { mutableStateOf(2500f) }
    var zoom by remember { mutableStateOf(0.07f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var selectedRegion by remember { mutableStateOf<MapFocusRegion?>(null) }
    var manageCityId by remember { mutableStateOf<String?>(null) }
    var recruitCityId by remember { mutableStateOf<String?>(null) }
    var activeLayer by remember { mutableStateOf(MapLayerMode.MILITARY) }
    var weatherPhase by remember { mutableStateOf(0f) }
    val cityMap = gameState.cities.associateBy { it.id }
    val armiesByCity = gameState.armies.groupBy { it.currentCityId }
    val officerNames = gameState.officers.associate { it.id to it.name }

    LaunchedEffect(Unit) {
        while (true) {
            weatherPhase = (weatherPhase + 0.012f) % 1f
            delay(33L)
        }
    }

    fun focus(region: MapFocusRegion) {
        cameraX = region.camX
        cameraY = region.camY
        zoom = region.zoom
        selectedId = null
        selectedRegion = region
    }

    Box(modifier = Modifier.fillMaxSize().background(MapBg)) {
        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.018f, 0.14f)
                        cameraX = (cameraX - pan.x / zoom).coerceIn(0f, 14000f)
                        cameraY = (cameraY - pan.y / zoom).coerceIn(0f, 8000f)
                    }
                }
                .pointerInput(activeLayer) {
                    detectTapGestures { tap ->
                        val wx = tap.x / zoom + cameraX
                        val wy = tap.y / zoom + cameraY
                        val nearest = MapData.nodes.minByOrNull { n -> (n.worldX - wx).pow(2) + (n.worldY - wy).pow(2) }
                        nearest?.let { n ->
                            val sx = (n.worldX - cameraX) * zoom
                            val sy = (n.worldY - cameraY) * zoom
                            val dist = sqrt((tap.x - sx).pow(2) + (tap.y - sy).pow(2))
                            selectedId = if (dist < 62f) {
                                selectedRegion = null
                                if (selectedId == n.id) null else n.id
                            } else null
                        }
                    }
                }
        ) {
            drawRect(Color(0xFF10190F))
            drawWorldBase(cameraX, cameraY, zoom)
            drawWorldRegions(cameraX, cameraY, zoom, activeLayer)
            if (activeLayer == MapLayerMode.MILITARY || activeLayer == MapLayerMode.DIPLOMACY) drawFactionRegions(cameraX, cameraY, zoom, activeLayer)
            drawMajorRivers(cameraX, cameraY, zoom)
            if (activeLayer != MapLayerMode.TRADE) drawMountainRanges(cameraX, cameraY, zoom)
            drawMapGrid(cameraX, cameraY, zoom)
            drawRoads(cameraX, cameraY, zoom, activeLayer)
            if (activeLayer == MapLayerMode.TRADE) drawTradeRoutes(cameraX, cameraY, zoom)
            if (activeLayer == MapLayerMode.MILITARY) drawArmyRoutes(gameState, cameraX, cameraY, zoom)
            if (activeLayer == MapLayerMode.ECONOMY) drawEconomicCityHalos(gameState, cameraX, cameraY, zoom)
            drawRegionLabels(cameraX, cameraY, zoom, activeLayer)
            if (activeLayer == MapLayerMode.MILITARY || activeLayer == MapLayerMode.ECONOMY) drawTerrainLabels(cameraX, cameraY, zoom)

            MapData.nodes.forEach { node ->
                val city = cityMap[node.id]
                val screen = w2s(node.worldX, node.worldY, cameraX, cameraY, zoom)
                val isJin = city?.owner == "jin" || (city == null && node.ownerHint == "jin")
                val isFrontline = city?.controlState == "FRONTLINE" || city?.controlState == "CONTESTED"
                val isSel = selectedId == node.id
                val r = ((if (node.isCapital) 15f else 10f) * zoom * 35f).coerceIn(7f, 25f)
                val mainColor = nodeColor(node, city, activeLayer)
                if (activeLayer == MapLayerMode.MILITARY && isFrontline) drawFrontlineGlow(screen, r, isJin)
                if (isSel) {
                    drawCircle(ImperialGold.copy(alpha = 0.34f), r * 2.9f, screen)
                    drawCircle(ImperialGold.copy(alpha = 0.55f), r * 2.1f, screen, style = Stroke(width = 2f))
                }
                if (activeLayer == MapLayerMode.MILITARY) {
                    city?.troops?.let { t ->
                        if (t > 5000) drawCircle((if (isJin) JinRed else SongBright).copy(alpha = 0.22f), r + (t / 60000f * 12f).coerceIn(3f, 13f), screen)
                    }
                }
                if (activeLayer == MapLayerMode.TRADE && (node.id in TradeRouteIds || node.nodeType == "trade" || city?.terrain == "coast")) {
                    drawCircle(Color(0xFF68D7FF).copy(alpha = 0.20f), r * 2.1f, screen)
                    drawCircle(Color(0xFF68D7FF).copy(alpha = 0.50f), r * 1.55f, screen, style = Stroke(width = 1.5f))
                }
                drawCityIcon(screen, r, mainColor, node.isCapital, isJin, isFrontline && activeLayer == MapLayerMode.MILITARY)
                drawCityName(node.name, screen, r, zoom)
            }

            if (activeLayer == MapLayerMode.MILITARY) drawArmyFlags(gameState, cameraX, cameraY, zoom)
            drawWeatherMotion(gameState.weather.label, weatherPhase)
        }

        MapStatusChip(gameState, activeLayer, Modifier.align(Alignment.TopStart).padding(12.dp))
        RegionQuickBar(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 58.dp, start = 8.dp, end = 8.dp),
            onFocus = ::focus
        )
        MapLayerBar(
            activeLayer = activeLayer,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 104.dp, start = 8.dp, end = 8.dp),
            onLayer = { layer ->
                activeLayer = layer
                selectedRegion = null
            }
        )
        MapLayerHint(activeLayer, Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp))
        MapLegend(activeLayer, Modifier.align(Alignment.BottomEnd).padding(end = 12.dp, bottom = 80.dp))

        selectedRegion?.let { region ->
            RegionIntelPanel(
                region = region,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 12.dp, end = 112.dp, bottom = 82.dp),
                onDismiss = { selectedRegion = null }
            )
        }

        selectedId?.let { id ->
            MapData.nodeMap[id]?.let { node ->
                CityDetailPanel(
                    node = node,
                    city = cityMap[id],
                    armies = armiesByCity[id].orEmpty().sortedByDescending { it.troops },
                    officerNames = officerNames,
                    layer = activeLayer,
                    onDismiss = { selectedId = null },
                    onDraft = { action ->
                        if (action == "manage") manageCityId = id
                        else if (action == "recruit") recruitCityId = id
                        else onCitySelected("$id|$action")
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        manageCityId?.let { mid ->
            cityMap[mid]?.let { mCity ->
                Dialog(onDismissRequest = { manageCityId = null }) {
                    CityManagePanel(
                        city = mCity,
                        onBuild = { buildingId -> onCitySelected("$mid|build:$buildingId") },
                        onDismiss = { manageCityId = null }
                    )
                }
            }
        }

        recruitCityId?.let { rid ->
            cityMap[rid]?.let { rCity ->
                Dialog(onDismissRequest = { recruitCityId = null }) {
                    RecruitPanel(
                        city = rCity,
                        onRecruit = { unitId -> onCitySelected("$rid|recruit:$unitId") },
                        onDismiss = { recruitCityId = null }
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionQuickBar(modifier: Modifier = Modifier, onFocus: (MapFocusRegion) -> Unit) {
    Row(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FocusRegions.forEach { region ->
            OutlinedButton(
                onClick = { onFocus(region) },
                border = BorderStroke(1.dp, region.color.copy(alpha = 0.70f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xCC0A0D0F), contentColor = region.color),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(region.name, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MapLayerBar(activeLayer: MapLayerMode, modifier: Modifier = Modifier, onLayer: (MapLayerMode) -> Unit) {
    Row(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        MapLayerMode.values().forEach { layer ->
            val selected = activeLayer == layer
            val color = layerColor(layer)
            OutlinedButton(
                onClick = { onLayer(layer) },
                border = BorderStroke(1.dp, color.copy(alpha = if (selected) 0.92f else 0.42f)),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) color.copy(alpha = 0.22f) else Color(0xCC0A0D0F), contentColor = color),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) { Text(layer.label, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun MapLayerHint(activeLayer: MapLayerMode, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xD9090806)), border = BorderStroke(1.dp, layerColor(activeLayer).copy(alpha = 0.38f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp), horizontalAlignment = Alignment.End) {
            Text("${activeLayer.label}图层", color = layerColor(activeLayer), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(activeLayer.desc, color = Color(0xFFC6A96C), fontSize = 9.sp)
        }
    }
}

@Composable
private fun RegionIntelPanel(region: MapFocusRegion, modifier: Modifier = Modifier, onDismiss: () -> Unit) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xEE0A0D0F)), border = BorderStroke(1.dp, region.color.copy(alpha = 0.70f)), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(region.name, color = region.color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text(region.power, color = Color(0xFFD7C08A), fontSize = 10.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) { Text("X", color = Color(0xFF8B7355), fontSize = 12.sp) }
            }
            Text(region.summary, color = Color(0xFFE8DCC0), fontSize = 11.sp, lineHeight = 16.sp)
            Text("战略：${region.strategy}", color = Color(0xFFC6A96C), fontSize = 10.sp, lineHeight = 15.sp)
            Text("危险度：${region.danger}", color = if (region.danger.contains("高")) Color(0xFFFF8A70) else Color(0xFFB9AA82), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun MapStatusChip(gameState: GameState, layer: MapLayerMode, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xD9090806)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(gameState.calendar.displayText(), color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${gameState.season.label} · ${gameState.weather.label} · V1.9 · ${layer.label}", color = Color(0xFFC6A96C), fontSize = 10.sp)
        }
    }
}

@Composable
private fun MapLegend(layer: MapLayerMode, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color(0xDD0A0D0F), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        when (layer) {
            MapLayerMode.MILITARY -> {
                LegendRow(SongBright, "■", "宋控")
                LegendRow(JinRed, "■", "金占")
                LegendRow(ImperialGold, "⚑", "军团")
                LegendRow(Color(0xFF8B2500), "━", "关隘")
            }
            MapLayerMode.ECONOMY -> {
                LegendRow(Color(0xFF7BD88F), "◎", "民心/农业")
                LegendRow(Color(0xFFE8C35A), "◎", "商贸/税源")
                LegendRow(Color(0xFF68B7E8), "━", "漕运水路")
                LegendRow(Color(0xFFD0A66A), "━", "陆路转运")
            }
            MapLayerMode.DIPLOMACY -> {
                LegendRow(SongBright, "■", "南宋")
                LegendRow(JinRed, "■", "金国")
                LegendRow(Color(0xFFB38A48), "■", "西夏")
                LegendRow(Color(0xFF3F7A4D), "■", "大理")
                LegendRow(Color(0xFF2376C9), "■", "海贸/高丽")
            }
            MapLayerMode.TRADE -> {
                LegendRow(Color(0xFF68D7FF), "◎", "港市/海路")
                LegendRow(Color(0xFFB38A48), "━", "马市/边贸")
                LegendRow(Color(0xFF3F7A4D), "━", "西南商路")
                LegendRow(Color(0xFF2376C9), "━", "外贸航线")
            }
        }
    }
}

private fun DrawScope.drawWorldBase(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(0f, 0f, 16000f, 10000f, Color(0xFF152417), camX, camY, zoom)
    drawWorldRect(0f, 0f, 16000f, 10000f, Color.Black.copy(alpha = 0.18f), camX, camY, zoom)
}

private fun DrawScope.drawWorldRegions(camX: Float, camY: Float, zoom: Float, layer: MapLayerMode) {
    WorldRegions.forEach { r ->
        val alphaBoost = when (layer) {
            MapLayerMode.DIPLOMACY -> 1.45f
            MapLayerMode.TRADE -> if (r.faction.contains("海贸") || r.faction.contains("西夏") || r.faction.contains("大理")) 1.55f else 0.55f
            MapLayerMode.ECONOMY -> if (r.name.contains("江南") || r.name.contains("海贸")) 1.15f else 0.65f
            MapLayerMode.MILITARY -> 1f
        }
        drawWorldRect(r.left, r.top, r.right, r.bottom, r.color.copy(alpha = (r.color.alpha * alphaBoost).coerceIn(0.04f, 0.28f)), camX, camY, zoom)
        drawWorldRect(r.left, r.top, r.right, r.bottom, Color.Black.copy(alpha = 0.08f), camX, camY, zoom)
    }
}

private fun DrawScope.drawRegionLabels(camX: Float, camY: Float, zoom: Float, layer: MapLayerMode) {
    WorldRegions.forEach { region ->
        if (layer == MapLayerMode.TRADE && !(region.faction.contains("海贸") || region.faction.contains("西夏") || region.faction.contains("大理"))) return@forEach
        val s = w2s(region.labelX, region.labelY, camX, camY, zoom)
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(230, 225, 202, 130)
                textSize = (18f * zoom * 33f).coerceIn(15f, 36f)
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                isFakeBoldText = true
                setShadowLayer(6f, 0f, 2f, android.graphics.Color.argb(240, 0, 0, 0))
            }
            canvas.nativeCanvas.drawText(region.name, s.x, s.y, p)
            p.textSize = (9f * zoom * 33f).coerceIn(10f, 18f)
            p.color = android.graphics.Color.argb(210, 190, 172, 116)
            canvas.nativeCanvas.drawText(region.faction, s.x, s.y + p.textSize + 6f, p)
        }
    }
}

private fun DrawScope.drawFactionRegions(camX: Float, camY: Float, zoom: Float, layer: MapLayerMode) {
    val alpha = if (layer == MapLayerMode.DIPLOMACY) 0.22f else 0.12f
    drawWorldRect(6900f, 1850f, 10500f, 4450f, JinRed.copy(alpha = alpha), camX, camY, zoom)
    drawWorldRect(3300f, 5100f, 13400f, 9050f, SongBright.copy(alpha = if (layer == MapLayerMode.DIPLOMACY) 0.16f else 0.10f), camX, camY, zoom)
    drawWorldRect(7100f, 5000f, 11100f, 6200f, Color(0xFF7A3B3B).copy(alpha = if (layer == MapLayerMode.DIPLOMACY) 0.16f else 0.10f), camX, camY, zoom)
    if (layer == MapLayerMode.DIPLOMACY) {
        drawWorldRect(800f, 1300f, 3900f, 3600f, Color(0xFFB38A48).copy(alpha = 0.20f), camX, camY, zoom)
        drawWorldRect(1450f, 6900f, 3600f, 9550f, Color(0xFF3F7A4D).copy(alpha = 0.18f), camX, camY, zoom)
        drawWorldRect(10800f, 6500f, 15400f, 9650f, Color(0xFF2376C9).copy(alpha = 0.18f), camX, camY, zoom)
    }
}

private fun DrawScope.drawMajorRivers(camX: Float, camY: Float, zoom: Float) {
    TerrainFeatures.rivers.forEach { river ->
        val pts = river.points.map { Offset(it.first, it.second) }
        val color = when (river.name) {
            "黄河" -> Color(0xFFB8995A).copy(alpha = 0.75f)
            "长江" -> Color(0xFF2D9CDB).copy(alpha = 0.78f)
            else -> Color(0xFF5BC0EB).copy(alpha = 0.62f)
        }
        val width = if (river.name == "长江" || river.name == "黄河") 9f else 5.5f
        drawWorldPath(pts, color, width, camX, camY, zoom)
    }
}

private fun DrawScope.drawMountainRanges(camX: Float, camY: Float, zoom: Float) {
    TerrainFeatures.mountains.forEach { mtn ->
        val pts = mtn.points.map { Offset(it.first, it.second) }
        drawWorldPath(pts, Color(0xFF7A5C38).copy(alpha = 0.68f), 7f, camX, camY, zoom)
        pts.forEach { p ->
            val s = w2s(p.x, p.y, camX, camY, zoom)
            val sz = (10f * zoom * 35f).coerceIn(4f, 12f)
            val peak = Path().apply {
                moveTo(s.x, s.y - sz)
                lineTo(s.x - sz * 0.7f, s.y + sz * 0.5f)
                lineTo(s.x + sz * 0.7f, s.y + sz * 0.5f)
                close()
            }
            drawPath(peak, Color(0xFF8B6B43).copy(alpha = 0.7f))
            drawPath(peak, Color(0xFF5A4329).copy(alpha = 0.8f), style = Stroke(width = 1f))
        }
    }
}

private fun DrawScope.drawTerrainLabels(camX: Float, camY: Float, zoom: Float) {
    if (zoom * 38f <= 0.7f) return
    TerrainFeatures.all.forEach { feature ->
        val s = w2s(feature.labelX, feature.labelY, camX, camY, zoom)
        drawIntoCanvas { canvas ->
            val p = android.graphics.Paint().apply {
                color = if (feature.kind == "river") android.graphics.Color.argb(220, 150, 200, 235) else android.graphics.Color.argb(220, 200, 170, 130)
                textSize = (11f * zoom * 38f).coerceIn(13f, 30f)
                isAntiAlias = true
                textAlign = android.graphics.Paint.Align.CENTER
                setShadowLayer(5f, 0f, 2f, android.graphics.Color.argb(230, 0, 0, 0))
                isFakeBoldText = true
            }
            canvas.nativeCanvas.drawText(feature.name, s.x, s.y, p)
        }
    }
}

private fun DrawScope.drawRoads(camX: Float, camY: Float, zoom: Float, layer: MapLayerMode) {
    MapData.roads.forEach { road ->
        val show = when (layer) {
            MapLayerMode.MILITARY -> true
            MapLayerMode.ECONOMY -> road.type == RoadType.RIVER || road.type == RoadType.CANAL || road.type == RoadType.LAND
            MapLayerMode.DIPLOMACY -> road.type == RoadType.LAND || road.type == RoadType.PASS || road.type == RoadType.SEA
            MapLayerMode.TRADE -> road.type == RoadType.SEA || road.type == RoadType.CANAL || road.type == RoadType.RIVER || road.involvesTradeNode()
        }
        if (!show) return@forEach
        val from = MapData.nodeMap[road.fromId] ?: return@forEach
        val to = MapData.nodeMap[road.toId] ?: return@forEach
        val fs = w2s(from.worldX, from.worldY, camX, camY, zoom)
        val ts = w2s(to.worldX, to.worldY, camX, camY, zoom)
        val sw = (2.7f * zoom * 38f).coerceIn(1.2f, 7f)
        val pair = when (road.type) {
            RoadType.RIVER -> Color(0xFF68B7E8) to null
            RoadType.CANAL -> Color(0xFF9DD7EA) to PathEffect.dashPathEffect(floatArrayOf(12f, 6f))
            RoadType.SEA -> Color(0xFF2376C9) to PathEffect.dashPathEffect(floatArrayOf(16f, 8f))
            RoadType.MOUNTAIN -> Color(0xFFB08A54) to PathEffect.dashPathEffect(floatArrayOf(5f, 9f))
            RoadType.LAND -> Color(0xFFD0A66A) to null
            RoadType.PASS -> Color(0xFF8B2500) to null
        }
        val layerColor = if (layer == MapLayerMode.TRADE && road.involvesTradeNode()) Color(0xFF68D7FF) else pair.first
        drawLine(Color.Black.copy(alpha = 0.35f), fs, ts, sw + 2f, pathEffect = pair.second, cap = StrokeCap.Round)
        drawLine(layerColor, fs, ts, sw, pathEffect = pair.second, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawTradeRoutes(camX: Float, camY: Float, zoom: Float) {
    val routes = listOf(
        listOf("mingzhou", "goryeo_route"),
        listOf("quanzhou", "south_sea"),
        listOf("guangzhou", "south_sea"),
        listOf("guangzhou", "jiaozhi_route"),
        listOf("chengdu", "dali", "shanchan"),
        listOf("lanzhou", "lingzhou", "xingqing")
    )
    routes.forEach { ids ->
        ids.zipWithNext().forEach { (a, b) ->
            val from = MapData.nodeMap[a] ?: return@forEach
            val to = MapData.nodeMap[b] ?: return@forEach
            val fs = w2s(from.worldX, from.worldY, camX, camY, zoom)
            val ts = w2s(to.worldX, to.worldY, camX, camY, zoom)
            drawLine(Color.Black.copy(alpha = 0.48f), fs, ts, 7f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f)), cap = StrokeCap.Round)
            drawLine(Color(0xFF68D7FF).copy(alpha = 0.78f), fs, ts, 3.2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 10f)), cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawEconomicCityHalos(gameState: GameState, camX: Float, camY: Float, zoom: Float) {
    gameState.cities.filter { it.owner == "song" }.forEach { city ->
        val node = MapData.nodeMap[city.id] ?: return@forEach
        val screen = w2s(node.worldX, node.worldY, camX, camY, zoom)
        val power = ((city.commerce + city.agriculture + city.popularSupport) / 3f).coerceIn(0f, 100f)
        val color = if (city.terrain == "coast" || city.isWaterNode) Color(0xFF68D7FF) else if (city.commerce >= city.agriculture) Color(0xFFE8C35A) else Color(0xFF7BD88F)
        drawCircle(color.copy(alpha = 0.10f + power / 500f), (18f + power / 3f) * zoom.coerceAtLeast(0.055f), screen)
        drawCircle(color.copy(alpha = 0.45f), (9f + power / 7f) * zoom.coerceAtLeast(0.055f), screen, style = Stroke(width = 1.4f))
    }
}

private fun DrawScope.drawArmyRoutes(gameState: GameState, camX: Float, camY: Float, zoom: Float) {
    gameState.armies.filter { it.troops > 0 && it.currentCityId.isNotBlank() && it.targetCityId.isNotBlank() }.forEach { army ->
        val from = MapData.nodeMap[army.currentCityId] ?: return@forEach
        val to = MapData.nodeMap[army.targetCityId] ?: return@forEach
        val start = w2s(from.worldX, from.worldY, camX, camY, zoom)
        val end = w2s(to.worldX, to.worldY, camX, camY, zoom)
        drawLine(Color.Black.copy(alpha = 0.45f), start, end, 5f, cap = StrokeCap.Round)
        drawLine((if (army.ownerFactionId == "jin") JinRed else ImperialGold).copy(alpha = 0.72f), start, end, 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f)), cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawArmyFlags(gameState: GameState, camX: Float, camY: Float, zoom: Float) {
    val slots = mutableMapOf<String, Int>()
    gameState.armies.filter { it.troops > 0 }.forEach { army ->
        val node = MapData.nodeMap[army.currentCityId] ?: return@forEach
        val base = w2s(node.worldX, node.worldY, camX, camY, zoom)
        val slotKey = "${(base.x / 44f).toInt()}_${(base.y / 44f).toInt()}"
        val slot = slots.getOrDefault(slotKey, 0)
        slots[slotKey] = slot + 1
        val color = if (army.ownerFactionId == "jin") JinRed else SongBright
        val offset = Offset(base.x + 22f + slot * 12f, base.y - 36f)
        drawArmyFlag(offset, if (army.ownerFactionId == "jin") "金" else army.name.take(1), color, "${army.troops / 1000}k")
    }
}

private fun DrawScope.drawArmyFlag(origin: Offset, label: String, color: Color, subLabel: String) {
    drawLine(Color(0xFF1A1208), origin, Offset(origin.x, origin.y + 28f), strokeWidth = 2.4f, cap = StrokeCap.Round)
    val banner = Path().apply {
        moveTo(origin.x, origin.y)
        lineTo(origin.x + 26f, origin.y + 5f)
        lineTo(origin.x + 19f, origin.y + 16f)
        lineTo(origin.x, origin.y + 13f)
        close()
    }
    drawPath(banner, color)
    drawPath(banner, Color.Black.copy(alpha = 0.52f), style = Stroke(width = 1.1f))
    drawTextOnCanvas(label, Offset(origin.x + 13f, origin.y + 12f), 10.5f, android.graphics.Color.WHITE, true)
    drawTextOnCanvas(subLabel, Offset(origin.x + 14f, origin.y + 38f), 8f, android.graphics.Color.WHITE, true)
}

private fun DrawScope.drawWeatherMotion(label: String, phase: Float) {
    val w = size.width.coerceAtLeast(1f)
    val h = size.height.coerceAtLeast(1f)
    when {
        label.contains("雨") -> {
            drawRect(Color(0xFF14304A).copy(alpha = 0.10f))
            for (i in 0 until 68) {
                val x = (i * 47f + phase * w * 0.70f) % w
                val y = (i * 83f + phase * h * 1.20f) % h
                drawLine(Color(0xFF8AC8FF).copy(alpha = 0.34f), Offset(x, y), Offset(x - 9f, y + 24f), 1.4f)
            }
        }
        label.contains("雪") || label.contains("寒") -> {
            drawRect(Color(0xFFEAF6FF).copy(alpha = 0.05f))
            for (i in 0 until 80) {
                val x = (i * 61f + phase * w * 0.22f) % w
                val y = (i * 97f + phase * h * 0.62f) % h
                drawCircle(Color.White.copy(alpha = 0.35f), if (i % 3 == 0) 2.1f else 1.2f, Offset(x, y))
            }
        }
        label.contains("雾") || label.contains("阴") -> {
            drawRect(Color(0xFFD6D6C8).copy(alpha = 0.07f))
            for (i in 0 until 7) {
                val x = ((i * 157f + phase * w * 0.16f) % (w + 220f)) - 110f
                val y = h * (0.18f + i * 0.10f)
                drawCircle(Color.White.copy(alpha = 0.055f), 130f, Offset(x, y))
            }
        }
        else -> {
            for (i in 0 until 30) {
                val x = (i * 101f + phase * w * 0.32f) % w
                val y = h - ((i * 73f + phase * h * 0.44f) % h)
                drawCircle(ImperialGold.copy(alpha = 0.16f), if (i % 4 == 0) 2.5f else 1.3f, Offset(x, y))
            }
        }
    }
}

private fun DrawScope.drawCityIcon(center: Offset, r: Float, color: Color, isCapital: Boolean, isJin: Boolean, isFrontline: Boolean) {
    drawRect(Color.Black.copy(alpha = 0.45f), Offset(center.x - r * 1.18f + 2f, center.y - r * 0.52f + 3f), Size(r * 2.36f, r * 1.22f))
    drawRect(color.copy(alpha = 0.95f), Offset(center.x - r * 1.18f, center.y - r * 0.52f), Size(r * 2.36f, r * 1.22f))
    drawRect(Color(0xFF0A0907), Offset(center.x - r * 0.30f, center.y + r * 0.05f), Size(r * 0.60f, r * 0.65f))
    val roof = Path().apply {
        moveTo(center.x - r * 1.35f, center.y - r * 0.52f)
        lineTo(center.x - r * 0.72f, center.y - r * 1.08f)
        lineTo(center.x, center.y - r * 0.62f)
        lineTo(center.x + r * 0.72f, center.y - r * 1.08f)
        lineTo(center.x + r * 1.35f, center.y - r * 0.52f)
    }
    drawPath(roof, if (isJin) Color(0xFF5A1111) else Color(0xFF092B44), style = Stroke(width = 2.0f, cap = StrokeCap.Round))
    drawRect(Color(0xFF050504), Offset(center.x - r * 1.28f, center.y - r * 0.62f), Size(r * 2.56f, r * 1.42f), style = Stroke(width = 1.4f))
    if (isCapital) {
        drawCircle(ImperialGold.copy(alpha = 0.35f), r * 2.05f, center)
        drawCircle(ImperialGold.copy(alpha = 0.80f), r * 1.42f, center, style = Stroke(width = 1.5f))
    }
    if (isFrontline) drawLine(ImperialGold.copy(alpha = 0.75f), Offset(center.x - r * 1.12f, center.y + r * 0.92f), Offset(center.x + r * 1.12f, center.y + r * 0.92f), 1.7f)
}

private fun DrawScope.drawCityName(name: String, screen: Offset, r: Float, zoom: Float) {
    val textScale = zoom * 38f
    if (textScale <= 0.55f) return
    drawTextOnCanvas(name, Offset(screen.x, screen.y + r + (9.5f * textScale).coerceIn(10f, 26f) + 5f), (9.5f * textScale).coerceIn(10f, 26f), android.graphics.Color.WHITE, false)
}

private fun DrawScope.drawMapGrid(camX: Float, camY: Float, zoom: Float) {
    val gridWorld = 2000f
    val gridColor = Color(0xFF233029).copy(alpha = 0.35f)
    val startX = (camX / gridWorld).toInt() * gridWorld
    val startY = (camY / gridWorld).toInt() * gridWorld
    var wx = startX
    while (wx < camX + size.width / zoom + gridWorld) {
        val sx = (wx - camX) * zoom
        drawLine(gridColor, Offset(sx, 0f), Offset(sx, size.height), 0.5f)
        wx += gridWorld
    }
    var wy = startY
    while (wy < camY + size.height / zoom + gridWorld) {
        val sy = (wy - camY) * zoom
        drawLine(gridColor, Offset(0f, sy), Offset(size.width, sy), 0.5f)
        wy += gridWorld
    }
}

private fun DrawScope.drawWorldRect(left: Float, top: Float, right: Float, bottom: Float, color: Color, camX: Float, camY: Float, zoom: Float) {
    val a = w2s(left, top, camX, camY, zoom)
    val b = w2s(right, bottom, camX, camY, zoom)
    drawRect(color, topLeft = a, size = Size(b.x - a.x, b.y - a.y))
}

private fun DrawScope.drawWorldPath(points: List<Offset>, color: Color, baseWidth: Float, camX: Float, camY: Float, zoom: Float) {
    if (points.size < 2) return
    val path = Path()
    val first = w2s(points.first().x, points.first().y, camX, camY, zoom)
    path.moveTo(first.x, first.y)
    points.drop(1).forEach { p ->
        val sp = w2s(p.x, p.y, camX, camY, zoom)
        path.lineTo(sp.x, sp.y)
    }
    val width = (baseWidth * zoom * 35f).coerceIn(2f, 14f)
    drawPath(path, Color.Black.copy(alpha = 0.30f), style = Stroke(width = width + 3f, cap = StrokeCap.Round))
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round))
}

private fun DrawScope.drawFrontlineGlow(center: Offset, r: Float, isJin: Boolean) {
    val glow = if (isJin) JinRed else ImperialGold
    drawCircle(glow.copy(alpha = 0.20f), r * 2.4f, center)
    drawCircle(glow.copy(alpha = 0.42f), r * 1.8f, center, style = Stroke(width = 1.6f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 5f))))
}

private fun DrawScope.drawTextOnCanvas(text: String, center: Offset, textSize: Float, color: Int, bold: Boolean) {
    drawIntoCanvas { canvas ->
        val p = android.graphics.Paint().apply {
            this.color = color
            this.textSize = textSize
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = bold
            setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(210, 0, 0, 0))
        }
        canvas.nativeCanvas.drawText(text, center.x, center.y, p)
    }
}

private fun w2s(wx: Float, wy: Float, camX: Float, camY: Float, zoom: Float) = Offset((wx - camX) * zoom, (wy - camY) * zoom)

private fun nodeColor(node: MapNode, city: City?, layer: MapLayerMode): Color = when (layer) {
    MapLayerMode.MILITARY -> when {
        city?.owner == "jin" || node.ownerHint == "jin" -> Color(0xFFE24A4A)
        node.isCapital -> ImperialGold
        city?.owner == "song" || node.ownerHint == "song" -> Color(0xFF4DA3E6)
        else -> ownerHintColor(node.ownerHint)
    }
    MapLayerMode.ECONOMY -> when {
        city == null -> ownerHintColor(node.ownerHint).copy(alpha = 0.78f)
        city.terrain == "coast" || city.isWaterNode -> Color(0xFF68D7FF)
        city.commerce >= city.agriculture -> Color(0xFFE8C35A)
        else -> Color(0xFF7BD88F)
    }
    MapLayerMode.DIPLOMACY -> ownerHintColor(city?.owner ?: node.ownerHint)
    MapLayerMode.TRADE -> when {
        node.id in TradeRouteIds || node.nodeType == "trade" -> Color(0xFF68D7FF)
        city?.terrain == "coast" || city?.isWaterNode == true -> Color(0xFF68D7FF)
        node.ownerHint == "xixia" -> Color(0xFFB38A48)
        node.ownerHint == "dali" -> Color(0xFF3F7A4D)
        else -> ownerHintColor(city?.owner ?: node.ownerHint).copy(alpha = 0.62f)
    }
}

private fun ownerHintColor(owner: String): Color = when (owner) {
    "song" -> SongBright
    "jin" -> JinRed
    "xixia" -> Color(0xFFB38A48)
    "dali" -> Color(0xFF3F7A4D)
    "goryeo" -> Color(0xFF7AA7D9)
    "sea_trade" -> Color(0xFF2376C9)
    else -> Color(0xFF7A8794)
}

private fun layerColor(layer: MapLayerMode): Color = when (layer) {
    MapLayerMode.MILITARY -> ImperialGold
    MapLayerMode.ECONOMY -> Color(0xFFE8C35A)
    MapLayerMode.DIPLOMACY -> Color(0xFFBFA3FF)
    MapLayerMode.TRADE -> Color(0xFF68D7FF)
}

private fun City?.ownerLabel(node: MapNode): String = when {
    this != null && owner == "jin" -> "金国占领 · $controlState"
    this != null && owner == "song" && node.isCapital -> "大宋京城 · $controlState"
    this != null && owner == "song" -> "大宋控制 · $controlState"
    node.ownerHint == "xixia" -> "西夏势力 · 战略节点"
    node.ownerHint == "dali" -> "大理势力 · 战略节点"
    node.ownerHint == "goryeo" -> "高丽方向 · 海东节点"
    node.ownerHint == "sea_trade" -> "海贸诸商 · 外贸节点"
    node.ownerHint == "jin" -> "金国势力 · 战略节点"
    else -> "未接入城池数值 · 战略节点"
}

private fun CityRoadLike(a: String, b: String) = a in TradeRouteIds || b in TradeRouteIds

private fun com.xiemingxin.nandu.game.CityRoad.involvesTradeNode(): Boolean = CityRoadLike(fromId, toId)

@Composable
private fun LegendRow(color: Color, symbol: String, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(symbol, color = color, fontSize = 11.sp)
        Text(label, color = Color(0xFFB9AA82), fontSize = 10.sp)
    }
}

@Composable
fun CityDetailPanel(node: MapNode, city: City?, armies: List<Army>, officerNames: Map<String, String>, layer: MapLayerMode = MapLayerMode.MILITARY, onDismiss: () -> Unit, modifier: Modifier = Modifier, onDraft: (String) -> Unit = {}) {
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xF01A1208)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.60f))) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (city != null) {
                AssetImage(path = ArtResourceRegistry.cityBackground(city.id), fallbackPath = ArtResourceRegistry.Fallback.city, contentDescription = node.name, contentScale = ContentScale.Crop, placeholderText = "城", modifier = Modifier.fillMaxWidth().height(120.dp))
                Spacer(Modifier.height(10.dp))
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(node.name, color = nodeColor(node, city, layer), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(city.ownerLabel(node), color = Color(0xFFB9AA82), fontSize = 12.sp)
                    if (city != null && city.route.isNotBlank()) Text("${city.route} · ${city.cityLevel} · ${terrainLabel(city.terrain)} · 口${city.population / 10000}万", color = Color(0xFF8C7A60), fontSize = 10.sp)
                    if (city == null) Text("${node.ownerHint.ifBlank { "world" }} · ${node.nodeType} · 后续可升级为完整城池", color = Color(0xFF8C7A60), fontSize = 10.sp)
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Text("X", color = Color(0xFF8B7355), fontSize = 14.sp) }
            }
            if (city != null) {
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    CityStatItem("兵", "兵力", "${city.troops / 1000}k")
                    CityStatItem("防", "城防", "${city.defense}")
                    CityStatItem("粮", "粮草", "${city.grain / 1000}k")
                    CityStatItem("财", "金库", "${city.gold / 1000}k")
                    CityStatItem("民", "民心", "${city.popularSupport}")
                }
                Spacer(Modifier.height(10.dp))
                Text(if (layer == MapLayerMode.ECONOMY) "经济民生" else "驻防军团", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                if (layer == MapLayerMode.ECONOMY) {
                    Text("商业${city.commerce} · 农业${city.agriculture} · 沿线${city.route}", color = Color(0xFFB9AA82), fontSize = 11.sp)
                } else if (armies.isEmpty()) Text("此城暂无正式军团驻防。", color = Color(0xFF8C7A60), fontSize = 11.sp)
                else armies.take(4).forEach { army -> CityArmyRow(army, officerNames[army.commanderId] ?: army.status) }
                Spacer(Modifier.height(12.dp))
                Text("拟旨操作", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                    CityActionButton("修城", Modifier.weight(1f)) { onDraft("repair") }
                    CityActionButton("调兵", Modifier.weight(1f)) { onDraft("dispatch") }
                    CityActionButton("筹粮", Modifier.weight(1f)) { onDraft("grain") }
                    CityActionButton(if (city.owner == "jin") "进取" else "备战", Modifier.weight(1f)) { onDraft(if (city.owner == "jin") "siege" else "attack") }
                }
                Spacer(Modifier.height(7.dp))
                CityActionButton("◈ 进 入 城 池 ◈", Modifier.fillMaxWidth()) { onDraft("enter") }
                if (city.owner == "song") {
                    Spacer(Modifier.height(7.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                        CityActionButton("营建城池", Modifier.weight(1f)) { onDraft("manage") }
                        CityActionButton("募兵练军", Modifier.weight(1f)) { onDraft("recruit") }
                    }
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text("此节点目前用于外交、外贸或战略显示；完整驻军、民心、经营数据会在后续城池扩容中接入。", color = Color(0xFFB9AA82), fontSize = 11.sp, lineHeight = 16.sp)
            }
        }
    }
}

@Composable
private fun CityArmyRow(army: Army, commander: String) {
    val color = if (army.ownerFactionId == "jin") JinRed else SongBright
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1D150B), RoundedCornerShape(7.dp)).padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(army.name, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${army.troops / 1000}k", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        val movement = if (army.status.contains("进军") && army.targetCityId.isNotBlank()) " · 目标${MapData.nodeMap[army.targetCityId]?.name ?: army.targetCityId} · 余${army.marchDaysRemaining}天" else ""
        Text("统帅：$commander · 士气${army.morale} · ${army.status}$movement", color = Color(0xFFB9AA82), fontSize = 10.sp)
    }
    Spacer(Modifier.height(6.dp))
}

@Composable
private fun CityActionButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(36.dp), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) {
        Text(text, fontSize = 11.sp)
    }
}

private fun terrainLabel(terrain: String): String = when (terrain) { "river" -> "水乡"; "mountain" -> "山地"; "pass" -> "关隘"; "coast" -> "沿海"; else -> "平原" }

@Composable
private fun CityStatItem(icon: String, label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 14.sp, color = ImperialGold)
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Color(0xFF8C7A60), fontSize = 9.sp)
    }
}

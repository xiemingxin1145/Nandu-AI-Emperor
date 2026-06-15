package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import com.xiemingxin.nandu.game.CityVisualRegistry
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
import kotlin.math.pow
import kotlin.math.sqrt

private data class MapFocusRegion(val name: String, val x: Float, val y: Float, val zoom: Float, val color: Color)

private val FocusRegions = listOf(
    MapFocusRegion("全图", 8000f, 5000f, 0.050f, ImperialGold),
    MapFocusRegion("宋境", 7200f, 7350f, 0.082f, SongBright),
    MapFocusRegion("江淮", 8500f, 5600f, 0.102f, ImperialGold),
    MapFocusRegion("荆襄", 5200f, 5200f, 0.100f, Color(0xFFD0A66A)),
    MapFocusRegion("中原", 8400f, 3300f, 0.092f, JinRed),
    MapFocusRegion("西夏", 2350f, 2450f, 0.082f, Color(0xFFB38A48)),
    MapFocusRegion("大理", 2550f, 8200f, 0.090f, Color(0xFF3F7A4D)),
    MapFocusRegion("海贸", 12800f, 7900f, 0.072f, Color(0xFF2376C9))
)

private val TradeRouteIds = setOf(
    "quanzhou", "mingzhou", "guangzhou", "chaozhou", "lianzhou_port", "qiongzhou",
    "jiaozhi_route", "south_sea", "goryeo_route", "xingqing", "lingzhou", "lanzhou",
    "dali", "shanchan", "chengdu"
)

@Composable
fun MapScreen(gameState: GameState, onCitySelected: (String) -> Unit = {}) {
    var cameraX by remember { mutableStateOf(7200f) }
    var cameraY by remember { mutableStateOf(7350f) }
    var zoom by remember { mutableStateOf(0.082f) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var manageCityId by remember { mutableStateOf<String?>(null) }
    var recruitCityId by remember { mutableStateOf<String?>(null) }
    var activeLayer by remember { mutableStateOf(MapLayerMode.MILITARY) }
    val cityMap = gameState.cities.associateBy { it.id }
    val armiesByCity = gameState.armies.groupBy { it.currentCityId }
    val officerNames = gameState.officers.associate { it.id to it.name }

    fun focus(region: MapFocusRegion) {
        cameraX = region.x
        cameraY = region.y
        zoom = region.zoom
        selectedId = null
    }

    Box(modifier = Modifier.fillMaxSize().background(MapBg)) {
        AssetImage(
            path = ArtResourceRegistry.mapBackground(activeLayer),
            fallbackPath = "images/map/song_world_parchment.webp",
            contentDescription = activeLayer.label,
            contentScale = ContentScale.Crop,
            placeholderText = activeLayer.label.take(1),
            modifier = Modifier.fillMaxSize()
        )
        Box(modifier = Modifier.fillMaxSize().background(Color(0x88000000)))

        Canvas(
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, gestureZoom, _ ->
                        zoom = (zoom * gestureZoom).coerceIn(0.026f, 0.16f)
                        cameraX = (cameraX - pan.x / zoom).coerceIn(0f, 16000f)
                        cameraY = (cameraY - pan.y / zoom).coerceIn(0f, 10000f)
                    }
                }
                .pointerInput(activeLayer) {
                    detectTapGestures { tap ->
                        val wx = (tap.x - size.width / 2f) / zoom + cameraX
                        val wy = (tap.y - size.height / 2f) / zoom + cameraY
                        val nearest = MapData.nodes.minByOrNull { n -> (n.worldX - wx).pow(2) + (n.worldY - wy).pow(2) }
                        nearest?.let { n ->
                            val sx = size.width / 2f + (n.worldX - cameraX) * zoom
                            val sy = size.height / 2f + (n.worldY - cameraY) * zoom
                            val dist = sqrt((tap.x - sx).pow(2) + (tap.y - sy).pow(2))
                            selectedId = if (dist < 64f) if (selectedId == n.id) null else n.id else null
                        }
                    }
                }
        ) {
            drawRect(Color(0x33000000))
            drawWorldBase(cameraX, cameraY, zoom)
            drawMapGrid(cameraX, cameraY, zoom)
            drawMajorRivers(cameraX, cameraY, zoom)
            if (activeLayer != MapLayerMode.TRADE) drawMountainRanges(cameraX, cameraY, zoom)
            drawRoads(cameraX, cameraY, zoom, activeLayer)
            if (activeLayer == MapLayerMode.TRADE) drawTradeLines(cameraX, cameraY, zoom)
            if (activeLayer == MapLayerMode.ECONOMY) drawEconomicHalos(gameState, cameraX, cameraY, zoom)
            if (activeLayer == MapLayerMode.MILITARY) drawArmyRoutes(gameState, cameraX, cameraY, zoom)

            MapData.nodes.forEach { node ->
                val city = cityMap[node.id]
                val visual = CityVisualRegistry.visualFor(city, node)
                val screen = w2s(node.worldX, node.worldY, cameraX, cameraY, zoom)
                val isFrontline = city?.controlState == "FRONTLINE" || city?.controlState == "CONTESTED"
                val color = nodeColor(node, city, activeLayer)
                val r = ((if (node.isCapital) 15f else 10f) * visual.tier.mapScale * zoom * 35f).coerceIn(7f, 28f)
                if (activeLayer == MapLayerMode.TRADE && (node.id in TradeRouteIds || node.nodeType == "trade" || city?.terrain == "coast")) drawCircle(Color(0xFF68D7FF).copy(alpha = 0.25f), r * 2.2f, screen)
                if (selectedId == node.id) drawCircle(ImperialGold.copy(alpha = 0.45f), r * 2.2f, screen, style = Stroke(width = 2f))
                drawCityIcon(screen, r, color, visual.iconKey, isFrontline && activeLayer == MapLayerMode.MILITARY)
                drawCityName(node.name, screen, r, zoom)
            }
            if (activeLayer == MapLayerMode.MILITARY) drawArmyFlags(gameState, cameraX, cameraY, zoom)
        }

        MapStatusChip(gameState, activeLayer, Modifier.align(Alignment.TopStart).padding(8.dp))
        CompactFocusBar(Modifier.align(Alignment.TopCenter).padding(top = 50.dp, start = 6.dp, end = 6.dp), ::focus)
        CompactLayerBar(activeLayer, Modifier.align(Alignment.TopCenter).padding(top = 88.dp, start = 6.dp, end = 6.dp)) { activeLayer = it; selectedId = null }
        MapLegend(activeLayer, Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 72.dp))

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
                        if (action == "manage") manageCityId = id else if (action == "recruit") recruitCityId = id else onCitySelected("$id|$action")
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        manageCityId?.let { id -> cityMap[id]?.let { Dialog(onDismissRequest = { manageCityId = null }) { CityManagePanel(it, { buildingId -> onCitySelected("$id|build:$buildingId") }, { manageCityId = null }) } } }
        recruitCityId?.let { id -> cityMap[id]?.let { Dialog(onDismissRequest = { recruitCityId = null }) { RecruitPanel(it, { unitId -> onCitySelected("$id|recruit:$unitId") }, { recruitCityId = null }) } } }
    }
}

@Composable
private fun CompactFocusBar(modifier: Modifier, onFocus: (MapFocusRegion) -> Unit) {
    Row(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        FocusRegions.forEach { r ->
            OutlinedButton(onClick = { onFocus(r) }, border = BorderStroke(1.dp, r.color.copy(alpha = 0.65f)), colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xB80A0D0F), contentColor = r.color), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(30.dp)) {
                Text(r.name, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompactLayerBar(activeLayer: MapLayerMode, modifier: Modifier, onLayer: (MapLayerMode) -> Unit) {
    Row(modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        MapLayerMode.values().forEach { layer ->
            val selected = activeLayer == layer
            val color = layerColor(layer)
            OutlinedButton(onClick = { onLayer(layer) }, border = BorderStroke(1.dp, color.copy(alpha = if (selected) 0.90f else 0.40f)), colors = ButtonDefaults.outlinedButtonColors(containerColor = if (selected) color.copy(alpha = 0.22f) else Color(0xB80A0D0F), contentColor = color), contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp), modifier = Modifier.height(30.dp)) {
                Text(layer.label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MapStatusChip(gameState: GameState, layer: MapLayerMode, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = Color(0xC9090806)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)), shape = RoundedCornerShape(10.dp)) {
        Column(modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp)) {
            Text(gameState.calendar.displayText(), color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${gameState.season.label} · ${gameState.weather.label} · V2.3 · ${layer.label}", color = Color(0xFFC6A96C), fontSize = 9.sp)
        }
    }
}

@Composable
private fun MapLegend(layer: MapLayerMode, modifier: Modifier = Modifier) {
    Column(modifier = modifier.background(Color(0xDD0A0D0F), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 7.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        when (layer) {
            MapLayerMode.MILITARY -> { LegendRow(SongBright, "■", "宋控"); LegendRow(JinRed, "■", "金占"); LegendRow(ImperialGold, "⚑", "军团") }
            MapLayerMode.ECONOMY -> { LegendRow(Color(0xFF7BD88F), "◎", "民心/农业"); LegendRow(Color(0xFFE8C35A), "◎", "商贸/税源") }
            MapLayerMode.DIPLOMACY -> { LegendRow(SongBright, "■", "南宋"); LegendRow(JinRed, "■", "金国"); LegendRow(Color(0xFFB38A48), "■", "西夏"); LegendRow(Color(0xFF3F7A4D), "■", "大理") }
            MapLayerMode.TRADE -> { LegendRow(Color(0xFF68D7FF), "◎", "港市"); LegendRow(Color(0xFFB38A48), "━", "马市"); LegendRow(Color(0xFF2376C9), "━", "海路") }
        }
    }
}

private fun DrawScope.drawWorldBase(camX: Float, camY: Float, zoom: Float) {
    drawWorldRect(0f, 0f, 16000f, 10000f, Color(0x55152417), camX, camY, zoom)
    drawWorldRect(3000f, 6100f, 11800f, 9300f, SongBright.copy(alpha = 0.10f), camX, camY, zoom)
    drawWorldRect(6200f, 4550f, 11200f, 6500f, ImperialGold.copy(alpha = 0.10f), camX, camY, zoom)
    drawWorldRect(6100f, 1750f, 10700f, 4650f, JinRed.copy(alpha = 0.13f), camX, camY, zoom)
    drawWorldRect(800f, 1300f, 3900f, 3600f, Color(0xFFB38A48).copy(alpha = 0.10f), camX, camY, zoom)
    drawWorldRect(1450f, 6900f, 3600f, 9550f, Color(0xFF3F7A4D).copy(alpha = 0.10f), camX, camY, zoom)
}

private fun DrawScope.drawMajorRivers(camX: Float, camY: Float, zoom: Float) { TerrainFeatures.rivers.forEach { drawWorldPath(it.points.map { p -> Offset(p.first, p.second) }, Color(0xFF5BC0EB).copy(alpha = 0.62f), 5.5f, camX, camY, zoom) } }
private fun DrawScope.drawMountainRanges(camX: Float, camY: Float, zoom: Float) { TerrainFeatures.mountains.forEach { drawWorldPath(it.points.map { p -> Offset(p.first, p.second) }, Color(0xFF7A5C38).copy(alpha = 0.65f), 6f, camX, camY, zoom) } }

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
        val color = if (layer == MapLayerMode.TRADE && road.involvesTradeNode()) Color(0xFF68D7FF) else when (road.type) { RoadType.RIVER, RoadType.CANAL -> Color(0xFF68B7E8); RoadType.SEA -> Color(0xFF2376C9); RoadType.PASS -> Color(0xFF8B2500); else -> Color(0xFFD0A66A) }
        val dash = if (road.type == RoadType.SEA || road.type == RoadType.CANAL || layer == MapLayerMode.TRADE) PathEffect.dashPathEffect(floatArrayOf(16f, 8f)) else null
        drawLine(Color.Black.copy(alpha = 0.35f), w2s(from.worldX, from.worldY, camX, camY, zoom), w2s(to.worldX, to.worldY, camX, camY, zoom), 4f, pathEffect = dash, cap = StrokeCap.Round)
        drawLine(color, w2s(from.worldX, from.worldY, camX, camY, zoom), w2s(to.worldX, to.worldY, camX, camY, zoom), 2f, pathEffect = dash, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawTradeLines(camX: Float, camY: Float, zoom: Float) {
    listOf(listOf("mingzhou", "goryeo_route"), listOf("quanzhou", "south_sea"), listOf("guangzhou", "south_sea"), listOf("chengdu", "dali", "shanchan"), listOf("lanzhou", "lingzhou", "xingqing")).forEach { ids ->
        ids.zipWithNext().forEach { (a, b) ->
            val from = MapData.nodeMap[a] ?: return@forEach
            val to = MapData.nodeMap[b] ?: return@forEach
            drawLine(Color(0xFF68D7FF).copy(alpha = 0.8f), w2s(from.worldX, from.worldY, camX, camY, zoom), w2s(to.worldX, to.worldY, camX, camY, zoom), 3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 8f)), cap = StrokeCap.Round)
        }
    }
}

private fun DrawScope.drawEconomicHalos(gameState: GameState, camX: Float, camY: Float, zoom: Float) { gameState.cities.filter { it.owner == "song" }.forEach { c -> MapData.nodeMap[c.id]?.let { n -> drawCircle(Color(0xFFE8C35A).copy(alpha = 0.22f), ((c.commerce + c.agriculture) / 7f).coerceIn(10f, 34f), w2s(n.worldX, n.worldY, camX, camY, zoom)) } } }
private fun DrawScope.drawArmyRoutes(gameState: GameState, camX: Float, camY: Float, zoom: Float) { gameState.armies.filter { it.troops > 0 && it.targetCityId.isNotBlank() }.forEach { a -> val f = MapData.nodeMap[a.currentCityId] ?: return@forEach; val t = MapData.nodeMap[a.targetCityId] ?: return@forEach; drawLine(ImperialGold.copy(alpha = 0.72f), w2s(f.worldX, f.worldY, camX, camY, zoom), w2s(t.worldX, t.worldY, camX, camY, zoom), 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f)), cap = StrokeCap.Round) } }
private fun DrawScope.drawArmyFlags(gameState: GameState, camX: Float, camY: Float, zoom: Float) { gameState.armies.filter { it.troops > 0 }.forEach { a -> MapData.nodeMap[a.currentCityId]?.let { n -> val s = w2s(n.worldX, n.worldY, camX, camY, zoom); drawTextOnCanvas(if (a.ownerFactionId == "jin") "金" else "军", Offset(s.x + 22f, s.y - 28f), 13f, android.graphics.Color.WHITE, true) } } }

private fun DrawScope.drawCityIcon(center: Offset, r: Float, color: Color, iconKey: String, mark: Boolean) { drawRect(color.copy(alpha = 0.95f), Offset(center.x - r, center.y - r * 0.55f), Size(r * 2f, r * 1.25f)); drawRect(Color.Black.copy(alpha = 0.55f), Offset(center.x - r, center.y - r * 0.55f), Size(r * 2f, r * 1.25f), style = Stroke(width = 1.2f)); if (iconKey.contains("capital")) drawCircle(ImperialGold.copy(alpha = 0.55f), r * 1.6f, center, style = Stroke(width = 1.5f)); if (mark) drawCircle(ImperialGold.copy(alpha = 0.40f), r * 2.1f, center, style = Stroke(width = 1.4f)) }
private fun DrawScope.drawCityName(name: String, screen: Offset, r: Float, zoom: Float) { if (zoom * 38f > 0.55f) drawTextOnCanvas(name, Offset(screen.x, screen.y + r + 14f), (9.5f * zoom * 38f).coerceIn(10f, 24f), android.graphics.Color.WHITE, false) }
private fun DrawScope.drawMapGrid(camX: Float, camY: Float, zoom: Float) { val grid = 2000f; var wx = ((camX - size.width / 2f / zoom) / grid).toInt() * grid; while (wx < camX + size.width / 2f / zoom + grid) { val sx = size.width / 2f + (wx - camX) * zoom; drawLine(Color(0xFF233029).copy(alpha = 0.35f), Offset(sx, 0f), Offset(sx, size.height), 0.5f); wx += grid }; var wy = ((camY - size.height / 2f / zoom) / grid).toInt() * grid; while (wy < camY + size.height / 2f / zoom + grid) { val sy = size.height / 2f + (wy - camY) * zoom; drawLine(Color(0xFF233029).copy(alpha = 0.35f), Offset(0f, sy), Offset(size.width, sy), 0.5f); wy += grid } }
private fun DrawScope.drawWorldRect(l: Float, t: Float, r: Float, b: Float, color: Color, camX: Float, camY: Float, zoom: Float) { val a = w2s(l, t, camX, camY, zoom); val z = w2s(r, b, camX, camY, zoom); drawRect(color, topLeft = a, size = Size(z.x - a.x, z.y - a.y)) }
private fun DrawScope.drawWorldPath(points: List<Offset>, color: Color, baseWidth: Float, camX: Float, camY: Float, zoom: Float) { points.zipWithNext().forEach { (a, b) -> drawLine(color, w2s(a.x, a.y, camX, camY, zoom), w2s(b.x, b.y, camX, camY, zoom), (baseWidth * zoom * 35f).coerceIn(2f, 12f), cap = StrokeCap.Round) } }
private fun DrawScope.drawTextOnCanvas(text: String, center: Offset, textSize: Float, color: Int, bold: Boolean) { drawIntoCanvas { canvas -> val p = android.graphics.Paint().apply { this.color = color; this.textSize = textSize; isAntiAlias = true; textAlign = android.graphics.Paint.Align.CENTER; isFakeBoldText = bold; setShadowLayer(4f, 0f, 2f, android.graphics.Color.argb(210, 0, 0, 0)) }; canvas.nativeCanvas.drawText(text, center.x, center.y, p) } }
private fun DrawScope.w2s(wx: Float, wy: Float, camX: Float, camY: Float, zoom: Float): Offset = Offset(size.width / 2f + (wx - camX) * zoom, size.height / 2f + (wy - camY) * zoom)

private fun nodeColor(node: MapNode, city: City?, layer: MapLayerMode): Color = when (layer) { MapLayerMode.MILITARY -> if (city?.owner == "jin" || node.ownerHint == "jin") JinRed else if (node.isCapital) ImperialGold else SongBright; MapLayerMode.ECONOMY -> if (city?.terrain == "coast" || city?.isWaterNode == true) Color(0xFF68D7FF) else Color(0xFFE8C35A); MapLayerMode.DIPLOMACY -> ownerHintColor(city?.owner ?: node.ownerHint); MapLayerMode.TRADE -> if (node.id in TradeRouteIds || node.nodeType == "trade") Color(0xFF68D7FF) else ownerHintColor(city?.owner ?: node.ownerHint) }
private fun ownerHintColor(owner: String): Color = when (owner) { "song" -> SongBright; "jin" -> JinRed; "xixia" -> Color(0xFFB38A48); "dali" -> Color(0xFF3F7A4D); "goryeo" -> Color(0xFF7AA7D9); "sea_trade" -> Color(0xFF2376C9); else -> Color(0xFF7A8794) }
private fun layerColor(layer: MapLayerMode): Color = when (layer) { MapLayerMode.MILITARY -> ImperialGold; MapLayerMode.ECONOMY -> Color(0xFFE8C35A); MapLayerMode.DIPLOMACY -> Color(0xFFBFA3FF); MapLayerMode.TRADE -> Color(0xFF68D7FF) }
private fun City?.ownerLabel(node: MapNode): String = when { this != null && owner == "jin" -> "金国占领 · $controlState"; this != null && owner == "song" -> "大宋控制 · $controlState"; node.ownerHint == "xixia" -> "西夏势力 · 战略节点"; node.ownerHint == "dali" -> "大理势力 · 战略节点"; node.ownerHint == "sea_trade" -> "海贸诸商 · 外贸节点"; else -> "未接入城池数值 · 战略节点" }
private fun com.xiemingxin.nandu.game.CityRoad.involvesTradeNode(): Boolean = fromId in TradeRouteIds || toId in TradeRouteIds
private fun terrainLabel(terrain: String): String = when (terrain) { "river" -> "水乡"; "mountain" -> "山地"; "pass" -> "关隘"; "coast" -> "沿海"; else -> "平原" }

@Composable private fun LegendRow(color: Color, symbol: String, label: String) { Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) { Text(symbol, color = color, fontSize = 11.sp); Text(label, color = Color(0xFFB9AA82), fontSize = 10.sp) } }
@Composable private fun CityStatItem(icon: String, label: String, value: String) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Text(icon, fontSize = 14.sp, color = ImperialGold); Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold); Text(label, color = Color(0xFF8C7A60), fontSize = 9.sp) } }
@Composable private fun CityActionButton(text: String, modifier: Modifier, onClick: () -> Unit) { OutlinedButton(onClick = onClick, modifier = modifier.height(36.dp), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold), contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)) { Text(text, fontSize = 11.sp) } }
@Composable private fun CityArmyRow(army: Army, commander: String) { Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF1D150B), RoundedCornerShape(7.dp)).padding(8.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(army.name, color = if (army.ownerFactionId == "jin") JinRed else SongBright, fontSize = 12.sp, fontWeight = FontWeight.Bold); Text("${army.troops / 1000}k", color = ImperialGold, fontSize = 12.sp, fontWeight = FontWeight.Bold) }; Text("统帅：$commander · 士气${army.morale} · ${army.status}", color = Color(0xFFB9AA82), fontSize = 10.sp) }; Spacer(Modifier.height(6.dp)) }

@Composable
fun CityDetailPanel(node: MapNode, city: City?, armies: List<Army>, officerNames: Map<String, String>, layer: MapLayerMode = MapLayerMode.MILITARY, onDismiss: () -> Unit, modifier: Modifier = Modifier, onDraft: (String) -> Unit = {}) {
    val visual = CityVisualRegistry.visualFor(city, node)
    Card(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xF01A1208)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.60f))) {
        Column(modifier = Modifier.padding(14.dp)) {
            if (city != null) { AssetImage(path = visual.panelBackgroundPath, fallbackPath = ArtResourceRegistry.cityBackground(city.id), contentDescription = node.name, contentScale = ContentScale.Crop, placeholderText = visual.tier.label.take(1), modifier = Modifier.fillMaxWidth().height(112.dp)); Spacer(Modifier.height(10.dp)) }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Column(modifier = Modifier.weight(1f)) { Text(visual.displayName, color = nodeColor(node, city, layer), fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("${visual.tier.label}｜${visual.tags.joinToString(" / ")}", color = ImperialGold, fontSize = 10.sp); Text(city.ownerLabel(node), color = Color(0xFFB9AA82), fontSize = 12.sp); if (city != null) Text("${city.route} · ${city.cityLevel} · ${terrainLabel(city.terrain)} · 口${city.population / 10000}万", color = Color(0xFF8C7A60), fontSize = 10.sp) }; IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) { Text("X", color = Color(0xFF8B7355), fontSize = 14.sp) } }
            Text(visual.visualHint, color = Color(0xFFC6A96C), fontSize = 10.sp, lineHeight = 14.sp)
            if (city != null) { Spacer(Modifier.height(10.dp)); Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) { CityStatItem("兵", "兵力", "${city.troops / 1000}k"); CityStatItem("防", "城防", "${city.defense}"); CityStatItem("粮", "粮草", "${city.grain / 1000}k"); CityStatItem("财", "金库", "${city.gold / 1000}k"); CityStatItem("民", "民心", "${city.popularSupport}") }; Spacer(Modifier.height(10.dp)); if (armies.isEmpty()) Text("此城暂无正式军团驻防。", color = Color(0xFF8C7A60), fontSize = 11.sp) else armies.take(3).forEach { CityArmyRow(it, officerNames[it.commanderId] ?: it.status) }; Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) { CityActionButton("修城", Modifier.weight(1f)) { onDraft("repair") }; CityActionButton("调兵", Modifier.weight(1f)) { onDraft("dispatch") }; CityActionButton("筹粮", Modifier.weight(1f)) { onDraft("grain") } }; Spacer(Modifier.height(7.dp)); CityActionButton("◈ 进 入 城 池 ◈", Modifier.fillMaxWidth()) { onDraft("enter") }; if (city.owner == "song") { Spacer(Modifier.height(7.dp)); Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) { CityActionButton("营建", Modifier.weight(1f)) { onDraft("manage") }; CityActionButton("募兵", Modifier.weight(1f)) { onDraft("recruit") } } } } else { Spacer(Modifier.height(10.dp)); Text("此节点用于外交、外贸或战略显示；完整经营数据会在后续城池扩容中接入。", color = Color(0xFFB9AA82), fontSize = 11.sp, lineHeight = 16.sp) }
        }
    }
}

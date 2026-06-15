package com.xiemingxin.nandu.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.ai.NpcResponse
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.ui.GamePhase
import com.xiemingxin.nandu.ui.UiState
import com.xiemingxin.nandu.ui.components.AssetImage
import com.xiemingxin.nandu.ui.components.StoryEventCard
import com.xiemingxin.nandu.ui.theme.*
import androidx.compose.ui.window.Dialog

private val CourtGold = Color(0xFFC9A227)
private val CourtCream = Color(0xFFE8DCC0)
private val CourtSub = Color(0xFF9A8862)
private val CourtInk = Color(0xFF0E0A05)
private val CourtRed = Color(0xFF7D1D16)
private val CourtGreen = Color(0xFF78B56A)
private val CourtBlue = Color(0xFF4DA3E6)

@Composable
fun EmperorMainScreen(
    uiState: UiState,
    draftEdictText: String = "",
    onSubmitEdict: (String) -> Unit,
    onConfirmEdict: (String) -> Unit,
    onCancelEdict: () -> Unit,
    onDismissResult: () -> Unit,
    onAdvanceTurn: () -> Unit,
    onStoryChoice: (String) -> Unit,
    onDismissStoryOutcome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var edictText by remember { mutableStateOf(draftEdictText) }

    LaunchedEffect(draftEdictText) {
        if (draftEdictText.isNotBlank()) edictText = draftEdictText
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            GameHUD(state = uiState.gameState, onSettings = onOpenSettings)

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (uiState.phase) {
                    GamePhase.IDLE, GamePhase.EXECUTING -> {
                        IdleView(
                            state = uiState.gameState,
                            edictText = edictText,
                            onEdictChange = { edictText = it },
                            onSubmit = { onSubmitEdict(edictText) },
                            isLoading = uiState.phase == GamePhase.EXECUTING
                        )
                    }
                    GamePhase.AI_PROCESSING -> LoadingView()
                    GamePhase.AWAITING_CONFIRM -> {
                        uiState.lastEdictResult?.let { result ->
                            ConfirmEdictView(
                                state = uiState.gameState,
                                result = result,
                                onConfirm = { onConfirmEdict(edictText) },
                                onCancel = onCancelEdict
                            )
                        }
                    }
                    GamePhase.SHOWING_RESULT -> {
                        ResultView(
                            outcomes = uiState.lastOutcomes,
                            rejected = uiState.lastRejected,
                            onDismiss = {
                                edictText = ""
                                onAdvanceTurn()
                            }
                        )
                    }
                }
            }

            uiState.errorMessage?.let { msg ->
                Text(
                    text = "⚠ $msg",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }

        uiState.currentStoryEvent?.let { event ->
            Dialog(onDismissRequest = { }) {
                StoryEventCard(
                    event = event,
                    onChoice = onStoryChoice
                )
            }
        }

        if (uiState.storyOutcomes.isNotEmpty() && uiState.currentStoryEvent == null) {
            Dialog(onDismissRequest = onDismissStoryOutcome) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1508)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, ImperialGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("【天下有变】", color = ImperialGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        uiState.storyOutcomes.forEach { o ->
                            Text(o, color = XuanCream, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = onDismissStoryOutcome,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ImperialGold)
                        ) { Text("朕知道了", color = InkBlack, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun GameHUD(state: GameState, onSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0D0A04))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.15f)) {
            Text(
                text = "🐉 ${state.calendar.displayText()}",
                color = ImperialGold,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "${state.season.label} · ${state.weather.label}｜${state.weather.effectText}",
                color = Color(0xFF8B7355),
                fontSize = 9.sp,
                maxLines = 1
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
            HudStat("💰", "${state.gold / 1000}k")
            HudStat("🌾", "${state.grain / 1000}k")
            HudStat("⚔", "${state.troopMorale}")
            HudStat("🏯", "${state.jinThreat}", if (state.jinThreat > 80) Color.Red else Color.White)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onSettings, modifier = Modifier.size(32.dp)) {
                Text("⚙", fontSize = 16.sp)
            }
            Text(
                "V1.4.2",
                color = Color(0xFF3A3020),
                fontSize = 8.sp
            )
        }
    }
}

@Composable
fun HudStat(icon: String, value: String, valueColor: Color = Color.White) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 11.sp)
        Spacer(Modifier.width(2.dp))
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun IdleView(
    state: GameState,
    edictText: String,
    onEdictChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    Box(modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CourtStageHeader(state = state, title = "垂拱殿听政", subtitle = "群臣列班 · 御笔候旨") }
            item { CourtOfficerRow(state = state) }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
                    border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("御笔下诏", color = CourtGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text("写得越具体，AI 越能把它拆成可执行军政命令。", color = CourtSub, fontSize = 11.sp)
                        OutlinedTextField(
                            value = edictText,
                            onValueChange = onEdictChange,
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            placeholder = {
                                Text(
                                    "传朕旨意…\n如：命岳飞率三万兵从鄂州取襄阳，韩世忠守建康，赵鼎筹粮，秦桧暂退中枢。",
                                    color = Color(0xFF8B7355),
                                    fontSize = 13.sp
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CourtGold,
                                unfocusedBorderColor = Color(0xFF4A3728),
                                focusedTextColor = CourtCream,
                                unfocusedTextColor = CourtCream,
                                cursorColor = CourtGold
                            ),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val hints = listOf("调兵出征", "任命守将", "修缮城防", "筹粮备战", "压制主和", "赏赐名将")
                    items(hints) { hint ->
                        FilterChip(
                            selected = false,
                            onClick = { onEdictChange(if (edictText.isBlank()) hint else "$edictText，$hint") },
                            label = { Text(hint, fontSize = 11.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = Color(0xFF2A1F12),
                                labelColor = CourtGold
                            )
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = onSubmit,
                    enabled = edictText.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ImperialRed,
                        disabledContainerColor = Color(0xFF4A1A1A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isLoading) "执行中…" else "朱批下发",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = CourtCream
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingView() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CourtGold)
            Spacer(Modifier.height(16.dp))
            Text("垂拱殿中，群臣传看圣旨…", color = CourtGold)
            Text("AI 正在拆解命令与朝堂反应", color = CourtSub, fontSize = 11.sp)
        }
    }
}

@Composable
fun ConfirmEdictView(state: GameState, result: EdictResult, onConfirm: () -> Unit, onCancel: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CourtStageHeader(state = state, title = "AI 奏议解析", subtitle = result.summary) }
            item { CourtDebatePanel(state = state, responses = result.npcResponses) }
            item { CommandPanel(result) }
            if (result.riskTags.isNotEmpty() || result.clarificationNeeded) {
                item { RiskPanel(result) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ImperialRed),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("准奏", color = CourtCream, fontWeight = FontWeight.Bold) }
                    OutlinedButton(
                        onClick = onCancel,
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.55f))
                    ) { Text("驳回再议", color = CourtGold) }
                }
            }
        }
    }
}

@Composable
private fun CourtBackground() {
    AssetImage(
        path = "images/buildings/building_imperial_palace_01.webp",
        contentDescription = "垂拱殿内景",
        contentScale = ContentScale.Crop,
        placeholderText = "殿",
        modifier = Modifier.fillMaxSize()
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(
                    CourtInk.copy(alpha = 0.40f),
                    CourtInk.copy(alpha = 0.18f),
                    CourtInk.copy(alpha = 0.93f)
                )
            )
        )
    )
}

@Composable
private fun CourtStageHeader(state: GameState, title: String, subtitle: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD70E0A05)),
        border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.45f))
    ) {
        Row(modifier = Modifier.padding(13.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = CourtGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = CourtCream, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("朝局 ${state.courtStability}", color = CourtGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("主战 ${state.warFactionPower} / 主和 ${state.peaceFactionPower}", color = CourtSub, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun CourtOfficerRow(state: GameState) {
    val ids = listOf("li_gang", "zhao_ding", "qin_hui", "yue_fei", "han_shizhong")
    val officers = ids.mapNotNull { id -> state.officers.firstOrNull { it.id == id } }
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(officers) { officer ->
            OfficerMiniCard(officer = officer)
        }
    }
}

@Composable
private fun OfficerMiniCard(officer: Officer) {
    Card(
        modifier = Modifier.width(136.dp).height(100.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD51E1508)),
        border = BorderStroke(1.dp, factionColor(officer.faction).copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(9.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(factionColor(officer.faction).copy(alpha = 0.26f)),
                    contentAlignment = Alignment.Center
                ) { Text(officer.name.take(1), color = CourtGold, fontSize = 17.sp, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(7.dp))
                Column {
                    Text(officer.name, color = CourtCream, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(officer.faction, color = CourtSub, fontSize = 9.sp, maxLines = 1)
                }
            }
            Text("忠 ${officer.loyalty} · 野 ${officer.ambition} · 官 ${officer.rankLevel}", color = CourtGold, fontSize = 9.sp)
            Text(officer.skills.take(2).joinToString(" / "), color = CourtSub, fontSize = 9.sp, maxLines = 1)
        }
    }
}

@Composable
private fun CourtDebatePanel(state: GameState, responses: List<NpcResponse>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
        border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("群臣当殿奏对", color = CourtGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            if (responses.isEmpty()) {
                Text("殿中暂无人出班。", color = CourtSub, fontSize = 12.sp)
            } else {
                responses.forEach { response ->
                    val officer = state.officers.firstOrNull { it.id == response.officerId }
                    DebateCard(response = response, officer = officer)
                }
            }
        }
    }
}

@Composable
private fun DebateCard(response: NpcResponse, officer: Officer?) {
    val c = attitudeColor(response.attitude)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xB90E0A05)),
        border = BorderStroke(1.dp, c.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(officer?.name ?: response.officerId, color = CourtCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("${officer?.faction ?: "朝臣"} · ${officer?.currentCityId ?: "御前"}", color = CourtSub, fontSize = 9.sp)
                }
                Text(attitudeLabel(response.attitude), color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text("“${response.text}”", color = CourtCream, fontSize = 12.sp, lineHeight = 18.sp)
            officer?.let {
                Text("忠诚 ${it.loyalty}｜野心 ${it.ambition}｜${it.skills.take(3).joinToString("、")}", color = CourtSub, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun CommandPanel(result: EdictResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
        border = BorderStroke(1.dp, CourtBlue.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text("拟执行命令", color = CourtGold, fontWeight = FontWeight.Bold)
            if (result.commands.isEmpty()) {
                Text("暂无可执行命令。", color = CourtSub, fontSize = 12.sp)
            } else {
                result.commands.forEachIndexed { index, cmd ->
                    Text(
                        "${index + 1}. ${commandLabel(cmd.type)} ${cmd.officerId.ifBlank { cmd.cityId }} ${cmd.fromCityId.ifBlank { "" }}${if (cmd.toCityId.isNotBlank()) "→${cmd.toCityId}" else ""} ${if (cmd.troops > 0) "${cmd.troops}兵" else ""}",
                        color = CourtCream,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun RiskPanel(result: EdictResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC21100A)),
        border = BorderStroke(1.dp, CourtRed.copy(alpha = 0.58f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("风险与追问", color = CourtGold, fontWeight = FontWeight.Bold)
            if (result.riskTags.isNotEmpty()) Text("风险：${result.riskTags.joinToString(" / ")}", color = Color(0xFFFFB08A), fontSize = 11.sp)
            if (result.clarificationNeeded) Text(result.clarificationHint, color = CourtCream, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
fun ResultView(outcomes: List<String>, rejected: List<String>, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("圣旨执行结果", color = CourtGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            outcomes.forEach { outcome ->
                Card(colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)), border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.25f))) {
                    Text(outcome, color = CourtCream, modifier = Modifier.padding(12.dp), fontSize = 13.sp, lineHeight = 19.sp)
                }
            }
            if (rejected.isNotEmpty()) {
                Text("未采纳命令", color = Color.Red, fontWeight = FontWeight.Bold)
                rejected.forEach { Text("• $it", color = Color.Red, fontSize = 12.sp) }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CourtGold),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("进入下一旬", color = CourtInk, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun attitudeColor(attitude: String): Color = when (attitude.lowercase()) {
    "support" -> CourtGreen
    "oppose" -> Color(0xFFE07162)
    "concerned" -> Color(0xFFE5B85E)
    else -> CourtBlue
}

private fun attitudeLabel(attitude: String): String = when (attitude.lowercase()) {
    "support" -> "支持"
    "oppose" -> "反对"
    "concerned" -> "忧虑"
    else -> "中立"
}

private fun factionColor(faction: String): Color = when {
    faction.contains("战") || faction.contains("武") || faction.contains("军") -> Color(0xFFD4A437)
    faction.contains("和") || faction.contains("文") -> Color(0xFF8A9BB5)
    faction.contains("新锐") -> Color(0xFF78B56A)
    else -> CourtBlue
}

private fun commandLabel(type: String): String = when (type) {
    "dispatch_army" -> "调兵"
    "assign_officer" -> "任命/寻访"
    "repair_city" -> "修城"
    "raise_grain" -> "筹粮"
    "suppress_officer" -> "压制"
    "reward_officer" -> "赏赐"
    "punish_officer" -> "惩处"
    "move_capital" -> "迁都"
    else -> type
}
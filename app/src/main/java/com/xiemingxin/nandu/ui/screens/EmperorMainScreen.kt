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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.ai.CourtInteractionRoute
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.ai.NpcResponse
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.CharacterAppearanceSystem
import com.xiemingxin.nandu.game.CharacterStateSource
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.ImperialDecision
import com.xiemingxin.nandu.game.ImperialMandate
import com.xiemingxin.nandu.game.ImperialMandatePolicy
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.game.PalaceIds
import com.xiemingxin.nandu.game.PalaceRegistry
import com.xiemingxin.nandu.game.controlledCityCount
import com.xiemingxin.nandu.game.garrisonTroopsOf
import com.xiemingxin.nandu.game.playerFaction
import com.xiemingxin.nandu.game.WorldPresentationPolicy
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

private enum class CourtInputMode(
    val route: String,
    val tabLabel: String,
    val title: String,
    val helper: String,
    val actionLabel: String
) {
    CONSULT(
        route = "CONSULT",
        tabLabel = "问政",
        title = "御前问政",
        helper = "问局势、问钱粮、问军情，或直接点名一位在殿大臣。问政只产生奏对，不会自行改动天下。",
        actionLabel = "问政"
    ),
    CHAT(
        route = "CHAT",
        tabLabel = "闲聊",
        title = "殿中闲谈",
        helper = "可以感叹、追问人物、聊眼下见闻。只要是在对殿中人说话，就应有人接话。",
        actionLabel = "开口闲谈"
    ),
    ORDER(
        route = "ORDER",
        tabLabel = "下旨",
        title = "御笔下诏",
        helper = "只有这里才把话当作正式军政命令。命令会先核人物、兵力、钱粮与地点，再进入朱批。",
        actionLabel = "呈旨"
    )
}

@Composable
fun EmperorMainScreen(
    uiState: UiState,
    draftEdictText: String = "",
    onSubmitEdict: (String) -> Unit,
    onConfirmEdict: (String) -> Unit,
    onCancelEdict: () -> Unit,
    onAmendEdict: (String) -> Unit,
    onToggleCouncilOpinion: (String) -> Unit,
    onSynthesizeCouncilOpinions: () -> Unit,
    onRevokeMandate: (String) -> Unit,
    onDismissResult: () -> Unit,
    onAdvanceTurn: () -> Unit,
    onStoryChoice: (String) -> Unit,
    onDismissStoryOutcome: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var edictText by remember { mutableStateOf(draftEdictText) }
    var inputMode by remember { mutableStateOf(CourtInputMode.CONSULT) }

    LaunchedEffect(draftEdictText) {
        if (draftEdictText.isNotBlank()) {
            edictText = draftEdictText
            inputMode = CourtInputMode.ORDER
        }
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
                            inputMode = inputMode,
                            onInputModeChange = { inputMode = it },
                            onEdictChange = { edictText = it },
                            onSubmit = {
                                CourtInteractionRoute.select(inputMode.route)
                                onSubmitEdict(edictText)
                            },
                            onRevokeMandate = onRevokeMandate,
                            isLoading = uiState.phase == GamePhase.EXECUTING
                        )
                    }
                    GamePhase.AI_PROCESSING -> LoadingView(inputMode)
                    GamePhase.AWAITING_CONFIRM -> {
                        uiState.lastEdictResult?.let { result ->
                            when (result.interactionType.uppercase()) {
                                "CHAT", "CONSULT" -> ConversationResponseView(
                                    state = uiState.gameState,
                                    result = result,
                                    onContinue = { nextMode ->
                                        inputMode = nextMode
                                        edictText = ""
                                        onCancelEdict()
                                    }
                                )
                                else -> ConfirmEdictView(
                                    state = uiState.gameState,
                                    result = result,
                                    decision = uiState.imperialDecision,
                                    edictText = edictText,
                                    onConfirm = { onConfirmEdict(edictText) },
                                    onCancel = onCancelEdict,
                                    onAmend = {
                                        inputMode = CourtInputMode.ORDER
                                        // 玩家输入框只属于玩家。原旨、臣议和追问由状态层暗中保存，
                                        // 绝不再把“参酌前议 / 待补圣意”塞进玩家正文。
                                        edictText = ""
                                        onAmendEdict("")
                                    },
                                    onToggleOpinion = onToggleCouncilOpinion,
                                    onSynthesize = onSynthesizeCouncilOpinions
                                )
                            }
                        }
                    }
                    GamePhase.SHOWING_RESULT -> {
                        ResultView(
                            outcomes = uiState.lastOutcomes,
                            rejected = uiState.lastRejected,
                            onDismiss = {
                                edictText = ""
                                inputMode = CourtInputMode.CONSULT
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
    val player = state.playerFaction()
    val controlledCities = player?.let { state.controlledCityCount(it.id) } ?: state.cities.count { it.owner == "song" }
    val totalTroops = player?.let { state.garrisonTroopsOf(it.id) } ?: state.cities.filter { it.owner == "song" }.sumOf { it.troops }
    val capitalName = state.cities.firstOrNull { it.id == CharacterStateSource.CAPITAL_CITY_ID }?.name ?: "南京应天府"
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF0D0A04))) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                    text = "${player?.name ?: "大宋"} · 行在 $capitalName · ${state.season.label} · ${state.weather.label}",
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
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp).padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HudStat("🏙", "控城 $controlledCities")
            HudStat("🛡", "总兵力 ${totalTroops / 1000}k")
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
    inputMode: CourtInputMode,
    onInputModeChange: (CourtInputMode) -> Unit,
    onEdictChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onRevokeMandate: (String) -> Unit,
    isLoading: Boolean
) {
    val courtName = PalaceRegistry.byId(PalaceIds.CHUIGONG).name
    val hints = courtConversationHints(state, inputMode)
    Box(modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CourtStageHeader(state = state, title = "$courtName 听政", subtitle = "问政、闲聊、正式下旨各走各的路，不再让模型猜。") }
            item { CourtOfficerRow(state = state) }
            val activeMandates = state.imperialMandates.filter { it.isActive && !it.isExpired(state.turn) }
            if (activeMandates.isNotEmpty()) {
                item {
                    Text("在行圣旨 · ${activeMandates.size}道", color = CourtGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                items(activeMandates, key = { it.id }) { mandate ->
                    ImperialMandateCard(state = state, mandate = mandate, onRevoke = { onRevokeMandate(mandate.id) })
                }
            }
            item {
                CourtModeSelector(selected = inputMode, onSelected = onInputModeChange)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
                    border = BorderStroke(1.dp, if (inputMode == CourtInputMode.ORDER) CourtRed.copy(alpha = 0.75f) else CourtGold.copy(alpha = 0.55f))
                ) {
                    Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(inputMode.title, color = CourtGold, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Text(inputMode.helper, color = CourtSub, fontSize = 11.sp, lineHeight = 16.sp)
                        OutlinedTextField(
                            value = edictText,
                            onValueChange = onEdictChange,
                            modifier = Modifier.fillMaxWidth().height(170.dp),
                            placeholder = {
                                Text(
                                    courtPlaceholder(inputMode),
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
            if (hints.isNotEmpty()) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(hints) { hint ->
                            FilterChip(
                                selected = false,
                                onClick = { onEdictChange(hint) },
                                label = { Text(hint, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(containerColor = Color(0xFF2A1F12), labelColor = CourtGold)
                            )
                        }
                    }
                }
            }
            item {
                Button(
                    onClick = onSubmit,
                    enabled = edictText.isNotBlank() && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inputMode == CourtInputMode.ORDER) ImperialRed else CourtGold,
                        disabledContainerColor = Color(0xFF4A1A1A)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        if (isLoading) "传达中…" else inputMode.actionLabel,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (inputMode == CourtInputMode.ORDER) CourtCream else CourtInk
                    )
                }
            }
        }
    }
}

@Composable
private fun CourtModeSelector(selected: CourtInputMode, onSelected: (CourtInputMode) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CourtInputMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode,
                onClick = { onSelected(mode) },
                label = {
                    Text(
                        mode.tabLabel,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontWeight = if (selected == mode) FontWeight.Bold else FontWeight.Medium
                    )
                },
                modifier = Modifier.weight(1f),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color(0xC51A1208),
                    selectedContainerColor = if (mode == CourtInputMode.ORDER) CourtRed else CourtGold,
                    labelColor = CourtCream,
                    selectedLabelColor = if (mode == CourtInputMode.ORDER) CourtCream else CourtInk
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == mode,
                    borderColor = CourtGold.copy(alpha = 0.5f),
                    selectedBorderColor = CourtGold
                )
            )
        }
    }
}

@Composable
fun LoadingView(inputMode: CourtInputMode) {
    val courtName = PalaceRegistry.byId(PalaceIds.CHUIGONG).name
    val detail = when (inputMode) {
        CourtInputMode.CONSULT -> "群臣正在据当前军政实情奏对…"
        CourtInputMode.CHAT -> "殿中正在应声，不会把闲话当成圣旨…"
        CourtInputMode.ORDER -> "中书正在核旨，确认人物、兵力、钱粮与地点…"
    }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = CourtGold)
            Spacer(Modifier.height(16.dp))
            Text("$courtName 中，${inputMode.tabLabel}传达中…", color = CourtGold)
            Text(detail, color = CourtSub, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ConversationResponseView(
    state: GameState,
    result: EdictResult,
    onContinue: (CourtInputMode) -> Unit
) {
    val isConsult = result.interactionType.equals("CONSULT", ignoreCase = true)
    val currentMode = if (isConsult) CourtInputMode.CONSULT else CourtInputMode.CHAT
    val title = if (isConsult) "御前问策" else "殿中闲谈"
    Box(modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CourtStageHeader(state = state, title = title, subtitle = result.summary.ifBlank { "群臣静候圣意。" }) }
            item {
                CourtDebatePanel(
                    state = state,
                    responses = result.npcResponses,
                    selectedOfficerIds = emptySet(),
                    onToggleOpinion = { },
                    onSynthesize = { }
                )
            }
            item {
                Text(
                    "这一轮只是对话，不会修改兵力、国库、官职或世界状态。可以继续追问；真要执行时再切到“下旨”。",
                    color = CourtSub,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onContinue(currentMode) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = CourtGold),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isConsult) "继续问政" else "继续闲聊", color = CourtInk, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onContinue(CourtInputMode.ORDER) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        border = BorderStroke(1.dp, CourtRed.copy(alpha = 0.75f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("据此下旨", color = CourtCream, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmEdictView(
    state: GameState,
    result: EdictResult,
    decision: ImperialDecision,
    edictText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    onAmend: () -> Unit,
    onToggleOpinion: (String) -> Unit,
    onSynthesize: () -> Unit
) {
    val mandate = remember(state, edictText, decision.selectedOfficerIds) {
        ImperialMandatePolicy.draft(state, edictText, decision.selectedOfficerIds)
    }
    Box(modifier = Modifier.fillMaxSize().background(CourtInk)) {
        CourtBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { CourtStageHeader(state = state, title = "御前奏议", subtitle = result.summary) }
            item {
                CourtDebatePanel(
                    state = state,
                    responses = result.npcResponses,
                    selectedOfficerIds = decision.selectedOfficerIds,
                    onToggleOpinion = onToggleOpinion,
                    onSynthesize = onSynthesize
                )
            }
            item { ImperialDecisionPreview(state, result, decision) }
            if (mandate != null) item { ImperialMandateCard(state = state, mandate = mandate, onRevoke = null) }
            item { CommandPanel(state, result) }
            if (result.riskTags.isNotEmpty() || result.clarificationNeeded) item { RiskPanel(result) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Button(
                        onClick = onConfirm,
                        enabled = decision.canExecute(result, mandate != null),
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ImperialRed),
                        shape = RoundedCornerShape(8.dp)
                    ) { Text("朱批准行", color = CourtCream, fontWeight = FontWeight.Bold) }
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        OutlinedButton(
                            onClick = onAmend,
                            modifier = Modifier.weight(1f).height(46.dp),
                            border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.65f))
                        ) { Text(if (result.clarificationNeeded) "补充圣意" else "朕再修改", color = CourtGold) }
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f).height(46.dp),
                            border = BorderStroke(1.dp, CourtSub.copy(alpha = 0.55f))
                        ) { Text("驳回重议", color = CourtCream) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ImperialMandateCard(state: GameState, mandate: ImperialMandate, onRevoke: (() -> Unit)?) {
    val officer = state.officers.firstOrNull { it.id == mandate.responsibleOfficerId }?.name ?: "受命之臣"
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xE31B160B)),
        border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.55f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text("${officer} · ${mandate.autonomyLevel.label}", color = CourtGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (onRevoke != null) TextButton(onClick = onRevoke) { Text("收回授权", color = CourtRed, fontSize = 11.sp) }
            }
            Text("辖区：${ImperialMandatePolicy.describeTerritory(state, mandate)}", color = CourtCream, fontSize = 11.sp)
            Text("准行：${mandate.allowedActions.joinToString("、") { it.label }.ifBlank { "需逐事请旨" }}", color = CourtGreen, fontSize = 11.sp)
            Text("军费：${mandate.remainingGold()} / ${mandate.budgetGold}贯；粮草：${mandate.remainingGrain()} / ${mandate.budgetGrain}石",
                color = CourtCream, fontSize = 11.sp)
            Text("底线：${ImperialMandatePolicy.describeRestrictions(state, mandate)}", color = CourtSub, fontSize = 11.sp)
            if (onRevoke == null) Text("朱批后持续有效，直至收回。", color = CourtBlue, fontSize = 10.sp)
        }
    }
}

@Composable
private fun CourtBackground() {
    AssetImage(
        path = "images/buildings/building_imperial_palace_01.webp",
        contentDescription = "应天行在内殿",
        contentScale = ContentScale.Crop,
        placeholderText = "殿",
        modifier = Modifier.fillMaxSize()
    )
    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(CourtInk.copy(alpha = 0.40f), CourtInk.copy(alpha = 0.18f), CourtInk.copy(alpha = 0.93f))
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

/**
 * 活朝堂：谁能站在行在内殿，统一由 CharacterAppearanceSystem.canAppearInPalace 决定。
 * 外任、领军、俘虏、未到时代、罢黜、赶路中的人物一律不能肉身列班。
 */
@Composable
private fun CourtOfficerRow(state: GameState) {
    val present = state.officers.filter {
        CharacterAppearanceSystem.canAppearInPalace(state, it.id, PalaceIds.CHUIGONG)
    }

    Box {
        val crowdScene = remember(state.turn) {
            val scenes = ArtResourceRegistry.CourtNpc.crowdScenes.values.toList()
            scenes[(state.turn.hashCode() and Int.MAX_VALUE) % scenes.size]
        }
        AssetImage(
            path = crowdScene,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            placeholderText = "",
            modifier = Modifier.fillMaxWidth().height(112.dp).clip(RoundedCornerShape(14.dp)).alpha(0.30f)
        )

        Column {
            if (present.isEmpty()) {
                DutyOfficialMiniCard(state)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(present) { officer -> OfficerMiniCard(officer = officer) }
                }
            }
            Spacer(Modifier.height(6.dp))
            CourtBackgroundRetinue(state)
        }
    }
}

@Composable
private fun CourtBackgroundRetinue(state: GameState) {
    val poses = remember(state.turn) {
        ArtResourceRegistry.CourtNpc.rankAndFilePoses.shuffled(kotlin.random.Random(state.turn))
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        poses.take(6).forEach { pose ->
            AssetImage(
                path = pose,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                placeholderText = "",
                modifier = Modifier.width(26.dp).height(46.dp).clip(RoundedCornerShape(4.dp)).alpha(0.55f)
            )
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
                AssetImage(
                    path = ArtResourceRegistry.portraitForOfficer(officer.id),
                    contentDescription = officer.name,
                    contentScale = ContentScale.Crop,
                    placeholderText = officer.name.take(1),
                    modifier = Modifier.size(34.dp).clip(RoundedCornerShape(9.dp))
                        .background(factionColor(officer.faction).copy(alpha = 0.26f))
                )
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
private fun DutyOfficialMiniCard(state: GameState) {
    val (label, portrait) = remember(state.turn) {
        ArtResourceRegistry.CourtNpc.officialBySeed(state.turn.toString())
    }
    Card(
        modifier = Modifier.width(220.dp).height(84.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD51E1508)),
        border = BorderStroke(1.dp, CourtSub.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.fillMaxSize().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AssetImage(
                path = portrait,
                contentDescription = label,
                contentScale = ContentScale.Crop,
                placeholderText = label.take(1),
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(9.dp))
                    .background(CourtSub.copy(alpha = 0.2f))
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text("$label 当值", color = CourtGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("重臣多在外任/领军，殿中暂由当值官员承接奏对。", color = CourtSub, fontSize = 9.sp, lineHeight = 12.sp)
            }
        }
    }
}

@Composable
private fun CourtDebatePanel(
    state: GameState,
    responses: List<NpcResponse>,
    selectedOfficerIds: Set<String>,
    onToggleOpinion: (String) -> Unit,
    onSynthesize: () -> Unit
) {
    val inCourtResponses = responses.filter { response ->
        state.officers.any { it.id == response.officerId } &&
            CharacterAppearanceSystem.canAppearInPalace(state, response.officerId, PalaceIds.CHUIGONG)
    }
    val remoteResponses = responses.filter { response ->
        val officer = state.officers.firstOrNull { it.id == response.officerId }
        officer != null && !CharacterAppearanceSystem.canAppearInPalace(state, response.officerId, PalaceIds.CHUIGONG) &&
            CharacterAppearanceSystem.visibilityFor(state, response.officerId) != com.xiemingxin.nandu.game.CharacterVisibility.HIDDEN
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
        border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("群臣奏对", color = CourtGold, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                if (responses.isNotEmpty() && selectedOfficerIds.isNotEmpty()) {
                    TextButton(onClick = onSynthesize) { Text("综合诸议", color = CourtGold, fontSize = 11.sp) }
                }
            }
            if (inCourtResponses.isEmpty()) {
                Text("殿中一时无人出班奏对。", color = CourtSub, fontSize = 12.sp)
            } else {
                Text("【当殿】", color = CourtGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                inCourtResponses.forEach { response ->
                    val officer = state.officers.first { it.id == response.officerId }
                    DebateCard(response, officer, remote = false, selected = response.officerId in selectedOfficerIds) {
                        onToggleOpinion(response.officerId)
                    }
                }
            }

            if (remoteResponses.isNotEmpty()) {
                Divider(color = CourtSub.copy(alpha = 0.25f))
                Text("【奏札 / 军报转呈】", color = CourtSub, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                remoteResponses.forEach { response ->
                    val officer = state.officers.first { it.id == response.officerId }
                    DebateCard(response, officer, remote = true, selected = response.officerId in selectedOfficerIds) {
                        onToggleOpinion(response.officerId)
                    }
                }
            }
        }
    }
}

@Composable
private fun DebateCard(response: NpcResponse, officer: Officer?, remote: Boolean, selected: Boolean, onSelect: () -> Unit) {
    val c = attitudeColor(response.attitude)
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) Color(0xD93A2910) else Color(0xB90E0A05)),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) CourtGold else c.copy(alpha = 0.55f))
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(officer?.name ?: "朝臣", color = CourtCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(
                        if (remote) "${officer?.faction ?: "朝臣"} · 远程奏报" else "${officer?.faction ?: "朝臣"} · 当殿",
                        color = CourtSub,
                        fontSize = 9.sp
                    )
                }
                Text(if (selected) "已采纳" else attitudeLabel(response.attitude), color = if (selected) CourtGold else c, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Text(if (remote) "奏称：“${response.text}”" else "“${response.text}”", color = CourtCream, fontSize = 12.sp, lineHeight = 18.sp)
            officer?.let {
                Text("忠诚 ${it.loyalty}｜野心 ${it.ambition}｜${it.skills.take(3).joinToString("、")}", color = CourtSub, fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun ImperialDecisionPreview(state: GameState, result: EdictResult, decision: ImperialDecision) {
    val names = result.npcResponses.filter { it.officerId in decision.selectedOfficerIds }
        .mapNotNull { response -> state.officers.firstOrNull { it.id == response.officerId }?.name }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
        border = BorderStroke(1.dp, CourtGold.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("御前裁决", color = CourtGold, fontWeight = FontWeight.Bold)
            Text(
                when {
                    names.isEmpty() && result.npcResponses.isNotEmpty() -> "尚未择定臣议，请点选一位或综合诸议。"
                    names.isEmpty() -> "本议无待采纳臣议，由陛下亲断。"
                    decision.synthesizeOpinions -> "综合诸议：${names.joinToString("、")}"
                    else -> "采纳臣议：${names.joinToString("、")}"
                },
                color = if (names.isEmpty() && result.npcResponses.isNotEmpty()) CourtSub else CourtCream,
                fontSize = 12.sp
            )
            Text("最终圣意：${result.summary}", color = CourtCream, fontSize = 12.sp, lineHeight = 17.sp)
            Text("拟执行 ${result.commands.size} 项军政命令", color = CourtSub, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CommandPanel(state: GameState, result: EdictResult) {
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
                        "${index + 1}. ${WorldPresentationPolicy.commandDescription(state, cmd)}",
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

private fun courtPlaceholder(mode: CourtInputMode): String = when (mode) {
    CourtInputMode.CONSULT -> "例如：\n“国库还能支应多久？”\n“李纲，你怎么看河北局势？”\n“诸卿认为眼下最急的是什么？”"
    CourtInputMode.CHAT -> "例如：\n“朕问你们话呢。”\n“宗泽，今日气色如何？”\n“唉，这天下什么时候才能安稳。”"
    CourtInputMode.ORDER -> "例如：\n“传旨：命枢密院调两万兵增援前线。”\n“拨三万贯军费，准宗泽便宜从事。”"
}

private fun courtConversationHints(state: GameState, mode: CourtInputMode): List<String> = when (mode) {
    CourtInputMode.CONSULT -> {
        val hints = mutableListOf("诸卿怎么看如今局势？")
        if (state.jinThreat >= 60) hints += "枢密院，说说前线军情。"
        if (state.gold < 80_000) hints += "国库还能支应多久？"
        if (state.armies.isNotEmpty()) hints += "诸军现在各在何处？"
        if (hints.size < 3) hints += "眼下最急的一件事是什么？"
        hints.take(4)
    }
    CourtInputMode.CHAT -> listOf("诸卿今日可还安好？", "朕问你们话呢。")
    CourtInputMode.ORDER -> emptyList()
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

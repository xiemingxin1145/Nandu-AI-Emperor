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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.ai.AiProviderType
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.ui.GamePhase
import com.xiemingxin.nandu.ui.UiState
import com.xiemingxin.nandu.ui.components.StoryEventCard
import com.xiemingxin.nandu.ui.theme.*
import androidx.compose.ui.window.Dialog

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
                "V1.3.2",
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
    edictText: String,
    onEdictChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "御笔下评",
            color = ImperialGold,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = edictText,
            onValueChange = onEdictChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = {
                Text(
                    "传朕旨意…\n（如：命岳飞率三万兵从鄂州取襄阳，韩世忠守建康，秦桧暂退中枢）",
                    color = Color(0xFF8B7355),
                    fontSize = 13.sp
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ImperialGold,
                unfocusedBorderColor = Color(0xFF4A3728),
                focusedTextColor = XuanCream,
                unfocusedTextColor = XuanCream,
                cursorColor = ImperialGold
            ),
            shape = RoundedCornerShape(4.dp)
        )

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val hints = listOf("调兵出征", "任命守将", "修缀城防", "筹粮备战", "压制主和", "赏赐名将")
            items(hints) { hint ->
                FilterChip(
                    selected = false,
                    onClick = { onEdictChange(edictText + hint) },
                    label = { Text(hint, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color(0xFF2A1F12),
                        labelColor = ImperialGold
                    )
                )
            }
        }

        Button(
            onClick = onSubmit,
            enabled = edictText.isNotBlank() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ImperialRed,
                disabledContainerColor = Color(0xFF4A1A1A)
            ),
            shape = RoundedCornerShape(4.dp)
        ) {
            Text(
                if (isLoading) "执行中…" else "🔌 朱批下发",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = XuanCream
            )
        }
    }
}

@Composable
fun LoadingView() {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = ImperialGold)
            Spacer(Modifier.height(16.dp))
            Text("群臣议奏中…", color = ImperialGold)
        }
    }
}

@Composable
fun ConfirmEdictView(result: EdictResult, onConfirm: () -> Unit, onCancel: () -> Unit) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AI 奏议解析", color = ImperialGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(result.summary, color = XuanCream, fontSize = 14.sp)
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1208))) {
                Column(Modifier.padding(12.dp)) {
                    Text("执行命令", color = ImperialGold, fontWeight = FontWeight.Bold)
                    result.commands.forEach { cmd ->
                        Text("• ${cmd.type}: ${cmd.officerId} ${cmd.fromCityId}→${cmd.toCityId} ${cmd.troops}兵", color = XuanCream, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            Text("群臣反应", color = ImperialGold, fontWeight = FontWeight.Bold)
            result.npcResponses.forEach { npc ->
                Text("${npc.officerId}：${npc.text}", color = Color(0xFFB8A088), fontSize = 12.sp)
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = ImperialRed)
                ) { Text("准奏", color = XuanCream) }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f)
                ) { Text("驳回") }
            }
        }
    }
}

@Composable
fun ResultView(outcomes: List<String>, rejected: List<String>, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("圣旨执行结果", color = ImperialGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)

        outcomes.forEach { outcome ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1208))) {
                Text(outcome, color = XuanCream, modifier = Modifier.padding(12.dp), fontSize = 13.sp)
            }
        }

        if (rejected.isNotEmpty()) {
            Text("未采纳命令", color = Color.Red, fontWeight = FontWeight.Bold)
            rejected.forEach { Text("• $it", color = Color.Red, fontSize = 12.sp) }
        }

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ImperialGold)
        ) {
            Text("进入下一旬", color = InkBlack, fontWeight = FontWeight.Bold)
        }
    }
}

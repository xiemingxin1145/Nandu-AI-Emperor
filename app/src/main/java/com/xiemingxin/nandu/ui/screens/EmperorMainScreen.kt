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
import com.xiemingxin.nandu.ui.theme.*


@Composable
fun EmperorMainScreen(
    uiState: UiState,
    onSubmitEdict: (String) -> Unit,
    onConfirmEdict: (String) -> Unit,
    onCancelEdict: () -> Unit,
    onDismissResult: () -> Unit,
    onOpenSettings: () -> Unit
) {
    var edictText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── HUD 顶栏 ──
            GameHUD(state = uiState.gameState, onSettings = onOpenSettings)

            // ── 主内容区 ──
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
                    GamePhase.AI_PROCESSING -> {
                        LoadingView()
                    }
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
                                onDismissResult()
                            }
                        )
                    }
                }
            }

            // 错误提示
            uiState.errorMessage?.let { msg ->
                Text(
                    text = "⚠ $msg",
                    color = Color.Red,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(8.dp)
                )
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
        Text(
            text = "🐉 ${state.era} 第${state.turn}旬",
            color = ImperialGold,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HudStat("💰", "${state.gold / 1000}k")
            HudStat("🌾", "${state.grain / 1000}k")
            HudStat("⚔", "${state.troopMorale}")
            HudStat("🏯", "${state.jinThreat}", if (state.jinThreat > 80) Color.Red else Color.White)
        }
        Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            IconButton(onClick = onSettings, modifier = Modifier.size(32.dp)) {
                Text("⚙", fontSize = 16.sp)
            }
            Text(
                "V0.3",
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

        // 圣旨输入框
        OutlinedTextField(
            value = edictText,
            onValueChange = onEdictChange,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            placeholder = {
                Text(
                    "传朕旨意\u2026\n（如：命岳飞率三万兵从鄂州取襄阳，韩世忠守建康，秦桧暂退中枢）",
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

        // 快捷提示
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

        // 朱批下发按钮
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
                color = Color.White
            )
        }
    }
}

@Composable
fun LoadingView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = ImperialGold)
        Spacer(Modifier.height(16.dp))
        Text("群臣入殿，AI推演中…", color = XuanCream, fontSize = 14.sp)
        Spacer(Modifier.height(8.dp))
        Text("御前推演官正在解读圣旨", color = Color(0xFF8B7355), fontSize = 12.sp)
    }
}

@Composable
fun ConfirmEdictView(
    result: EdictResult,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 圣旨摘要
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1508)),
            border = androidx.compose.foundation.BorderStroke(1.dp, ImperialGold)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("📜 御前推演官解读", color = ImperialGold, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
                Text(result.summary, color = XuanCream, fontSize = 14.sp)

                if (result.riskTags.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠ 风险：${result.riskTags.joinToString("  ")}", color = Color(0xFFFF8C42), fontSize = 12.sp)
                }
            }
        }

        // 群臣反应
        if (result.npcResponses.isNotEmpty()) {
            Text("【御前议政】", color = ImperialGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            result.npcResponses.forEach { response ->
                NpcResponseCard(response.officerId, response.attitude, response.text)
            }
        }

        // 命令预览
        if (result.commands.isNotEmpty()) {
            Text("【待执行命令】", color = ImperialGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            result.commands.forEach { cmd ->
                Text(
                    "• ${cmd.type}：${cmd.officerId} ${cmd.fromCityId} → ${cmd.toCityId} ${if (cmd.troops > 0) "${cmd.troops}兵" else ""}",
                    color = XuanCream,
                    fontSize = 12.sp
                )
            }
        }

        if (result.clarificationNeeded) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A0A))) {
                Text(
                    "📖 ${result.clarificationHint}",
                    color = Color(0xFFFFDD44),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 三个按钮
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = JadeGreen)
            ) { Text("✓ 照准", fontWeight = FontWeight.Bold) }

            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f).height(44.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, ImperialGold)
            ) { Text("改旨", color = ImperialGold) }
        }
    }
}

@Composable
fun NpcResponseCard(officerId: String, attitude: String, text: String) {
    val (icon, name) = when (officerId) {
        "yue_fei"       -> "⚔" to "岳飞"
        "qin_hui"       -> "🐍" to "秦桧"
        "zhao_ding"     -> "📊" to "赵鼎"
        "han_shizhong"  -> "🚢" to "韩世忠"
        "li_gang"       -> "🏯" to "李纲"
        "zong_ze"       -> "🌅" to "宗泽"
        "wu_jie"        -> "🏔" to "吴玠"
        "zhang_jun"     -> "⚖" to "张泼"
        else            -> "👤" to officerId
    }
    val attitudeColor = when (attitude) {
        "support"   -> Color(0xFF2ECC71)
        "oppose"    -> Color(0xFFE74C3C)
        "concerned" -> Color(0xFFF39C12)
        else        -> Color.Gray
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12100A)),
        modifier = Modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, attitudeColor.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
            Text("$icon $name", color = attitudeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.width(72.dp))
            Text("：$text", color = XuanCream, fontSize = 13.sp)
        }
    }
}

@Composable
fun ResultView(outcomes: List<String>, rejected: List<String>, onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("【圣旨已下，天下有变】", color = ImperialGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)

        outcomes.forEach { outcome ->
            Text("✦ $outcome", color = XuanCream, fontSize = 13.sp)
        }

        if (rejected.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            rejected.forEach { r ->
                Text("✗ $r", color = Color(0xFFFF6B6B), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1F12)),
            border = androidx.compose.foundation.BorderStroke(1.dp, ImperialGold)
        ) { Text("下一旬", color = ImperialGold, fontWeight = FontWeight.Bold) }
    }
}

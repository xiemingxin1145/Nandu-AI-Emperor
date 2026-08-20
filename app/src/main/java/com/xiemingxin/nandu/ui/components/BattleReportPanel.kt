package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.game.BattleOutcome

private val Gold   = Color(0xFFC9A227)
private val Cream  = Color(0xFFE8DCC0)
private val Dark   = Color(0xFF0E0A05)
private val Sub    = Color(0xFF9A8862)
private val Green  = Color(0xFF8FB573)
private val Red    = Color(0xFFE57373)

/**
 * Stage 5 战报面板
 */
@Composable
fun BattleReportPanel(
    outcome: BattleOutcome,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Dark),
            border = BorderStroke(1.dp, if (outcome.attackerWins) Gold else Red.copy(alpha = 0.8f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp)
            ) {
                // 标题
                val titleColor = if (outcome.attackerWins) Gold else Red
                val titleText = when {
                    outcome.attackerWins && outcome.cityCaptured -> "⚔ 克城！"
                    outcome.attackerWins -> "⚔ 胜战"
                    else -> "⚔ 败北"
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text(titleText, color = titleColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("X", color = Sub, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = titleColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))

                // 战斗类型
                Text(
                    if (outcome.battleType == "FIELD") "野战" else "攻城战",
                    color = Sub, fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))

                // 核心数据
                BattleStatRow("综合优势", "${outcome.advantage}%",
                    if (outcome.advantage >= 55) Green else if (outcome.advantage >= 40) Gold else Red)
                BattleStatRow("我方兵力", "${outcome.attackerRemaining / 1000}k（折${outcome.attackerLosses / 1000}k）",
                    Cream)
                BattleStatRow("敌方兵力", "${outcome.defenderRemaining / 1000}k（折${outcome.defenderLosses / 1000}k）",
                    if (outcome.attackerWins) Green else Red)
                BattleStatRow("我军士气", "${outcome.attackerMoraleAfter}%",
                    if (outcome.attackerMoraleAfter >= 65) Green else Red)

                if (outcome.cityCaptured) {
                    Spacer(Modifier.height(6.dp))
                    Text("✅ 城池易主，旗帜变更", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // 主要因素
                if (outcome.modifiers.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("战场要素", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    outcome.modifiers.take(6).forEach { mod ->
                        Text("· $mod", color = Sub, fontSize = 11.sp)
                    }
                }

                // 战报全文
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Sub.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))
                Text("战报", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    outcome.report,
                    color = Cream,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )

                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = titleColor.copy(alpha = 0.8f))
                ) {
                    Text("收战报", color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun BattleStatRow(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Sub, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.agent.AgentPlanType
import com.xiemingxin.nandu.agent.AgentProposal
import com.xiemingxin.nandu.agent.CharacterAgentState
import com.xiemingxin.nandu.agent.RelationTag
import com.xiemingxin.nandu.game.GameState

private val Gold = Color(0xFFC9A227)
private val Cream = Color(0xFFE8DCC0)
private val Sub = Color(0xFF9A8862)
private val Dark = Color(0xFF1A1208)
private val Green = Color(0xFF8FB573)
private val Red = Color(0xFFE57373)

/** Stage 8 人物 Agent 状态面板。 */
@Composable
fun AgentStatusPanel(
    agentState: CharacterAgentState,
    gameState: GameState,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Dark.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text("内心动态", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        AgentRow("长期志向", agentState.longTermGoal.label, Gold)
        AgentRow(
            "当前计划",
            agentState.currentPlan.label,
            if (agentState.currentPlan != AgentPlanType.OBSERVE && agentState.currentPlan != AgentPlanType.PRIVATE_ALLIANCE) Cream else Sub
        )
        AgentRow(
            "对陛下态度",
            agentState.emperorAttitude.label,
            when {
                agentState.loyaltyToEmperor >= 70 -> Green
                agentState.loyaltyToEmperor >= 40 -> Color(0xFFFFD54F)
                else -> Red
            }
        )

        Spacer(Modifier.height(6.dp))
        HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 0.5.dp)
        Spacer(Modifier.height(6.dp))

        Row(Modifier.fillMaxWidth()) {
            MiniStat("忠诚", agentState.loyaltyToEmperor, Green, Modifier.weight(1f))
            MiniStat("野心", agentState.ambition, Color(0xFFFFD54F), Modifier.weight(1f))
            MiniStat("压力", agentState.fearLevel, Red, Modifier.weight(1f))
        }

        if (agentState.adviceAdoptedCount + agentState.adviceRejectedCount > 0) {
            Spacer(Modifier.height(6.dp))
            Text(
                "建言：采纳${agentState.adviceAdoptedCount}次  驳回${agentState.adviceRejectedCount}次",
                color = Sub,
                fontSize = 11.sp
            )
        }

        if (agentState.relations.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))
            Text("人际关系", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            agentState.relations.values.take(3).forEach { rel ->
                val name = gameState.officers.find { it.id == rel.targetOfficerId }?.name ?: rel.targetOfficerId
                val color = when (rel.tag) {
                    RelationTag.ALLY, RelationTag.SUPPORTER, RelationTag.FACTION_BROTHER -> Green
                    RelationTag.RIVAL, RelationTag.ENEMY -> Red
                    else -> Sub
                }
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(name, color = Cream, fontSize = 11.sp)
                    Text(rel.tag.label, color = color, fontSize = 10.sp)
                }
                if (rel.lastEventSummary.isNotBlank()) {
                    Text("  ${rel.lastEventSummary}", color = Sub, fontSize = 9.sp)
                }
            }
        }

        if (agentState.recentMemory.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))
            Text("近期记忆", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            agentState.recentMemory.takeLast(3).reversed().forEach { mem ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        "第${mem.turn}旬",
                        color = Sub,
                        fontSize = 9.sp,
                        modifier = Modifier.width(42.dp)
                    )
                    Text(mem.summary, color = Cream, fontSize = 10.sp, modifier = Modifier.weight(1f))
                    if (mem.significance >= 2) {
                        Text("★", color = Gold, fontSize = 9.sp)
                    }
                }
                Spacer(Modifier.height(2.dp))
            }
        }

        if (agentState.currentPlan != AgentPlanType.OBSERVE && agentState.currentPlan != AgentPlanType.PRIVATE_ALLIANCE) {
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(6.dp))
            Text("拟上奏请", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("· ${agentState.currentPlan.label}", color = Cream, fontSize = 10.sp)
            if (agentState.currentPlan.edict.isNotBlank()) {
                Text("  建议：${agentState.currentPlan.edict}", color = Sub, fontSize = 9.sp)
            }
        }
    }
}

/** 活跃提案卡片（供朝议/奏报界面使用）。 */
@Composable
fun AgentProposalCard(
    proposal: AgentProposal,
    officerName: String,
    onAccept: () -> Unit = {},
    onReject: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Dark),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, Gold.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(officerName, color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(proposal.kind.label, color = Sub, fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(proposal.reason, color = Cream, fontSize = 12.sp)
            if (proposal.edictSuggestion.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text("建议：${proposal.edictSuggestion}", color = Sub, fontSize = 10.sp)
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onReject) { Text("不准", color = Red, fontSize = 11.sp) }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onAccept,
                    colors = ButtonDefaults.buttonColors(containerColor = Gold.copy(alpha = 0.8f))
                ) {
                    Text("准", color = Color.White, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun AgentRow(label: String, value: String, valueColor: Color) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Sub, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MiniStat(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 2.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value.toString(), color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Text(label, color = Sub, fontSize = 9.sp)
    }
}

package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.game.BattleOutcome
import com.xiemingxin.nandu.game.VideoResourceRegistry

private val Gold   = Color(0xFFC9A227)
private val Cream  = Color(0xFFE8DCC0)
private val Dark   = Color(0xFF0E0A05)
private val Sub    = Color(0xFF9A8862)
private val Green  = Color(0xFF8FB573)
private val Red    = Color(0xFFE57373)

/**
 * Stage 5 战报面板 + V3 战斗视频演出。
 * 先播战斗类型短片，再自动播胜利/失败短片；任何视频解码失败都只跳过视频，不影响战报。
 */
@Composable
fun BattleReportPanel(
    outcome: BattleOutcome,
    onDismiss: () -> Unit
) {
    val battleClipId = when (outcome.battleType.uppercase()) {
        "SIEGE" -> "siege_assault"
        "NAVAL", "RIVER_NAVAL" -> "naval_clash"
        "MOUNTAIN", "PASS" -> "mountain_pass"
        else -> "field_clash"
    }
    val resultClipId = if (outcome.attackerWins) "victory" else "defeat"
    val videoSequence = remember(outcome.report, battleClipId, resultClipId) {
        listOfNotNull(
            VideoResourceRegistry.find(battleClipId),
            VideoResourceRegistry.find(resultClipId)
        )
    }
    var videoIndex by remember(outcome.report) { mutableStateOf(0) }

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
                val titleColor = if (outcome.attackerWins) Gold else Red
                val titleText = when {
                    outcome.attackerWins && outcome.cityCaptured -> "⚔ 克城！"
                    outcome.attackerWins -> "⚔ 胜战"
                    else -> "⚔ 败北"
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(titleText, color = titleColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("X", color = Sub, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(8.dp))

                // 真正把 V3 战斗 MP4 接到战报。视频静音，游戏 BGM/SFX 继续由声音系统负责。
                if (videoSequence.isNotEmpty() && videoIndex < videoSequence.size) {
                    val clip = videoSequence[videoIndex]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .background(Color.Black)
                    ) {
                        AssetVideoSurface(
                            path = clip.path,
                            loop = false,
                            muted = true,
                            onCompletion = {
                                if (videoIndex < videoSequence.lastIndex) videoIndex += 1
                            },
                            onError = {
                                if (videoIndex < videoSequence.lastIndex) videoIndex += 1
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (videoIndex == 0) "战场演出 · ${videoSequence.first().name}" else "战果演出 · ${clip.name}",
                        color = Sub,
                        fontSize = 9.sp
                    )
                    Spacer(Modifier.height(6.dp))
                }

                HorizontalDivider(color = titleColor.copy(alpha = 0.4f))
                Spacer(Modifier.height(8.dp))

                Text(
                    when (outcome.battleType.uppercase()) {
                        "FIELD" -> "野战"
                        "SIEGE" -> "攻城战"
                        "NAVAL", "RIVER_NAVAL" -> "水战"
                        "MOUNTAIN", "PASS" -> "山地/关隘战"
                        else -> outcome.battleType
                    },
                    color = Sub,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(4.dp))

                BattleStatRow(
                    "综合优势",
                    "${outcome.advantage}%",
                    if (outcome.advantage >= 55) Green else if (outcome.advantage >= 40) Gold else Red
                )
                BattleStatRow(
                    "我方兵力",
                    "${outcome.attackerRemaining / 1000}k（折${outcome.attackerLosses / 1000}k）",
                    Cream
                )
                BattleStatRow(
                    "敌方兵力",
                    "${outcome.defenderRemaining / 1000}k（折${outcome.defenderLosses / 1000}k）",
                    if (outcome.attackerWins) Green else Red
                )
                BattleStatRow(
                    "我军士气",
                    "${outcome.attackerMoraleAfter}%",
                    if (outcome.attackerMoraleAfter >= 65) Green else Red
                )

                if (outcome.cityCaptured) {
                    Spacer(Modifier.height(6.dp))
                    Text("✅ 城池易主，旗帜变更", color = Gold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                if (outcome.modifiers.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text("战场要素", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    outcome.modifiers.take(6).forEach { mod ->
                        Text("· $mod", color = Sub, fontSize = 11.sp)
                    }
                }

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
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Sub, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

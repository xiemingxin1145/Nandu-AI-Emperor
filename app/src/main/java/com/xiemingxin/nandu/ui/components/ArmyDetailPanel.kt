package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.Army
import com.xiemingxin.nandu.game.ArmyStatus
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.MapData
import com.xiemingxin.nandu.game.Officer

private val Gold   = Color(0xFFC9A227)
private val Cream  = Color(0xFFE8DCC0)
private val Dark   = Color(0xFF1A1208)
private val Sub    = Color(0xFF9A8862)
private val Green  = Color(0xFF8FB573)
private val Red    = Color(0xFFE57373)

/**
 * Stage 4 军团详情面板
 */
@Composable
fun ArmyDetailPanel(
    army: Army,
    gameState: GameState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onAttack: (() -> Unit)? = null,      // Stage5: 进攻按钮回调
    onRetreat: (() -> Unit)? = null,     // Stage5: 撤退按钮回调
    onCommanderClick: ((Officer) -> Unit)? = null
) {
    val commander = gameState.officers.find { it.id == army.commanderId }
    val currentCity = gameState.cities.find { it.id == army.currentCityId }
    val targetCity  = gameState.cities.find { it.id == army.targetCityId }

    val routeDesc = if (army.routeNodeIds.size > 1) {
        army.routeNodeIds.mapNotNull { id ->
            gameState.cities.find { it.id == id }?.name ?: MapData.nodeMap[id]?.name
        }.joinToString(" → ")
    } else ""

    val statusColor = when (army.statusCode) {
        ArmyStatus.GARRISONED         -> Green
        ArmyStatus.MARCHING           -> Color(0xFFFFD54F)
        ArmyStatus.ENGAGEMENT_PENDING -> Red
        ArmyStatus.STANDBY            -> Sub
        ArmyStatus.DISBANDED          -> Sub
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Dark),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(army.name, color = Gold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Text("X", color = Sub, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.height(4.dp))
            val commanderName = commander?.name ?: "无"
            if (commander != null && onCommanderClick != null) {
                val opened = commander
                Text(
                    text = "主帅：$commanderName  · 点此阅履历",
                    color = Cream,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable { onCommanderClick.invoke(opened) }
                )
            } else {
                Text("主帅：$commanderName", color = Cream, fontSize = 13.sp)
            }
            Text("所属：${army.ownerFactionId}", color = Sub, fontSize = 11.sp)
            Spacer(Modifier.height(8.dp))

            // 状态
            Text("状态：${army.status}", color = statusColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            currentCity?.let { Text("驻地：${it.name}", color = Cream, fontSize = 12.sp) }
            if (targetCity != null && army.statusCode == ArmyStatus.MARCHING) {
                Text("目标：${targetCity.name}", color = Color(0xFFFFD54F), fontSize = 12.sp)
                if (army.marchDaysRemaining > 0)
                    Text("预计：还需约${army.marchDaysRemaining}日", color = Sub, fontSize = 11.sp)
                if (routeDesc.isNotBlank())
                    Text("路线：$routeDesc", color = Sub, fontSize = 10.sp)
            }
            if (army.statusCode == ArmyStatus.ENGAGEMENT_PENDING && targetCity != null)
                Text("敌前：${targetCity.name}（等候皇命）", color = Red, fontSize = 12.sp)

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Gold.copy(alpha = 0.2f), thickness = 0.5.dp)
            Spacer(Modifier.height(8.dp))

            // 核心数值（Stage5：内联StatRow避免private function scope问题）
            ArmyStatLine("兵力", "${army.troops / 1000}k", Gold)
            ArmyStatLine("士气", "${army.morale}%", if (army.morale >= 70) Green else if (army.morale >= 40) Color(0xFFFFD54F) else Red)
            ArmyStatLine("补给", "${army.supplyLevel}%", when {
                army.supplyLevel >= 60 -> Green
                army.supplyLevel >= 25 -> Color(0xFFFFD54F)
                else -> Red
            })
            ArmyStatLine("军型", army.armyType, Sub)
            commander?.let {
                ArmyStatLine("主帅统率", "${it.command}", if (it.command >= 80) Gold else Cream)
            }
            // Stage 5: 战争操作按钮
            if (army.statusCode == ArmyStatus.ENGAGEMENT_PENDING) {
                Spacer(Modifier.height(12.dp))
                val targetName = gameState.cities.find { it.id == army.targetCityId }?.name ?: army.targetCityId
                if (targetName.isNotBlank()) {
                    Text("目标：$targetName", color = Red.copy(alpha = 0.8f), fontSize = 11.sp)
                    Spacer(Modifier.height(6.dp))
                }
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onAttack != null) {
                        androidx.compose.material3.Button(
                            onClick = onAttack,
                            modifier = Modifier.weight(1f),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = Red.copy(alpha = 0.85f)
                            )
                        ) { Text("⚔ 进攻", color = Color.White, fontSize = 13.sp) }
                    }
                    if (onRetreat != null) {
                        androidx.compose.material3.OutlinedButton(
                            onClick = onRetreat,
                            modifier = Modifier.weight(1f)
                        ) { Text("← 撤退", fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArmyStatLine(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Sub, fontSize = 12.sp)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.BattleDirectiveSystem
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.ShunchangDirective

private val DirectiveInk = Color(0xFF0A0704)
private val DirectiveGold = Color(0xFFC9A227)
private val DirectiveCream = Color(0xFFE8DCC0)
private val DirectiveSub = Color(0xFF9A8862)
private val DirectiveRed = Color(0xFF8B1A1A)

/**
 * STAB-003：正式战役军令栏。
 *
 * 这里只发出“选择”，不在 UI 里直接改任何数值；实际修改统一交给
 * BattleDirectiveSystem -> GameState。每旬下令一次，防止反复点击刷资源/士气。
 */
@Composable
fun ShunchangDirectivePanel(
    state: GameState,
    feedback: String?,
    onDirective: (ShunchangDirective) -> Unit,
    modifier: Modifier = Modifier
) {
    val issued = BattleDirectiveSystem.directiveIssuedThisTurn(state)
    val latest = BattleDirectiveSystem.latestDirectiveEntry(state)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
        colors = CardDefaults.cardColors(containerColor = DirectiveInk),
        border = BorderStroke(1.dp, DirectiveGold.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                if (issued) "本旬军令已发" else "御前军令 · 选择将真实改变世界",
                color = DirectiveGold,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                DirectiveButton(
                    label = "固守",
                    enabled = !issued,
                    modifier = Modifier.weight(1f),
                    onClick = { onDirective(ShunchangDirective.HOLD) }
                )
                DirectiveButton(
                    label = "驰援",
                    enabled = !issued,
                    modifier = Modifier.weight(1f),
                    onClick = { onDirective(ShunchangDirective.REINFORCE) }
                )
                DirectiveButton(
                    label = "再议",
                    enabled = !issued,
                    modifier = Modifier.weight(1f),
                    onClick = { onDirective(ShunchangDirective.DELIBERATE) }
                )
            }

            val line = feedback?.takeIf { it.contains("军令") }
                ?: latest?.outcomes?.lastOrNull()
                ?: "固守会动用真实粮草与城防；驰援会让真实军团沿地图行军；再议也会承担前线等待的代价。"
            Text(
                line,
                color = if (line.contains("失败") || line.contains("未发")) DirectiveRed else DirectiveSub,
                fontSize = 9.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun DirectiveButton(
    label: String,
    enabled: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DirectiveGold.copy(alpha = 0.16f),
            contentColor = DirectiveCream,
            disabledContainerColor = Color(0xFF17120B),
            disabledContentColor = DirectiveSub
        ),
        border = BorderStroke(1.dp, DirectiveGold.copy(alpha = if (enabled) 0.55f else 0.2f))
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

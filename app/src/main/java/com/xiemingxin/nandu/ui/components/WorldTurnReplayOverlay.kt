package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.SeasonalTransition
import com.xiemingxin.nandu.game.WorldTurnAction
import com.xiemingxin.nandu.game.WorldTurnReplay

private val ReplayGold = Color(0xFFC9A227)
private val ReplayCream = Color(0xFFE8DCC0)

@Composable
fun SeasonalTransitionOverlay(transition: SeasonalTransition, onDismiss: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        // The actual seasonal CG is laid first and survives every Media3 failure.
        AssetImage(
            path = transition.fallbackImagePath,
            fallbackPath = "images/map/song_world_parchment.webp",
            contentDescription = "${transition.to.label}季山河",
            placeholderText = "",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        AssetVideoSurface(
            path = transition.videoPath,
            modifier = Modifier.fillMaxSize(),
            loop = false,
            onCompletion = onDismiss
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0x22000000), Color(0x66000000)))
            )
        )
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("时序流转", color = ReplayCream, fontSize = 15.sp)
            Spacer(Modifier.height(8.dp))
            Text("${transition.from.label}去${transition.to.label}来", color = ReplayGold, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            border = BorderStroke(1.dp, ReplayGold)
        ) { Text("跳过", color = ReplayCream) }
    }
}

@Composable
fun WorldTurnReplayOverlay(
    replay: WorldTurnReplay,
    currentAction: WorldTurnAction?,
    currentActionNumber: Int,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember(replay.turn) { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xED171007)),
        border = BorderStroke(1.dp, ReplayGold.copy(alpha = 0.75f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("天下推演", color = ReplayGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(replay.dateLabel, color = ReplayCream, fontSize = 11.sp)
            }
            if (currentAction != null) {
                Text(
                    "${currentAction.factionName} · ${currentAction.kind.label} · ${currentActionNumber}/${replay.actions.size}",
                    color = ReplayGold,
                    fontSize = 12.sp
                )
                Text(currentAction.detail, color = ReplayCream, fontSize = 13.sp)
            } else if (replay.actions.isEmpty()) {
                Text("本旬未发生可标注的军团移动、战斗或城池易手。", color = ReplayCream, fontSize = 12.sp)
            } else {
                Text("本旬共记录 ${replay.actions.size} 项真实天下变化。", color = ReplayCream, fontSize = 12.sp)
            }

            if (expanded) {
                Column(
                    modifier = Modifier.height(150.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    val lines = replay.actions.map { "【${it.factionName}】${it.detail}" } + replay.reports
                    if (lines.isEmpty()) {
                        Text("本旬暂无军政异动。", color = ReplayCream, fontSize = 11.sp)
                    } else {
                        lines.forEach { Text(it, color = ReplayCream, fontSize = 11.sp, lineHeight = 16.sp) }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, ReplayGold.copy(alpha = 0.65f))
                ) { Text(if (expanded) "收起纪要" else "本旬天下纪要", color = ReplayGold, fontSize = 11.sp) }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7D1D16))
                ) { Text("结束推演", color = ReplayCream, fontSize = 11.sp) }
            }
        }
    }
}

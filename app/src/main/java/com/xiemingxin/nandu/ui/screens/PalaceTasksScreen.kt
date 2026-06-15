package com.xiemingxin.nandu.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.PalaceRegistry
import com.xiemingxin.nandu.game.PalaceTask
import com.xiemingxin.nandu.game.PalaceTaskSystem
import com.xiemingxin.nandu.game.TaskSeverity
import com.xiemingxin.nandu.ui.components.AssetImage

private val TaskGold = Color(0xFFC9A227)
private val TaskCream = Color(0xFFE8DCC0)
private val TaskInk = Color(0xFF0E0A05)
private val TaskSub = Color(0xFF9A8862)
private val TaskRed = Color(0xFF7D1D16)

@Composable
fun PalaceTasksScreen(
    state: GameState,
    palaceId: String,
    onBack: () -> Unit,
    onDraftEdict: (String) -> Unit,
    onOpenTab: (Int) -> Unit
) {
    val palace = PalaceRegistry.byId(palaceId)
    val tasks = PalaceTaskSystem.tasksForPalace(state, palaceId)

    Box(modifier = Modifier.fillMaxSize().background(TaskInk)) {
        AssetImage(
            path = palace.backgroundPath,
            contentDescription = palace.name,
            contentScale = ContentScale.Crop,
            placeholderText = palace.name.take(1),
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(TaskInk.copy(alpha = 0.40f), TaskInk.copy(alpha = 0.25f), TaskInk.copy(alpha = 0.94f))
                )
            )
        )

        Column(modifier = Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xD70E0A05)),
                border = BorderStroke(1.dp, TaskGold.copy(alpha = 0.55f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(palace.icon, fontSize = 28.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(palace.name, color = TaskGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(palace.subtitle, color = TaskCream, fontSize = 12.sp)
                        Text("${state.calendar.displayText()} · 待办 ${tasks.size} 件", color = TaskSub, fontSize = 10.sp)
                    }
                    OutlinedButton(onClick = onBack, border = BorderStroke(1.dp, TaskGold.copy(alpha = 0.55f))) {
                        Text("回宫", color = TaskGold, fontSize = 12.sp)
                    }
                }
            }

            if (tasks.isEmpty()) {
                EmptyPalaceTaskCard(palace.name)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    items(tasks) { task ->
                        PalaceTaskCard(
                            task = task,
                            state = state,
                            onDraftEdict = {
                                if (task.edictDraft.isNotBlank()) onDraftEdict(task.edictDraft)
                                else onOpenTab(task.recommendedTab)
                            },
                            onOpenTab = { onOpenTab(task.recommendedTab) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyPalaceTaskCard(palaceName: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xD61A1208)),
        border = BorderStroke(1.dp, TaskGold.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("暂无急务", color = TaskGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("$palaceName 本旬无专属待办。可回皇宫查看其他宫殿，或进入垂拱殿主动下旨。", color = TaskCream, fontSize = 13.sp, lineHeight = 20.sp)
        }
    }
}

@Composable
private fun PalaceTaskCard(
    task: PalaceTask,
    state: GameState,
    onDraftEdict: () -> Unit,
    onOpenTab: () -> Unit
) {
    val severityColor = when (task.severity) {
        TaskSeverity.URGENT -> Color(0xFFFF6B5A)
        TaskSeverity.HIGH -> Color(0xFFE0A747)
        TaskSeverity.MEDIUM -> TaskGold
        TaskSeverity.LOW -> Color(0xFF8FB573)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xDF1A1208)),
        border = BorderStroke(1.dp, severityColor.copy(alpha = 0.58f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, color = TaskCream, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("${task.source.label} · ${task.severity.label}", color = severityColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("Turn ${state.turn}", color = TaskSub, fontSize = 9.sp)
            }
            Text(task.description, color = TaskCream, fontSize = 12.sp, lineHeight = 19.sp)
            if (task.relatedOfficerIds.isNotEmpty() || task.relatedCityIds.isNotEmpty()) {
                Text(
                    listOf(
                        task.relatedOfficerIds.takeIf { it.isNotEmpty() }?.joinToString("、") { officerName(state, it) }?.let { "相关官员：$it" },
                        task.relatedCityIds.takeIf { it.isNotEmpty() }?.joinToString("、") { cityName(state, it) }?.let { "相关城池：$it" }
                    ).filterNotNull().joinToString("｜"),
                    color = TaskSub,
                    fontSize = 10.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = onDraftEdict,
                    modifier = Modifier.weight(1f).height(42.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TaskRed),
                    shape = RoundedCornerShape(8.dp)
                ) { Text(if (task.edictDraft.isNotBlank()) "拟旨处理" else "进入处理", color = TaskCream, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                OutlinedButton(
                    onClick = onOpenTab,
                    modifier = Modifier.weight(1f).height(42.dp),
                    border = BorderStroke(1.dp, TaskGold.copy(alpha = 0.55f))
                ) { Text("查看相关页", color = TaskGold, fontSize = 12.sp) }
            }
        }
    }
}

private fun officerName(state: GameState, officerId: String): String =
    state.officers.firstOrNull { it.id == officerId }?.name ?: officerId

private fun cityName(state: GameState, cityId: String): String =
    state.cities.firstOrNull { it.id == cityId }?.name ?: cityId

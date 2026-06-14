package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.story.EventDirector
import com.xiemingxin.nandu.story.StoryEvent
import com.xiemingxin.nandu.story.StoryEventEffectApplier
import com.xiemingxin.nandu.story.StoryEventLoader
import com.xiemingxin.nandu.ui.components.StoryEventCard

/**
 * V0.7.1 剧情事件页面骨架。
 * 可独立接入任意导航；内部完成读取、筛选、选择、结算、触发记录。
 */
@Composable
fun StoryEventScreen(
    initialState: GameState,
    modifier: Modifier = Modifier,
    initialFiredEventIds: Set<String> = emptySet(),
    initialFlags: Set<String> = emptySet(),
    onStateChange: (GameState) -> Unit = {},
    onEventResolved: (StoryEvent, String, Set<String>) -> Unit = { _, _, _ -> }
) {
    val context = LocalContext.current
    var events by remember { mutableStateOf<List<StoryEvent>>(emptyList()) }
    var currentState by remember(initialState) { mutableStateOf(initialState) }
    var firedEventIds by remember { mutableStateOf(initialFiredEventIds) }
    var flags by remember { mutableStateOf(initialFlags) }
    var lastOutcomes by remember { mutableStateOf<List<String>>(emptyList()) }

    LaunchedEffect(Unit) {
        events = StoryEventLoader.loadJianyan01(context)
    }

    val candidate = remember(currentState, events, firedEventIds, flags) {
        EventDirector.firstCandidate(
            state = currentState,
            events = events,
            firedEventIds = firedEventIds,
            flags = flags
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StoryEventStatusPanel(
            state = currentState,
            totalEvents = events.size,
            firedCount = firedEventIds.size,
            flagCount = flags.size
        )

        if (candidate != null) {
            StoryEventCard(event = candidate) { choiceId ->
                val result = StoryEventEffectApplier.applyChoice(currentState, candidate, choiceId)
                currentState = result.newState
                firedEventIds = firedEventIds + candidate.eventId
                flags = flags + result.flags
                lastOutcomes = result.outcomes
                onStateChange(result.newState)
                onEventResolved(candidate, choiceId, result.flags)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "本旬暂无可触发剧情", style = MaterialTheme.typography.titleMedium)
                    Text(text = "可继续推进回合，或等待金国威胁、粮草、民心、派系力量变化。")
                }
            }
        }

        if (lastOutcomes.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "剧情结算", style = MaterialTheme.typography.titleMedium)
                    lastOutcomes.forEach { Text(text = it, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun StoryEventStatusPanel(
    state: GameState,
    totalEvents: Int,
    firedCount: Int,
    flagCount: Int
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = "剧情事件 V0.7.1", style = MaterialTheme.typography.titleLarge)
            Text(text = "${state.calendar.displayText()} · 第${state.turn}旬")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "事件 $firedCount/$totalEvents")
                Text(text = "Flag $flagCount")
            }
            Text(
                text = "国库 ${state.gold} · 粮草 ${state.grain} · 军心 ${state.troopMorale} · 朝局 ${state.courtStability} · 金威胁 ${state.jinThreat}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "主战 ${state.warFactionPower} / 主和 ${state.peaceFactionPower}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

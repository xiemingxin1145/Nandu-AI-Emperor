package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xiemingxin.nandu.story.StoryEvent

/** V0.7.0 剧情事件展示卡片。暂不接导航，供后续页面复用。 */
@Composable
fun StoryEventCard(
    event: StoryEvent,
    modifier: Modifier = Modifier,
    onChoice: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(text = event.chapter, style = MaterialTheme.typography.labelMedium)
            Text(text = event.title, style = MaterialTheme.typography.titleLarge)
            Text(text = event.description, style = MaterialTheme.typography.bodyMedium)

            if (event.riskTags.isNotEmpty()) {
                Text(
                    text = "风险：${event.riskTags.joinToString(" / ")}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            if (event.artHint.isNotBlank()) {
                Text(
                    text = "美术提示：${event.artHint}",
                    style = MaterialTheme.typography.labelSmall
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            event.choices.forEachIndexed { index, choice ->
                if (index == 0) {
                    Button(
                        onClick = { onChoice(choice.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(choice.text)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onChoice(choice.id) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(choice.text)
                    }
                }
            }

            if (event.npcReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    event.npcReactions.take(3).forEach { reaction ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${reaction.npcId}：${reaction.text}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

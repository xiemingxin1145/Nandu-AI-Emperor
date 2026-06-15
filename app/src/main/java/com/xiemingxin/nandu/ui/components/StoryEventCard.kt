package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.story.StoryEvent

private val EventGold = Color(0xFFC9A227)
private val EventCream = Color(0xFFE8DCC0)
private val EventDark = Color(0xF21A1208)
private val EventPanel = Color(0xFF25190D)
private val EventSub = Color(0xFF9A8862)
private val EventRed = Color(0xFF7D1D16)

/**
 * V1.3.2 剧情/数据事件卡片。
 * 旧剧情与 JSON 数据包共用此卡片；chapter=数据事件包 时会显示数据事件标识。
 */
@Composable
fun StoryEventCard(
    event: StoryEvent,
    modifier: Modifier = Modifier,
    onChoice: (String) -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = EventDark),
        border = BorderStroke(1.dp, EventGold.copy(alpha = 0.65f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AssetImage(
                path = ArtResourceRegistry.eventImage(event.eventId),
                fallbackPath = ArtResourceRegistry.Fallback.event,
                contentDescription = event.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp)),
                placeholderText = "事件"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TagText(text = if (event.chapter == "数据事件包") "JSON事件" else event.chapter.ifBlank { "朝堂事件" })
                TagText(text = eventTypeName(event.type))
            }

            Text(
                text = event.title,
                color = EventGold,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = event.description,
                color = EventCream,
                fontSize = 13.sp,
                lineHeight = 21.sp
            )

            if (event.riskTags.isNotEmpty()) {
                Text(
                    text = "风险：${event.riskTags.joinToString(" / ")}",
                    color = Color(0xFFFFB36B),
                    fontSize = 11.sp
                )
            }

            if (event.artHint.isNotBlank()) {
                Text(
                    text = "画面：${event.artHint}",
                    color = EventSub,
                    fontSize = 10.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            event.choices.forEachIndexed { index, choice ->
                if (index == 0) {
                    Button(
                        onClick = { onChoice(choice.id) },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EventGold)
                    ) {
                        Text(choice.text, color = Color(0xFF120A02), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { onChoice(choice.id) },
                        modifier = Modifier.fillMaxWidth().height(42.dp),
                        border = BorderStroke(1.dp, EventGold.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = EventGold)
                    ) {
                        Text(choice.text, fontSize = 13.sp)
                    }
                }
            }

            if (event.npcReactions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(EventPanel, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("群臣反应", color = EventGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    event.npcReactions.take(3).forEach { reaction ->
                        Text(
                            text = "${reaction.npcId}：${reaction.text}",
                            color = EventCream,
                            fontSize = 11.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TagText(text: String) {
    Text(
        text = text,
        color = EventGold,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(EventRed.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

private fun eventTypeName(type: String): String = when (type) {
    "main_story" -> "主线"
    "history_event" -> "历史"
    "city_event", "city_crisis" -> "城池"
    "jin_event" -> "金军"
    "diplomacy_event" -> "外交"
    "talent_discovery" -> "人才"
    "random_military" -> "军务"
    "random_court" -> "朝局"
    "folk_rumor" -> "民间"
    else -> type.ifBlank { "事件" }
}

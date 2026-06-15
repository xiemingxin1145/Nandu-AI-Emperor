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
 * V2.5 剧情/数据事件卡片。
 *
 * 事件 CG 不再只依赖 eventId 精确命中，也会根据事件类型、标题、描述、artHint
 * 选择最贴近的事件图，避免动态事件全部落到默认图。
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
                path = eventCgPath(event),
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
                TagText(text = "CG")
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

private fun eventCgPath(event: StoryEvent): String {
    val direct = ArtResourceRegistry.eventImage(event.eventId)
    if (direct != ArtResourceRegistry.Fallback.event) return direct

    val text = listOf(event.title, event.description, event.artHint, event.type, event.chapter).joinToString(" ")
    val key = when {
        text.contains("岳飞") || text.contains("请战") || text.contains("北伐") -> "yuefei_petition"
        text.contains("风波亭") || text.contains("秦桧") || text.contains("冤") -> "fengboting_crisis"
        text.contains("皇后") || text.contains("后苑") || text.contains("内廷") -> "empress_secret"
        text.contains("御书房") || text.contains("密折") || text.contains("夜议") -> "yushufang_night"
        text.contains("太庙") || text.contains("誓师") || text.contains("礼") -> "taimiao_oath"
        text.contains("金使") || text.contains("金国") || text.contains("完颜") -> "jin_envoy"
        text.contains("海贸") || text.contains("市舶") || text.contains("南海") || text.contains("海商") -> "sea_trade_boom"
        text.contains("临安") || text.contains("繁华") || text.contains("民生") -> "linan_prosperity"
        text.contains("江淮") || text.contains("大战") || text.contains("军务") || text.contains("前线") -> "jianghuai_battle"
        text.contains("西夏") || text.contains("马市") -> "xixia_horse_market"
        text.contains("大理") || text.contains("西南") -> "dali_trade"
        text.contains("皇城司") || text.contains("密探") || text.contains("暗线") || text.contains("谍") -> "secret_police"
        event.type == "jin_event" || event.type == "diplomacy_event" -> "jin_envoy"
        event.type == "random_military" || event.type == "city_crisis" -> "jianghuai_battle"
        event.type == "talent_discovery" -> "yuefei_petition"
        event.type == "random_court" -> "yushufang_night"
        event.type == "city_event" -> "linan_prosperity"
        event.type == "folk_rumor" -> "secret_police"
        else -> "default"
    }
    return ArtResourceRegistry.eventImage(key)
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
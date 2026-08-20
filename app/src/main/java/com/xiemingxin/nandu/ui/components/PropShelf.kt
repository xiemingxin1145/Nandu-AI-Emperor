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
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.xiemingxin.nandu.game.PropArt

private val PropGold = Color(0xFFC9A227)
private val PropCream = Color(0xFFE8DCC0)
private val PropSub = Color(0xFF9A8862)
private val PropCard = Color(0xFF21170C)

/**
 * 人物页与事件页共用的物件陈列组件。
 *
 * 若首批 WebP 尚未放入 assets，AssetImage 会走统一 fallback，文字与交互结构仍可先工作；
 * 后续只要把对应文件补进 images/props/v1/，无需再次修改这里。
 */
@Composable
fun PropShelf(
    title: String,
    props: List<PropArt>,
    modifier: Modifier = Modifier
) {
    if (props.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, color = PropGold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            props.take(3).forEach { prop ->
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = PropCard),
                    border = BorderStroke(1.dp, PropGold.copy(alpha = 0.28f))
                ) {
                    Column(modifier = Modifier.padding(7.dp)) {
                        AssetImage(
                            path = prop.imagePath,
                            fallbackPath = ArtResourceRegistry.Fallback.ui,
                            contentDescription = prop.name,
                            contentScale = ContentScale.Fit,
                            placeholderText = prop.name.take(1),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(72.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(Color(0xFF160F08))
                        )
                        Spacer(Modifier.height(5.dp))
                        Text(
                            text = prop.name,
                            color = PropCream,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = prop.category.label,
                            color = PropSub,
                            fontSize = 9.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

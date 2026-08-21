package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.PropArt
import com.xiemingxin.nandu.game.PropResourceRegistry

private val PropGold = Color(0xFFC9A227)
private val PropCream = Color(0xFFE8DCC0)
private val PropSub = Color(0xFF9A8862)
private val PropCard = Color(0xFF21170C)
private val PropDark = Color(0xF21A1208)

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
    modifier: Modifier = Modifier,
    onPropClick: ((PropArt) -> Unit)? = null
) {
    if (props.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, color = PropGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(7.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            props.take(3).forEach { prop ->
                PropThumbCard(
                    prop = prop,
                    modifier = Modifier.weight(1f),
                    onClick = onPropClick
                )
            }
        }
    }
}

@Composable
fun ImperialTreasuryCatalog(
    props: List<PropArt> = PropResourceRegistry.catalog(),
    onPropClick: (PropArt) -> Unit,
    modifier: Modifier = Modifier
) {
    val grouped = props.groupBy { it.category }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        grouped.forEach { (category, items) ->
            Text(category.label, color = PropGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            items.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { prop ->
                        PropThumbCard(
                            prop = prop,
                            modifier = Modifier.weight(1f),
                            imageHeight = 88,
                            onClick = onPropClick
                        )
                    }
                    if (row.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun PropDetailDialog(
    prop: PropArt,
    onDismiss: () -> Unit
) {
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.82f).dp
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = PropDark),
            border = BorderStroke(1.dp, PropGold.copy(alpha = 0.6f))
        ) {
            Column(
                modifier = Modifier
                    .heightIn(max = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(prop.name.ifBlank { "无名器物" }, color = PropGold, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Text("X", color = PropSub, fontSize = 14.sp)
                    }
                }
                Text(prop.category.label, color = PropCream, fontSize = 13.sp)
                Spacer(Modifier.height(12.dp))
                AssetImage(
                    path = prop.imagePath,
                    fallbackPath = PropResourceRegistry.imageFallbackPath(),
                    contentDescription = prop.name,
                    contentScale = ContentScale.Fit,
                    placeholderText = prop.name.take(1).ifBlank { "器" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF160F08))
                )
                Spacer(Modifier.height(12.dp))
                Text("器物说明", color = PropGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    prop.shortDescription.ifBlank { "尚无说明。" },
                    color = PropCream,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
                if (prop.artDirection.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text("陈设说明", color = PropGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(prop.artDirection, color = PropSub, fontSize = 12.sp, lineHeight = 18.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "此为内库图鉴陈列，不代表动态背包或可消耗资源。",
                    color = PropSub,
                    fontSize = 11.sp,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun PropThumbCard(
    prop: PropArt,
    modifier: Modifier = Modifier,
    imageHeight: Int = 72,
    onClick: ((PropArt) -> Unit)? = null
) {
    Card(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick(prop) } else Modifier
        ),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PropCard),
        border = BorderStroke(1.dp, PropGold.copy(alpha = 0.28f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AssetImage(
                path = prop.imagePath,
                fallbackPath = PropResourceRegistry.imageFallbackPath().ifBlank { ArtResourceRegistry.Fallback.ui },
                contentDescription = prop.name,
                contentScale = ContentScale.Fit,
                placeholderText = prop.name.take(1).ifBlank { "器" },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color(0xFF160F08))
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = prop.name.ifBlank { "无名器物" },
                color = PropCream,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = prop.category.label,
                color = PropSub,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

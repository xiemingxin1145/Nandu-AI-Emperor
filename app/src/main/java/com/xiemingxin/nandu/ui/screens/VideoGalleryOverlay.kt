package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.game.VideoClip
import com.xiemingxin.nandu.game.VideoResourceRegistry
import com.xiemingxin.nandu.ui.components.AssetVideoSurface
import com.xiemingxin.nandu.ui.theme.ImperialGold

private val GalleryBg = Color(0xFF0B0805)
private val GalleryPanel = Color(0xF21A1208)
private val GalleryCream = Color(0xFFE8DCC0)
private val GallerySub = Color(0xFF9A8862)

/**
 * V3 资产验收/鉴赏入口。
 * 目的不是把视频藏在图鉴里，而是让真机能快速确认 51 条生成视频是否已真正进入 APK、能否解码。
 */
@Composable
fun VideoGalleryOverlay(onBack: () -> Unit) {
    val clips = VideoResourceRegistry.all
    var selected by remember { mutableStateOf(clips.firstOrNull()) }
    val grouped = remember(clips) { clips.groupBy { it.category }.toList() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GalleryBg)
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("天 命 绘 卷", color = ImperialGold, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("V3 动态资产 · ${clips.size} 条视频", color = GallerySub, fontSize = 11.sp)
                }
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1E08))
                ) {
                    Text("返回", color = ImperialGold)
                }
            }

            Spacer(Modifier.height(12.dp))

            selected?.let { clip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 720.dp)
                            .aspectRatio(clip.aspectRatio)
                            .background(Color.Black)
                    ) {
                        AssetVideoSurface(
                            path = clip.path,
                            loop = clip.loop,
                            muted = true,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(clip.name, color = GalleryCream, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(clip.path, color = GallerySub, fontSize = 9.sp)
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = ImperialGold.copy(alpha = 0.25f))
            Spacer(Modifier.height(6.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                grouped.forEach { (category, categoryClips) ->
                    item(key = "header_$category") {
                        Text(
                            categoryLabel(category),
                            color = ImperialGold,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                        )
                    }
                    items(categoryClips, key = { it.id }) { clip ->
                        VideoRow(
                            clip = clip,
                            selected = selected?.id == clip.id,
                            onClick = { selected = clip }
                        )
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun VideoRow(clip: VideoClip, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) ImperialGold else ImperialGold.copy(alpha = 0.18f)
    val bg = if (selected) Color(0xFF2A1E08) else GalleryPanel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(clip.name, color = if (selected) ImperialGold else GalleryCream, fontSize = 12.sp)
            Text(clip.id, color = GallerySub, fontSize = 9.sp)
        }
        Text(if (clip.loop) "循环" else "演出", color = border, fontSize = 9.sp)
    }
}

private fun categoryLabel(category: String): String = when (category) {
    "intro" -> "片头 / 主菜单"
    "seasons" -> "四季"
    "battle" -> "战场"
    "units" -> "兵种"
    "skills" -> "武将技能"
    "char_live" -> "人物动态立绘"
    "ui_effects" -> "界面特效"
    "cinematic" -> "剧情 CG"
    else -> category
}

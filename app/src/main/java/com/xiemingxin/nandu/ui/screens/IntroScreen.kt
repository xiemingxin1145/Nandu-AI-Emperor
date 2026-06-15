package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.ui.components.AssetImage

private val IntroGold = Color(0xFFC9A227)
private val IntroCream = Color(0xFFE8DCC0)
private val IntroInk = Color(0xFF0E0A05)

/**
 * V1.0 开局画面：皇宫背景 + 赵构立绘 + 靖康开场叙事。
 * 让玩家一眼知道"我是康王赵构，将于乱世中重建社稷"。
 */
@Composable
fun IntroScreen(onStart: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(IntroInk)) {
        // 皇宫背景
        AssetImage(
            path = "images/buildings/building_imperial_palace_01.webp",
            contentDescription = "皇宫",
            contentScale = ContentScale.Crop,
            placeholderText = "宫",
            modifier = Modifier.fillMaxSize()
        )
        // 暗化遮罩，让文字可读
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        IntroInk.copy(alpha = 0.5f),
                        IntroInk.copy(alpha = 0.35f),
                        IntroInk.copy(alpha = 0.88f)
                    )
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(Modifier.height(40.dp))
            Text("南 渡 无 悔", color = IntroGold, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("靖康二年 · 北宋倾覆", color = IntroCream, fontSize = 14.sp)

            Spacer(Modifier.height(28.dp))
            // 赵构立绘
            AssetImage(
                path = "images/characters/halfbody_zhao_gou.webp",
                contentDescription = "康王赵构",
                contentScale = ContentScale.Crop,
                placeholderText = "构",
                modifier = Modifier.size(180.dp).clip(RoundedCornerShape(14.dp))
            )
            Spacer(Modifier.height(8.dp))
            Text("康王 · 赵构", color = IntroGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(20.dp))
            // 开场叙事
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(IntroInk.copy(alpha = 0.6f))
                    .padding(16.dp)
            ) {
                Text(
                    "靖康二年，金人破汴京，掳徽钦二帝北去，宗室公卿尽没于尘。\n\n" +
                    "你是康王赵构，侥幸在外，未陷敌手。河山残破，社稷飘摇，群臣望你如望旱苗之雨。\n\n" +
                    "南渡，还是死守？议和，还是北伐？岳飞尚在行伍，秦桧归途未明，金人铁骑已饮马长江。\n\n" +
                    "中兴之业，今日始于你手。",
                    color = IntroCream, fontSize = 13.sp, lineHeight = 21.sp, textAlign = TextAlign.Start
                )
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IntroGold),
                border = BorderStroke(1.dp, IntroCream.copy(alpha = 0.5f))
            ) {
                Text("即 位 · 中 兴 大 宋", color = IntroInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

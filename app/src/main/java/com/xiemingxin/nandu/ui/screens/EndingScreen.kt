package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.xiemingxin.nandu.game.GameEnding
import com.xiemingxin.nandu.ui.components.AssetImage

private val EndGold = Color(0xFFC9A227)
private val EndCream = Color(0xFFE8DCC0)
private val EndInk = Color(0xFF0A0703)

/**
 * V1.1 结局画面：显示评级、结局、史评、重开按钮。
 * 给游戏一个有仪式感的终点。
 */
@Composable
fun EndingScreen(ending: GameEnding, controlledCities: Int, onRestart: () -> Unit) {
    val rankColor = when (ending.rank) {
        "S" -> Color(0xFFFFD700)
        "A" -> Color(0xFF8FB573)
        "B" -> Color(0xFFD4A437)
        "C" -> Color(0xFF8A9BB5)
        else -> Color(0xFFCC6655)
    }
    val isVictory = ending.rank in listOf("S", "A", "B")
    val bgImage = if (isVictory) "images/cities/city_imperial_capital_dawn_01.webp"
                  else "images/cities/city_kaifeng_crisis_01.webp"

    Box(modifier = Modifier.fillMaxSize().background(EndInk)) {
        AssetImage(
            path = bgImage,
            contentDescription = ending.title,
            contentScale = ContentScale.Crop,
            placeholderText = "终",
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(EndInk.copy(alpha = 0.7f), EndInk.copy(alpha = 0.6f), EndInk.copy(alpha = 0.95f))
                )
            )
        )

        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (isVictory) "大 业 有 成" else "国 祚 已 终", color = EndCream, fontSize = 16.sp)
            Spacer(Modifier.height(20.dp))

            // 评级大字
            Box(
                modifier = Modifier.size(96.dp).clip(RoundedCornerShape(48.dp))
                    .background(EndInk.copy(alpha = 0.7f))
                    .border(BorderStroke(2.dp, rankColor), RoundedCornerShape(48.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(ending.rank, color = rankColor, fontSize = 52.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))
            Text(ending.title, color = rankColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("终局控城 $controlledCities / 36", color = EndCream, fontSize = 13.sp)

            Spacer(Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(EndInk.copy(alpha = 0.6f)).padding(18.dp)
            ) {
                Text(ending.desc, color = EndCream, fontSize = 14.sp, lineHeight = 23.sp, textAlign = TextAlign.Center)
            }

            Spacer(Modifier.height(32.dp))
            Button(
                onClick = onRestart,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EndGold)
            ) {
                Text("重 开 一 局", color = EndInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}


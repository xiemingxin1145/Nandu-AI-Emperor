package com.xiemingxin.nandu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.game.ArtResourceRegistry
import com.xiemingxin.nandu.game.CgVideoArt

private val VideoGold = Color(0xFFC9A227)
private val VideoInk = Color(0xFF120C06)

/** Uses the same silent Media3 asset player as every other formal video entry. */
@Composable
fun CgVideoDialog(video: CgVideoArt, onDismiss: () -> Unit) {
    var playbackFailure by remember(video.path) { mutableStateOf<AssetVideoFailure?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = VideoInk,
            border = BorderStroke(1.dp, VideoGold.copy(alpha = 0.75f))
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(video.name, color = VideoGold, fontSize = 15.sp)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                ) {
                    AssetImage(
                        path = ArtResourceRegistry.Fallback.event,
                        contentDescription = "过场 CG 静态保底",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    AssetVideoSurface(
                        path = video.path,
                        loop = false,
                        muted = true,
                        onFailure = { playbackFailure = it },
                        modifier = Modifier.fillMaxSize()
                    )
                    playbackFailure?.let { failure ->
                        Text(
                            text = failure.message,
                            color = Color.White,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .background(VideoInk.copy(alpha = 0.86f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = VideoGold)
                    ) {
                        Text("返回", color = VideoInk)
                    }
                }
            }
        }
    }
}

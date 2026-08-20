package com.xiemingxin.nandu.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import com.xiemingxin.nandu.game.CgVideoArt

private val VideoGold = Color(0xFFC9A227)
private val VideoInk = Color(0xFF120C06)

/** Plays a bundled H.264/AAC short CG once. CG must never loop like BGM. */
@Composable
fun CgVideoDialog(video: CgVideoArt, onDismiss: () -> Unit) {
    var videoView by remember(video.path) { mutableStateOf<VideoView?>(null) }

    DisposableEffect(video.path) {
        onDispose {
            videoView?.stopPlayback()
            videoView = null
        }
    }

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

                AndroidView(
                    factory = { context ->
                        VideoView(context).also { view ->
                            videoView = view
                            val controls = MediaController(context)
                            controls.setAnchorView(view)
                            view.setMediaController(controls)
                            view.setVideoURI(Uri.parse("file:///android_asset/${video.path}"))
                            view.setOnPreparedListener { player ->
                                player.isLooping = false
                                view.start()
                            }
                            view.setOnCompletionListener { /* stay on final frame until user closes */ }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(Color.Black)
                )

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

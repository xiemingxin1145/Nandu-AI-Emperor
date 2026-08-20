package com.xiemingxin.nandu.ui.components

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.max

/**
 * Plays an MP4 bundled in Android assets using MediaPlayer + TextureView.
 *
 * Generated/asset video is VISUAL ONLY. Its embedded audio track is never allowed into the game mix.
 * BGM, ambience, SFX and voice must always be routed through GameAudioPlayer independently.
 *
 * Why this exists instead of VideoView:
 * - TextureView can be center-cropped, which is important for portrait phones.
 * - generated V3 videos live under repository-root assets/videos and are packaged as Android assets.
 * - failure is non-fatal: when decoding/opening fails this composable removes itself so the caller's
 *   static image underneath remains visible.
 *
 * Some legacy callers may still pass muted=false. That flag is intentionally ignored: asset videos
 * remain silent by policy so generated AAC noise/voice can never leak into gameplay again.
 */
@Composable
fun AssetVideoSurface(
    path: String,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    muted: Boolean = true,
    onPrepared: (() -> Unit)? = null,
    onCompletion: (() -> Unit)? = null,
    onError: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var failed by remember(path) { mutableStateOf(false) }
    var player by remember(path) { mutableStateOf<MediaPlayer?>(null) }

    // Keep the parameter for source compatibility with older named-argument call sites.
    @Suppress("UNUSED_VARIABLE")
    val legacyMutedFlag = muted

    DisposableEffect(path) {
        onDispose {
            runCatching { player?.stop() }
            runCatching { player?.release() }
            player = null
        }
    }

    if (failed) return

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            TextureView(ctx).apply {
                isOpaque = false
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        val mediaPlayer = MediaPlayer()
                        player = mediaPlayer
                        try {
                            val afd = context.assets.openFd(path)
                            mediaPlayer.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                            afd.close()
                            mediaPlayer.setSurface(Surface(surfaceTexture))
                            mediaPlayer.isLooping = loop

                            // Hard rule: every generated/asset video is visual-only.
                            mediaPlayer.setVolume(0f, 0f)

                            mediaPlayer.setOnVideoSizeChangedListener { _, videoWidth, videoHeight ->
                                applyCenterCrop(this@apply, videoWidth, videoHeight)
                            }
                            mediaPlayer.setOnPreparedListener { mp ->
                                applyCenterCrop(this@apply, mp.videoWidth, mp.videoHeight)
                                // Re-assert mute after prepare in case the platform reset the volume.
                                mp.setVolume(0f, 0f)
                                onPrepared?.invoke()
                                mp.start()
                            }
                            mediaPlayer.setOnCompletionListener {
                                if (!loop) onCompletion?.invoke()
                            }
                            mediaPlayer.setOnErrorListener { _, _, _ ->
                                failed = true
                                onError?.invoke()
                                true
                            }
                            mediaPlayer.prepareAsync()
                        } catch (_: Throwable) {
                            runCatching { mediaPlayer.release() }
                            player = null
                            failed = true
                            onError?.invoke()
                        }
                    }

                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        player?.let { applyCenterCrop(this@apply, it.videoWidth, it.videoHeight) }
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        runCatching { player?.stop() }
                        runCatching { player?.release() }
                        player = null
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) = Unit
                }
            }
        }
    )
}

private fun applyCenterCrop(view: TextureView, videoWidth: Int, videoHeight: Int) {
    if (videoWidth <= 0 || videoHeight <= 0 || view.width <= 0 || view.height <= 0) return

    val viewWidth = view.width.toFloat()
    val viewHeight = view.height.toFloat()
    val scale = max(viewWidth / videoWidth.toFloat(), viewHeight / videoHeight.toFloat())
    val scaledWidth = videoWidth * scale
    val scaledHeight = videoHeight * scale

    val matrix = Matrix()
    matrix.setScale(
        scaledWidth / viewWidth,
        scaledHeight / viewHeight,
        viewWidth / 2f,
        viewHeight / 2f
    )
    view.setTransform(matrix)
}

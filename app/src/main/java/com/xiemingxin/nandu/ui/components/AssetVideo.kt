package com.xiemingxin.nandu.ui.components

import android.graphics.Color as AndroidColor
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

enum class AssetVideoFailureKind {
    RESOURCE_MISSING,
    DATA_SOURCE,
    FORMAT,
    DECODER,
    PLAYER
}

data class AssetVideoFailure(
    val kind: AssetVideoFailureKind,
    val message: String,
    val errorCodeName: String? = null
)

/**
 * Plays a packaged MP4 through one shared Media3/ExoPlayer implementation.
 *
 * Rules:
 * - repository-root /assets and app/src/main/assets are both Android assets;
 * - asset videos are VISUAL ONLY: volume is always forced to 0;
 * - the composable removes itself on failure so the caller's static CG underneath remains visible;
 * - failures are classified instead of reporting every problem as "codec unsupported".
 *
 * `onError` is retained for source compatibility with older callers. New code should prefer
 * `onFailure`, which receives the real failure category and diagnostic message.
 */
@Composable
fun AssetVideoSurface(
    path: String,
    modifier: Modifier = Modifier,
    loop: Boolean = false,
    muted: Boolean = true,
    onPrepared: (() -> Unit)? = null,
    onCompletion: (() -> Unit)? = null,
    onError: (() -> Unit)? = null,
    onFailure: ((AssetVideoFailure) -> Unit)? = null
) {
    val context = LocalContext.current
    var failure by remember(path) { mutableStateOf<AssetVideoFailure?>(null) }
    var readySignalled by remember(path) { mutableStateOf(false) }

    // Keep this parameter only so old named-argument call sites remain source-compatible.
    // Asset videos are muted regardless of its value.
    @Suppress("UNUSED_VARIABLE")
    val legacyMutedFlag = muted

    val assetExists = remember(path) {
        runCatching {
            context.assets.open(path).use { /* opening is enough; do not read the whole video */ }
            true
        }.getOrDefault(false)
    }

    LaunchedEffect(path, assetExists) {
        if (!assetExists && failure == null) {
            val missing = AssetVideoFailure(
                kind = AssetVideoFailureKind.RESOURCE_MISSING,
                message = "视频资源不存在：$path"
            )
            failure = missing
            onFailure?.invoke(missing)
            onError?.invoke()
        }
    }

    if (!assetExists || failure != null) return

    val player = remember(context, path, loop) {
        ExoPlayer.Builder(context).build().apply {
            volume = 0f
            repeatMode = if (loop) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            playWhenReady = true
            setMediaItem(MediaItem.fromUri(Uri.parse("asset:///$path")))
            prepare()
        }
    }

    DisposableEffect(player, path, loop) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        player.volume = 0f
                        if (!readySignalled) {
                            readySignalled = true
                            onPrepared?.invoke()
                        }
                    }
                    Player.STATE_ENDED -> {
                        if (!loop) onCompletion?.invoke()
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val classified = classifyAssetVideoFailure(error)
                failure = classified
                onFailure?.invoke(classified)
                onError?.invoke()
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.stop()
            player.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setShutterBackgroundColor(AndroidColor.TRANSPARENT)
                setKeepContentOnPlayerReset(true)
                this.player = player
            }
        },
        update = { view ->
            view.player = player
            player.volume = 0f
        }
    )
}

internal fun classifyAssetVideoFailure(error: PlaybackException): AssetVideoFailure {
    val kind = when (error.errorCode) {
        in 2000..2999 -> AssetVideoFailureKind.DATA_SOURCE
        in 3000..3999 -> AssetVideoFailureKind.FORMAT
        in 4000..4999 -> AssetVideoFailureKind.DECODER
        else -> AssetVideoFailureKind.PLAYER
    }

    val message = when (kind) {
        AssetVideoFailureKind.RESOURCE_MISSING -> "视频资源不存在"
        AssetVideoFailureKind.DATA_SOURCE -> "视频资源存在，但播放器无法读取 asset 数据"
        AssetVideoFailureKind.FORMAT -> "视频容器或轨道格式无法解析"
        AssetVideoFailureKind.DECODER -> "设备无法初始化或完成视频解码"
        AssetVideoFailureKind.PLAYER -> "视频播放器发生异常"
    }

    return AssetVideoFailure(
        kind = kind,
        message = message,
        errorCodeName = error.errorCodeName
    )
}

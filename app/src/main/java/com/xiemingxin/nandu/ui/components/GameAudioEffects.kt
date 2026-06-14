package com.xiemingxin.nandu.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.xiemingxin.nandu.audio.GameAudioPlayer

/**
 * V0.6.7 Compose 音频钩子。
 * 页面层后续只需要调用 PlayBgmEffect / PlaySfxEffect，不直接管理 MediaPlayer 生命周期。
 */
@Composable
fun rememberGameAudioPlayer(): GameAudioPlayer {
    val context = LocalContext.current.applicationContext as Context
    val player = remember { GameAudioPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}

@Composable
fun PlayBgmEffect(
    path: String,
    sceneKey: Any = path,
    enabled: Boolean = true,
    loop: Boolean = true,
    volume: Float = 0.75f,
    player: GameAudioPlayer = rememberGameAudioPlayer()
) {
    LaunchedEffect(sceneKey, path, enabled, loop, volume) {
        if (enabled) {
            player.playBgm(path = path, loop = loop, volume = volume)
        } else {
            player.stopBgm()
        }
    }
    DisposableEffect(sceneKey, path) {
        onDispose { player.stopBgm() }
    }
}

@Composable
fun PlaySfxEffect(
    path: String,
    triggerKey: Any?,
    enabled: Boolean = true,
    volume: Float = 1f,
    player: GameAudioPlayer = rememberGameAudioPlayer()
) {
    LaunchedEffect(triggerKey, path, enabled, volume) {
        if (enabled && triggerKey != null) {
            player.playSfx(path = path, volume = volume)
        }
    }
}

@Composable
fun PreloadSfxEffect(
    paths: List<String>,
    player: GameAudioPlayer = rememberGameAudioPlayer()
) {
    LaunchedEffect(paths) {
        player.preloadSfx(paths)
    }
}

package com.xiemingxin.nandu.ui.components

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import com.xiemingxin.nandu.audio.GameAudioPlayer

/**
 * V0.6.7 / V1.4.1 Compose 音频钩子。
 * 页面层只需调用 PlayBgmEffect / PlaySfxEffect / PlayAmbienceEffect，
 * 不直接管理 MediaPlayer 生命周期。整个 App 共用一个播放器（LocalGameAudioPlayer），
 * 避免每个调用点各自 new 出 MediaPlayer 导致 BGM 叠加。
 */
val LocalGameAudioPlayer = staticCompositionLocalOf<GameAudioPlayer?> { null }

@Composable
fun rememberGameAudioPlayer(): GameAudioPlayer {
    val context = LocalContext.current.applicationContext as Context
    val player = remember { GameAudioPlayer(context) }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    return player
}

/** 取共享播放器；若未在上层 provide，则回退到局部实例（仍可工作，只是不共享）。 */
@Composable
fun currentAudioPlayer(): GameAudioPlayer = LocalGameAudioPlayer.current ?: rememberGameAudioPlayer()

@Composable
fun PlayBgmEffect(
    path: String,
    sceneKey: Any = path,
    enabled: Boolean = true,
    loop: Boolean = true,
    volume: Float = 0.75f,
    player: GameAudioPlayer = currentAudioPlayer()
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
fun PlayAmbienceEffect(
    path: String?,
    enabled: Boolean = true,
    volume: Float = 0.5f,
    player: GameAudioPlayer = currentAudioPlayer()
) {
    LaunchedEffect(path, enabled, volume) {
        if (enabled && path != null) {
            player.playAmbience(path = path, volume = volume)
        } else {
            player.stopAmbience()
        }
    }
    DisposableEffect(path) {
        onDispose { player.stopAmbience() }
    }
}

@Composable
fun PlaySfxEffect(
    path: String,
    triggerKey: Any?,
    enabled: Boolean = true,
    volume: Float = 1f,
    player: GameAudioPlayer = currentAudioPlayer()
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
    player: GameAudioPlayer = currentAudioPlayer()
) {
    LaunchedEffect(paths) {
        player.preloadSfx(paths)
    }
}

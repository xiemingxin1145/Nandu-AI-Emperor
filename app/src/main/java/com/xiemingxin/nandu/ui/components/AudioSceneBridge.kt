package com.xiemingxin.nandu.ui.components

import androidx.compose.runtime.Composable
import com.xiemingxin.nandu.game.AudioResourceRegistry

/** V0.6.8 页面音频桥接层。 */
@Composable
fun AudioSceneBridge(scene: String, enabled: Boolean = true, volume: Float = 0.72f) {
    PlayBgmEffect(
        path = AudioResourceRegistry.bgmForScene(scene),
        sceneKey = scene,
        enabled = enabled,
        loop = true,
        volume = volume
    )
}

@Composable
fun CourtSceneAudio(enabled: Boolean = true) {
    AudioSceneBridge(scene = "court", enabled = enabled, volume = 0.68f)
}

@Composable
fun MapSceneAudio(enabled: Boolean = true) {
    AudioSceneBridge(scene = "map", enabled = enabled, volume = 0.70f)
}

@Composable
fun MainMenuSceneAudio(enabled: Boolean = true) {
    AudioSceneBridge(scene = "main_menu", enabled = enabled, volume = 0.70f)
}

@Composable
fun PlayUiClick(triggerKey: Any?, enabled: Boolean = true) {
    PlaySfxEffect(
        path = AudioResourceRegistry.Ui.click,
        triggerKey = triggerKey,
        enabled = enabled,
        volume = 0.75f
    )
}

@Composable
fun PlayEdictStamp(triggerKey: Any?, enabled: Boolean = true) {
    PlaySfxEffect(
        path = AudioResourceRegistry.Ui.stampEdict,
        triggerKey = triggerKey,
        enabled = enabled,
        volume = 0.95f
    )
}

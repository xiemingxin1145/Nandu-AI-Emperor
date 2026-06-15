package com.xiemingxin.nandu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File

/**
 * V0.6.6 安全音频播放器骨架。
 * 只从 assets/audio/ 读音频；资源缺失时静默失败，不影响游戏运行。
 */
class GameAudioPlayer(private val context: Context) {
    private val soundPool: SoundPool
    private val soundIds = mutableMapOf<String, Int>()
    private var bgmPlayer: MediaPlayer? = null
    private var ambiencePlayer: MediaPlayer? = null
    private var currentAmbiencePath: String? = null
    var masterEnabled: Boolean = true
    var bgmEnabled: Boolean = true
    var sfxEnabled: Boolean = true

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attrs)
            .build()
    }

    fun preloadSfx(paths: List<String>) {
        paths.forEach { path ->
            if (!soundIds.containsKey(path)) {
                loadSfx(path)?.let { soundIds[path] = it }
            }
        }
    }

    fun playSfx(path: String, volume: Float = 1f) {
        if (!masterEnabled || !sfxEnabled) return
        val soundId = soundIds[path] ?: loadSfx(path)?.also { soundIds[path] = it } ?: return
        soundPool.play(soundId, volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f), 1, 0, 1f)
    }

    fun playBgm(path: String, loop: Boolean = true, volume: Float = 0.75f) {
        if (!masterEnabled || !bgmEnabled) return
        stopBgm()
        val file = materializeAsset(path) ?: return
        bgmPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            isLooping = loop
            setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            setOnPreparedListener { it.start() }
            setOnCompletionListener { if (!loop) stopBgm() }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                if (bgmPlayer === mp) bgmPlayer = null
                true
            }
            prepareAsync()
        }
    }

    fun stopBgm() {
        bgmPlayer?.runCatching {
            stop()
            release()
        }
        bgmPlayer = null
    }

    /** 循环环境音通道，与 BGM 并行（如酒楼人声、市集喧闹、雨雪风）。相同路径不重启。 */
    fun playAmbience(path: String, volume: Float = 0.5f) {
        if (!masterEnabled || !bgmEnabled) return
        if (path == currentAmbiencePath && ambiencePlayer != null) return
        stopAmbience()
        val file = materializeAsset(path) ?: return
        currentAmbiencePath = path
        ambiencePlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            isLooping = true
            setVolume(volume.coerceIn(0f, 1f), volume.coerceIn(0f, 1f))
            setOnPreparedListener { it.start() }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                if (ambiencePlayer === mp) { ambiencePlayer = null; currentAmbiencePath = null }
                true
            }
            prepareAsync()
        }
    }

    fun stopAmbience() {
        ambiencePlayer?.runCatching {
            stop()
            release()
        }
        ambiencePlayer = null
        currentAmbiencePath = null
    }

    fun release() {
        stopBgm()
        stopAmbience()
        soundPool.release()
        soundIds.clear()
    }

    private fun loadSfx(path: String): Int? {
        val file = materializeAsset(path) ?: return null
        return runCatching { soundPool.load(file.absolutePath, 1) }.getOrNull()
    }

    private fun materializeAsset(path: String): File? {
        return try {
            val safeName = path.replace('/', '_').replace('.', '_')
            val outFile = File(context.cacheDir, "nandu_audio_$safeName")
            if (!outFile.exists() || outFile.length() == 0L) {
                context.assets.open(path).use { input ->
                    outFile.outputStream().use { output -> input.copyTo(output) }
                }
            }
            outFile
        } catch (_: Throwable) {
            null
        }
    }
}

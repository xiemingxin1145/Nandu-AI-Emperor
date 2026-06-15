package com.xiemingxin.nandu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.io.File
import kotlin.random.Random

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

    private val variantCache = mutableMapOf<String, List<String>>()

    /**
     * 同类音效随机播放：给定基础路径（如 audio/sfx/sfx_drum_war.ogg），
     * 自动收集同目录下 sfx_drum_war.ogg / sfx_drum_war_2.ogg / _3.ogg ... 随机挑一个播。
     * 何老师只需丢入带 _2/_3 后缀的同名文件即可扩充随机池，无需改代码。缺文件则回退基础路径。
     */
    fun playSfxVariant(basePath: String, volume: Float = 1f) {
        if (!masterEnabled || !sfxEnabled) return
        val variants = variantCache.getOrPut(basePath) { resolveVariants(basePath) }
        val pick = if (variants.isEmpty()) basePath else variants[Random.nextInt(variants.size)]
        playSfx(pick, volume)
    }

    private fun resolveVariants(basePath: String): List<String> {
        val slash = basePath.lastIndexOf('/')
        val dir = if (slash >= 0) basePath.substring(0, slash) else ""
        val file = basePath.substring(slash + 1)
        val dot = file.lastIndexOf('.')
        val stem = if (dot >= 0) file.substring(0, dot) else file
        val ext = if (dot >= 0) file.substring(dot) else ""
        return try {
            val all = context.assets.list(dir)?.toList() ?: emptyList()
            val re = Regex("${Regex.escape(stem)}(_\\d+)?${Regex.escape(ext)}")
            val matched = all.filter { re.matches(it) }
                .map { if (dir.isEmpty()) it else "$dir/$it" }
            matched.ifEmpty { listOf(basePath) }
        } catch (_: Throwable) {
            listOf(basePath)
        }
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

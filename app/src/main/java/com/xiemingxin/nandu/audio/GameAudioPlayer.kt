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
    private var voicePlayer: MediaPlayer? = null
    private var currentAmbiencePath: String? = null
    private var currentBgmVolume: Float = 0.75f
    private var isVoicePlaying: Boolean = false

    // ═══════════════════════════════════════════════════════
    // 音量控制层（V1.0 七声道独立调节）
    // ═══════════════════════════════════════════════════════
    data class VolumeSettings(
        var master: Float = 1f,
        var bgm: Float = 0.75f,
        var ambience: Float = 0.5f,
        var ui: Float = 0.8f,
        var sfx: Float = 0.85f,
        var voice: Float = 1f,
        var narrator: Float = 1f,
        var battle: Float = 0.9f,
        var video: Float = 0.8f
    )

    var volume = VolumeSettings()
    var masterEnabled: Boolean = true
    var bgmEnabled: Boolean = true
    var sfxEnabled: Boolean = true
    var voiceEnabled: Boolean = true

    /** 人声播放时 BGM 自动降低到的比例（ducking） */
    private val voiceDuckFactor: Float = 0.35f

    companion object {
        /**
         * Demo 期曾把几十 KB 的占位/提示语音误放进 BGM 槽位。真正 60–120 秒的
         * OGG 配乐不应小到这个程度。低于阈值的 BGM 直接静默跳过，避免把提示词
         * 当背景音乐无限循环。替换为真实配乐后无需改代码，会自动恢复播放。
         */
        private const val MIN_BGM_BYTES = 128 * 1024L
    }

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
     * 只需丢入带 _2/_3 后缀的同名文件即可扩充随机池，无需改代码。缺文件则回退基础路径。
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
        // 防止把短占位音/生成器提示语误当 BGM 无限循环。
        if (file.length() < MIN_BGM_BYTES) return
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

    // ═══════════════════════════════════════════════════════
    // 人声/旁白通道（V1.0）
    // 播放时自动降低 BGM 音量（ducking），播放结束恢复
    // ═══════════════════════════════════════════════════════
    fun playVoice(path: String, voiceVolume: Float? = null, onComplete: (() -> Unit)? = null) {
        if (!masterEnabled || !voiceEnabled) {
            onComplete?.invoke()
            return
        }
        stopVoice()
        val file = materializeAsset(path) ?: run { onComplete?.invoke(); return }
        val vol = (voiceVolume ?: this@GameAudioPlayer.volume.voice).coerceIn(0f, 1f) * this@GameAudioPlayer.volume.master
        isVoicePlaying = true
        applyBgmDucking(true)
        voicePlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            setVolume(vol, vol)
            setOnPreparedListener { it.start() }
            setOnCompletionListener {
                isVoicePlaying = false
                applyBgmDucking(false)
                release()
                if (voicePlayer === this@apply) voicePlayer = null
                onComplete?.invoke()
            }
            setOnErrorListener { mp, _, _ ->
                isVoicePlaying = false
                applyBgmDucking(false)
                mp.release()
                if (voicePlayer === mp) voicePlayer = null
                onComplete?.invoke()
                true
            }
            prepareAsync()
        }
    }

    fun stopVoice() {
        voicePlayer?.runCatching {
            stop()
            release()
        }
        voicePlayer = null
        if (isVoicePlaying) {
            isVoicePlaying = false
            applyBgmDucking(false)
        }
    }

    /** BGM 人声闪避：人声播放时降低 BGM 音量 */
    private fun applyBgmDucking(duck: Boolean) {
        bgmPlayer?.let { player ->
            val target = if (duck) currentBgmVolume * voiceDuckFactor else currentBgmVolume
            player.setVolume(target, target)
        }
    }

    fun release() {
        stopBgm()
        stopAmbience()
        stopVoice()
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

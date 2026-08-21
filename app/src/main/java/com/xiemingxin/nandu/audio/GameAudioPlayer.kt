package com.xiemingxin.nandu.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import java.io.File
import java.util.Locale
import kotlin.random.Random

/**
 * 游戏音频播放器。
 * 只从 assets/audio/ 读取打包音频；缺资源时静默失败，不影响主流程。
 *
 * 序章额外提供 Android 中文 TTS 兜底，保证“有画面却完全没旁白”的情况不再发生。
 * 正式真人/AI配音到位后，仍可继续使用 playVoice() 播放预生成音频文件。
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

    private var tts: TextToSpeech? = null
    private var ttsReady: Boolean = false
    private var pendingTtsText: String? = null

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

    /** 人声播放时 BGM 自动降低到的比例。 */
    private val voiceDuckFactor: Float = 0.30f

    companion object {
        /** 过滤 Demo 期误塞进 BGM 槽位的极短占位音。 */
        private const val MIN_BGM_BYTES = 128 * 1024L

        /**
         * STAB-007 真机反馈：这些环境层在序幕中被明确听成“说话声/杂音”。
         * 在重新人工试听并单独放行前，宁可静默，也不允许它们继续污染正式体验。
         */
        private val DEVICE_REJECTED_AMBIENCE = setOf(
            "audio/ambience/amb_frontier_wind.ogg",
            "audio/ambience/amb_river.ogg",
            "audio/ambience/amb_palace_murmur.ogg"
        )

        /**
         * 前四幕旧预生成旁白的音色曾被真机判定不合格。
         * 旧实现直接把它们静音，导致“资源6/6存在但玩家听不到”。
         * 现在不再静音：在正式重配音替换前，这四幕自动走设备中文 TTS，
         * 第五幕主角内心声与第六幕内侍仍播放打包人声。
         */
        private val OPENING_TTS_FALLBACK = mapOf(
            "audio/voice/prologue/prologue_act1_shanhejiangqing.m4a" to
                "大宋靖康年间，金军铁骑再度南下。汴京以北，烽火连天，山河将倾。",
            "audio/voice/prologue/prologue_act2_jingkang.m4a" to
                "汴京陷落，二帝北狩。百余年东京繁华，一夕倾覆。宗室百官，尽被掳北去。",
            "audio/voice/prologue/prologue_act3_nandu.m4a" to
                "康王赵构即位于南京应天府，改元建炎。然而江山未稳，金兵已经渡河而来。",
            "audio/voice/prologue/prologue_act4_lishipianzhuan.m4a" to
                "靖康已成旧史，山河却仍在烽火之中。从这一刻起，未来不再只有一条路。"
        )
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
        val safeVolume = volume.coerceIn(0f, 1f)
        soundPool.play(soundId, safeVolume, safeVolume, 1, 0, 1f)
    }

    private val variantCache = mutableMapOf<String, List<String>>()

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
        if (file.length() < MIN_BGM_BYTES) return
        currentBgmVolume = volume.coerceIn(0f, 1f) * this.volume.master
        bgmPlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            isLooping = loop
            setVolume(currentBgmVolume, currentBgmVolume)
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

    fun playAmbience(path: String, volume: Float = 0.5f) {
        if (!masterEnabled || !bgmEnabled) return
        if (path in DEVICE_REJECTED_AMBIENCE) {
            stopAmbience()
            return
        }
        if (path == currentAmbiencePath && ambiencePlayer != null) return
        stopAmbience()
        val file = materializeAsset(path) ?: return
        currentAmbiencePath = path
        val safeVolume = volume.coerceIn(0f, 1f) * this.volume.master
        ambiencePlayer = MediaPlayer().apply {
            setDataSource(file.absolutePath)
            isLooping = true
            setVolume(safeVolume, safeVolume)
            setOnPreparedListener { it.start() }
            setOnErrorListener { mp, _, _ ->
                mp.release()
                if (ambiencePlayer === mp) {
                    ambiencePlayer = null
                    currentAmbiencePath = null
                }
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

    /**
     * 播放正式人声文件。前四幕旧旁白若尚未替换，会自动转为 TTS，绝不再静默。
     */
    fun playVoice(path: String, voiceVolume: Float? = null, onComplete: (() -> Unit)? = null) {
        if (!masterEnabled || !voiceEnabled) {
            onComplete?.invoke()
            return
        }

        OPENING_TTS_FALLBACK[path]?.let { narration ->
            speakNarration(narration)
            return
        }

        stopVoice()
        val file = materializeAsset(path) ?: run {
            onComplete?.invoke()
            return
        }
        val vol = (voiceVolume ?: this.volume.voice).coerceIn(0f, 1f) * this.volume.master
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

    /**
     * 序章兜底旁白：直接调用设备中文 TTS。
     * 正式可验证的配音文件替换后，只需从 OPENING_TTS_FALLBACK 移除对应路径即可切回真人/AI配音。
     */
    fun speakNarration(text: String) {
        if (!masterEnabled || !voiceEnabled || text.isBlank()) return
        stopVoice()
        pendingTtsText = text
        isVoicePlaying = true
        applyBgmDucking(true)

        val existing = tts
        if (existing != null && ttsReady) {
            speakNow(existing, text)
            pendingTtsText = null
            return
        }

        if (existing == null) {
            tts = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    ttsReady = true
                    tts?.let { engine ->
                        val langResult = engine.setLanguage(Locale.SIMPLIFIED_CHINESE)
                        if (langResult != TextToSpeech.LANG_MISSING_DATA &&
                            langResult != TextToSpeech.LANG_NOT_SUPPORTED
                        ) {
                            engine.setSpeechRate(0.88f)
                            engine.setPitch(0.94f)
                            pendingTtsText?.let { pending ->
                                speakNow(engine, pending)
                                pendingTtsText = null
                            }
                        } else {
                            isVoicePlaying = false
                            applyBgmDucking(false)
                        }
                    }
                } else {
                    ttsReady = false
                    isVoicePlaying = false
                    applyBgmDucking(false)
                }
            }
        }
    }

    private fun speakNow(engine: TextToSpeech, text: String) {
        engine.speak(
            text,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "nandu_narration_${System.nanoTime()}"
        )
    }

    fun stopVoice() {
        voicePlayer?.runCatching {
            stop()
            release()
        }
        voicePlayer = null
        tts?.runCatching { stop() }
        pendingTtsText = null
        if (isVoicePlaying) {
            isVoicePlaying = false
            applyBgmDucking(false)
        }
    }

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
        tts?.runCatching { shutdown() }
        tts = null
        ttsReady = false
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

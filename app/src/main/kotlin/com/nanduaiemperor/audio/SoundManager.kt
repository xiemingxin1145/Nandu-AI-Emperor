package com.nanduaiemperor.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import java.io.IOException

/**
 * 古风音效管理器 - 专为《南渡无悔·AI皇帝沙盒》游戏设计
 * 支持分类随机播放、事件触发、环境音乐循环
 * 分类：战场、朝堂、勾栏听曲、大气碧碴、UI、环境
 */
enum class SoundCategory {
    BATTLEFIELD,    // 战场：战鼓、马兵、剑击、箭響、军号
    COURT,          // 朝堂：编钟、趟声、卷轴、墨笔、宫廷环境
    GOULAN,         // 勾栏听曲：琴琶、二胡、古筝软曲、舞乐
    EPIC,           // 大气碧碴：帝王范、大鼓鸣锣、苏纳军乐、壮阔古风
    UI,             // UI提示：木鱼、清铃、页面翻动
    AMBIENT         // 环境：城市、江河、山林风声
}

class SoundManager(private val context: Context) {

    private val soundPool: SoundPool
    private val loadedSounds = mutableMapOf<SoundCategory, MutableList<Int>>()
    private var currentMusicPlayer: MediaPlayer? = null
    private var musicVolume = 0.4f
    private var sfxVolume = 0.75f
    private val tag = "SoundManager"

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(12)  // 支持多个SFX同时播放
            .setAudioAttributes(audioAttributes)
            .build()
    }

    /**
     * 加载指定类别的音效文件列表
     * @param category 音效类别
     * @param assetPaths 资产路径列表，如 "sounds/battlefield/drum1.ogg"
     */
    fun loadCategory(category: SoundCategory, assetPaths: List<String>) {
        val soundIds = loadedSounds.getOrPut(category) { mutableListOf() }
        soundIds.clear()

        for (path in assetPaths) {
            try {
                val afd = context.assets.openFd(path)
                val soundId = soundPool.load(afd, 1)
                soundIds.add(soundId)
                Log.d(tag, "Loaded: $path -> id=$soundId")
            } catch (e: IOException) {
                Log.e(tag, "Failed to load sound: $path", e)
            }
        }
    }

    /**
     * 随机播放某类别一个音效
     */
    fun playRandom(category: SoundCategory) {
        val sounds = loadedSounds[category] ?: return
        if (sounds.isEmpty()) {
            Log.w(tag, "No sounds loaded for category: $category")
            return
        }
        val soundId = sounds.random()
        soundPool.play(soundId, sfxVolume, sfxVolume, 1, 0, 1.0f)
    }

    /**
     * 播放指定索引的音效
     */
    fun playSpecific(category: SoundCategory, index: Int) {
        val sounds = loadedSounds[category] ?: return
        if (index in sounds.indices) {
            soundPool.play(sounds[index], sfxVolume, sfxVolume, 1, 0, 1.0f)
        }
    }

    /**
     * 开始环境/背景音乐（循环）
     */
    fun startAmbient(category: SoundCategory, musicAssetName: String) {
        stopAmbient()
        try {
            val afd = context.assets.openFd("sounds/${category.name.lowercase()}/$musicAssetName")
            currentMusicPlayer = MediaPlayer().apply {
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                isLooping = true
                setVolume(musicVolume, musicVolume)
                prepareAsync()
                setOnPreparedListener { it.start() }
            }
        } catch (e: Exception) {
            Log.e(tag, "Failed to start ambient music", e)
        }
    }

    fun stopAmbient() {
        currentMusicPlayer?.let {
            if (it.isPlaying) it.stop()
            it.release()
        }
        currentMusicPlayer = null
    }

    /**
     * 根据游戏事件/命令自动触发音效（推荐在 GameRuleEngine 中调用）
     */
    fun playForGameEvent(eventType: String, riskTags: List<String> = emptyList()) {
        when (eventType.lowercase()) {
            "dispatch_army", "battle", "army_move" -> {
                playRandom(SoundCategory.BATTLEFIELD)
                // 可以层叠一个战鼓和一个军号
            }
            "assign_officer", "reward_officer", "punish_officer", "court", "edict" -> {
                playRandom(SoundCategory.COURT)
            }
            "raise_grain", "repair_city" -> {
                playRandom(SoundCategory.AMBIENT)  // 城市/民生环境
            }
            "epic_victory", "imperial" -> {
                playRandom(SoundCategory.EPIC)
            }
            else -> {
                // 默认朝堂或UI
                if (riskTags.any { it.contains("grain") || it.contains("pressure") }) {
                    playRandom(SoundCategory.COURT)
                } else {
                    playRandom(SoundCategory.UI)
                }
            }
        }

        // 风险标签额外音效
        if (riskTags.contains("grain_pressure")) {
            playRandom(SoundCategory.COURT)  // 紧张朝堂气氛
        }
    }

    /**
     * 调整音量 (0.0 - 1.0)
     */
    fun setVolumes(music: Float, sfx: Float) {
        musicVolume = music.coerceIn(0f, 1f)
        sfxVolume = sfx.coerceIn(0f, 1f)
        currentMusicPlayer?.setVolume(musicVolume, musicVolume)
    }

    /**
     * 释放资源（在Activity onDestroy调用）
     */
    fun release() {
        stopAmbient()
        soundPool.release()
    }

    /**
     * 示例：初始化并加载启动音效（在MainActivity或GameViewModel中调用）
     * 使用前请在 assets/sounds/ 下创建对应子文件夹并放入.ogg文件
     */
    companion object {
        fun createAndLoadDefault(context: Context): SoundManager {
            val manager = SoundManager(context)

            // 示例加载 - 用户需自行下载替换为实际文件名
            manager.loadCategory(SoundCategory.BATTLEFIELD, listOf(
                "sounds/battlefield/war_drum1.ogg",
                "sounds/battlefield/cavalry_gallop.ogg",
                "sounds/battlefield/sword_clash.ogg",
                "sounds/battlefield/arrow_whoosh.ogg",
                "sounds/battlefield/battle_horn.ogg"
            ))

            manager.loadCategory(SoundCategory.COURT, listOf(
                "sounds/court/court_bell.ogg",
                "sounds/court/scroll_unroll.ogg",
                "sounds/court/palace_footsteps.ogg",
                "sounds/court/guzheng_ambient.ogg",
                "sounds/court/woodblock.ogg"
            ))

            manager.loadCategory(SoundCategory.GOULAN, listOf(
                "sounds/goulan/pipa_solo.ogg",
                "sounds/goulan/erhu_melody.ogg",
                "sounds/goulan/guzheng_soft.ogg"
            ))

            manager.loadCategory(SoundCategory.EPIC, listOf(
                "sounds/epic/grand_drums.ogg",
                "sounds/epic/imperial_fanfare.ogg",
                "sounds/epic/suona_majestic.ogg"
            ))

            manager.loadCategory(SoundCategory.UI, listOf(
                "sounds/ui/wood_click.ogg",
                "sounds/ui/chime_notify.ogg"
            ))

            return manager
        }
    }
}

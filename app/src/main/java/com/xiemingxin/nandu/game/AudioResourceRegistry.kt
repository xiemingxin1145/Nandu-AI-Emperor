package com.xiemingxin.nandu.game

/**
 * V0.6.6 音频资源注册表。
 * Claude 可逐批上传到 app/src/main/assets/audio/，这里先稳定引用路径。
 */
object AudioResourceRegistry {
    private const val BASE = "audio"

    object Bgm {
        const val mainMenu = "$BASE/bgm/bgm_main_menu.ogg"
        const val court = "$BASE/bgm/bgm_court.ogg"
        const val map = "$BASE/bgm/bgm_map.ogg"
        const val battle = "$BASE/bgm/bgm_battle.ogg"
        const val eventSad = "$BASE/bgm/bgm_event_sad.ogg"
        const val victory = "$BASE/bgm/bgm_victory.ogg"
        const val defeat = "$BASE/bgm/bgm_defeat.ogg"
    }

    object Ui {
        const val click = "$BASE/ui/ui_click.ogg"
        const val confirm = "$BASE/ui/ui_confirm.ogg"
        const val cancel = "$BASE/ui/ui_cancel.ogg"
        const val openPanel = "$BASE/ui/ui_open_panel.ogg"
        const val closePanel = "$BASE/ui/ui_close_panel.ogg"
        const val warning = "$BASE/ui/ui_warning.ogg"
        const val stampEdict = "$BASE/ui/ui_stamp_edict.ogg"
    }

    object Sfx {
        const val march = "$BASE/sfx/sfx_march.ogg"
        const val battleStart = "$BASE/sfx/sfx_battle_start.ogg"
        const val swordClash = "$BASE/sfx/sfx_sword_clash.ogg"
        const val arrowVolley = "$BASE/sfx/sfx_arrow_volley.ogg"
        const val cityFire = "$BASE/sfx/sfx_city_fire.ogg"
        const val horseRun = "$BASE/sfx/sfx_horse_run.ogg"
        const val navalWave = "$BASE/sfx/sfx_naval_wave.ogg"
        const val drumWar = "$BASE/sfx/sfx_drum_war.ogg"
        const val courtMurmur = "$BASE/sfx/sfx_court_murmur.ogg"
        const val reportArrive = "$BASE/sfx/sfx_report_arrive.ogg"
    }

    object Ambience {
        const val rain = "$BASE/ambience/amb_rain.ogg"
        const val storm = "$BASE/ambience/amb_storm.ogg"
        const val snowWind = "$BASE/ambience/amb_snow_wind.ogg"
        const val cityDay = "$BASE/ambience/amb_city_day.ogg"
        const val cityNight = "$BASE/ambience/amb_city_night.ogg"
        const val campNight = "$BASE/ambience/amb_camp_night.ogg"
        const val river = "$BASE/ambience/amb_river.ogg"
        const val tavern = "$BASE/ambience/amb_tavern.ogg"     // V1.4.1 酒楼人声环境
        const val market = "$BASE/ambience/amb_market.ogg"     // V1.4.1 市集喧闹
    }

    object Voice {
        const val edictReceived = "$BASE/voice/voice_edict_received.ogg"
        const val militaryReport = "$BASE/voice/voice_military_report.ogg"
        const val courtResponse = "$BASE/voice/voice_court_response.ogg"
        const val enemyWarning = "$BASE/voice/voice_enemy_warning.ogg"
        const val battleCry = "$BASE/voice/voice_battle_cry.ogg"  // V1.4.1 叫阵（需TTS/录音，占位空）
    }

    fun bgmForScene(scene: String): String = when (scene) {
        "main_menu" -> Bgm.mainMenu
        "court" -> Bgm.court
        "map" -> Bgm.map
        "battle" -> Bgm.battle
        "event_sad" -> Bgm.eventSad
        "victory" -> Bgm.victory
        "defeat" -> Bgm.defeat
        else -> Bgm.map
    }
}

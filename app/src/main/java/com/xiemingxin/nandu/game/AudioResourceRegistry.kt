package com.xiemingxin.nandu.game

/**
 * V2.7 音频资源注册表。
 *
 * Claude/Cursor 可逐批上传到 app/src/main/assets/audio/。
 * 这里先把 BGM、UI、SFX、环境声、语音提示的路径和路由稳定下来。
 *
 * 设计原则：
 * - 测试期允许缺文件，播放器组件应当静默跳过缺失资源。
 * - 资源命名稳定后，后续只替换 ogg 文件，不改代码。
 * - 场景、事件、天气、按钮行为都从这里取音频路径。
 */
object AudioResourceRegistry {
    private const val BASE = "audio"

    object Bgm {
        const val mainMenu = "$BASE/bgm/bgm_main_menu.ogg"
        const val court = "$BASE/bgm/bgm_court.ogg"
        const val palace = "$BASE/bgm/bgm_palace_hall.ogg"
        const val council = "$BASE/bgm/bgm_court_council.ogg"
        const val map = "$BASE/bgm/bgm_map.ogg"
        const val city = "$BASE/bgm/bgm_city.ogg"
        const val market = "$BASE/bgm/bgm_market.ogg"
        const val diplomacy = "$BASE/bgm/bgm_diplomacy.ogg"
        const val military = "$BASE/bgm/bgm_military.ogg"
        const val crisis = "$BASE/bgm/bgm_crisis.ogg"
        const val eventSad = "$BASE/bgm/bgm_event_sad.ogg"
        const val ritual = "$BASE/bgm/bgm_ritual.ogg"
        const val victory = "$BASE/bgm/bgm_victory.ogg"
        const val defeat = "$BASE/bgm/bgm_defeat.ogg"

        // Backward-compatible names used by older screens.
        const val battle = military
    }

    object Ui {
        const val click = "$BASE/ui/ui_click.ogg"
        const val confirm = "$BASE/ui/ui_confirm.ogg"
        const val cancel = "$BASE/ui/ui_cancel.ogg"
        const val openPanel = "$BASE/ui/ui_open_panel.ogg"
        const val closePanel = "$BASE/ui/ui_close_panel.ogg"
        const val switchTab = "$BASE/ui/ui_switch_tab.ogg"
        const val warning = "$BASE/ui/ui_warning.ogg"
        const val unlock = "$BASE/ui/ui_unlock.ogg"
        const val select = "$BASE/ui/ui_select.ogg"
        const val stampEdict = "$BASE/ui/ui_stamp_edict.ogg"
        const val brushWrite = "$BASE/ui/ui_brush_write.ogg"
        const val scrollOpen = "$BASE/ui/ui_scroll_open.ogg"
        const val scrollClose = "$BASE/ui/ui_scroll_close.ogg"
    }

    object Sfx {
        const val march = "$BASE/sfx/sfx_march.ogg"
        const val encounterStart = "$BASE/sfx/sfx_encounter_start.ogg"
        const val metalClash = "$BASE/sfx/sfx_metal_clash.ogg"
        const val arrows = "$BASE/sfx/sfx_arrows.ogg"
        const val cityAlarm = "$BASE/sfx/sfx_city_alarm.ogg"
        const val horseRun = "$BASE/sfx/sfx_horse_run.ogg"
        const val navalWave = "$BASE/sfx/sfx_naval_wave.ogg"
        const val drumWar = "$BASE/sfx/sfx_drum_war.ogg"
        const val courtMurmur = "$BASE/sfx/sfx_court_murmur.ogg"
        const val reportArrive = "$BASE/sfx/sfx_report_arrive.ogg"
        const val bell = "$BASE/sfx/sfx_bell.ogg"
        const val gong = "$BASE/sfx/sfx_gong.ogg"
        const val pageTurn = "$BASE/sfx/sfx_page_turn.ogg"
        const val coin = "$BASE/sfx/sfx_coin.ogg"
        const val build = "$BASE/sfx/sfx_build.ogg"
        const val recruit = "$BASE/sfx/sfx_recruit.ogg"
        const val relationUp = "$BASE/sfx/sfx_relation_up.ogg"
        const val relationDown = "$BASE/sfx/sfx_relation_down.ogg"

        // Backward-compatible names used by older screens.
        const val battleStart = encounterStart
        const val swordClash = metalClash
        const val arrowVolley = arrows
        const val cityFire = cityAlarm
    }

    object Ambience {
        const val rain = "$BASE/ambience/amb_rain.ogg"
        const val storm = "$BASE/ambience/amb_storm.ogg"
        const val snowWind = "$BASE/ambience/amb_snow_wind.ogg"
        const val cityDay = "$BASE/ambience/amb_city_day.ogg"
        const val cityNight = "$BASE/ambience/amb_city_night.ogg"
        const val campNight = "$BASE/ambience/amb_camp_night.ogg"
        const val river = "$BASE/ambience/amb_river.ogg"
        const val tavern = "$BASE/ambience/amb_tavern.ogg"
        const val market = "$BASE/ambience/amb_market.ogg"
        const val palaceMurmur = "$BASE/ambience/amb_palace_murmur.ogg"
        const val harbor = "$BASE/ambience/amb_harbor.ogg"
        const val frontierWind = "$BASE/ambience/amb_frontier_wind.ogg"
    }

    object Voice {
        const val edictReceived = "$BASE/voice/voice_edict_received.ogg"
        const val militaryReport = "$BASE/voice/voice_military_report.ogg"
        const val courtResponse = "$BASE/voice/voice_court_response.ogg"
        const val enemyWarning = "$BASE/voice/voice_enemy_warning.ogg"
        const val cityReport = "$BASE/voice/voice_city_report.ogg"
        const val diplomacyReport = "$BASE/voice/voice_diplomacy_report.ogg"
    }

    fun bgmForScene(scene: String): String = when (scene) {
        "main_menu" -> Bgm.mainMenu
        "palace" -> Bgm.palace
        "court" -> Bgm.court
        "council" -> Bgm.council
        "map" -> Bgm.map
        "city" -> Bgm.city
        "market" -> Bgm.market
        "diplomacy" -> Bgm.diplomacy
        "battle", "military" -> Bgm.military
        "crisis" -> Bgm.crisis
        "event_sad" -> Bgm.eventSad
        "ritual" -> Bgm.ritual
        "victory" -> Bgm.victory
        "defeat" -> Bgm.defeat
        else -> Bgm.map
    }

    fun bgmForTab(tabIndex: Int): String = when (tabIndex) {
        0 -> Bgm.palace
        1 -> Bgm.court
        2 -> Bgm.map
        3 -> Bgm.court
        4 -> Bgm.military
        else -> Bgm.court
    }

    fun bgmForPalace(palaceId: String): String = when (palaceId) {
        PalaceIds.CHUIGONG -> Bgm.court
        PalaceIds.ZHENGSHI -> Bgm.council
        PalaceIds.SHUMI -> Bgm.military
        PalaceIds.WENDE -> Bgm.ritual
        PalaceIds.YUSHU -> Bgm.council
        PalaceIds.HUANGCHENG -> Bgm.crisis
        PalaceIds.HOUYUAN -> Bgm.eventSad
        PalaceIds.TAIMIAO -> Bgm.ritual
        else -> Bgm.palace
    }

    fun sfxForUi(action: String): String = when (action) {
        "click" -> Ui.click
        "confirm" -> Ui.confirm
        "cancel" -> Ui.cancel
        "open_panel" -> Ui.openPanel
        "close_panel" -> Ui.closePanel
        "switch_tab" -> Ui.switchTab
        "select" -> Ui.select
        "warning" -> Ui.warning
        "unlock" -> Ui.unlock
        "edict_stamp" -> Ui.stampEdict
        "brush_write" -> Ui.brushWrite
        "scroll_open" -> Ui.scrollOpen
        "scroll_close" -> Ui.scrollClose
        else -> Ui.click
    }

    fun sfxForGameEvent(event: String): String = when (event) {
        "click", "confirm", "cancel", "open_panel", "close_panel", "switch_tab",
        "select", "warning", "unlock", "edict_stamp", "brush_write", "scroll_open", "scroll_close" -> sfxForUi(event)
        "edict_submitted" -> Ui.scrollOpen
        "edict_confirmed" -> Ui.stampEdict
        "edict_cancelled" -> Ui.cancel
        "council_open" -> Sfx.courtMurmur
        "council_choice" -> Ui.confirm
        "story_event" -> Sfx.bell
        "story_outcome" -> Sfx.reportArrive
        "turn_advance" -> Sfx.gong
        "military_report" -> Sfx.drumWar
        "military_start" -> Sfx.encounterStart
        "city_build" -> Sfx.build
        "city_recruit" -> Sfx.recruit
        "gold_gain" -> Sfx.coin
        "relation_up" -> Sfx.relationUp
        "relation_down" -> Sfx.relationDown
        else -> Ui.click
    }

    fun sfxForCommand(commandType: String): String = when (commandType) {
        "dispatch_army" -> Sfx.march
        "assign_officer" -> Sfx.reportArrive
        "repair_city" -> Sfx.build
        "raise_grain" -> Sfx.coin
        "suppress_officer" -> Ui.warning
        "reward_officer" -> Sfx.relationUp
        "punish_officer" -> Sfx.relationDown
        "move_capital" -> Sfx.gong
        else -> Ui.confirm
    }

    fun ambienceForWeather(weatherKey: String): String? = when {
        weatherKey.contains("rain", ignoreCase = true) || weatherKey.contains("雨") -> Ambience.rain
        weatherKey.contains("storm", ignoreCase = true) || weatherKey.contains("雷") -> Ambience.storm
        weatherKey.contains("snow", ignoreCase = true) || weatherKey.contains("雪") -> Ambience.snowWind
        else -> null
    }

    fun ambienceForScene(scene: String): String? = when (scene) {
        "palace", "court", "council" -> Ambience.palaceMurmur
        "city" -> Ambience.cityDay
        "market" -> Ambience.market
        "map" -> Ambience.river
        "military" -> Ambience.frontierWind
        "harbor" -> Ambience.harbor
        else -> null
    }
}
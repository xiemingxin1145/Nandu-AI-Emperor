package com.xiemingxin.nandu.game

/**
 * 音频资源注册表。
 *
 * BGM 已于 2026-08-20 做“清仓重建”：旧 15 首全部退出运行时，
 * 新库只允许人工试听通过的纯器乐场景曲进入。
 *
 * 规则：
 * - 已验收场景曲使用稳定文件名；后续只替换音频，不乱改路由。
 * - 尚未制作/验收的用途使用 __pending_*.ogg，播放器找不到时静默，宁缺毋滥。
 * - SFX、环境声、语音继续沿用现有路径，不与本次 BGM 清仓混在一起。
 */
object AudioResourceRegistry {
    private const val BASE = "audio"

    object Bgm {
        // === 已验收的新南宋场景音乐 ===
        const val chuigongEntry = "$BASE/bgm/bgm_chuigong_hall_entry.ogg"
        const val chuigongLoop = "$BASE/bgm/bgm_chuigong_hall_loop.ogg"
        const val garden = "$BASE/bgm/bgm_garden_loop.ogg"
        const val study = "$BASE/bgm/bgm_study_loop.ogg"
        const val worldMap = "$BASE/bgm/bgm_worldmap_loop.ogg"
        const val linan = "$BASE/bgm/bgm_linan_loop.ogg"
        const val militaryCamp = "$BASE/bgm/bgm_military_camp_loop.ogg"

        // === 尚待专门制作/试听的音乐：故意指向不存在文件，避免错曲顶包 ===
        const val pendingMainMenu = "$BASE/bgm/__pending_main_menu.ogg"
        const val pendingPrologueCrisis = "$BASE/bgm/__pending_prologue_crisis.ogg"
        const val pendingPrologueSad = "$BASE/bgm/__pending_prologue_sad.ogg"
        const val pendingBattle = "$BASE/bgm/__pending_battle.ogg"
        const val pendingVictory = "$BASE/bgm/__pending_victory.ogg"
        const val pendingDefeat = "$BASE/bgm/__pending_defeat.ogg"
        const val pendingDiplomacy = "$BASE/bgm/__pending_diplomacy.ogg"
        const val pendingRitual = "$BASE/bgm/__pending_ritual.ogg"
        const val pendingMarket = "$BASE/bgm/__pending_market.ogg"

        // 兼容旧调用名，但全部映射到“新白名单/待制作槽位”。
        const val mainMenu = pendingMainMenu
        const val court = chuigongLoop
        const val palace = chuigongLoop
        const val council = chuigongLoop
        const val map = worldMap
        const val city = linan
        const val market = pendingMarket
        const val diplomacy = pendingDiplomacy
        const val military = militaryCamp
        const val crisis = pendingPrologueCrisis
        const val eventSad = pendingPrologueSad
        const val ritual = pendingRitual
        const val victory = pendingVictory
        const val defeat = pendingDefeat
        const val battle = pendingBattle
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
        "court", "council" -> Bgm.chuigongLoop
        "study" -> Bgm.study
        "garden" -> Bgm.garden
        "map" -> Bgm.worldMap
        "city", "linan" -> Bgm.linan
        "market" -> Bgm.market
        "diplomacy" -> Bgm.diplomacy
        "camp", "military" -> Bgm.militaryCamp
        "battle" -> Bgm.battle
        "crisis" -> Bgm.crisis
        "event_sad" -> Bgm.eventSad
        "ritual" -> Bgm.ritual
        "victory" -> Bgm.victory
        "defeat" -> Bgm.defeat
        else -> Bgm.worldMap
    }

    fun bgmForTab(tabIndex: Int): String = when (tabIndex) {
        0 -> Bgm.chuigongLoop
        1 -> Bgm.chuigongLoop
        2 -> Bgm.worldMap
        3 -> Bgm.study
        4 -> Bgm.militaryCamp
        else -> Bgm.chuigongLoop
    }

    fun bgmForPalace(palaceId: String): String = when (palaceId) {
        PalaceIds.CHUIGONG -> Bgm.chuigongLoop
        PalaceIds.ZHENGSHI -> Bgm.study
        PalaceIds.SHUMI -> Bgm.militaryCamp
        PalaceIds.WENDE -> Bgm.study
        PalaceIds.YUSHU -> Bgm.study
        PalaceIds.HUANGCHENG -> Bgm.pendingPrologueCrisis
        PalaceIds.HOUYUAN -> Bgm.garden
        PalaceIds.TAIMIAO -> Bgm.pendingRitual
        else -> Bgm.chuigongLoop
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
        "palace", "court", "council", "study", "garden" -> Ambience.palaceMurmur
        "city", "linan" -> Ambience.cityDay
        "market" -> Ambience.market
        "map" -> Ambience.river
        "military", "camp" -> Ambience.frontierWind
        "harbor" -> Ambience.harbor
        else -> null
    }
}

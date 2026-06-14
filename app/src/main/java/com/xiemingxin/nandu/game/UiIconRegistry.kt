package com.xiemingxin.nandu.game

/**
 * V0.6.5 UI图标注册表。
 * Claude 已上传到 app/src/main/assets/images/ui/icons/，这里统一给 Compose 调用。
 */
object UiIconRegistry {
    private const val BASE = "images/ui/icons"

    const val diplomacyDeclareWar = "$BASE/diplomacy_declare_war.webp"
    const val diplomacyPeace = "$BASE/diplomacy_peace.webp"

    const val weatherSpring = "$BASE/weather_spring.webp"
    const val weatherSummer = "$BASE/weather_summer.webp"
    const val weatherAutumn = "$BASE/weather_autumn.webp"
    const val weatherWinter = "$BASE/weather_winter.webp"
    const val weatherClear = "$BASE/weather_clear.webp"
    const val weatherRain = "$BASE/weather_rain.webp"
    const val weatherSnow = "$BASE/weather_snow.webp"
    const val weatherFog = "$BASE/weather_fog.webp"
    const val weatherWind = "$BASE/weather_wind.webp"

    const val statForce = "$BASE/stat_force.webp"
    const val statCommand = "$BASE/stat_command.webp"
    const val statStrategy = "$BASE/stat_strategy.webp"
    const val statPolitics = "$BASE/stat_politics.webp"
    const val statCharm = "$BASE/stat_charm.webp"
    const val statLoyalty = "$BASE/stat_loyalty.webp"

    const val battleAmbush = "$BASE/battle_ambush.webp"
    const val battleBreakout = "$BASE/battle_breakout.webp"
    const val battleAttack = "$BASE/battle_attack.webp"
    const val battleDefense = "$BASE/battle_defense.webp"

    const val actionAppoint = "$BASE/action_appoint.webp"
    const val actionPromote = "$BASE/action_promote.webp"
    const val actionBuild = "$BASE/action_build.webp"
    const val actionPunish = "$BASE/action_punish.webp"
    const val actionRecruit = "$BASE/action_recruit.webp"
    const val actionResearch = "$BASE/action_research.webp"
    const val actionTrain = "$BASE/action_train.webp"
    const val actionReward = "$BASE/action_reward.webp"

    const val statusPopularSupport = "$BASE/status_popular_support.webp"
    const val statusFamine = "$BASE/status_famine.webp"
    const val statusEpidemic = "$BASE/status_epidemic.webp"
    const val statusStability = "$BASE/status_stability.webp"
    const val statusProsperity = "$BASE/status_prosperity.webp"
    const val statusCorruption = "$BASE/status_corruption.webp"

    const val systemTask = "$BASE/system_task.webp"
    const val systemCancel = "$BASE/system_cancel.webp"
    const val systemSave = "$BASE/system_save.webp"
    const val systemHelp = "$BASE/system_help.webp"
    const val systemAchievement = "$BASE/system_achievement.webp"
    const val systemConfirm = "$BASE/system_confirm.webp"
    const val systemSettings = "$BASE/system_settings.webp"
    const val systemLoad = "$BASE/system_load.webp"
    const val systemBack = "$BASE/system_back.webp"

    const val resourceWeapon = "$BASE/resource_weapon.webp"
    const val resourceCloth = "$BASE/resource_cloth.webp"
    const val resourceWood = "$BASE/resource_wood.webp"
    const val resourceIron = "$BASE/resource_iron.webp"
    const val resourceHorse = "$BASE/resource_horse.webp"

    const val factionVolunteer = "$BASE/faction_volunteer.webp"
    const val factionSong = "$BASE/faction_song.webp"
    const val factionRebel = "$BASE/faction_rebel.webp"
    const val factionJin = "$BASE/faction_jin.webp"

    fun weatherIcon(weather: WeatherType): String = when (weather) {
        WeatherType.CLEAR -> weatherClear
        WeatherType.RAIN -> weatherRain
        WeatherType.STORM -> weatherRain
        WeatherType.FOG -> weatherFog
        WeatherType.SNOW -> weatherSnow
        WeatherType.WIND -> weatherWind
    }

    fun seasonIcon(season: Season): String = when (season) {
        Season.SPRING -> weatherSpring
        Season.SUMMER -> weatherSummer
        Season.AUTUMN -> weatherAutumn
        Season.WINTER -> weatherWinter
    }

    fun factionIcon(factionId: String): String = when (factionId) {
        "song" -> factionSong
        "jin" -> factionJin
        "rebel" -> factionRebel
        "volunteer" -> factionVolunteer
        else -> factionSong
    }
}

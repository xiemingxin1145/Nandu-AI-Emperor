package com.xiemingxin.nandu.game

/**
 * V0.6.5 美术资源注册表。
 * 图片本体可后续逐批上传到 app/src/main/assets/images/，代码先稳定引用这些路径。
 */
data class CharacterArt(
    val id: String,
    val name: String,
    val portrait: String,
    val halfbody: String,
    val faction: String
)

data class NamedArt(
    val id: String,
    val name: String,
    val path: String
)

object ArtResourceRegistry {
    const val BASE = "images"

    object Fallback {
        const val portrait = "$BASE/characters/placeholder_portrait.webp"
        const val halfbody = "$BASE/characters/placeholder_halfbody.webp"
        const val city = "$BASE/cities/placeholder_city.webp"
        const val event = "$BASE/events/placeholder_event.webp"
        const val battle = "$BASE/battles/placeholder_battle.webp"
    }

    val historicalCharacters: Map<String, CharacterArt> = listOf(
        character("zhao_gou", "赵构", "song"),
        character("yue_fei", "岳飞", "song"),
        character("han_shizhong", "韩世忠", "song"),
        character("wu_jie", "吴玠", "song"),
        character("liu_qi", "刘锜", "song"),
        character("li_gang", "李纲", "song"),
        character("zhao_ding", "赵鼎", "song"),
        character("qin_hui", "秦桧", "song"),
        character("zhang_jun", "张浚", "song"),
        character("zong_ze", "宗泽", "song"),
        character("wanyan_zongbi", "完颜宗弼", "jin")
    ).associateBy { it.id }

    val cityBackgrounds: Map<String, NamedArt> = mapOf(
        "linan" to named("linan", "临安", "$BASE/cities/city_linan.webp"),
        "kaifeng" to named("kaifeng", "开封", "$BASE/cities/city_kaifeng.webp"),
        "xiangyang" to named("xiangyang", "襄阳", "$BASE/cities/city_xiangyang.webp"),
        "ezhou" to named("ezhou", "鄂州", "$BASE/cities/city_ezhou.webp"),
        "jiankang" to named("jiankang", "建康", "$BASE/cities/city_jiankang.webp"),
        "yangzhou" to named("yangzhou", "扬州", "$BASE/cities/city_yangzhou.webp"),
        "chengdu" to named("chengdu", "成都", "$BASE/cities/city_chengdu.webp")
    )

    val eventImages: Map<String, NamedArt> = mapOf(
        "jingkang" to named("jingkang", "靖康之变", "$BASE/events/event_jingkang.webp"),
        "north_crossing" to named("north_crossing", "二帝北狩", "$BASE/events/event_north_crossing.webp"),
        "yue_fei_execute" to named("yue_fei_execute", "岳飞被害", "$BASE/events/event_yue_fei_execute.webp"),
        "shaoxing_peace" to named("shaoxing_peace", "绍兴和议", "$BASE/events/event_shaoxing_peace.webp"),
        "default" to named("default", "通用事件", "$BASE/events/event_default.webp")
    )

    val battleImages: Map<String, NamedArt> = mapOf(
        "river_naval" to named("river_naval", "水战", "$BASE/battles/battle_river_naval.webp"),
        "field_battle" to named("field_battle", "野战", "$BASE/battles/battle_field.webp"),
        "siege" to named("siege", "攻城战", "$BASE/battles/battle_siege.webp"),
        "mountain" to named("mountain", "山地战", "$BASE/battles/battle_mountain.webp"),
        "default" to named("default", "通用战役", "$BASE/battles/battle_default.webp")
    )

    val mapImages: Map<String, NamedArt> = mapOf(
        "parchment_bg" to named("parchment_bg", "羊皮纸底图", "$BASE/maps/map_parchment_bg.webp"),
        "terrain_overlay" to named("terrain_overlay", "地形叠加层", "$BASE/maps/map_terrain_overlay.webp")
    )

    val uiImages: Map<String, NamedArt> = mapOf(
        "btn_primary" to named("btn_primary", "主按钮", "$BASE/ui/ui_btn_primary.webp"),
        "btn_secondary" to named("btn_secondary", "副按钮", "$BASE/ui/ui_btn_secondary.webp"),
        "btn_danger" to named("btn_danger", "警告按钮", "$BASE/ui/ui_btn_danger.webp"),
        "frame_gold" to named("frame_gold", "金色边框", "$BASE/ui/ui_frame_gold.webp"),
        "frame_jade" to named("frame_jade", "玉色边框", "$BASE/ui/ui_frame_jade.webp")
    )

    fun portraitForOfficer(officerId: String): String = historicalCharacters[officerId]?.portrait ?: templatePortraitFor(officerId)

    fun halfbodyForOfficer(officerId: String): String = historicalCharacters[officerId]?.halfbody ?: templateHalfbodyFor(officerId)

    fun cityBackground(cityId: String): String = cityBackgrounds[cityId]?.path ?: "$BASE/cities/city_default.webp"

    fun eventImage(eventId: String): String = eventImages[eventId]?.path ?: Fallback.event

    fun battleImage(battleType: String): String = battleImages[battleType]?.path ?: Fallback.battle

    fun templatePortraitFor(seed: String, group: String = "song_military"): String {
        val index = ((seed.hashCode() and Int.MAX_VALUE) % 20) + 1
        return when (group) {
            "song_civil" -> "$BASE/characters/templates/song_civil/tmpl_civil_${index.twoDigits()}.webp"
            "song_soldier" -> "$BASE/characters/templates/song_soldier/tmpl_soldier_${index.twoDigits()}.webp"
            "song_official" -> "$BASE/characters/templates/song_official/tmpl_official_${index.twoDigits()}.webp"
            "jin_military" -> "$BASE/characters/templates/jin_military/tmpl_jin_mil_${index.twoDigits()}.webp"
            "jin_civil" -> "$BASE/characters/templates/jin_civil/tmpl_jin_civ_${((index - 1) % 12 + 1).twoDigits()}.webp"
            "enemy_soldier" -> "$BASE/characters/templates/enemy_soldier/tmpl_enemy_sol_${((index - 1) % 16 + 1).twoDigits()}.webp"
            "rebel" -> "$BASE/characters/templates/rebel/tmpl_rebel_${((index - 1) % 16 + 1).twoDigits()}.webp"
            "neutral" -> "$BASE/characters/templates/neutral/tmpl_neutral_${((index - 1) % 24 + 1).twoDigits()}.webp"
            else -> "$BASE/characters/templates/song_military/tmpl_military_${((index - 1) % 25 + 1).twoDigits()}.webp"
        }
    }

    fun templateHalfbodyFor(seed: String, group: String = "song_military"): String {
        val index = ((seed.hashCode() and Int.MAX_VALUE) % 10) + 1
        return when (group) {
            "song_civil" -> "$BASE/characters/templates/song_civil/tmpl_civil_${index.twoDigits()}.webp"
            "song_soldier" -> "$BASE/characters/templates/song_soldier/tmpl_soldier_${index.twoDigits()}.webp"
            "song_official" -> "$BASE/characters/templates/song_official/tmpl_official_${index.twoDigits()}.webp"
            "jin_military" -> "$BASE/characters/templates/jin_military/tmpl_jin_mil_${index.twoDigits()}.webp"
            "jin_civil" -> "$BASE/characters/templates/jin_civil/tmpl_jin_civ_${((index - 1) % 6 + 1).twoDigits()}.webp"
            "enemy_soldier" -> "$BASE/characters/templates/enemy_soldier/tmpl_enemy_sol_${((index - 1) % 8 + 1).twoDigits()}.webp"
            "rebel" -> "$BASE/characters/templates/rebel/tmpl_rebel_${((index - 1) % 8 + 1).twoDigits()}.webp"
            "neutral" -> "$BASE/characters/templates/neutral/tmpl_neutral_${((index - 1) % 12 + 1).twoDigits()}.webp"
            else -> "$BASE/characters/templates/song_military/tmpl_military_${((index - 1) % 15 + 1).twoDigits()}.webp"
        }
    }

    private fun character(id: String, name: String, faction: String): CharacterArt = CharacterArt(
        id = id,
        name = name,
        portrait = "$BASE/characters/portrait_$id.webp",
        halfbody = "$BASE/characters/halfbody_$id.webp",
        faction = faction
    )

    private fun named(id: String, name: String, path: String): NamedArt = NamedArt(id, name, path)

    private fun Int.twoDigits(): String = toString().padStart(2, '0')
}

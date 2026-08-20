package com.xiemingxin.nandu.game

/** Central registry for short in-game CG videos. */
data class CgVideoArt(
    val id: String,
    val name: String,
    val path: String
)

object CgResourceRegistry {
    val prewar = CgVideoArt(
        id = "prewar_departure_approach_v01",
        name = "战前叙事：出征与压境",
        path = "video/VID-CZ-001-PREWAR-V01.mp4"
    )

    private val exactPrewarEvents = setOf(
        "han_shizhong_requests_battle",
        "jin_army_crosses_huai",
        "yangzhou_panic_flee",
        "city_siege_jiankang"
    )

    fun videoFor(
        eventId: String,
        type: String,
        title: String,
        description: String,
        artHint: String
    ): CgVideoArt? {
        if (eventId in exactPrewarEvents) return prewar

        val text = "$eventId $title $description $artHint"
        val militaryType = type == "jin_event" || type == "random_military" || type == "city_crisis"
        val militaryText = listOf("出征", "压境", "金军", "金兵", "渡淮", "攻城", "迎战", "前线")
            .any(text::contains)
        return if (militaryType && militaryText) prewar else null
    }
}

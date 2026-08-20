package com.xiemingxin.nandu.game

/**
 * 游戏内短 CG 注册与剧情匹配。
 *
 * 当前 Batch 1 视频仍带生成平台水印，只作为原型素材。播放器入口集中走本表，
 * 后续替换无水印正式版时无需修改剧情与 UI 代码。
 */
data class CgVideoArt(
    val id: String,
    val name: String,
    val path: String,
    val prototypeOnly: Boolean = true
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

        val text = "$title $description $artHint"
        val militaryType = type == "jin_event" || type == "random_military" || type == "city_crisis"
        val militaryText = listOf("出征", "压境", "金军", "金兵", "渡淮", "攻城", "迎战")
            .any(text::contains)
        return if (militaryType && militaryText) prewar else null
    }
}

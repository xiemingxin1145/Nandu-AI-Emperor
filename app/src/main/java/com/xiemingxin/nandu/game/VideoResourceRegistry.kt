package com.xiemingxin.nandu.game

/** Runtime registry for the V3 generated MP4 library under repository-root assets/videos/. */
data class VideoClip(
    val id: String,
    val name: String,
    val path: String,
    val category: String,
    val loop: Boolean = false,
    val aspectRatio: Float = 16f / 9f
)

object VideoResourceRegistry {
    private fun clip(id: String, name: String, category: String, file: String, loop: Boolean = false, aspectRatio: Float = 16f / 9f) =
        VideoClip(id, name, "videos/$category/$file", category, loop, aspectRatio)

    val intro = listOf(
        clip("splash_logo", "水墨片头", "intro", "V01_splash_logo.mp4"),
        clip("menu_loop", "主菜单动态背景", "intro", "V02_menu_bg_loop.mp4", loop = true),
        clip("jingkang_intro", "靖康之难", "intro", "V03_intro_cinematic.mp4")
    )

    val seasons = listOf(
        clip("spring", "春", "seasons", "V04_season_spring.mp4"),
        clip("summer", "夏", "seasons", "V05_season_summer.mp4"),
        clip("autumn", "秋", "seasons", "V06_season_autumn.mp4"),
        clip("winter", "冬", "seasons", "V07_season_winter.mp4")
    )

    val battle = listOf(
        clip("field_clash", "平原对冲", "battle", "V08_battle_field_clash.mp4"),
        clip("siege_assault", "攻城", "battle", "V09_battle_siege_assault.mp4"),
        clip("naval_clash", "水战", "battle", "V10_battle_naval_clash.mp4"),
        clip("mountain_pass", "山地关隘", "battle", "V11_battle_mountain_pass.mp4"),
        clip("victory", "战胜", "battle", "V12_battle_victory.mp4"),
        clip("defeat", "战败", "battle", "V13_battle_defeat.mp4")
    )

    val units = listOf(
        clip("song_infantry", "宋军步兵", "units", "V14_unit_song_infantry.mp4"),
        clip("song_archer", "宋军弓手", "units", "V15_unit_song_archer.mp4"),
        clip("song_crossbow", "宋军弩手", "units", "V16_unit_song_crossbow.mp4"),
        clip("divine_arm", "神臂弓", "units", "V17_unit_divine_arm.mp4"),
        clip("song_cavalry", "宋军骑兵", "units", "V18_unit_song_cavalry.mp4"),
        clip("song_navy", "宋军水军", "units", "V19_unit_song_navy.mp4"),
        clip("beiwei", "背嵬军", "units", "V20_unit_beiwei.mp4"),
        clip("shengjie", "胜捷军", "units", "V21_unit_shengjie.mp4"),
        clip("tabai", "踏白军", "units", "V22_unit_tabai.mp4"),
        clip("scout", "探马", "units", "V23_unit_scout.mp4"),
        clip("jin_infantry", "金军步兵", "units", "V24_unit_jin_infantry.mp4"),
        clip("jin_horse_archer", "金军马弓手", "units", "V25_unit_jin_horse_archer.mp4"),
        clip("jin_heavy", "金军重装", "units", "V26_unit_jin_heavy.mp4"),
        clip("jin_guaizi", "拐子马", "units", "V27_unit_jin_guaizi.mp4"),
        clip("iron_pagoda", "铁浮屠", "units", "V28_unit_iron_pagoda.mp4")
    )

    val skills = listOf(
        clip("skill_yue_fei", "岳飞技能", "skills", "V29_skill_yue_fei.mp4"),
        clip("skill_han_shizhong", "韩世忠技能", "skills", "V30_skill_han_shizhong.mp4"),
        clip("skill_li_gang", "李纲技能", "skills", "V31_skill_li_gang.mp4"),
        clip("skill_wu_jie", "吴玠技能", "skills", "V32_skill_wu_jie.mp4"),
        clip("skill_qin_hui", "秦桧技能", "skills", "V33_skill_qin_hui.mp4"),
        clip("skill_zong_ze", "宗泽技能", "skills", "V34_skill_zong_ze.mp4")
    )

    val characterLive = listOf(
        clip("char_yue_fei", "岳飞动态立绘", "char_live", "V35_char_yue_fei.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_han_shizhong", "韩世忠动态立绘", "char_live", "V36_char_han_shizhong.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_qin_hui", "秦桧动态立绘", "char_live", "V37_char_qin_hui.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_zhao_gou", "赵构动态立绘", "char_live", "V38_char_zhao_gou.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_li_gang", "李纲动态立绘", "char_live", "V39_char_li_gang.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_zhao_ding", "赵鼎动态立绘", "char_live", "V40_char_zhao_ding.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_zhang_jun", "张浚动态立绘", "char_live", "V41_char_zhang_jun.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_wu_jie", "吴玠动态立绘", "char_live", "V42_char_wu_jie.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_liu_qi", "刘锜动态立绘", "char_live", "V43_char_liu_qi.mp4", loop = true, aspectRatio = 3f / 4f),
        clip("char_zong_ze", "宗泽动态立绘", "char_live", "V44_char_zong_ze.mp4", loop = true, aspectRatio = 3f / 4f)
    )

    val uiEffects = listOf(
        clip("edict_stamp", "朱批落印", "ui_effects", "V45_ui_edict_stamp.mp4"),
        clip("city_capture", "城池易手", "ui_effects", "V46_ui_city_capture.mp4"),
        clip("level_up", "升级", "ui_effects", "V47_ui_level_up.mp4"),
        clip("gold_reward", "钱粮赏赐", "ui_effects", "V48_ui_gold_reward.mp4"),
        clip("grain_reward", "粮草入库", "ui_effects", "V49_ui_grain_reward.mp4"),
        clip("morale_boost", "士气提升", "ui_effects", "V50_ui_morale_boost.mp4")
    )

    val cinematic = listOf(
        clip("prewar", "战前叙事", "cinematic", "NanduWuhui_Prewar_CG_V01.mp4")
    )

    val all: List<VideoClip> = intro + seasons + battle + units + skills + characterLive + uiEffects + cinematic
    private val byId = all.associateBy { it.id }

    fun find(id: String): VideoClip? = byId[id]
    fun characterLiveFor(officerId: String): VideoClip? = when (officerId) {
        "yue_fei" -> find("char_yue_fei")
        "han_shizhong" -> find("char_han_shizhong")
        "qin_hui" -> find("char_qin_hui")
        "zhao_gou" -> find("char_zhao_gou")
        "li_gang" -> find("char_li_gang")
        "zhao_ding" -> find("char_zhao_ding")
        "zhang_jun" -> find("char_zhang_jun")
        "wu_jie" -> find("char_wu_jie")
        "liu_qi" -> find("char_liu_qi")
        "zong_ze" -> find("char_zong_ze")
        else -> null
    }
}

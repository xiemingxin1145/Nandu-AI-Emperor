package com.xiemingxin.nandu.game

/**
 * V2.4 美术资源注册表。
 *
 * 资源已统一进入 app/src/main/assets/images/。
 * 本表负责把旧代码里的角色、城池、事件、地图、UI 路径对齐到真实 WebP 文件。
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
        const val portrait = "$BASE/characters/silhouettes/silhouette_unknown.webp"
        const val halfbody = "$BASE/characters/silhouettes/silhouette_unknown.webp"
        const val city = "$BASE/city/linan_palace.webp"
        const val event = "$BASE/events/event_yushufang_night.webp"
        const val battle = "$BASE/events/event_jianghuai_battle.webp"
        const val mapIcon = "$BASE/map/icons/city_county.webp"
        const val ui = "$BASE/ui/dialog_frame.webp"
    }

    val historicalCharacters: Map<String, CharacterArt> = listOf(
        // 有专属 portrait_ + halfbody_ 文件的核心人物
        character("zhao_gou",     "赵构",       "song",      "portrait_zhao_gou.webp",      "halfbody_zhao_gou.webp"),
        character("yue_fei",      "岳飞",       "song",      "portrait_yue_fei.webp",       "halfbody_yue_fei.webp"),
        character("han_shizhong", "韩世忠",     "song",      "portrait_han_shizhong.webp",  "halfbody_han_shizhong.webp"),
        character("wu_jie",       "吴玠",       "song",      "wu_jie.webp",                 "halfbody_wu_jie.webp"),
        character("liu_qi",       "刘锜",       "song",      "portrait_liu_qi.webp",        "halfbody_liu_qi.webp"),
        character("li_gang",      "李纲",       "song",      "portrait_li_gang.webp",       "halfbody_li_gang.webp"),
        character("zhao_ding",    "赵鼎",       "song",      "portrait_zhao_ding.webp",     "halfbody_zhao_ding.webp"),
        character("qin_hui",      "秦桧",       "song",      "portrait_qin_hui.webp",       "halfbody_qin_hui.webp"),
        character("zhang_jun",    "张浚",       "song",      "portrait_zhang_jun.webp",     "halfbody_zhang_jun.webp"),
        character("zong_ze",      "宗泽",       "song",      "portrait_zong_ze.webp",       "halfbody_zong_ze.webp"),
        character("wanyan_zongbi","完颜宗弼",   "jin",       "portrait_wanyan_zongbi.webp", "halfbody_wanyan_zongbi.webp"),
        // 有简单 *.webp 无 halfbody 的人物
        character("wu_lin",        "吴璘",       "song",      "wu_lin.webp"),
        character("zhang_jun2",    "张俊",       "song",      "zhang_jun_general.webp"),
        character("yang_yizhong",  "杨沂中",     "song",      "yang_yizhong.webp"),
        character("lv_yihao",      "吕颐浩",     "song",      "lv_yihao.webp"),
        character("wang_boyan",    "汪伯彦",     "song",      "wang_boyan.webp"),
        character("huang_qianshan","黄潜善",     "song",      "huang_qianshan.webp"),
        character("zhu_shengfei",  "朱胜非",     "song",      "zhu_shengfei.webp"),
        character("hu_quan",       "胡铨",       "song",      "hu_quan.webp"),
        character("empress",       "皇后",       "palace",    "empress.webp"),
        character("dowager",       "太后",       "palace",    "dowager.webp"),
        character("eunuch",        "内侍押班",   "palace",    "eunuch_pressman.webp"),
        character("bureau_clerk",  "皇城司勾当官","bureau",   "bureau_clerk.webp"),
        character("jin_envoy",     "金使",       "jin",       "jin_envoy.webp"),
        character("xixia_envoy",   "西夏使者",   "xixia",     "xixia_envoy.webp"),
        character("dali_envoy",    "大理使者",   "dali",      "dali_envoy.webp"),
        character("goryeo_envoy",  "高丽使者",   "goryeo",    "goryeo_envoy.webp"),
        character("sea_merchant",  "海商首领",   "sea_trade", "sea_merchant.webp")
    ).associateBy { it.id }

    // 序章专用 NPC 头像（512×512，对话框 / 人物志用）
    val storyPortraits: Map<String, NamedArt> = mapOf(
        "li_gang"        to story("li_gang",        "李纲",     "npc_li_gang.webp"),
        "wang_boyan"     to story("wang_boyan",      "汪伯彦",   "npc_wang_boyan.webp"),
        "huang_qianshan" to story("huang_qianshan",  "黄潜善",   "npc_huang_qianshan.webp"),
        "zong_ze"        to story("zong_ze",          "宗泽",     "npc_zong_ze.webp"),
        "han_shizhong"   to story("han_shizhong",    "韩世忠",   "npc_han_shizhong.webp"),
        "zhao_ding"      to story("zhao_ding",        "赵鼎",     "npc_zhao_ding.webp"),
        "yue_fei"        to story("yue_fei",          "岳飞",     "npc_yue_fei.webp"),
        "eunuch_steward" to story("eunuch_steward",  "内侍总管", "npc_eunuch_steward.webp")
    )

    // 主菜单背景
    val menuImages: Map<String, NamedArt> = mapOf(
        "main_bg" to named("main_bg", "主菜单背景", "$BASE/menu/menu_main_bg.webp")
    )

    // 场景/位置背景（序章及游戏内通用场景）
    val locationBackgrounds: Map<String, NamedArt> = mapOf(
        "mingdao_hall_night" to location("mingdao_hall_night", "明道宫偏殿（夜）", "bg_mingdao_hall_night.webp"),
        "yingtian_court_day" to location("yingtian_court_day", "应天府朝堂（日）", "bg_yingtian_court_day.webp"),
        "yingtian_corridor"  to location("yingtian_corridor",  "应天府走廊",        "bg_yingtian_corridor.webp"),
        "kaifeng_war_report" to location("kaifeng_war_report", "开封战局配图",      "bg_kaifeng_war_report.webp")
    )

    val cityBackgrounds: Map<String, NamedArt> = mapOf(
        "linan" to city("linan", "临安", "linan_palace.webp"),
        "jiankang" to city("jiankang", "建康", "jiankang_river_fortress.webp"),
        "xiangyang" to city("xiangyang", "襄阳", "xiangyang_fortress.webp"),
        "kaifeng" to city("kaifeng", "汴京", "kaifeng_old_capital.webp"),
        "luoyang" to city("luoyang", "洛阳", "luoyang_old_capital.webp"),
        "yanjing" to city("yanjing", "燕京", "yanjing_north.webp"),
        "chengdu" to city("chengdu", "成都", "chengdu_west.webp"),
        "quanzhou" to city("quanzhou", "泉州", "quanzhou_port.webp"),
        "mingzhou" to city("mingzhou", "明州", "mingzhou_shipyard.webp"),
        "guangzhou" to city("guangzhou", "广州", "guangzhou_southsea.webp"),
        "xingqing" to city("xingqing", "兴庆府", "xingqing_xixia.webp"),
        "dali" to city("dali", "大理", "dali_southwest.webp"),
        "hefei" to city("hefei", "合肥", "hefei_front.webp"),
        "shouchun" to city("shouchun", "寿春", "shouchun_front.webp"),
        "ezhou" to city("ezhou", "鄂州", "ezhou_river.webp"),
        "yangzhou" to city("yangzhou", "扬州", "yangzhou_canal.webp"),
        "tanzhou" to city("tanzhou", "潭州", "tanzhou.webp"),
        "hongzhou" to city("hongzhou", "洪州", "hongzhou.webp"),
        "fuzhou" to city("fuzhou", "福州", "fuzhou.webp"),
        "raozhou" to city("raozhou", "饶州", "raozhou.webp"),
        "jiangzhou" to city("jiangzhou", "江州", "jiangzhou.webp"),
        "jingzhou" to city("jingzhou", "荆州", "jingzhou.webp"),
        "luzhou" to city("luzhou", "庐州", "luzhou.webp"),
        "chuzhou" to city("chuzhou", "楚州", "chuzhou.webp"),
        "sizhou" to city("sizhou", "泗州", "sizhou.webp"),
        "fengxiang" to city("fengxiang", "凤翔府", "fengxiang.webp"),
        "yanan" to city("yanan", "延安府", "yanan.webp"),
        "hezhong" to city("hezhong", "河中府", "hezhong.webp"),
        "lanzhou" to city("lanzhou", "兰州", "lanzhou.webp"),
        "lingzhou" to city("lingzhou", "灵州", "lingzhou.webp")
    )

    val eventImages: Map<String, NamedArt> = mapOf(
        "yuefei_petition" to event("yuefei_petition", "岳飞请战", "event_yuefei_petition.webp"),
        "fengboting_crisis" to event("fengboting_crisis", "风波亭危机", "event_fengboting_crisis.webp"),
        "empress_secret" to event("empress_secret", "皇后密陈", "event_empress_secret.webp"),
        "yushufang_night" to event("yushufang_night", "御书房夜议", "event_yushufang_night.webp"),
        "taimiao_oath" to event("taimiao_oath", "太庙誓师", "event_taimiao_oath.webp"),
        "jin_envoy" to event("jin_envoy", "金使来朝", "event_jin_envoy.webp"),
        "sea_trade_boom" to event("sea_trade_boom", "市舶盛景", "event_sea_trade_boom.webp"),
        "linan_prosperity" to event("linan_prosperity", "临安繁华", "event_linan_prosperity.webp"),
        "jianghuai_battle" to event("jianghuai_battle", "江淮防线", "event_jianghuai_battle.webp"),
        "xixia_horse_market" to event("xixia_horse_market", "西夏马市", "event_xixia_horse_market.webp"),
        "dali_trade" to event("dali_trade", "大理商路", "event_dali_trade.webp"),
        "secret_police" to event("secret_police", "皇城司密奏", "event_secret_police.webp"),
        "default" to event("default", "通用事件", "event_yushufang_night.webp"),
        // 序章 CG
        "prologue_night_escape"        to event("prologue_night_escape",        "序章：南渡夜境",    "cg_prologue_night_escape.webp"),
        "prologue_mingdao_awake"       to event("prologue_mingdao_awake",       "序章：明道宫惊醒",  "cg_prologue_mingdao_awake.webp"),
        "prologue_identity_reflection" to event("prologue_identity_reflection",  "序章：身份确认",    "cg_prologue_identity_reflection.webp"),
        "prologue_first_choice"        to event("prologue_first_choice",         "第一抉择：朝局初动","cg_first_choice_court_tension.webp")
    )

    val palaceBackgrounds: Map<String, NamedArt> = mapOf(
        PalaceIds.CHUIGONG to palace(PalaceIds.CHUIGONG, "垂拱殿", "chuigongdian.webp"),
        PalaceIds.ZHENGSHI to palace(PalaceIds.ZHENGSHI, "政事堂", "zhengshitang.webp"),
        PalaceIds.SHUMI to palace(PalaceIds.SHUMI, "枢密院", "shumiyuan.webp"),
        PalaceIds.WENDE to palace(PalaceIds.WENDE, "文德殿", "wendedian.webp"),
        PalaceIds.YUSHU to palace(PalaceIds.YUSHU, "御书房", "yushufang.webp"),
        PalaceIds.HUANGCHENG to palace(PalaceIds.HUANGCHENG, "皇城司", "huangchengsi.webp"),
        PalaceIds.HOUYUAN to palace(PalaceIds.HOUYUAN, "后苑", "houyuan.webp"),
        PalaceIds.TAIMIAO to palace(PalaceIds.TAIMIAO, "太庙", "taimiao.webp")
    )

    val mapImages: Map<String, NamedArt> = mapOf(
        "parchment_bg" to named("parchment_bg", "南宋天下图", "$BASE/map/song_world_parchment.webp"),
        "military" to named("military", "军事山河图", "$BASE/map/song_world_military.webp"),
        "political" to named("political", "外交势力图", "$BASE/map/song_world_political.webp"),
        "trade" to named("trade", "市舶海贸图", "$BASE/map/song_world_trade.webp"),
        "strategy" to named("strategy", "战略沙盘图", "$BASE/map/song_world_strategy.webp")
    )

    val mapIconImages: Map<String, NamedArt> = mapOf(
        "capital_song" to icon("capital_song", "宋都城", "city_capital_song.webp"),
        "capital_jin" to icon("capital_jin", "金都城", "city_capital_jin.webp"),
        "capital_xixia" to icon("capital_xixia", "西夏都城", "city_capital_xixia.webp"),
        "capital_dali" to icon("capital_dali", "大理都城", "city_capital_dali.webp"),
        "xixia_capital" to icon("xixia_capital", "西夏都城", "city_capital_xixia.webp"),
        "dali_capital" to icon("dali_capital", "大理都城", "city_capital_dali.webp"),
        "metropolis" to icon("metropolis", "巨城", "city_metropolis.webp"),
        "west_metropolis" to icon("west_metropolis", "西南巨城", "city_metropolis.webp"),
        "fortress" to icon("fortress", "重镇", "city_fortress.webp"),
        "front_fortress" to icon("front_fortress", "前线城", "city_frontline.webp"),
        "river_fortress" to icon("river_fortress", "江防重镇", "city_fortress_river.webp"),
        "north_fortress" to icon("north_fortress", "北方重镇", "city_fortress.webp"),
        "yanjing" to icon("yanjing", "燕云重镇", "city_fortress_jin.webp"),
        "port" to icon("port", "港市", "city_port.webp"),
        "sea_port" to icon("sea_port", "港市", "city_port.webp"),
        "ship_port" to icon("ship_port", "船材港市", "city_port.webp"),
        "south_port" to icon("south_port", "南海港市", "city_port.webp"),
        "old_capital" to icon("old_capital", "旧都", "city_old_capital.webp"),
        "old_capital_west" to icon("old_capital_west", "西京旧都", "city_old_capital.webp"),
        "county" to icon("county", "州县", "city_county.webp"),
        "pass" to icon("pass", "关隘", "city_pass.webp"),
        "strategic" to icon("strategic", "战略节点", "city_strategic.webp"),
        "trade" to icon("trade", "商贸节点", "city_trade.webp"),
        "water" to icon("water", "水路城", "city_water.webp")
    )

    val uiImages: Map<String, NamedArt> = mapOf(
        "dialog_frame" to ui("dialog_frame", "奏议框", "dialog_frame.webp"),
        "edict_bar" to ui("edict_bar", "圣旨落笔", "edict_bar.webp"),
        "danger_badge" to ui("danger_badge", "危急", "danger_badge.webp"),
        "faction_tag" to ui("faction_tag", "派系", "faction_tag.webp"),
        "choice_button" to ui("choice_button", "御前裁断", "choice_button.webp"),
        "palace_tab" to ui("palace_tab", "宫殿页签", "palace_tab.webp"),
        "npc_card_frame" to ui("npc_card_frame", "人物卡框", "npc_card_frame.webp"),
        "city_panel" to ui("city_panel", "城池面板", "city_panel.webp"),
        "map_panel" to ui("map_panel", "山河面板", "map_panel.webp"),
        "edict_scroll" to ui("edict_scroll", "圣旨卷轴", "edict_scroll.webp"),
        "relation_badge" to ui("relation_badge", "好感信任", "relation_badge.webp"),
        "trade_badge" to ui("trade_badge", "商税外贸", "trade_badge.webp"),
        "war_badge" to ui("war_badge", "军务战报", "war_badge.webp"),
        "ritual_badge" to ui("ritual_badge", "礼制名望", "ritual_badge.webp"),
        "locked_badge" to ui("locked_badge", "未登场", "locked_badge.webp")
    )

    fun storyPortrait(npcId: String): String = storyPortraits[npcId]?.path ?: portraitForOfficer(npcId)
    fun menuBackground(key: String = "main_bg"): String = menuImages[key]?.path ?: Fallback.ui
    fun locationBackground(locationId: String): String = locationBackgrounds[locationId]?.path ?: Fallback.event
    fun prologueCg(cgKey: String): String = eventImages["prologue_$cgKey"]?.path ?: Fallback.event

    fun portraitForOfficer(officerId: String): String = historicalCharacters[officerId]?.portrait ?: templatePortraitFor(officerId)
    fun halfbodyForOfficer(officerId: String): String = historicalCharacters[officerId]?.halfbody ?: templateHalfbodyFor(officerId)
    fun cityBackground(cityId: String): String = cityBackgrounds[cityId]?.path ?: Fallback.city
    fun eventImage(eventId: String): String = eventImages[eventId]?.path ?: Fallback.event
    fun uiImage(id: String): String = uiImages[id]?.path ?: Fallback.ui
    fun mapIcon(iconKey: String): String = mapIconImages[iconKey]?.path ?: mapIconImages[iconKey.substringAfterLast('_')]?.path ?: Fallback.mapIcon
    fun battleImage(battleType: String): String = when (battleType) {
        "river_naval" -> "$BASE/events/event_jianghuai_battle.webp"
        "siege" -> "$BASE/events/event_jianghuai_battle.webp"
        else -> Fallback.battle
    }
    fun palaceBackground(palaceId: String): String = palaceBackgrounds[palaceId]?.path ?: "$BASE/palace/chuigongdian.webp"
    fun mapBackground(layer: MapLayerMode): String = when (layer) {
        MapLayerMode.MILITARY -> "$BASE/map/song_world_military.webp"
        MapLayerMode.ECONOMY -> "$BASE/map/song_world_parchment.webp"
        MapLayerMode.DIPLOMACY -> "$BASE/map/song_world_political.webp"
        MapLayerMode.TRADE -> "$BASE/map/song_world_trade.webp"
    }

    fun templatePortraitFor(seed: String, group: String = "song_military"): String {
        return when {
            group.contains("civil") || group.contains("official") -> "$BASE/characters/generic_scholar.webp"
            group.contains("jin") -> "$BASE/characters/jin_envoy.webp"
            group.contains("enemy") -> "$BASE/characters/generic_general.webp"
            group.contains("rebel") -> "$BASE/characters/generic_spy.webp"
            group.contains("neutral") -> "$BASE/characters/generic_merchant.webp"
            else -> "$BASE/characters/generic_general.webp"
        }
    }

    fun templateHalfbodyFor(seed: String, group: String = "song_military"): String = templatePortraitFor(seed, group)

    private fun story(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/characters/$file")
    private fun location(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/locations/$file")
    private fun character(
        id: String, name: String, faction: String,
        portraitFile: String, halfbodyFile: String = portraitFile
    ): CharacterArt = CharacterArt(
        id = id,
        name = name,
        portrait = "$BASE/characters/$portraitFile",
        halfbody = "$BASE/characters/$halfbodyFile",
        faction = faction
    )
    private fun city(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/city/$file")
    private fun event(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/events/$file")
    private fun palace(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/palace/$file")
    private fun ui(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/ui/$file")
    private fun icon(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/map/icons/$file")
    private fun named(id: String, name: String, path: String): NamedArt = NamedArt(id, name, path)
}
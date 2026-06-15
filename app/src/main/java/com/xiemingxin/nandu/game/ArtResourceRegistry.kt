package com.xiemingxin.nandu.game

/**
 * V2.2 美术资源注册表。
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
    }

    val historicalCharacters: Map<String, CharacterArt> = listOf(
        character("zhao_gou", "赵构", "song", "generic_scholar.webp"),
        character("yue_fei", "岳飞", "song", "yue_fei.webp"),
        character("han_shizhong", "韩世忠", "song", "han_shizhong.webp"),
        character("wu_jie", "吴玠", "song", "wu_jie.webp"),
        character("wu_lin", "吴璘", "song", "wu_lin.webp"),
        character("liu_qi", "刘锜", "song", "liu_qi.webp"),
        character("li_gang", "李纲", "song", "li_gang.webp"),
        character("zhao_ding", "赵鼎", "song", "zhao_ding.webp"),
        character("qin_hui", "秦桧", "song", "qin_hui.webp"),
        character("zhang_jun", "张浚", "song", "zhang_jun_chancellor.webp"),
        character("zhang_jun2", "张俊", "song", "zhang_jun_general.webp"),
        character("zong_ze", "宗泽", "song", "zong_ze.webp"),
        character("yang_yizhong", "杨沂中", "song", "yang_yizhong.webp"),
        character("lv_yihao", "吕颐浩", "song", "lv_yihao.webp"),
        character("wang_boyan", "汪伯彦", "song", "wang_boyan.webp"),
        character("huang_qianshan", "黄潜善", "song", "huang_qianshan.webp"),
        character("zhu_shengfei", "朱胜非", "song", "zhu_shengfei.webp"),
        character("hu_quan", "胡铨", "song", "hu_quan.webp"),
        character("empress", "皇后", "palace", "empress.webp"),
        character("dowager", "太后", "palace", "dowager.webp"),
        character("eunuch", "内侍押班", "palace", "eunuch_pressman.webp"),
        character("bureau_clerk", "皇城司勾当官", "bureau", "bureau_clerk.webp"),
        character("jin_envoy", "金使", "jin", "jin_envoy.webp"),
        character("xixia_envoy", "西夏使者", "xixia", "xixia_envoy.webp"),
        character("dali_envoy", "大理使者", "dali", "dali_envoy.webp"),
        character("goryeo_envoy", "高丽使者", "goryeo", "goryeo_envoy.webp"),
        character("sea_merchant", "海商首领", "sea_trade", "sea_merchant.webp"),
        character("wanyan_zongbi", "完颜宗弼", "jin", "jin_envoy.webp")
    ).associateBy { it.id }

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
        "default" to event("default", "通用事件", "event_yushufang_night.webp")
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

    val uiImages: Map<String, NamedArt> = mapOf(
        "dialog_frame" to ui("dialog_frame", "奏议框", "dialog_frame.webp"),
        "edict_bar" to ui("edict_bar", "圣旨落笔", "edict_bar.webp"),
        "danger_badge" to ui("danger_badge", "危急", "danger_badge.webp"),
        "faction_tag" to ui("faction_tag", "派系", "faction_tag.webp"),
        "choice_button" to ui("choice_button", "御前裁断", "choice_button.webp"),
        "npc_card_frame" to ui("npc_card_frame", "人物卡框", "npc_card_frame.webp"),
        "city_panel" to ui("city_panel", "城池面板", "city_panel.webp"),
        "map_panel" to ui("map_panel", "山河面板", "map_panel.webp")
    )

    fun portraitForOfficer(officerId: String): String = historicalCharacters[officerId]?.portrait ?: templatePortraitFor(officerId)
    fun halfbodyForOfficer(officerId: String): String = historicalCharacters[officerId]?.halfbody ?: templateHalfbodyFor(officerId)
    fun cityBackground(cityId: String): String = cityBackgrounds[cityId]?.path ?: Fallback.city
    fun eventImage(eventId: String): String = eventImages[eventId]?.path ?: Fallback.event
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

    private fun character(id: String, name: String, faction: String, file: String): CharacterArt = CharacterArt(
        id = id,
        name = name,
        portrait = "$BASE/characters/$file",
        halfbody = "$BASE/characters/$file",
        faction = faction
    )
    private fun city(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/city/$file")
    private fun event(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/events/$file")
    private fun palace(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/palace/$file")
    private fun ui(id: String, name: String, file: String): NamedArt = named(id, name, "$BASE/ui/$file")
    private fun named(id: String, name: String, path: String): NamedArt = NamedArt(id, name, path)
}
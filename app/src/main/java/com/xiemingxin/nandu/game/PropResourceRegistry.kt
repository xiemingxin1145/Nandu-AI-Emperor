package com.xiemingxin.nandu.game

/**
 * 道具美术注册表 V1。
 *
 * 这层只描述“物件是什么、应该画成什么、谁/什么事件会用到它”。
 * 真实游戏状态仍由 GameState / command 系统负责，避免把美术道具误当成可凭空产生的资源。
 *
 * 图片统一预留在 images/props/v1/ 下。首批图片可逐张补入而无需再改 UI 接线。
 */
enum class PropCategory(val label: String) {
    IMPERIAL("御用"),
    OFFICIAL("官署"),
    MILITARY("军务"),
    DOCUMENT("文书"),
    SCHOLAR("文房"),
    PERSONAL("随身"),
    WEAPON("兵器"),
    CEREMONIAL("礼制"),
    DAILY("日用")
}

data class PropArt(
    val id: String,
    val name: String,
    val category: PropCategory,
    val imagePath: String,
    val shortDescription: String,
    val artDirection: String
)

object PropResourceRegistry {
    private const val BASE = "images/props/v1"

    val all: Map<String, PropArt> = listOf(
        prop(
            "imperial_seal", "传国御玺", PropCategory.IMPERIAL,
            "玉质御玺与盘龙钮，厚重温润，不出现现代印章结构",
            "南宋宫廷玉玺，青白玉，盘龙钮，边角有自然旧痕，庄严而克制"
        ),
        prop(
            "imperial_edict", "黄绫诏书", PropCategory.IMPERIAL,
            "卷起或半展开的御旨，用于下诏、任免与军国大事",
            "南宋黄绫诏书，轴头、织纹与朱印清晰，不画可辨识现代字体"
        ),
        prop(
            "golden_tablet", "金字牌", PropCategory.IMPERIAL,
            "驿传急诏所用金字牌，适合召还、催军等高压事件",
            "南宋驿传金字牌，金属与漆木结合，旧化真实，避免夸张玄幻造型"
        ),
        prop(
            "tiger_tally", "虎符", PropCategory.MILITARY,
            "调兵验符的军权象征，不直接等同于实际兵力资源",
            "宋代铜制虎符，左右合符结构，铜锈与磨痕真实，沉稳军用品质"
        ),
        prop(
            "official_seal", "官印", PropCategory.OFFICIAL,
            "官署权力与任命身份象征，可用于宰执、地方官与军府",
            "宋代铜官印，方印、钮柄、印泥残痕，桌面陈设视角"
        ),
        prop(
            "memorial", "奏章", PropCategory.DOCUMENT,
            "朝臣公开上奏所用文书，可与朝议、弹劾、任免事件联动",
            "宋代纸本奏章，折叠文书与封套，墨迹只做不可辨识纹理"
        ),
        prop(
            "secret_memorial", "密奏", PropCategory.DOCUMENT,
            "密封急递的私密奏报，适合皇城司、密折与权臣暗线",
            "封缄密奏，蜡/泥封与细绳，纸张旧化，强调保密与紧迫感"
        ),
        prop(
            "military_report", "军报", PropCategory.MILITARY,
            "前线战报、塘报与急递军情的统一视觉物件",
            "宋代前线军报，卷折纸、急递封签、泥点与磨损，真实军旅感"
        ),
        prop(
            "campaign_map", "军机舆图", PropCategory.MILITARY,
            "用于战略讨论的地图，不替代游戏内真实地图与情报系统",
            "宋代纸绢军用舆图，河流山川、城寨符号但无可读现代文字，桌面俯视"
        ),
        prop(
            "military_manual", "兵书", PropCategory.SCHOLAR,
            "将领研判战法、军制与守城经验的象征物",
            "线装/卷册兵书，宋代纸本与函套，磨损边角，禁现代印刷字体"
        ),
        prop(
            "writing_set", "文房四宝", PropCategory.SCHOLAR,
            "笔墨纸砚组合，用于御书房、政事堂与文臣人物页",
            "宋式笔墨纸砚成组静物，砚台、墨锭、毛笔、素纸，雅致克制"
        ),
        prop(
            "vermilion_paste", "朱印泥", PropCategory.OFFICIAL,
            "御批、钤印时使用的朱色印泥盒",
            "宋式漆盒印泥，朱砂质感，盒盖微开，宫廷案头静物"
        ),
        prop(
            "jade_pendant", "玉佩", PropCategory.PERSONAL,
            "人物随身身份、旧交或赏赐线索的可视化载体",
            "宋代青白玉佩，丝绦与结穗，温润通透，避免现代首饰设计"
        ),
        prop(
            "officer_sword", "佩剑", PropCategory.WEAPON,
            "武将身份与临阵气质的随身兵器，不用于直接替代战斗数值",
            "宋军高级将领佩剑，鞘装完整，金属不过度发亮，实战旧痕"
        ),
        prop(
            "long_spear", "长枪", PropCategory.WEAPON,
            "军中常用长兵，适合作为韩世忠等武将象征物",
            "宋军长枪局部静物，铁枪头、木杆、缨穗，真实军械比例"
        ),
        prop(
            "tea_bowl", "茶盏", PropCategory.DAILY,
            "朝堂间歇、私谈与后苑场景的日常物件",
            "南宋建盏/青瓷茶盏，釉色含蓄，案几静物，蒸汽轻微"
        ),
        prop(
            "incense_burner", "香炉", PropCategory.CEREMONIAL,
            "宫廷、御书房与祭礼场景的氛围物件",
            "宋代铜香炉，简洁器型，烟气轻薄，避免明清繁复纹饰"
        ),
        prop(
            "dispatch_box", "急递匣", PropCategory.DOCUMENT,
            "盛放急报、密奏或诏令的封装器具",
            "宋代漆木急递匣，铜扣、绳封、运输磨损，官府实用品质"
        )
    ).associateBy { it.id }

    /**
     * 核心人物的“象征性持有物”。
     * 这不是动态背包，只用于人物视觉识别；真正获得/失去物件仍应走后续 GameState inventory。
     */
    private val signatureByOfficer: Map<String, List<String>> = mapOf(
        "zhao_gou" to listOf("imperial_seal", "vermilion_paste", "imperial_edict"),
        "yue_fei" to listOf("tiger_tally", "campaign_map", "officer_sword"),
        "han_shizhong" to listOf("tiger_tally", "long_spear", "military_report"),
        "liu_qi" to listOf("campaign_map", "tiger_tally", "military_report"),
        "wu_jie" to listOf("military_manual", "campaign_map", "tiger_tally"),
        "zhao_ding" to listOf("official_seal", "memorial", "writing_set"),
        "qin_hui" to listOf("official_seal", "secret_memorial", "writing_set"),
        "wanyan_zongbi" to listOf("campaign_map", "officer_sword", "military_report")
    )

    fun byId(id: String): PropArt? = all[id]

    fun catalog(): List<PropArt> = all.values.toList()

    fun catalogByCategory(): Map<PropCategory, List<PropArt>> =
        catalog().groupBy { it.category }

    fun imageFallbackPath(): String = ArtResourceRegistry.Fallback.ui

    fun signaturePropsForOfficer(officerId: String): List<PropArt> =
        signatureByOfficer[officerId].orEmpty().mapNotNull(all::get)

    /**
     * 从事件语义选择最多三件可视物件。
     * 只负责展示线索，不修改资源、不授予兵权、不改人物背包。
     */
    fun propsForEvent(
        eventId: String,
        type: String,
        title: String,
        description: String,
        artHint: String
    ): List<PropArt> {
        val text = listOf(eventId, type, title, description, artHint).joinToString(" ")
        val ids = linkedSetOf<String>()

        fun add(vararg values: String) {
            values.forEach { if (it in all) ids += it }
        }

        when {
            containsAny(text, "十二道金牌", "金字牌", "金牌召回", "班师诏") ->
                add("golden_tablet", "imperial_edict", "dispatch_box")

            containsAny(text, "下诏", "诏书", "圣旨", "御旨", "任命", "册封") ->
                add("imperial_edict", "imperial_seal", "vermilion_paste")

            containsAny(text, "密奏", "密报", "密折", "皇城司", "暗线", "谍报") ->
                add("secret_memorial", "dispatch_box", "official_seal")

            containsAny(text, "出征", "请战", "迎战", "压境", "攻城", "守城", "前线", "军议", "大战") ||
                type in setOf("jin_event", "random_military", "city_crisis") ->
                add("tiger_tally", "campaign_map", "military_report")

            containsAny(text, "朝议", "弹劾", "上奏", "奏章", "政事堂") || type == "random_court" ->
                add("memorial", "official_seal", "writing_set")

            containsAny(text, "御书房", "御批", "批红") ->
                add("writing_set", "vermilion_paste", "memorial")

            containsAny(text, "太庙", "祭", "誓师") ->
                add("incense_burner", "imperial_edict", "tiger_tally")
        }

        if (containsAny(text, "岳飞") && containsAny(text, "召回", "班师", "金牌")) {
            add("golden_tablet")
        }
        if (containsAny(text, "茶", "私谈", "后苑")) add("tea_bowl")
        if (containsAny(text, "玉佩", "信物", "旧物")) add("jade_pendant")

        return ids.take(3).mapNotNull(all::get)
    }

    /** 给美术批处理/测试使用，确保每个预留路径稳定。 */
    fun expectedAssetPaths(): Set<String> = all.values.map { it.imagePath }.toSet()

    private fun prop(
        id: String,
        name: String,
        category: PropCategory,
        shortDescription: String,
        artDirection: String
    ): PropArt = PropArt(
        id = id,
        name = name,
        category = category,
        imagePath = "$BASE/prop_${id}.webp",
        shortDescription = shortDescription,
        artDirection = artDirection
    )

    private fun containsAny(text: String, vararg keywords: String): Boolean =
        keywords.any(text::contains)
}

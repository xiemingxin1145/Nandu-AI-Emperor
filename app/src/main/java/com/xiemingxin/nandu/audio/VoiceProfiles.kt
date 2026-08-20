package com.xiemingxin.nandu.audio

/**
 * 人物语音配置（V1.0）
 * 为每个历史人物定义语音档案和多场景台词池
 * 点击头像时随机播放符合性格的短话，避免所有NPC重复同一句
 */
data class VoiceProfile(
    val characterId: String,
    val characterName: String,
    val voiceId: String,
    val voiceType: VoiceType,
    val basePitch: Float = 1.0f,
    val baseSpeed: Float = 1.0f,
    val greetingLines: List<String> = emptyList(),
    val courtLines: List<String> = emptyList(),
    val battleLines: List<String> = emptyList(),
    val angryLines: List<String> = emptyList(),
    val loyalLines: List<String> = emptyList(),
    val privateLines: List<String> = emptyList(),
    val audioSamples: Map<VoiceLineType, String> = emptyMap() // 预生成的音频文件路径
)

enum class VoiceType {
    YOUNG_GENERAL,      // 年轻武将（如岳飞）
    MATURE_GENERAL,     // 成熟武将（如韩世忠、刘锜）
    SCHOLAR_OFFICIAL,   // 文臣（如赵鼎、李纲）
    CRAFTY_MINISTER,    // 奸臣（如秦桧）
    EMPEROR,            // 皇帝（赵构）
    ELDER_STATESMAN,    // 老臣（如宗泽、李纲）
    JIN_GENERAL         // 金国将领
}

enum class VoiceLineType {
    GREETING, COURT, BATTLE, ANGRY, LOYAL, PRIVATE
}

/**
 * 宋代称谓规范：
 * - 官员对皇帝："臣等拜见官家。" "臣有本奏。" "臣请奏一事。"
 * - 禁止："吾皇万岁万岁万万岁"（明清才有的夸张三呼）
 */
object VoiceProfiles {

    val yueFei = VoiceProfile(
        characterId = "yue_fei",
        characterName = "岳飞",
        voiceId = "voice_yue_fei",
        voiceType = VoiceType.YOUNG_GENERAL,
        basePitch = 1.05f,
        baseSpeed = 0.95f,
        greetingLines = listOf(
            "臣岳飞，拜见官家。",
            "末将在。官家有何差遣？",
            "岳飞恭候圣谕。"
        ),
        courtLines = listOf(
            "臣以为，当趁金军立足未稳，主动出击。",
            "文臣不爱钱，武臣不惜死，天下太平矣。",
            "臣愿领兵北上，收复中原。"
        ),
        battleLines = listOf(
            "将士们！随我杀贼！",
            "直捣黄龙，与诸君痛饮！",
            "金兵虽众，不足为惧！"
        ),
        angryLines = listOf(
            "此等和议，臣万难从命！",
            "奸臣误国！臣痛心疾首！",
            "十年之功，废于一旦！"
        ),
        loyalLines = listOf(
            "臣生是宋臣，死是宋鬼。",
            "精忠报国，臣不敢忘。",
            "官家但有差遣，臣万死不辞。"
        ),
        privateLines = listOf(
            "老母刺字，臣不敢忘。",
            "岳云这孩子，太急躁了。",
            "何时才能收复汴京啊……"
        ),
        audioSamples = mapOf(
            VoiceLineType.GREETING to "audio/voice/characters/yue_fei_greeting_01.wav",
            VoiceLineType.LOYAL to "audio/voice/characters/yue_fei_loyal_01.wav"
        )
    )

    val qinHui = VoiceProfile(
        characterId = "qin_hui",
        characterName = "秦桧",
        voiceId = "voice_qin_hui",
        voiceType = VoiceType.CRAFTY_MINISTER,
        basePitch = 0.92f,
        baseSpeed = 1.05f,
        greetingLines = listOf(
            "臣秦桧，拜见官家。",
            "老臣在。",
            "臣恭候圣驾。"
        ),
        courtLines = listOf(
            "臣以为，当以和为贵，休养生息。",
            "兵者凶器，圣人不得已而用之。",
            "臣请官家三思，开战易，收场难。"
        ),
        battleLines = listOf(
            "战事一起，百姓遭殃啊。",
            "臣不懂兵事，但知民力已疲。"
        ),
        angryLines = listOf(
            "岳飞拥兵自重，官家不可不防！",
            "此等武将，眼里还有朝廷吗？",
            "和议大局，岂容一介武夫破坏！"
        ),
        loyalLines = listOf(
            "臣一切都是为官家着想。",
            "臣的忠心，天日可鉴。",
            "臣所做的一切，都是为了官家的江山。"
        ),
        privateLines = listOf(
            "金国那边，应该有回信了吧。",
            "岳飞不死，和议难成。",
            "这步棋，不能走错。"
        ),
        audioSamples = mapOf(
            VoiceLineType.GREETING to "audio/voice/characters/qin_hui_greeting_01.wav",
            VoiceLineType.COURT to "audio/voice/characters/qin_hui_court_01.wav"
        )
    )

    val hanShizhong = VoiceProfile(
        characterId = "han_shizhong",
        characterName = "韩世忠",
        voiceId = "voice_han_shizhong",
        voiceType = VoiceType.MATURE_GENERAL,
        basePitch = 0.98f,
        baseSpeed = 0.98f,
        greetingLines = listOf(
            "末将韩世忠，拜见官家。",
            "老韩在。",
            "韩世忠候命。"
        ),
        courtLines = listOf(
            "臣以为，和议可以谈，但不能不设防。",
            "臣的兵马，随时可以出征。",
            "官家，金兵不可信啊。"
        ),
        battleLines = listOf(
            "孩儿们！跟老子冲！",
            "黄天荡一战，叫金兵知道我大宋的厉害！",
            "梁红玉，擂鼓！"
        ),
        angryLines = listOf(
            "岳飞犯了什么罪？！莫须有三字，何以服天下？！",
            "朝廷这是要寒了将士们的心啊！",
            "老子不服！"
        ),
        loyalLines = listOf(
            "臣这条命，是官家的。",
            "只要官家一声令下，臣赴汤蹈火。",
            "臣忠的是大宋，不是某个人。"
        ),
        privateLines = listOf(
            "红玉，你说这朝廷，还能撑多久？",
            "岳飞那小子，太刚了。",
            "老子打了一辈子仗，图个啥？"
        ),
        audioSamples = mapOf(
            VoiceLineType.GREETING to "audio/voice/characters/han_shizhong_greeting_01.wav"
        )
    )

    val liuQi = VoiceProfile(
        characterId = "liu_qi",
        characterName = "刘锜",
        voiceId = "voice_liu_qi",
        voiceType = VoiceType.MATURE_GENERAL,
        basePitch = 1.0f,
        baseSpeed = 1.0f,
        greetingLines = listOf(
            "臣刘锜，拜见官家。",
            "末将在。",
            "刘锜恭候圣谕。"
        ),
        courtLines = listOf(
            "顺昌虽小，臣必死守。",
            "臣已凿船沉舟，示无退意。",
            "八字军皆愿死战。"
        ),
        battleLines = listOf(
            "城在人在，城亡人亡！",
            "将士们！今日有死无生！",
            "金兵虽众，我八字军何惧！"
        ),
        angryLines = listOf(
            "援军迟迟不至，臣不解！",
            "朝廷难道要弃顺昌于不顾？",
            "臣死不足惜，惜的是大宋江山！"
        ),
        loyalLines = listOf(
            "臣誓与顺昌共存亡。",
            "官家信任，臣以死报之。",
            "臣生为宋将，死为宋鬼。"
        ),
        privateLines = listOf(
            "这顺昌城，能守多久？",
            "将士们的家眷，还在后方。",
            "但愿朝廷能派援军来。"
        ),
        audioSamples = mapOf(
            VoiceLineType.GREETING to "audio/voice/characters/liu_qi_greeting_01.wav"
        )
    )

    val zhaoGou = VoiceProfile(
        characterId = "zhao_gou",
        characterName = "赵构",
        voiceId = "voice_zhao_gou",
        voiceType = VoiceType.EMPEROR,
        basePitch = 1.02f,
        baseSpeed = 0.97f,
        greetingLines = listOf(
            "平身。",
            "卿来了。",
            "赐座。"
        ),
        courtLines = listOf(
            "众卿平身。今日有何奏报？",
            "此事……容朕三思。",
            "传朕旨意。"
        ),
        battleLines = listOf(
            "朕意已决，开战。",
            "诸将努力，朕在临安等捷报。"
        ),
        angryLines = listOf(
            "放肆！",
            "朕的旨意，谁敢不从？",
            "退下！"
        ),
        loyalLines = listOf(
            "朕的江山，朕自己守。",
            "太祖创业不易，朕不敢失。"
        ),
        privateLines = listOf(
            "这皇帝，当得真累。",
            "父皇和皇兄，在北国还好吗？",
            "朕……真的能收复中原吗？"
        ),
        audioSamples = mapOf(
            VoiceLineType.GREETING to "audio/voice/characters/zhao_gou_greeting_01.wav"
        )
    )

    val all = mapOf(
        "yue_fei" to yueFei,
        "qin_hui" to qinHui,
        "han_shizhong" to hanShizhong,
        "liu_qi" to liuQi,
        "zhao_gou" to zhaoGou
    )

    fun byId(id: String): VoiceProfile? = all[id]

    /** 随机获取一条指定类型的台词 */
    fun randomLine(characterId: String, type: VoiceLineType): String? {
        val profile = byId(characterId) ?: return null
        val lines = when (type) {
            VoiceLineType.GREETING -> profile.greetingLines
            VoiceLineType.COURT -> profile.courtLines
            VoiceLineType.BATTLE -> profile.battleLines
            VoiceLineType.ANGRY -> profile.angryLines
            VoiceLineType.LOYAL -> profile.loyalLines
            VoiceLineType.PRIVATE -> profile.privateLines
        }
        return if (lines.isEmpty()) null else lines.random()
    }
}

/**
 * 朝会语音框架（V1.0）
 * 垂拱殿升朝时的基础语音：衣袍摩擦/脚步/殿内混响/很轻的礼乐/开朝提示
 * 宋代称谓规范
 */
object CourtVoiceFramework {

    /** 升朝流程音效序列 */
    data class CourtOpeningSequence(
        val steps: List<CourtSoundStep>
    )

    data class CourtSoundStep(
        val soundPath: String,
        val delayMs: Long,
        val volume: Float,
        val description: String
    )

    /** 默认升朝序列：脚步→衣袍→礼乐→开朝提示→百官朝拜 */
    val defaultOpening = CourtOpeningSequence(
        steps = listOf(
            CourtSoundStep("audio/sfx/sfx_page_turn.ogg", 0, 0.3f, "殿门开启"),
            CourtSoundStep("audio/ambience/amb_palace_murmur.ogg", 200, 0.4f, "百官低语"),
            CourtSoundStep("audio/sfx/sfx_gong.ogg", 800, 0.5f, "鸣钟开朝"),
            CourtSoundStep("audio/bgm/bgm_ritual.ogg", 1200, 0.3f, "礼乐轻奏"),
            CourtSoundStep("audio/sfx/sfx_court_murmur.ogg", 2000, 0.4f, "百官整肃")
        )
    )

    /** 宋代百官朝拜台词（随机选一句） */
    val courtGreetings = listOf(
        "臣等拜见官家。",
        "臣等恭请圣安。",
        "百官朝见，吾皇圣躬安否？",
        "臣等恭候圣驾。"
    )

    /** 官员出列奏事台词 */
    val courtRequestLines = listOf(
        "臣有本奏。",
        "臣请奏一事。",
        "臣有本，冒死上陈。",
        "臣启奏官家。"
    )

    /** 退朝台词 */
    val courtDismissLines = listOf(
        "退朝。",
        "众卿退下。",
        "今日朝议到此。"
    )

    /** 随机获取一句百官朝拜台词 */
    fun randomGreeting(): String = courtGreetings.random()

    /** 随机获取一句奏事台词 */
    fun randomRequest(): String = courtRequestLines.random()
}

/**
 * 历史时间线原则（V1.0）
 * 玩家出现前已发生的历史→已成事实（不可改变）
 * 玩家附近正在发生的→可干预
 * 更远未来史实→只是历史压力和潜在事件，不保证必然发生
 * 游戏遵循"历史惯性+玩家蝴蝶效应"
 */
object HistoricalTimeline {

    enum class EventStatus {
        FIXED_HISTORY,      // 已成事实，玩家不可改变
        INTERVENABLE,       // 正在发生，玩家可干预
        POTENTIAL_FUTURE,   // 潜在未来，可能发生也可能不发生
        ALREADY_CHANGED     // 已被玩家改变
    }

    data class HistoricalEvent(
        val id: String,
        val name: String,
        val year: Int,
        val status: EventStatus,
        val description: String,
        val inertia: Float = 0.5f, // 历史惯性 0-1，越高越难改变
        val butterflyThreshold: Float = 0.3f // 蝴蝶效应阈值，玩家影响超过此值可改变
    )

    /** 靖康之变前的固定历史（玩家不可改变） */
    val fixedHistory = listOf(
        HistoricalEvent("jingkang_incident", "靖康之变", 1127, EventStatus.FIXED_HISTORY, "汴京陷落，徽钦二帝被掳北去", inertia = 1.0f),
        HistoricalEvent("emperor_huiqin_captured", "二帝北狩", 1127, EventStatus.FIXED_HISTORY, "宋徽宗、宋钦宗被金军掳往北方", inertia = 1.0f),
        HistoricalEvent("northern_territory_lost", "北方沦陷", 1127, EventStatus.FIXED_HISTORY, "黄河以北大片领土被金国占领", inertia = 1.0f)
    )

    /** 玩家可干预的事件（建炎年间） */
    val intervenableEvents = listOf(
        HistoricalEvent("shunchang_battle", "顺昌之战", 1140, EventStatus.INTERVENABLE, "刘锜率八字军死守顺昌", inertia = 0.4f),
        HistoricalEvent("yancheng_battle", "郾城大捷", 1140, EventStatus.INTERVENABLE, "岳飞郾城大败金军铁浮图", inertia = 0.35f),
        HistoricalEvent("yue_fei_recall", "岳飞被召回", 1140, EventStatus.INTERVENABLE, "十二道金牌召岳飞班师", inertia = 0.6f),
        HistoricalEvent("shaoxing_peace", "绍兴和议", 1141, EventStatus.INTERVENABLE, "宋金和议，称臣纳贡", inertia = 0.7f),
        HistoricalEvent("yue_fei_execution", "岳飞被害", 1142, EventStatus.INTERVENABLE, "岳飞以莫须有罪名被害于风波亭", inertia = 0.65f)
    )

    /** 潜在未来事件（可能发生也可能不发生） */
    val potentialFuture = listOf(
        HistoricalEvent("caishi_battle", "采石之战", 1161, EventStatus.POTENTIAL_FUTURE, "虞允文采石大败完颜亮", inertia = 0.3f),
        HistoricalEvent("longxing_northern_expedition", "隆兴北伐", 1163, EventStatus.POTENTIAL_FUTURE, "宋孝宗隆兴北伐失败", inertia = 0.4f),
        HistoricalEvent("kaifu_northern_expedition", "开禧北伐", 1206, EventStatus.POTENTIAL_FUTURE, "韩侂胄开禧北伐失败", inertia = 0.45f),
        HistoricalEvent("jiaxing_peace", "嘉定和议", 1208, EventStatus.POTENTIAL_FUTURE, "宋金嘉定和议", inertia = 0.5f),
        HistoricalEvent("mongol_rise", "蒙古崛起", 1206, EventStatus.POTENTIAL_FUTURE, "铁木真统一蒙古，建立大蒙古国", inertia = 0.6f),
        HistoricalEvent("jin_destruction", "金国灭亡", 1234, EventStatus.POTENTIAL_FUTURE, "宋蒙联军灭金", inertia = 0.55f),
        HistoricalEvent("yashan_battle", "崖山海战", 1279, EventStatus.POTENTIAL_FUTURE, "南宋灭亡，陆秀夫背帝昺投海", inertia = 0.7f)
    )

    /**
     * 计算玩家对事件的影响是否足以改变历史
     * @param eventInertia 事件历史惯性 0-1
     * @param playerInfluence 玩家影响力 0-1
     * @return true=可以改变，false=历史惯性太强，无法改变
     */
    fun canChangeHistory(eventInertia: Float, playerInfluence: Float): Boolean {
        return playerInfluence > eventInertia * 0.6f
    }

    /** 获取所有事件 */
    fun allEvents(): List<HistoricalEvent> = fixedHistory + intervenableEvents + potentialFuture
}

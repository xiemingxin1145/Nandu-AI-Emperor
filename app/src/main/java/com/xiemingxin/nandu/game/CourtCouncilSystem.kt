package com.xiemingxin.nandu.game

/**
 * 大殿朝会 / 宫廷事件。
 *
 * 所有“谁能肉身说话”最终都经过 CharacterAppearanceSystem.filterCouncilLines；人物状态与
 * 真实位置是最终出席权威。STAB-004 同时要求当前 UI choice id 与后果系统保持一致，
 * 避免按钮看似可点却只落到通用占位结果。
 */
data class CouncilLine(
    val speakerId: String,
    val speakerName: String,
    val role: String,
    val attitude: String,
    val text: String
)

data class CouncilChoice(
    val id: String,
    val label: String,
    val edictDraft: String,
    val preview: String
)

data class CouncilScene(
    val id: String,
    val palaceId: String,
    val title: String,
    val summary: String,
    val lines: List<CouncilLine>,
    val choices: List<CouncilChoice>
)

object CourtCouncilSystem {

    fun sceneForTask(state: GameState, task: PalaceTask): CouncilScene {
        val raw = when (task.palaceId) {
            PalaceIds.CHUIGONG -> courtScene(state, task)
            PalaceIds.SHUMI -> militaryScene(state, task)
            PalaceIds.ZHENGSHI -> fiscalScene(state, task)
            PalaceIds.WENDE -> talentScene(state, task)
            PalaceIds.YUSHU -> secretMemorialScene(state, task)
            PalaceIds.HUANGCHENG -> bureauScene(state, task)
            PalaceIds.HOUYUAN -> innerPalaceScene(state, task)
            PalaceIds.TAIMIAO -> ritualScene(state, task)
            else -> genericScene(state, task)
        }
        return raw.copy(lines = CharacterAppearanceSystem.filterCouncilLines(state, raw.palaceId, raw.lines))
    }

    private fun courtScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "${PalaceRegistry.byId(PalaceIds.CHUIGONG).name}朝议",
        summary = "行在草创，守中原还是避锋南幸，正是建炎初政最尖锐的分歧。",
        lines = listOf(
            line(state, "li_gang", "右相", "support", "臣以为国家新造，尤不可先自弃中原。边备、钱粮、招抚三事，当并力而行。"),
            line(state, "zong_ze", "入对老臣", "support", "东京虽残，人心未尽失。若朝廷示以恢复之志，两河义士尚可招集。"),
            line(state, "huang_qianshan", "执政文臣", "concerned", "金兵锋锐，行在根基未固。臣请先保乘舆与宗社，再图后举。"),
            line(state, "wang_boyan", "藩邸旧臣", "concerned", "敌骑去来无常，若久驻近河之地，恐仓卒再震。南幸之议不可尽废。")
        ),
        choices = listOf(
            choice("war", "经营中原", "传朕旨意：应天行在暂不轻徙。李纲会诸司整边备、钱粮与招抚，凡两河军民愿守者，朝廷皆为之援。", "主战与中原经营路线加强，行在安全压力上升。"),
            choice("balance", "守备兼筹退路", "传朕旨意：行在先修守备、清点钱粮，各路军民毋得自弃；同时勘验东南道路，以备非常。", "朝局较稳，但主战主和双方都不会完全满意。"),
            choice("peace", "准备南幸", "传朕旨意：有司暗备南行舟车钱粮，前线诸军仍守其地，不得因此先自溃散。", "行在风险下降，中原军民信心受压。")
        )
    )

    private fun militaryScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "枢密院军议",
        summary = "军报入院，在京武臣与奉召将领只按真实状态参与。",
        lines = listOf(
            line(state, "han_shizhong", "御营武臣", "support", "臣请先整宿卫与机动兵马。金骑若逼行在，须有一支能战之军护驾而不乱。"),
            line(state, "li_gang", "右相", "concerned", "将可用，亦须先明粮道与守土之责。诸军不可只知趋避，不知所守。"),
            line(state, "yue_fei", "新进武臣", "support", "臣愿受军令整伍练卒，先求兵可用，再言进取。")
        ),
        choices = listOf(
            choice("defend", "整备行在与前线", "传朕旨意：枢密院核诸路兵马、粮道与守土责任，先补最危处，不得虚报兵额。", "防务更加可靠，耗费钱粮。"),
            choice("train", "整军练兵", "传朕旨意：御营及诸路军各整部伍，旬报训练与粮储，滥兵冒饷者严查。", "军心与可战度提升，财政压力增加。"),
            choice("scout", "先探敌势", "传朕旨意：遣轻骑、土豪熟户分路探敌，枢密院汇成军报，未明敌势不得浪战。", "获得后续情报，短期收益较低。")
        )
    )

    private fun fiscalScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = if (task.source == TaskSource.TRADE) "政事堂商税议" else "政事堂钱粮议",
        summary = if (task.source == TaskSource.TRADE) {
            "外贸、港市与商税必须对应当前贸易路线和外交状态，不能只显示一张商税卡。"
        } else {
            "行在要养兵、安民、维持百司，钱粮选择会直接改变府库与民心。"
        },
        lines = listOf(
            line(state, "li_gang", "右相", "support", "兵食皆出于民。今日之急，不在巧取，而在使转输不断、军民各得其食。"),
            line(state, "huang_qianshan", "执政文臣", "concerned", "行在百司初聚，用度纷繁。若无定额，未待敌至，府库先空。"),
            line(state, "wang_boyan", "执政文臣", "neutral", "东南财赋可为后援，但道路未定，不可把远方钱粮当作眼前已有。")
        ),
        choices = if (task.source == TaskSource.TRADE) {
            listOf(
                choice("trade", "整顿市舶互市", "传朕旨意：核泉州、明州、广州诸港市舶与互市，许其通商，但税额、缉私与外商约束一并具奏。", "真实调整海贸路线、港市商业与外贸关系。"),
                choice("audit", "先清查商税", "传朕旨意：先核市舶账册与港市税额，虚冒侵渔者具名奏闻。", "先整秩序，再求增收。"),
                choice("light_tax", "减扰商民", "传朕旨意：商税须有定额，不许州县借市舶横加科敛。", "民心与商路风险改善，短期收入承压。")
            )
        } else {
            listOf(
                choice("grain", "先保军民口粮", "传朕旨意：诸路转运先核军粮民食，行在百司裁冗费，急处先给，不许层层侵耗。", "粮草压力缓解，其他工程放慢。"),
                choice("audit", "清查行在用度", "传朕旨意：有司逐项核行在钱粮支出，军费、赈济不得混账，虚冒者具名奏闻。", "财政秩序改善，官僚阻力上升。"),
                choice("light_tax", "安民轻敛", "传朕旨意：国用虽急，不得借军兴横征。州县先减浮费扰役，再议增收。", "民心提升，短期财政更紧。")
            )
        }
    )

    private fun talentScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "文班公署访才议",
        summary = "人才先有线索、再召见考校；后世名将名臣不会因为玩家认识名字就自动出现。",
        lines = listOf(
            line(state, "li_gang", "右相", "support", "国难用人，当求实才。然未见其人、未验其能，亦不可只凭传闻骤授重任。"),
            line(state, "huang_qianshan", "执政文臣", "concerned", "破格用人可以救急，但名器不可太滥。宜令有司先核履历与实绩。"),
            line(state, "wang_boyan", "执政文臣", "neutral", "地方荐书多有请托，召见之前最好再遣一层查访。")
        ),
        choices = listOf(
            choice("summon", "召见考校", "传朕旨意：已得线索之人才，先核身份与实绩；可用者召至行在考校，再议差遣。", "稳妥推进人才线。"),
            choice("field", "就地试用", "传朕旨意：边地急缺之才，可先令本路长官给以差遣，立功后再奏名除授。", "人才起用更快，但中央掌握更弱。"),
            choice("delay", "留档再察", "传朕旨意：诸路荐才先入册，证据不明者继续查访，不得只凭声名授官。", "风险低，但可能错失时机。")
        )
    )

    private fun secretMemorialScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = if (task.source == TaskSource.DIPLOMACY) "御前便阁外交密议" else "御前便阁密折",
        summary = "密折不可尽信，亦不可不察；能入此处的人仍受真实位置约束。",
        lines = listOf(
            line(state, "li_gang", "右相", "concerned", "军国密奏最忌一闻即信。臣请分来源、日期、互证三层，再定处置。"),
            line(state, "wang_boyan", "执政文臣", "neutral", "外邦往来，辞气与礼数都须谨慎。国势未稳，尤其不可轻许不可践之言。")
        ),
        choices = listOf(
            choice("envoy", "遣使探问", "传朕旨意：择谨慎通敏之臣出使探问，先明彼意，不得擅许盟约重利。", "开启外交线索。"),
            choice("verify", "先核密折", "传朕旨意：近日密折分真假缓急，涉边防钱粮者至少两路互证再奏。", "风险较低，推进较慢。"),
            choice("shelve", "留中不发", "传朕旨意：此事留中，毋令外廷无据妄议。", "暂稳朝局，但可能错过时机。")
        )
    )

    private fun bureauScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "皇城司密奏",
        summary = "耳目可用，不可让谍报系统替皇帝自动判忠奸。",
        lines = listOf(
            CouncilLine("bureau_clerk", "皇城司勾当官", "密奏官", "concerned", "臣等只据耳目所闻，不敢妄断忠奸。请官家裁其缓急。")
        ),
        choices = listOf(
            choice("observe", "暗中留意", "传朕旨意：皇城司谨慎核察往来，不得妄兴风波，凡有实据再具密奏。", "稳妥，风险低。"),
            choice("warn", "召见问对", "传朕旨意：有实据牵涉者，先召本人问对，不许以流言即成罪名。", "震慑较强，也可能引发戒心。"),
            choice("ignore", "无据不究", "传朕旨意：无实据者不得上纲，皇城司退下再查。", "朝臣安心，隐患可能累积。")
        )
    )

    private fun innerPalaceScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "行在内廷问对",
        summary = task.description,
        lines = listOf(
            CouncilLine("empress", "皇后", "中宫", "support", "官家日夜忧勤，外朝诸事固重，亦当保养圣躬，使内外安心。"),
            CouncilLine("dowager", "太后", "宫中尊长", "concerned", "祖宗社稷在上，行在新立，内廷尤当节俭，不可使外朝有浮言。"),
            CouncilLine("eunuch", "内侍押班", "内侍", "neutral", "宫中只据眼前实情奏闻，不敢以固定时日无事生事。")
        ),
        choices = listOf(
            choice("frugal", "裁减内廷用度", "传朕旨意：内廷诸费从简，所省钱粮拨入军储，毋扰外朝。", "国库略稳，内廷满意下降。"),
            choice("comfort", "安抚宫中", "传朕旨意：宫中诸人各安其职，不得因军报自乱，更不得干预外朝。", "内廷稳定，财政影响小。"),
            choice("queen_advice", "听取内廷近事", "传朕旨意：宫中近事可密陈，凡涉外朝者由朕亲裁。", "开启内廷建议线。")
        )
    )

    private fun ritualScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "行在礼制议",
        summary = "建炎草创期先处理正统、祭告与军民信心，不把后来临安固定宫庙设施提前搬来。",
        lines = listOf(
            line(state, "li_gang", "右相", "support", "国难之时，礼所以明名分、聚人心，不在铺张器用。"),
            line(state, "zong_ze", "老臣", "support", "若告军民，当明朝廷不忘故土。中原父老所望，也正在此。"),
            line(state, "huang_qianshan", "执政文臣", "concerned", "礼不可废，费用却当从简。行在百务方兴，不宜再重困民力。")
        ),
        choices = listOf(
            choice("simple_rite", "从简告祭", "传朕旨意：礼官议从简告祭之仪，以安军民；所费不得侵军粮民食。", "名望小升，耗费低。"),
            choice("war_oath", "申明恢复之志", "传朕旨意：告军民与诸路将士，朝廷不忘中原故土；但军令仍以实情进退，不许躁进邀功。", "军心提升，中原期待也随之提高。"),
            choice("delay", "缓办大礼", "传朕旨意：大礼暂缓，小节不废，待钱粮与行在稍定再议。", "节省资源，名望无明显提升。")
        )
    )

    private fun genericScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = task.title,
        summary = task.description,
        lines = listOf(
            line(state, "li_gang", "右相", "concerned", "此事关军国，请官家先命有司核实，再定轻重。"),
            line(state, "huang_qianshan", "执政文臣", "neutral", "国势未稳，臣请兼顾眼前安危，不可只图一端。")
        ),
        choices = listOf(
            choice("draft", "依奏拟旨", task.edictDraft.ifBlank { "传朕旨意：此事交有司详议，限期具奏。" }, "进入圣旨处理。"),
            choice("hold", "留中再议", "传朕旨意：此事留中，俟诸司详报再断。", "暂缓处理。")
        )
    )

    private fun line(state: GameState, officerId: String, role: String, attitude: String, fallbackText: String): CouncilLine {
        val officer = state.officers.firstOrNull { it.id == officerId }
        val appear = CharacterAppearanceSystem.infoFor(state, officerId, officer?.name ?: officerId)
        // V1.1 历史 Canon 收尾：此历史人物此时尚未进入玩家视野（HIDDEN，如 NOT_YET_RELEVANT
        // 的赵鼎），不能让他"隐身"却仍然顶着真实身份、发表实质政见——那等于变相剧透。
        // 改由一名虚构的普通官员代为奏对，用 Nandu_Court_NPC_Art_V1 素材池配真实头像，
        // 呼应最初任务书"改由中书舍人/给事中/御史等合适普通NPC奏报"的要求。
        if (appear.visibility == CharacterVisibility.HIDDEN) {
            val virtualId = ArtResourceRegistry.CourtNpc.officialIdBySeed(officerId)
            val label = ArtResourceRegistry.CourtNpc.officialLabelForId(virtualId)
            return CouncilLine(
                speakerId = virtualId,
                speakerName = label,
                role = label,
                attitude = attitude,
                text = fallbackText
            )
        }
        return CouncilLine(
            speakerId = officerId,
            speakerName = appear.displayName,
            role = role,
            attitude = attitude,
            text = fallbackText
        )
    }

    private fun choice(id: String, label: String, draft: String, preview: String): CouncilChoice =
        CouncilChoice(id = id, label = label, edictDraft = draft, preview = preview)
}

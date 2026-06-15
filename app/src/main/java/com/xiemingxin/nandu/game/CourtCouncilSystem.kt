package com.xiemingxin.nandu.game

/**
 * V1.7 大殿朝会 / 橙光式宫廷事件骨架。
 *
 * 目标：
 * - 让待办不只是“卡片 + 拟旨”，而是带出多角色上奏、互相反驳、皇帝选择。
 * - 朝臣必须符合古代思维：奏对围绕君臣、社稷、粮道、军心、民力、祖宗法度。
 * - 后苑内廷也走“人物发言 + 选择后果预览”的轻剧情结构。
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
        return when (task.palaceId) {
            PalaceIds.CHUIGONG -> courtScene(state, task)
            PalaceIds.SHUMI -> militaryScene(state, task)
            PalaceIds.ZHENGSHI -> fiscalScene(state, task)
            PalaceIds.WENDE -> talentScene(state, task)
            PalaceIds.YUSHU -> secretMemorialScene(state, task)
            PalaceIds.HUANGCHENG -> bureauScene(state, task)
            PalaceIds.HOUYUAN -> innerPalaceScene(state, task)
            PalaceIds.TAIMIAO -> ancestralTempleScene(state, task)
            else -> genericScene(state, task)
        }
    }

    private fun courtScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "垂拱殿朝会",
        summary = "群臣列班，主战、主和、理财诸臣各陈所见。",
        lines = listOf(
            line(state, "li_gang", "主战老臣", "support", "陛下南渡未久，国耻未雪。臣请先定守江淮之策，再议进取中原。"),
            line(state, "zhao_ding", "执政文臣", "concerned", "兵不可无粮，粮不可无民。若军兴过急，东南财赋恐难支数路之师。"),
            line(state, "qin_hui", "主和文臣", "oppose", "臣非敢沮兵，惟恐轻动则民力先敝。愿陛下审度敌势，毋使社稷再震。"),
            line(state, "yue_fei", "新锐武将", "support", "臣愿整军听命。若朝廷粮道可继，愿以死报国，不负陛下北望。")
        ),
        choices = listOf(
            choice("war", "定主战方略", "传朕旨意：垂拱殿议定守江淮、图中原之策，赵鼎核粮，李纲督防，岳飞候命整军。", "主战派振奋，粮草压力上升。"),
            choice("balance", "先守后图", "传朕旨意：诸路先修城积粮，军政并举，未得粮道详报，不许轻启大战。", "朝局较稳，军心略缓。"),
            choice("peace", "暂缓兵议", "传朕旨意：边防不可废，然今日先安民力，诸军守要害，毋得擅动。", "主和派得势，主战将臣不满。")
        )
    )

    private fun militaryScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "枢密院军议",
        summary = "军报入院，诸将争论守城、出击、粮道。",
        lines = listOf(
            line(state, "han_shizhong", "江防宿将", "support", "金人若窥江面，臣请守建康水路。船军虽残，尚可一战。"),
            line(state, "yue_fei", "统兵武将", "support", "臣不敢轻言进取，只请练兵整伍，使诸军有可用之日。"),
            line(state, "zhao_ding", "执政文臣", "concerned", "诸军一动，粮道须先行。若无十旬之粮，前线虽勇亦难久持。"),
            line(state, "qin_hui", "主和文臣", "oppose", "军报可畏，然更当防诸将邀功轻进。愿陛下以守为先。")
        ),
        choices = listOf(
            choice("defend", "加固防线", "传朕旨意：枢密院会诸将加固江淮要城，先修城防，后议出师。", "城防提升，耗费金粮。"),
            choice("train", "整军练兵", "传朕旨意：诸军各整部伍，十日一报训练与粮储，不得虚报。", "军心提升，财政压力上升。"),
            choice("scout", "先探敌势", "传朕旨意：遣轻骑与水军哨探敌情，枢密院汇为军报。", "获得后续情报，短期收益较低。")
        )
    )

    private fun fiscalScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = if (task.source == TaskSource.TRADE) "政事堂市舶议" else "政事堂钱粮议",
        summary = "钱粮、民力、漕运与外贸税，是南渡朝廷的根本。",
        lines = listOf(
            line(state, "zhao_ding", "理财执政", "support", "东南财赋虽厚，亦不可竭泽而渔。臣请先清漕运，再核诸港商税。"),
            line(state, "li_gang", "主战老臣", "concerned", "有财而不养兵，是弃边防；养兵而扰民，是损根本。二者须并举。"),
            line(state, "qin_hui", "主和文臣", "neutral", "市舶之利可补国用，惟豪商交结州县，亦须防其乱法。")
        ),
        choices = listOf(
            choice("trade", "整顿市舶", "传朕旨意：政事堂清点泉州、明州、广州商税，市舶之利入公库，沿海兼修海防。", "国库收入预期上升，海商风险增加。"),
            choice("grain", "先保军粮", "传朕旨意：诸路转运司先保军粮民食，漕运、屯田、赈济分轻重具奏。", "粮草压力缓解，商税推进较慢。"),
            choice("light_tax", "安民轻敛", "传朕旨意：钱粮虽急，不得横征。诸路先核浮费，减民间扰役。", "民心提升，短期财政不足。")
        )
    )

    private fun talentScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "文德殿召贤",
        summary = "文臣议人才，武臣盼拔擢，旧臣担心骤进失序。",
        lines = listOf(
            line(state, "zhao_ding", "执政文臣", "support", "人才不可埋没，亦不可骤贵。宜先召见考校，再授差遣。"),
            line(state, "li_gang", "主战老臣", "support", "国难之时，拘常格则失豪杰。臣愿保举可战可守之士。"),
            line(state, "qin_hui", "主和文臣", "concerned", "骤拔寒微，旧臣未必心服。愿陛下明定法度，以塞浮议。")
        ),
        choices = listOf(
            choice("summon", "召见考校", "传朕旨意：文德殿整理在野人才线索，择其可用者召见考校，再议授官。", "稳妥推进人才线。"),
            choice("promote_fast", "破格拔擢", "传朕旨意：国难用人不拘常格，凡有军功才略者，先授差遣，以观后效。", "人才推进快，旧臣疑虑上升。"),
            choice("delay", "留档再议", "传朕旨意：诸路荐才先入册，待军政稍定再行召见。", "风险低，但可能错失人才。")
        )
    )

    private fun secretMemorialScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = if (task.source == TaskSource.DIPLOMACY) "御书房外交密议" else "御书房密折",
        summary = "密折不可尽信，亦不可不察。",
        lines = listOf(
            line(state, "zhao_ding", "执政文臣", "concerned", "密折须分缓急真假。若凡闻即行，州县必生扰动。"),
            line(state, "qin_hui", "主和文臣", "neutral", "外邦往来，辞气最要谨慎。一纸诏书，可动边心。"),
            line(state, "li_gang", "主战老臣", "support", "若能以西夏、海东牵制金人，亦是兵法之助。惟不可失国体。")
        ),
        choices = listOf(
            choice("envoy", "遣使探问", "传朕旨意：御书房会同礼官，择谨慎之臣通问西夏与海东，务求牵制金人。", "开启外交线索。"),
            choice("verify", "先核密折", "传朕旨意：御书房将近日密折分真假缓急，涉边防钱粮者先奏。", "风险较低，推进较慢。"),
            choice("shelve", "留中不发", "传朕旨意：此事留中，毋令外廷妄议。", "暂稳朝局，但可能错过时机。")
        )
    )

    private fun bureauScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "皇城司密奏",
        summary = "皇城司奏报朝臣动向，须防误判与党争。",
        lines = listOf(
            CouncilLine("bureau_clerk", "皇城司勾当官", "密奏官", "concerned", "臣等只据耳目所闻，不敢妄断忠奸。请陛下裁其缓急。"),
            line(state, "zhao_ding", "执政文臣", "concerned", "耳目之司可用，不可纵。若人人自危，朝堂反不安。"),
            line(state, "qin_hui", "主和文臣", "neutral", "朝臣往来，本有公私。愿陛下勿使流言伤国体。")
        ),
        choices = listOf(
            choice("observe", "暗中留意", "传朕旨意：皇城司谨慎核察朝臣往来，不得妄兴风波，凡有实据再具密奏。", "稳妥，风险低。"),
            choice("warn", "召见敲打", "传朕旨意：召相关臣僚入对，明示国难当前，不许结党误政。", "震慑力强，可能激化派系。"),
            choice("ignore", "暂不追究", "传朕旨意：无实据者不得上纲，皇城司退下。", "朝臣安心，隐患可能累积。")
        )
    )

    private fun innerPalaceScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "后苑内廷问对",
        summary = "后宫不是单纯装饰，而是声望、内廷、外朝观感的平衡场。",
        lines = listOf(
            CouncilLine("empress", "皇后", "中宫", "support", "官家日夜忧勤，外朝诸事固重，亦当保养圣躬，使内外安心。"),
            CouncilLine("dowager", "太后", "宫中尊长", "concerned", "祖宗社稷在上，行在新立，内廷尤当节俭，不可使外朝有浮言。"),
            CouncilLine("eunuch", "内侍押班", "内侍", "neutral", "诸宫用度皆已裁减，惟近来军报频至，宫中亦多惶惶。")
        ),
        choices = listOf(
            choice("frugal", "裁减内廷用度", "传朕旨意：内廷诸费从简，所省钱粮拨入军储，毋扰外朝。", "国库略稳，内廷满意下降。"),
            choice("comfort", "安抚宫中", "传朕旨意：内廷但守节俭，宫中诸人各安其职，勿因军报自乱。", "内廷稳定，财政影响小。"),
            choice("queen_advice", "听中宫建议", "传朕旨意：皇后可择宫中近事密陈，凡涉外朝者，由朕亲裁。", "开启后宫建议线。")
        )
    )

    private fun ancestralTempleScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = "太庙礼议",
        summary = "正统、民心、军心皆可借礼制凝聚，但不能空耗钱粮。",
        lines = listOf(
            line(state, "li_gang", "主战老臣", "support", "祭告祖宗，当以复仇雪耻为誓，不可徒饰虚文。"),
            line(state, "zhao_ding", "执政文臣", "concerned", "礼不可废，费亦不可奢。国用方艰，宜从简而有实。"),
            line(state, "qin_hui", "主和文臣", "neutral", "太庙之礼关国体，辞命须稳，不可使边臣误作轻战之令。")
        ),
        choices = listOf(
            choice("simple_rite", "从简祭告", "传朕旨意：礼官择日祭告太庙，以安军民之心；钱粮从简，不得扰民。", "名望小升，耗费低。"),
            choice("war_oath", "誓师雪耻", "传朕旨意：太庙祭告，以恢复中原为志，诸军各励忠义。", "军心提升，金国压力可能上升。"),
            choice("delay", "暂缓礼议", "传朕旨意：太庙礼议待钱粮稍定再行。", "节省资源，名望无提升。")
        )
    )

    private fun genericScene(state: GameState, task: PalaceTask): CouncilScene = CouncilScene(
        id = "council_${task.id}",
        palaceId = task.palaceId,
        title = task.title,
        summary = task.description,
        lines = listOf(
            line(state, "zhao_ding", "执政文臣", "concerned", "此事关国用与朝局，愿陛下审其轻重。"),
            line(state, "li_gang", "主战老臣", "support", "国难当前，凡有益社稷者，不可迟疑。")
        ),
        choices = listOf(
            choice("draft", "依奏拟旨", task.edictDraft.ifBlank { "传朕旨意：此事交有司详议，限期具奏。" }, "进入圣旨处理。"),
            choice("hold", "留中再议", "传朕旨意：此事留中，俟诸司详报再断。", "暂缓处理。")
        )
    )

    private fun line(state: GameState, officerId: String, role: String, attitude: String, fallbackText: String): CouncilLine {
        val officer = state.officers.firstOrNull { it.id == officerId }
        return CouncilLine(
            speakerId = officerId,
            speakerName = officer?.name ?: officerId,
            role = role,
            attitude = attitude,
            text = fallbackText
        )
    }

    private fun choice(id: String, label: String, draft: String, preview: String): CouncilChoice =
        CouncilChoice(id = id, label = label, edictDraft = draft, preview = preview)
}

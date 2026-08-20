package com.xiemingxin.nandu.agent

/**
 * 核心历史人物的初始 AgentState 注册表。
 *
 * 只提供"出生时"的初始值，后续由 CharacterAgentSystem 动态演化。
 * 对应 OfficerProfile 的静态人格，但这里是动态 Agent 的起始锚点。
 */
object CharacterAgentRegistry {

    private val defaults: Map<String, CharacterAgentState> = mapOf(

        "yue_fei" to CharacterAgentState(
            officerId = "yue_fei",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentPlan = AgentPlanType.REQUEST_BATTLE,
            loyaltyToEmperor = 92,
            ambition = 18,
            fearLevel = 8,
            ambition = 0,
            attitudeToEmperor = EmperorAttitude.LOYAL_DEVOTED,
            relations = listOf(
                CharacterRelation("han_shizhong", RelationKind.ALLY, 75, note = "同为主战，惺惺相惜"),
                CharacterRelation("qin_hui",      RelationKind.HOSTILE, 85, note = "政见相左，深为所忌"),
                CharacterRelation("zhao_ding",    RelationKind.NEUTRAL, 40, note = "财政重臣，立场不同")
            )
        ),

        "han_shizhong" to CharacterAgentState(
            officerId = "han_shizhong",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentPlan = AgentPlanType.WARN_DANGER,
            loyaltyToEmperor = 85,
            ambition = 32,
            fearLevel = 12,
            ambition = 0,
            attitudeToEmperor = EmperorAttitude.LOYAL_DEVOTED,
            relations = listOf(
                CharacterRelation("yue_fei",   RelationKind.ALLY,    72, note = "同为武将，共谋北伐"),
                CharacterRelation("qin_hui",   RelationKind.HOSTILE, 90, note = "与秦桧水火不容"),
                CharacterRelation("wu_jie",    RelationKind.ALLY,    55, note = "同守一线，相互倚重")
            )
        ),

        "qin_hui" to CharacterAgentState(
            officerId = "qin_hui",
            longTermGoal = AgentGoal.COURT_DOMINANCE,
            currentPlan = AgentPlanType.OPPOSE_POLICY,
            loyaltyToEmperor = 55,
            ambition = 88,
            fearLevel = 45,
            ambition = 10,
            attitudeToEmperor = EmperorAttitude.RESPECTFUL,
            relations = listOf(
                CharacterRelation("yue_fei",   RelationKind.HOSTILE,   88, note = "主战派之首，必欲除之"),
                CharacterRelation("han_shizhong", RelationKind.HOSTILE, 80, note = "武将势大，暗中提防"),
                CharacterRelation("zhao_ding", RelationKind.RIVAL,     55, note = "同为文臣，相互倾轧")
            )
        ),

        "zhao_ding" to CharacterAgentState(
            officerId = "zhao_ding",
            longTermGoal = AgentGoal.FISCAL_STABILITY,
            currentPlan = AgentPlanType.PETITION_EMPEROR,
            loyaltyToEmperor = 78,
            ambition = 38,
            fearLevel = 25,
            ambition = 5,
            attitudeToEmperor = EmperorAttitude.RESPECTFUL,
            relations = listOf(
                CharacterRelation("li_gang",  RelationKind.ALLY,   60, note = "同为国事忧心"),
                CharacterRelation("qin_hui",  RelationKind.RIVAL,  52, note = "文臣相争"),
                CharacterRelation("yue_fei",  RelationKind.NEUTRAL,35, note = "敬其忠勇，忧其好战耗粮")
            )
        ),

        "li_gang" to CharacterAgentState(
            officerId = "li_gang",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentPlan = AgentPlanType.OPPOSE_POLICY,
            loyaltyToEmperor = 82,
            ambition = 22,
            fearLevel = 15,
            ambition = 20,
            attitudeToEmperor = EmperorAttitude.RESPECTFUL,
            relations = listOf(
                CharacterRelation("zhao_ding", RelationKind.ALLY,  62, note = "共事多年，志同道合"),
                CharacterRelation("qin_hui",   RelationKind.RIVAL, 70, note = "力斥和议，屡遭阻挠")
            )
        ),

        "wu_jie" to CharacterAgentState(
            officerId = "wu_jie",
            longTermGoal = AgentGoal.HOLD_FRONTIER,
            currentPlan = AgentPlanType.REQUEST_SUPPLY,
            loyaltyToEmperor = 80,
            ambition = 28,
            fearLevel = 20,
            ambition = 12,
            attitudeToEmperor = EmperorAttitude.RESPECTFUL,
            relations = listOf(
                CharacterRelation("han_shizhong", RelationKind.ALLY, 55, note = "各守一方，互相声援"),
                CharacterRelation("yue_fei",      RelationKind.ALLY, 62, note = "同为主战派，互相仰仗")
            )
        ),

        "liu_qi" to CharacterAgentState(
            officerId = "liu_qi",
            longTermGoal = AgentGoal.HOLD_FRONTIER,
            currentPlan = AgentPlanType.REQUEST_BATTLE,
            loyaltyToEmperor = 82,
            ambition = 22,
            fearLevel = 18,
            ambition = 8
        ),

        "zong_ze" to CharacterAgentState(
            officerId = "zong_ze",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentPlan = AgentPlanType.PETITION_EMPEROR,
            loyaltyToEmperor = 96,
            ambition = 10,
            fearLevel = 5,
            ambition = 35,  // 多次请战未获准，积郁已深
            attitudeToEmperor = EmperorAttitude.RESPECTFUL,
            recentMemory = listOf(
                AgentMemoryEntry(
                    turn = 1, category = "petition_denied",
                    summary = "数度上书请战北上，圣意未定，老将扼腕。",
                    emotionalImpact = -8
                )
            )
        ),

        "zhang_jun" to CharacterAgentState(
            officerId = "zhang_jun",
            longTermGoal = AgentGoal.MILITARY_REFORM,
            currentPlan = AgentPlanType.RECOMMEND_TALENT,
            loyaltyToEmperor = 76,
            ambition = 42,
            fearLevel = 30,
            ambition = 0
        ),

        // 金方人物
        "wanyan_zongbi" to CharacterAgentState(
            officerId = "wanyan_zongbi",
            longTermGoal = AgentGoal.COURT_DOMINANCE,
            currentPlan = AgentPlanType.WARN_DANGER,
            loyaltyToEmperor = 30,   // 对金帝的忠诚，非宋皇帝
            ambition = 75,
            fearLevel = 15,
            ambition = 0,
            attitudeToEmperor = EmperorAttitude.NEUTRAL  // 对宋皇帝态度
        )
    )

    /** 获取指定人物初始 Agent 状态，未注册的返回通用默认值 */
    fun initialFor(officerId: String, ambition: Int = 40, loyalty: Int = 60): CharacterAgentState =
        defaults[officerId] ?: CharacterAgentState(
            officerId = officerId,
            longTermGoal = when {
                ambition >= 70 -> AgentGoal.PERSONAL_POWER
                loyalty >= 80  -> AgentGoal.PROTECT_EMPEROR
                else           -> AgentGoal.SURVIVAL
            },
            loyaltyToEmperor = loyalty,
            ambition = ambition
        )

    val allRegisteredIds: Set<String> get() = defaults.keys
}

package com.xiemingxin.nandu.agent

/**
 * Stage 8 核心历史人物初始 Agent 状态注册表
 *
 * 使用 CharacterAgentState 的实际类型（AgentGoal / AgentPlanType / RelationRecord / RelationTag）
 */
object CharacterAgentRegistry {

    private fun rel(targetId: String, tag: RelationTag, score: Int, note: String = "") =
        targetId to RelationRecord(targetOfficerId = targetId, score = score, tag = tag, lastEventSummary = note)

    private val defaults: Map<String, CharacterAgentState> = mapOf(

        "yue_fei" to CharacterAgentState(
            officerId = "yue_fei",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentGoal  = AgentGoal.SEEK_BATTLE,
            currentPlan  = AgentPlanType.PETITION_BATTLE,
            loyaltyToEmperor = 92,
            ambition  = 18,
            fearLevel = 8,
            warBias   = 95,
            emperorAttitude = EmperorAttitude.DEVOTED,
            relations = mapOf(
                rel("han_shizhong", RelationTag.ALLY,     75, "同为主战，惺惺相惜"),
                rel("qin_hui",      RelationTag.ENEMY,   -80, "政见相左，深为所忌"),
                rel("zhao_ding",    RelationTag.ACQUAINTANCE, -10, "财政重臣，立场不同")
            )
        ),

        "han_shizhong" to CharacterAgentState(
            officerId = "han_shizhong",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentGoal  = AgentGoal.DEFEND_FRONTLINE,
            currentPlan  = AgentPlanType.WARN_DANGER,
            loyaltyToEmperor = 85,
            ambition  = 32,
            fearLevel = 12,
            warBias   = 85,
            emperorAttitude = EmperorAttitude.DEVOTED,
            relations = mapOf(
                rel("yue_fei",   RelationTag.ALLY,   70, "同为武将，共谋北伐"),
                rel("qin_hui",   RelationTag.ENEMY, -88, "与秦桧水火不容"),
                rel("wu_jie",    RelationTag.ALLY,   55, "同守一线，相互倚重")
            )
        ),

        "qin_hui" to CharacterAgentState(
            officerId = "qin_hui",
            longTermGoal = AgentGoal.BUILD_INFLUENCE,
            currentGoal  = AgentGoal.OPPOSE_FACTIONS,
            currentPlan  = AgentPlanType.OPPOSE_POLICY,
            loyaltyToEmperor = 50,
            ambition  = 88,
            fearLevel = 45,
            warBias   = 10,
            emperorAttitude = EmperorAttitude.SUPPORTIVE,
            relations = mapOf(
                rel("yue_fei",      RelationTag.ENEMY,  -85, "主战派之首，必欲除之"),
                rel("han_shizhong", RelationTag.ENEMY,  -78, "武将势大，暗中提防"),
                rel("zhao_ding",    RelationTag.RIVAL,  -40, "同为文臣，相互倾轧")
            )
        ),

        "zhao_ding" to CharacterAgentState(
            officerId = "zhao_ding",
            longTermGoal = AgentGoal.SECURE_SUPPLY,
            currentGoal  = AgentGoal.STRENGTHEN_COURT,
            currentPlan  = AgentPlanType.PETITION_BATTLE,
            loyaltyToEmperor = 78,
            ambition  = 38,
            fearLevel = 25,
            warBias   = 45,
            emperorAttitude = EmperorAttitude.SUPPORTIVE,
            relations = mapOf(
                rel("li_gang",  RelationTag.FACTION_BROTHER, 60, "同为国事忧心"),
                rel("qin_hui",  RelationTag.RIVAL,          -50, "文臣相争"),
                rel("yue_fei",  RelationTag.ACQUAINTANCE,    30, "敬其忠勇，忧其好战耗粮")
            )
        ),

        "li_gang" to CharacterAgentState(
            officerId = "li_gang",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentGoal  = AgentGoal.OPPOSE_FACTIONS,
            currentPlan  = AgentPlanType.OPPOSE_POLICY,
            loyaltyToEmperor = 82,
            ambition  = 22,
            fearLevel = 15,
            warBias   = 80,
            emperorAttitude = EmperorAttitude.SUPPORTIVE,
            relations = mapOf(
                rel("zhao_ding", RelationTag.FACTION_BROTHER, 60, "共事多年，志同道合"),
                rel("qin_hui",   RelationTag.RIVAL,          -68, "力斥和议，屡遭阻挠")
            )
        ),

        "wu_jie" to CharacterAgentState(
            officerId = "wu_jie",
            longTermGoal = AgentGoal.DEFEND_FRONTLINE,
            currentGoal  = AgentGoal.SECURE_SUPPLY,
            currentPlan  = AgentPlanType.MILITARY_REQUEST,
            loyaltyToEmperor = 80,
            ambition  = 28,
            fearLevel = 20,
            warBias   = 72,
            emperorAttitude = EmperorAttitude.SUPPORTIVE,
            relations = mapOf(
                rel("han_shizhong", RelationTag.ALLY, 55, "各守一方，互相声援"),
                rel("yue_fei",      RelationTag.ALLY, 60, "同为主战派，互相仰仗")
            )
        ),

        "liu_qi" to CharacterAgentState(
            officerId = "liu_qi",
            longTermGoal = AgentGoal.DEFEND_FRONTLINE,
            currentGoal  = AgentGoal.SEEK_BATTLE,
            currentPlan  = AgentPlanType.PETITION_BATTLE,
            loyaltyToEmperor = 82,
            ambition  = 22,
            fearLevel = 18,
            warBias   = 78,
            emperorAttitude = EmperorAttitude.DEVOTED
        ),

        "zong_ze" to CharacterAgentState(
            officerId = "zong_ze",
            longTermGoal = AgentGoal.NORTHERN_EXPEDITION,
            currentGoal  = AgentGoal.SEEK_BATTLE,
            currentPlan  = AgentPlanType.PETITION_BATTLE,
            loyaltyToEmperor = 96,
            ambition  = 10,
            fearLevel = 5,
            warBias   = 98,
            emperorAttitude = EmperorAttitude.SUPPORTIVE,
            recentMemory = listOf(
                AgentMemoryEntry(
                    turn = 1,
                    category = MemoryCategory.EMPEROR_DECISION,
                    summary = "数度上书请战北上，圣意未定，老将扼腕。",
                    significance = 2
                )
            )
        ),

        "zhang_jun" to CharacterAgentState(
            officerId = "zhang_jun",
            longTermGoal = AgentGoal.CONSOLIDATE_MILITARY,
            currentGoal  = AgentGoal.RECOMMEND_TALENT,
            currentPlan  = AgentPlanType.RECOMMEND_OFFICER,
            loyaltyToEmperor = 76,
            ambition  = 42,
            fearLevel = 30,
            warBias   = 60,
            emperorAttitude = EmperorAttitude.NEUTRAL
        ),

        "wanyan_zongbi" to CharacterAgentState(
            officerId = "wanyan_zongbi",
            longTermGoal = AgentGoal.BUILD_INFLUENCE,
            currentGoal  = AgentGoal.OPPOSE_FACTIONS,
            currentPlan  = AgentPlanType.WARN_DANGER,
            loyaltyToEmperor = 30,
            ambition  = 75,
            fearLevel = 15,
            warBias   = 90,
            emperorAttitude = EmperorAttitude.NEUTRAL
        )
    )

    fun initialFor(officerId: String, ambition: Int = 40, loyalty: Int = 60): CharacterAgentState =
        defaults[officerId] ?: CharacterAgentState(
            officerId = officerId,
            longTermGoal = when {
                ambition >= 70 -> AgentGoal.BUILD_INFLUENCE
                loyalty >= 80  -> AgentGoal.DEFEND_FRONTLINE
                else           -> AgentGoal.SURVIVE_POLITICAL
            },
            loyaltyToEmperor = loyalty,
            ambition = ambition
        )

    val allRegisteredIds: Set<String> get() = defaults.keys
}

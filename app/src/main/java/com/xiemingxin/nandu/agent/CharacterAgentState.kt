package com.xiemingxin.nandu.agent

import kotlinx.serialization.Serializable

// ──────────────────────────────────────────────────────────────────────────────
// Stage 8 人物 Agent 系统核心数据结构
//
// 设计原则：
//   - Agent 只存意图和记忆，不直接存修改过的 GameState 数值
//   - 所有实际状态改变必须经过 GameRuleEngine 确定性校验
//   - 记忆有上限防止无限增长
// ──────────────────────────────────────────────────────────────────────────────

/** 人物长期目标类型 */
enum class CharacterGoalType(val label: String) {
    NORTHERN_EXPEDITION("北伐复土"),
    PEACE_NEGOTIATION("议和止战"),
    FISCAL_STABILITY("充实国库"),
    MILITARY_REFORM("整军备战"),
    PERSONAL_POWER("扩张权势"),
    PROTECT_EMPEROR("护佑圣上"),
    HOLD_FRONTIER("守御边关"),
    TALENT_CULTIVATION("培植人才"),
    COURT_DOMINANCE("控制朝局"),
    SURVIVAL("明哲保身")
}

/** 当前短期计划 */
enum class CharacterPlanType(val label: String) {
    REQUEST_BATTLE("请战出征"),
    REQUEST_SUPPLY("请求补给"),
    OPPOSE_POLICY("反对政策"),
    SUPPORT_ALLY("支持盟友"),
    UNDERMINE_RIVAL("掣肘政敌"),
    PETITION_EMPEROR("上奏陈情"),
    RECOMMEND_TALENT("举荐人才"),
    REQUEST_TRANSFER("请求调任"),
    WARN_DANGER("示警危机"),
    SEEK_ALLIANCE("私下交好"),
    DIPLOMATIC_ADVICE("建议外交"),
    WAIT_AND_SEE("静观其变"),
    NONE("无特定计划")
}

/** 人物态度（对皇帝） */
enum class EmperorAttitude(val label: String) {
    LOYAL_DEVOTED("忠心耿耿"),
    RESPECTFUL("恭敬克制"),
    NEUTRAL("不偏不倚"),
    DISAPPOINTED("心有失望"),
    ALIENATED("日渐疏离"),
    RESENTFUL("心存怨望")
}

/** 关系类型 */
enum class RelationKind(val label: String) {
    ALLY("政治盟友"),
    RIVAL("政治对手"),
    MENTOR("授业恩师"),
    PROTEGE("提携门生"),
    NEUTRAL("关系平淡"),
    HOSTILE("嫌隙颇深"),
    SUSPICIOUS("互存猜疑")
}

/** 人际关系记录 */
@Serializable
data class CharacterRelation(
    val targetOfficerId: String,
    val kind: RelationKind,
    val intensity: Int = 50,            // 0-100，数值越高关系越深
    val lastInteractionTurn: Int = -1,
    val note: String = ""
)

/** 记忆条目 */
@Serializable
data class AgentMemoryEntry(
    val turn: Int,
    val category: String,               // "edict_rejected" / "rewarded" / "battle_result" / "rivalry" 等
    val summary: String,
    val emotionalImpact: Int = 0,       // -10..+10，负为负面
    val relatedOfficerIds: List<String> = emptyList()
)

/** 自主行为候选（不修改 GameState，由规则校验后才能生效） */
@Serializable
data class AgentProposal(
    val id: String,
    val kind: CharacterPlanType,
    val targetOfficerId: String = "",
    val targetCityId: String = "",
    val edictSuggestion: String = "",
    val reason: String,
    val urgency: Int = 50,             // 0-100
    val score: Double = 0.0,
    val turn: Int = 0
) {
    /** 是否允许直接改 GameState（永远 false，只是意图） */
    val canModifyState: Boolean get() = false
}

/**
 * 人物 Agent 核心状态
 *
 * 存储在 GameState.characterAgentStates[officerId]，随存档持久化。
 * 记忆上限：recentMemories ≤ 10，keyMemories ≤ 5，activeProposals ≤ 3。
 */
@Serializable
data class CharacterAgentState(
    val officerId: String,

    // ── 长期目标 ─────────────────────────────────────────────────────────────
    val longTermGoal: CharacterGoalType = CharacterGoalType.SURVIVAL,
    val longTermGoalTurnSet: Int = 0,   // 目标确立于第几旬（防止无理由翻转）

    // ── 当前计划 ─────────────────────────────────────────────────────────────
    val currentPlan: CharacterPlanType = CharacterPlanType.NONE,
    val currentPlanTurnSet: Int = 0,    // 计划确立于第几旬
    val currentPlanTargetId: String = "", // 计划针对的人物/城池

    // ── 内心数值 ──────────────────────────────────────────────────────────────
    val loyaltyToEmperor: Int = 70,     // 0-100（≠ Officer.loyalty，这是动态的）
    val ambitionLevel: Int = 30,        // 0-100
    val fearLevel: Int = 20,            // 0-100（越高越倾向自保）
    val frustration: Int = 0,           // 0-100（建议被驳回累积，影响态度）

    // ── 对皇帝态度 ───────────────────────────────────────────────────────────
    val attitudeToEmperor: EmperorAttitude = EmperorAttitude.LOYAL_DEVOTED,

    // ── 人际关系 ──────────────────────────────────────────────────────────────
    val relations: List<CharacterRelation> = emptyList(),

    // ── 记忆（有上限） ────────────────────────────────────────────────────────
    val recentMemories: List<AgentMemoryEntry> = emptyList(), // 最多10条，旧的自动淘汰
    val keyMemories: List<AgentMemoryEntry> = emptyList(),    // 最多5条，高情感冲击永久保留

    // ── 活跃提案（等待皇帝处理） ──────────────────────────────────────────────
    val activeProposals: List<AgentProposal> = emptyList(),   // 最多3条

    // ── 统计：皇帝采纳/驳回建议次数 ───────────────────────────────────────────
    val edictAcceptedCount: Int = 0,
    val edictRejectedCount: Int = 0,

    // ── 上次战役结果 ──────────────────────────────────────────────────────────
    val lastBattleWon: Boolean? = null,
    val lastBattleTurn: Int = -1,

    // ── 上次被赏/罚旬 ─────────────────────────────────────────────────────────
    val lastRewardedTurn: Int = -1,
    val lastPunishedTurn: Int = -1,

    // ── 激活标志（DECEASED/HIDDEN 不参与 Agent 计算） ─────────────────────────
    val isActive: Boolean = true
) {
    companion object {
        const val MAX_RECENT_MEMORIES = 10
        const val MAX_KEY_MEMORIES = 5
        const val MAX_ACTIVE_PROPOSALS = 3
        /** 计划/目标最少持续旬数（防止频繁翻转） */
        const val MIN_GOAL_STABILITY_TURNS = 3
    }

    /** 添加记忆，自动淘汰旧的 */
    fun addMemory(entry: AgentMemoryEntry): CharacterAgentState {
        val isKey = kotlin.math.abs(entry.emotionalImpact) >= 7
        val newRecent = (recentMemories + entry).takeLast(MAX_RECENT_MEMORIES)
        val newKey = if (isKey) (keyMemories + entry).takeLast(MAX_KEY_MEMORIES) else keyMemories
        return copy(recentMemories = newRecent, keyMemories = newKey)
    }

    /** 更新关系（若已存在则更新，否则新增） */
    fun updateRelation(relation: CharacterRelation): CharacterAgentState {
        val existing = relations.filter { it.targetOfficerId != relation.targetOfficerId }
        return copy(relations = existing + relation)
    }

    /** 计算对皇帝态度（基于各数值动态派生） */
    fun deriveAttitude(): EmperorAttitude = when {
        loyaltyToEmperor >= 80 && frustration < 20 -> EmperorAttitude.LOYAL_DEVOTED
        loyaltyToEmperor >= 60 && frustration < 40 -> EmperorAttitude.RESPECTFUL
        loyaltyToEmperor >= 50 && frustration < 60 -> EmperorAttitude.NEUTRAL
        frustration >= 60 || loyaltyToEmperor < 40 -> EmperorAttitude.DISAPPOINTED
        frustration >= 75 || loyaltyToEmperor < 25 -> EmperorAttitude.ALIENATED
        else -> EmperorAttitude.RESENTFUL
    }

    /** 建议被驳回后的状态更新 */
    fun onProposalRejected(turn: Int, reason: String): CharacterAgentState {
        val newFrustration = (frustration + 8).coerceAtMost(100)
        val newLoyalty = (loyaltyToEmperor - if (frustration >= 50) 3 else 1).coerceAtLeast(0)
        return addMemory(AgentMemoryEntry(
            turn = turn, category = "proposal_rejected",
            summary = "所上奏章未获圣纳：$reason",
            emotionalImpact = -4
        )).copy(
            frustration = newFrustration,
            loyaltyToEmperor = newLoyalty,
            edictRejectedCount = edictRejectedCount + 1
        ).let { it.copy(attitudeToEmperor = it.deriveAttitude()) }
    }

    /** 建议被采纳后的状态更新 */
    fun onProposalAccepted(turn: Int): CharacterAgentState {
        val newFrustration = (frustration - 5).coerceAtLeast(0)
        val newLoyalty = (loyaltyToEmperor + 3).coerceAtMost(100)
        return addMemory(AgentMemoryEntry(
            turn = turn, category = "proposal_accepted",
            summary = "所请之事蒙圣允，甚感圣恩。",
            emotionalImpact = +5
        )).copy(
            frustration = newFrustration,
            loyaltyToEmperor = newLoyalty,
            edictAcceptedCount = edictAcceptedCount + 1
        ).let { it.copy(attitudeToEmperor = it.deriveAttitude()) }
    }
}

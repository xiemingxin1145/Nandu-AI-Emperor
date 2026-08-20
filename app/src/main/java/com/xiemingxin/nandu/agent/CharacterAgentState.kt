package com.xiemingxin.nandu.agent

/**
 * Stage 8：人物 Agent 状态
 *
 * 每个核心历史人物拥有独立的持久 Agent 状态，跨旬保持目标连续性。
 *
 * 设计原则：
 *  - 所有字段均有默认值（旧存档兼容）
 *  - AgentState 只记录"人物的主观意图/记忆/关系"
 *  - 任何对 GameState 的真实修改必须经过 GameRuleEngine 的确定性规则校验
 *  - Agent 只能通过 IntentProposal 表达意图，不能直接写权威数据
 */
data class CharacterAgentState(

    val officerId: String,

    // ── 目标层 ───────────────────────────────────────────────────────────────
    /** 长期目标（几乎不变，代表人物历史宿命）*/
    val longTermGoal: AgentGoal = AgentGoal.UNDEFINED,

    /** 当前旬的短期目标（受局势影响，但不能每旬无理由翻转）*/
    val currentGoal: AgentGoal = AgentGoal.UNDEFINED,

    /** 上一旬的目标 ID（用于连续性校验，防止随机翻转）*/
    val previousGoalId: String = "",

    /** 当前目标已持续的旬数（连续性权重依据）*/
    val goalPersistTurns: Int = 0,

    // ── 性格/立场 ────────────────────────────────────────────────────────────
    /** 对战争/进攻的态度 0=绝对主和 100=狂热主战 */
    val warBias: Int = 50,

    /** 对皇帝的忠诚度（动态，会受事件影响）0=叛逆 100=绝对忠诚 */
    val loyaltyToEmperor: Int = 70,

    /** 野心值（高野心者在受压时更可能结党/消极）*/
    val ambition: Int = 40,

    /** 风险偏好（高=愿意冒险，低=倾向保守）*/
    val riskTolerance: Int = 50,

    /** 当前恐惧/压力值（高时倾向保守或求和）*/
    val fearLevel: Int = 20,

    // ── 对皇帝的态度 ─────────────────────────────────────────────────────────
    /** 人物对皇帝的整体态度 */
    val emperorAttitude: EmperorAttitude = EmperorAttitude.NEUTRAL,

    /** 建议被皇帝采纳次数 */
    val adviceAdoptedCount: Int = 0,

    /** 建议被皇帝驳回次数 */
    val adviceRejectedCount: Int = 0,

    /** 被赏赐次数 */
    val rewardCount: Int = 0,

    /** 被责罚次数 */
    val punishCount: Int = 0,

    // ── 与其他人物的关系 ─────────────────────────────────────────────────────
    /**
     * 关系网 officerId → RelationRecord
     * 注意：这是人物的"主观感受"，不是权威游戏数据。
     * 不允许根据此字段直接改变游戏状态，只允许影响评分和台词。
     */
    val relations: Map<String, RelationRecord> = emptyMap(),

    // ── 计划层 ───────────────────────────────────────────────────────────────
    /** 当前旬的行动计划（从候选中选出的最优意图）*/
    val currentPlan: AgentPlanType = AgentPlanType.OBSERVE,

    /** 计划附带的目标说明 */
    val planDetail: String = "",

    // ── 记忆层（最多10条，自动压缩）─────────────────────────────────────────
    /** 近期记忆列表（滑动窗口，超过上限后摘要压缩）*/
    val recentMemory: List<AgentMemoryEntry> = emptyList(),

    /** 压缩后的长期记忆摘要文本 */
    val compressedMemorySummary: String = "",

    // ── 元数据 ───────────────────────────────────────────────────────────────
    /** 上一次触发 Agent 决策的旬数 */
    val lastActiveTurn: Int = -1,

    /** 是否已死亡/被罢黜（此状态下不再产生任何行动候选）*/
    val inactive: Boolean = false
) {
    companion object {
        const val MAX_RECENT_MEMORY = 10
        const val MIN_GOAL_PERSIST_TURNS = 2  // 目标至少坚持这么多旬才能改变

        /** 把超出窗口的记忆压缩成摘要 */
        fun compressMemory(
            existing: List<AgentMemoryEntry>,
            newEntry: AgentMemoryEntry,
            existingSummary: String
        ): Pair<List<AgentMemoryEntry>, String> {
            val all = existing + newEntry
            return if (all.size <= MAX_RECENT_MEMORY) {
                all to existingSummary
            } else {
                // 把最老的一半压缩成摘要
                val half = all.size / 2
                val toCompress = all.take(half)
                val keep = all.drop(half)
                val newSummary = buildString {
                    if (existingSummary.isNotBlank()) append(existingSummary).append("；")
                    toCompress.filter { it.significance >= 2 }.forEach { e ->
                        append("[旬${e.turn}]${e.summary}；")
                    }
                }.trimEnd('；').take(300)
                keep to newSummary
            }
        }
    }
}

// ── 目标枚举 ─────────────────────────────────────────────────────────────────

enum class AgentGoal(val label: String, val description: String) {
    UNDEFINED("无明确目标", "等待局势明朗"),

    // 军事类
    NORTHERN_EXPEDITION("北伐收复", "收复中原、迎回二圣"),
    DEFEND_FRONTLINE("固守前线", "确保前线稳固，防止金军南下"),
    CONSOLIDATE_MILITARY("整顿军务", "整编军队、提升战力"),
    SEEK_BATTLE("请战出征", "主动请求统兵出征"),

    // 政治类
    PEACE_NEGOTIATION("议和止兵", "推动与金国和谈，结束战争"),
    STRENGTHEN_COURT("巩固朝堂", "稳定朝局，防范政敌"),
    BUILD_INFLUENCE("积累威望", "扩大自身在朝堂的影响力"),
    OPPOSE_FACTIONS("打压政敌", "削弱政见相反的势力"),
    RECOMMEND_TALENT("举荐人才", "向皇帝推荐有才干的人物"),

    // 财政类
    SECURE_SUPPLY("充实粮草", "确保军需和民用粮草供给"),
    RESTORE_ECONOMY("恢复民力", "减轻赋税，恢复生产"),

    // 个人类
    SURVIVE_POLITICAL("明哲保身", "在政治夹缝中保持安全"),
    SEEK_PROMOTION("寻求升迁", "争取更高官职和更大权力"),
    SEEK_REVENGE("伺机报复", "等待机会打击曾经压制自己的人")
}

// ── 对皇帝态度 ───────────────────────────────────────────────────────────────

enum class EmperorAttitude(val label: String) {
    DEVOTED("竭诚效忠"),
    SUPPORTIVE("拥护信任"),
    NEUTRAL("恭谨侍奉"),
    DISAPPOINTED("失望消极"),
    ESTRANGED("疏远冷淡"),
    RESENTFUL("心存芥蒂")
}

// ── 关系记录 ─────────────────────────────────────────────────────────────────

data class RelationRecord(
    val targetOfficerId: String,
    /** -100=死仇 0=陌路 100=至交 */
    val score: Int = 0,
    val tag: RelationTag = RelationTag.ACQUAINTANCE,
    /** 最近一次影响关系的事件摘要 */
    val lastEventSummary: String = ""
)

enum class RelationTag(val label: String) {
    ALLY("盟友"),
    SUPPORTER("支持者"),
    ACQUAINTANCE("相识"),
    RIVAL("政敌"),
    ENEMY("死敌"),
    FACTION_BROTHER("同僚"),
    MENTOR("提携"),
    SUBORDINATE("部属")
}

// ── 记忆条目 ─────────────────────────────────────────────────────────────────

data class AgentMemoryEntry(
    val turn: Int,
    val category: MemoryCategory,
    val summary: String,
    /** 重要程度 1=普通 2=重要 3=极重要（影响压缩权重）*/
    val significance: Int = 1,
    /** 相关人物 ID 列表 */
    val relatedOfficerIds: List<String> = emptyList()
)

enum class MemoryCategory(val label: String) {
    BATTLE("战役"),
    EMPEROR_DECISION("皇帝决策"),
    COURT_CONFLICT("朝堂冲突"),
    PERSONAL_INTERACTION("人物互动"),
    POLITICAL_CHANGE("政治变局"),
    APPOINTMENT("任命调动"),
    REWARD_PUNISHMENT("赏罚"),
    STRATEGY("军事动向")
}

// ── 行动计划类型 ─────────────────────────────────────────────────────────────

enum class AgentPlanType(val label: String, val edict: String) {
    OBSERVE("观望局势", ""),
    PETITION_BATTLE("上奏请战", "臣请率军出征，收复故土。"),
    PETITION_PEACE("上奏议和", "臣以为当务之急在于休养生息，宜遣使议和。"),
    RECOMMEND_OFFICER("举荐人才", "臣举荐一人，可委以重任。"),
    OPPOSE_POLICY("反对政策", "臣以为此议不妥，恳请圣上三思。"),
    REQUEST_TRANSFER("请求调任", "臣愿赴边境，为国效命。"),
    SUPPORT_ALLY("声援同僚", "臣以为此议甚善，附议。"),
    WARN_DANGER("警告危险", "臣有要事密奏，事关社稷安危。"),
    SUGGEST_DIPLOMACY("建议外交", "臣建议遣使，以外交手段破局。"),
    MILITARY_REQUEST("军务请求", "臣请求补充粮草辎重。"),
    PRIVATE_ALLIANCE("私下结交", "") // 仅记录关系变化，不触发公开行为
}

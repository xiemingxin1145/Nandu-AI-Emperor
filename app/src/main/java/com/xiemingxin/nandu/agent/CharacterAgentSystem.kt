package com.xiemingxin.nandu.agent

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.game.OfficerStatus
import kotlin.math.abs

/**
 * Stage 8 人物 Agent 系统主逻辑
 *
 * 职责：
 *  1. 初始化/补全 characterAgentStates（新游戏或旧存档升级）
 *  2. 每旬推进：更新记忆、重评目标和计划、产生提案
 *  3. 处理玩家采纳/驳回建议
 *  4. 人物间意见冲突检测
 *
 * 成本控制：
 *  - 每旬只处理 ACTIVE 且 IN_COURT/DEPLOYED 的人物（最多8人）
 *  - 完全用 UtilityDecisionEngine 本地评分，无 LLM 调用
 *  - LLM 的 NpcInitiative 文本生成在 WorldAiProtocol 里批量进行
 *
 * 不变量：
 *  - Agent 不修改 GameState 里的任何权威字段（troops/gold/grain/owner/loyalty）
 *  - AgentProposal.canModifyState 永远 false
 */
object CharacterAgentSystem {

    // ── 初始化 ─────────────────────────────────────────────────────────────────

    /**
     * 确保所有活跃人物都有 AgentState。
     * 用于新游戏、旧存档升级（V6→V7）时补全。
     */
    fun ensureInitialized(
        states: Map<String, CharacterAgentState>,
        officers: List<Officer>
    ): Map<String, CharacterAgentState> {
        val result = states.toMutableMap()
        officers.forEach { o ->
            if (o.id !in result && o.status != OfficerStatus.DECEASED) {
                result[o.id] = CharacterAgentRegistry.initialFor(o.id, o.ambition, o.loyalty)
            }
        }
        return result
    }

    // ── 每旬推进 ───────────────────────────────────────────────────────────────

    /**
     * 每旬 tick：更新所有活跃人物的 Agent 状态，产生行为提案。
     * 返回更新后的 states 和本旬产生的提案列表。
     */
    fun tickAll(
        states: Map<String, CharacterAgentState>,
        state: GameState
    ): Pair<Map<String, CharacterAgentState>, List<AgentProposal>> {
        val newStates = states.toMutableMap()
        val allProposals = mutableListOf<AgentProposal>()

        // 只处理活跃、可见的人物（DECEASED/HIDDEN 不参与）
        val activeOfficers = state.officers.filter { o ->
            o.status !in setOf(OfficerStatus.DECEASED, OfficerStatus.HIDDEN) &&
            newStates[o.id]?.isActive != false
        }.sortedByDescending { priorityScore(it, newStates[it.id]) }
         .take(8) // 每旬最多处理8人（成本控制）

        activeOfficers.forEach { officer ->
            val agentState = newStates[officer.id]
                ?: CharacterAgentRegistry.initialFor(officer.id, officer.ambition, officer.loyalty)

            val updated = tickOfficer(agentState, officer, state)
            newStates[officer.id] = updated.first
            allProposals.addAll(updated.second)
        }

        return newStates to allProposals
    }

    /** 单个人物的旬推进 */
    private fun tickOfficer(
        agentState: CharacterAgentState,
        officer: Officer,
        state: GameState
    ): Pair<CharacterAgentState, List<AgentProposal>> {
        var updated = agentState

        // 1. 刷新激活状态
        val shouldBeActive = officer.status !in setOf(OfficerStatus.DECEASED, OfficerStatus.HIDDEN)
        updated = updated.copy(isActive = shouldBeActive)
        if (!shouldBeActive) return updated to emptyList()

        // 2. 情绪自然恢复（每旬挫败感-1，下限0）
        updated = updated.copy(
            ambition = (updated.frustration - 1).coerceAtLeast(0)
        )

        // 3. 重评长期目标（只有距上次设定≥3旬且条件变化明显才考虑）
        updated = maybeUpdateGoal(updated, officer, state)

        // 4. 重评当前计划（距上次设定≥2旬才重评）
        updated = maybeUpdatePlan(updated, officer, state)

        // 5. 产生行为提案
        val proposals = generateProposals(updated, officer, state)
        val capped = proposals.take(CharacterAgentState.MAX_ACTIVE_PROPOSALS)
        updated = updated.copy(activeProposals = capped)

        return updated to capped
    }

    // ── 目标评估 ───────────────────────────────────────────────────────────────

    private fun maybeUpdateGoal(
        agentState: CharacterAgentState, officer: Officer, state: GameState
    ): CharacterAgentState {
        val turnsSinceSet = state.turn - agentState.longTermGoalTurnSet
        if (turnsSinceSet < 3) return agentState

        val metrics = buildMetrics(agentState, officer, state)
        val goalOptions = AgentGoal.entries.map { goal ->
            UtilityOption(
                id = goal.name,
                payload = goal,
                baseScore = if (goal == agentState.longTermGoal) 0.0 else 0.0,
                factors = goalFactors(goal, officer),
                continuityBonus = 0.20 // 长期目标连续性 bonus 更高
            )
        }

        val best = UtilityDecisionEngine.choose(metrics, goalOptions, agentState.longTermGoal.name)
            ?: return agentState
        if (best.option.payload == agentState.longTermGoal) return agentState

        // 目标改变时记录记忆
        return agentState.addMemory(AgentMemoryEntry(
            turn = state.turn,
            category = "goal_shift",
            summary = "志向转变：由「${agentState.longTermGoal.label}」转为「${best.option.payload.label}」。",
            emotionalImpact = 0
        )).copy(
            longTermGoal = best.option.payload,
            longTermGoalTurnSet = state.turn
        )
    }

    private fun goalFactors(goal: CharacterGoalType, officer: Officer): List<UtilityFactor> = when (goal) {
        AgentGoal.NORTHERN_EXPEDITION -> listOf(
            UtilityFactor("jinThreat", 1.2, UtilityCurve.LINEAR),
            UtilityFactor("command_score", 0.9, UtilityCurve.QUADRATIC),
            UtilityFactor("officer_loyal", 0.7, UtilityCurve.LINEAR)
        )
        AgentGoal.PEACE_NEGOTIATION -> listOf(
            UtilityFactor("jinThreat", 0.8, UtilityCurve.QUADRATIC, invert = true),
            UtilityFactor("grain_ratio", 0.6, UtilityCurve.LINEAR, invert = true),
            UtilityFactor("ambition_score", 0.5, UtilityCurve.LINEAR)
        )
        AgentGoal.FISCAL_STABILITY -> listOf(
            UtilityFactor("grain_ratio", 1.0, UtilityCurve.SQRT, invert = true),
            UtilityFactor("politics_score", 0.8, UtilityCurve.LINEAR)
        )
        AgentGoal.COURT_DOMINANCE -> listOf(
            UtilityFactor("ambition_score", 1.5, UtilityCurve.QUADRATIC),
            UtilityFactor("frustration_rate", 0.6, UtilityCurve.LINEAR)
        )
        AgentGoal.PERSONAL_POWER -> listOf(
            UtilityFactor("ambition_score", 1.3, UtilityCurve.QUADRATIC),
            UtilityFactor("loyalty_rate", 0.4, UtilityCurve.LINEAR, invert = true)
        )
        AgentGoal.PROTECT_EMPEROR -> listOf(
            UtilityFactor("loyalty_rate", 1.4, UtilityCurve.QUADRATIC),
            UtilityFactor("courtStability", 0.6, UtilityCurve.LINEAR, invert = true)
        )
        AgentGoal.HOLD_FRONTIER -> listOf(
            UtilityFactor("command_score", 1.0, UtilityCurve.LINEAR),
            UtilityFactor("jinThreat", 0.9, UtilityCurve.SQRT)
        )
        AgentGoal.SURVIVAL -> listOf(
            UtilityFactor("frustration_rate", 1.0, UtilityCurve.QUADRATIC),
            UtilityFactor("loyalty_rate", 0.5, UtilityCurve.LINEAR, invert = true)
        )
        else -> listOf(UtilityFactor("politics_score", 0.8, UtilityCurve.LINEAR))
    }

    // ── 计划评估 ───────────────────────────────────────────────────────────────

    private fun maybeUpdatePlan(
        agentState: CharacterAgentState, officer: Officer, state: GameState
    ): CharacterAgentState {
        val turnsSinceSet = state.turn - agentState.currentPlanTurnSet
        if (turnsSinceSet < 2) return agentState

        val metrics = buildMetrics(agentState, officer, state)
        val planOptions = buildPlanOptions(agentState, officer, state)
        val best = UtilityDecisionEngine.choose(metrics, planOptions, agentState.currentPlan.name)
            ?: return agentState

        if (best.option.payload == agentState.currentPlan) return agentState
        return agentState.copy(
            currentPlan = best.option.payload,
            currentPlanTurnSet = state.turn
        )
    }

    private fun buildPlanOptions(
        agentState: CharacterAgentState, officer: Officer, state: GameState
    ): List<UtilityOption<CharacterPlanType>> {
        val goal = agentState.longTermGoal
        return buildList {
            add(UtilityOption("REQUEST_BATTLE", AgentPlanType.REQUEST_BATTLE, 0.0,
                listOf(UtilityFactor("command_score", 1.2), UtilityFactor("jinThreat", 0.8)),
                continuityBonus = 0.12))
            add(UtilityOption("REQUEST_SUPPLY", AgentPlanType.REQUEST_SUPPLY, 0.0,
                listOf(UtilityFactor("grain_ratio", 1.0, invert = true)), continuityBonus = 0.10))
            add(UtilityOption("OPPOSE_POLICY", AgentPlanType.OPPOSE_POLICY, 0.0,
                listOf(UtilityFactor("frustration_rate", 1.0), UtilityFactor("politics_score", 0.5))))
            add(UtilityOption("PETITION_EMPEROR", AgentPlanType.PETITION_EMPEROR, 0.0,
                listOf(UtilityFactor("loyalty_rate", 0.9), UtilityFactor("frustration_rate", 0.6))))
            add(UtilityOption("RECOMMEND_TALENT", AgentPlanType.RECOMMEND_TALENT, 0.0,
                listOf(UtilityFactor("politics_score", 0.8), UtilityFactor("loyalty_rate", 0.6))))
            add(UtilityOption("WARN_DANGER", AgentPlanType.WARN_DANGER, 0.0,
                listOf(UtilityFactor("jinThreat", 1.1, curve = UtilityCurve.QUADRATIC))))
            add(UtilityOption("DIPLOMATIC_ADVICE", AgentPlanType.DIPLOMATIC_ADVICE, 0.0,
                listOf(UtilityFactor("politics_score", 1.0), UtilityFactor("jinThreat", 0.5))))
            add(UtilityOption("WAIT_AND_SEE", AgentPlanType.WAIT_AND_SEE, 0.0,
                listOf(UtilityFactor("frustration_rate", 0.6, invert = true),
                       UtilityFactor("loyalty_rate", 0.5, invert = true))))
        }.map { opt ->
            // 根据长期目标给对应计划加分
            val goalBonus = when {
                goal == AgentGoal.NORTHERN_EXPEDITION && opt.id == "REQUEST_BATTLE" -> 0.25
                goal == AgentGoal.FISCAL_STABILITY && opt.id == "REQUEST_SUPPLY"   -> 0.20
                goal == AgentGoal.COURT_DOMINANCE && opt.id == "OPPOSE_POLICY"     -> 0.18
                goal == AgentGoal.PROTECT_EMPEROR && opt.id == "PETITION_EMPEROR"  -> 0.15
                goal == AgentGoal.HOLD_FRONTIER && opt.id == "WARN_DANGER"         -> 0.15
                else -> 0.0
            }
            opt.copy(baseScore = opt.baseScore + goalBonus)
        }
    }

    // ── 提案生成 ───────────────────────────────────────────────────────────────

    private fun generateProposals(
        agentState: CharacterAgentState, officer: Officer, state: GameState
    ): List<AgentProposal> {
        val proposals = mutableListOf<AgentProposal>()
        val metrics = buildMetrics(agentState, officer, state)

        when (agentState.currentPlan) {
            AgentPlanType.REQUEST_BATTLE -> {
                val frontCities = state.armies.filter {
                    it.ownerFactionId == "song" &&
                    it.commanderId == officer.id &&
                    it.statusCode.name == "ENGAGEMENT_PENDING"
                }
                val city = frontCities.firstOrNull()
                val urgency = (50 + agentState.ambition / 3).coerceAtMost(95)
                proposals.add(AgentProposal(
                    id = "${officer.id}_battle_${state.turn}",
                    kind = AgentPlanType.REQUEST_BATTLE,
                    targetCityId = city?.targetCityId ?: "",
                    edictSuggestion = if (city != null)
                        "命${officer.name}部即刻进攻${state.cities.find { it.id == city.targetCityId }?.name ?: "前线"}，克复失地。"
                    else "命${officer.name}率军北上，试探金军动向。",
                    reason = "${officer.name}以为金虏猖獗，今正宜出兵，请陛下圣裁。",
                    urgency = urgency,
                    score = metrics.getOrDefault("command_score", 0.5),
                    turn = state.turn
                ))
            }
            AgentPlanType.REQUEST_SUPPLY -> {
                proposals.add(AgentProposal(
                    id = "${officer.id}_supply_${state.turn}",
                    kind = AgentPlanType.REQUEST_SUPPLY,
                    edictSuggestion = "命户部调拨军粮，以资${officer.name}所部。",
                    reason = "${officer.name}奏称粮道吃紧，请求朝廷接济。",
                    urgency = if (state.grain < 120000) 80 else 50,
                    score = metrics.getOrDefault("grain_ratio", 0.5).let { 1.0 - it },
                    turn = state.turn
                ))
            }
            AgentPlanType.OPPOSE_POLICY -> {
                val rival = agentState.relations
                    .filter { it.kind == RelationKind.HOSTILE || it.kind == RelationKind.RIVAL }
                    .maxByOrNull { it.intensity }
                val rivalName = state.officers.find { it.id == rival?.targetOfficerId }?.name
                proposals.add(AgentProposal(
                    id = "${officer.id}_oppose_${state.turn}",
                    kind = AgentPlanType.OPPOSE_POLICY,
                    targetOfficerId = rival?.targetOfficerId ?: "",
                    edictSuggestion = if (rivalName != null)
                        "请陛下三思，勿轻信${rivalName}之议，以免误国。"
                    else "请陛下三思当前政议，臣恐有碍军国大计。",
                    reason = "${officer.name}对当前政策持异议，${agentState.attitudeToEmperor.label}。",
                    urgency = (agentState.ambition / 2 + 30).coerceAtMost(85),
                    score = metrics.getOrDefault("frustration_rate", 0.3),
                    turn = state.turn
                ))
            }
            AgentPlanType.WARN_DANGER -> {
                val threat = state.jinThreat
                proposals.add(AgentProposal(
                    id = "${officer.id}_warn_${state.turn}",
                    kind = AgentPlanType.WARN_DANGER,
                    edictSuggestion = "金军威胁不可轻视（当前威胁值${threat}），请陛下早作部署。",
                    reason = "${officer.name}示警：金军动向有异，请圣上加强戒备。",
                    urgency = (threat * 0.9).toInt().coerceAtMost(95),
                    score = threat / 100.0,
                    turn = state.turn
                ))
            }
            AgentPlanType.RECOMMEND_TALENT -> {
                val hiddenTalent = state.officers.firstOrNull {
                    it.status == OfficerStatus.HIDDEN && it.id !in state.talentLeads
                }
                if (hiddenTalent != null) {
                    proposals.add(AgentProposal(
                        id = "${officer.id}_talent_${state.turn}",
                        kind = AgentPlanType.RECOMMEND_TALENT,
                        targetOfficerId = hiddenTalent.id,
                        edictSuggestion = "臣举荐${hiddenTalent.name}，其人才干出众，可为朝廷所用。",
                        reason = "${officer.name}举荐在野人才${hiddenTalent.name}。",
                        urgency = 55,
                        score = 0.6,
                        turn = state.turn
                    ))
                }
            }
            AgentPlanType.PETITION_EMPEROR -> {
                proposals.add(AgentProposal(
                    id = "${officer.id}_petition_${state.turn}",
                    kind = AgentPlanType.PETITION_EMPEROR,
                    edictSuggestion = "臣${officer.name}上奏：${agentState.longTermGoal.label}乃当务之急，请圣上定夺。",
                    reason = "${officer.name}就${agentState.longTermGoal.label}一事上奏陈情。",
                    urgency = 60,
                    score = agentState.loyaltyToEmperor / 100.0,
                    turn = state.turn
                ))
            }
            else -> { /* WAIT_AND_SEE / NONE：不产生提案 */ }
        }

        return proposals
    }

    // ── 冲突检测 ───────────────────────────────────────────────────────────────

    /**
     * 检测本旬产生的提案中是否存在人物意见冲突。
     * 返回冲突描述列表（供 UI/Chronicle 显示），不改变任何状态。
     */
    fun detectConflicts(
        proposals: List<AgentProposal>,
        states: Map<String, CharacterAgentState>,
        state: GameState
    ): List<String> {
        val conflicts = mutableListOf<String>()

        // 检查：主战派提案 vs 主和派提案
        val warProposals  = proposals.filter { it.kind == AgentPlanType.REQUEST_BATTLE }
        val peaceProposals = proposals.filter { it.kind == AgentPlanType.OPPOSE_POLICY &&
            it.edictSuggestion.contains("议和", ignoreCase = true) }

        if (warProposals.isNotEmpty() && peaceProposals.isNotEmpty()) {
            val warOfficer   = warProposals.first().id.split("_").first()
            val peaceOfficer = peaceProposals.first().id.split("_").first()
            val wName = state.officers.find { it.id == warOfficer }?.name ?: warOfficer
            val pName = state.officers.find { it.id == peaceOfficer }?.name ?: peaceOfficer
            conflicts.add("【朝堂争议】${wName}力主出兵，${pName}力主持重，双方意见相左，请陛下圣裁。")
        }

        // 检查：同一目标城的多个军团请战
        val battleByCity = warProposals.groupBy { it.targetCityId }.filter { it.value.size > 1 }
        battleByCity.forEach { (cityId, ps) ->
            val cityName = state.cities.find { it.id == cityId }?.name ?: cityId
            val names = ps.mapNotNull { p ->
                state.officers.find { it.id == p.id.split("_").first() }?.name
            }.joinToString("、")
            if (names.isNotBlank())
                conflicts.add("【请战竞争】${names}同时请攻${cityName}，可选一将为主帅。")
        }

        // 检查：关系敌对的人物产生支持同一政策的提案（意外同盟）
        proposals.forEachIndexed { i, p1 ->
            proposals.drop(i + 1).forEach { p2 ->
                val id1 = p1.id.split("_").first()
                val id2 = p2.id.split("_").first()
                val rel = states[id1]?.relations?.find { it.targetOfficerId == id2 }
                if (rel?.kind == RelationKind.HOSTILE && p1.kind == p2.kind) {
                    val n1 = state.officers.find { it.id == id1 }?.name ?: id1
                    val n2 = state.officers.find { it.id == id2 }?.name ?: id2
                    conflicts.add("【异常同盟】${n1}与${n2}向来不睦，却同时提出「${p1.kind.label}」，或各有盘算。")
                }
            }
        }

        return conflicts
    }

    // ── 反馈处理 ───────────────────────────────────────────────────────────────

    /**
     * 玩家通过圣旨采纳了某人物的提案
     */
    fun onProposalAccepted(
        states: Map<String, CharacterAgentState>,
        officerId: String,
        turn: Int
    ): Map<String, CharacterAgentState> {
        val current = states[officerId] ?: return states
        return states + (officerId to current.onProposalAccepted(turn))
    }

    /**
     * 玩家通过圣旨明确驳回了某人物的提案
     */
    fun onProposalRejected(
        states: Map<String, CharacterAgentState>,
        officerId: String,
        turn: Int,
        reason: String = "圣意另有考量"
    ): Map<String, CharacterAgentState> {
        val current = states[officerId] ?: return states
        return states + (officerId to current.onProposalRejected(turn, reason))
    }

    /**
     * 记录战役结果（影响主帅 Agent 的士气和记忆）
     */
    fun onBattleResult(
        states: Map<String, CharacterAgentState>,
        commanderId: String,
        won: Boolean,
        cityName: String,
        turn: Int
    ): Map<String, CharacterAgentState> {
        val current = states[commanderId] ?: return states
        val entry = AgentMemoryEntry(
            turn = turn,
            category = if (won) "battle_win" else "battle_loss",
            summary = if (won) "率军克复${cityName}，一雪国耻。" else "战于${cityName}，攻势受挫，折损兵马。",
            emotionalImpact = if (won) +7 else -6
        )
        val updated = current.addMemory(entry).copy(
            lastBattleWon = won,
            lastBattleTurn = turn,
            loyaltyToEmperor = if (won) (current.loyaltyToEmperor + 2).coerceAtMost(100) else current.loyaltyToEmperor
        )
        return states + (commanderId to updated.copy(attitudeToEmperor = updated.deriveAttitude()))
    }

    /**
     * 记录赏赐/责罚（直接影响态度和记忆）
     */
    fun onRewardOrPunish(
        states: Map<String, CharacterAgentState>,
        officerId: String,
        isReward: Boolean,
        turn: Int,
        desc: String = ""
    ): Map<String, CharacterAgentState> {
        val current = states[officerId] ?: return states
        val entry = AgentMemoryEntry(
            turn = turn,
            category = if (isReward) "rewarded" else "punished",
            summary = if (isReward) "蒙圣上嘉赏：$desc" else "受圣上责罚：$desc",
            emotionalImpact = if (isReward) +6 else -8
        )
        val updated = current.addMemory(entry).copy(
            loyaltyToEmperor = if (isReward)
                (current.loyaltyToEmperor + 5).coerceAtMost(100)
            else (current.loyaltyToEmperor - 8).coerceAtLeast(0),
            frustration = if (isReward) (current.frustration - 8).coerceAtLeast(0)
                          else (current.frustration + 15).coerceAtMost(100),
            lastRewardedTurn = if (isReward) turn else current.lastRewardedTurn,
            lastPunishedTurn = if (!isReward) turn else current.lastPunishedTurn
        )
        return states + (officerId to updated.copy(attitudeToEmperor = updated.deriveAttitude()))
    }

    // ── 工具函数 ───────────────────────────────────────────────────────────────

    /** 构建 Utility 计算用的 metrics */
    private fun buildMetrics(
        agentState: CharacterAgentState, officer: Officer, state: GameState
    ): Map<String, Double> = mapOf(
        "jinThreat"         to state.jinThreat / 100.0,
        "courtStability"    to state.courtStability / 100.0,
        "grain_ratio"       to (state.grain / 200000.0).coerceIn(0.0, 1.0),
        "command_score"     to officer.command / 100.0,
        "politics_score"    to officer.politics / 100.0,
        "loyalty_rate"      to agentState.loyaltyToEmperor / 100.0,
        "ambition_score"    to agentState.ambition / 100.0,
        "frustration_rate"  to agentState.ambition / 100.0,
        "officer_loyal"     to officer.loyalty / 100.0,
        "fear_rate"         to agentState.fearLevel / 100.0
    )

    /** 计算人物本旬的优先级分数（决定处理顺序） */
    private fun priorityScore(officer: Officer, agentState: CharacterAgentState?): Int {
        if (agentState == null) return 0
        return agentState.ambition / 2 +
            (if (agentState.activeProposals.isNotEmpty()) 20 else 0) +
            officer.politics / 5 +
            officer.command / 5
    }
}

package com.xiemingxin.nandu.agent

import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.Officer
import com.xiemingxin.nandu.game.OfficerStatus
import kotlin.math.abs

/**
 * Stage 8 人物 Agent 系统主逻辑
 *
 * 成本控制：每旬只处理最多8个活跃人物，完全本地 UtilityDecisionEngine，
 * 无 LLM 调用。LLM 文本生成在 WorldAiProtocol 批量进行。
 *
 * 不变量：Agent 不直接修改 GameState 权威字段。
 */
object CharacterAgentSystem {

    private const val MAX_ACTIVE_PER_TURN = 8
    private const val MIN_GOAL_TURNS = 3  // 目标最少持续旬数
    private const val MAX_RECENT_MEMORIES = 10
    private const val MAX_PROPOSALS = 3

    // ── 初始化 ─────────────────────────────────────────────────────────────────

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

    fun tickAll(
        states: Map<String, CharacterAgentState>,
        gameState: GameState
    ): Pair<Map<String, CharacterAgentState>, List<AgentProposal>> {
        val newStates = states.toMutableMap()
        val allProposals = mutableListOf<AgentProposal>()

        val activeOfficers = gameState.officers
            .filter { o ->
                o.status !in setOf(OfficerStatus.DECEASED, OfficerStatus.HIDDEN) &&
                newStates[o.id]?.let { it.longTermGoal != AgentGoal.UNDEFINED } != false
            }
            .sortedByDescending { priorityScore(it, newStates[it.id]) }
            .take(MAX_ACTIVE_PER_TURN)

        activeOfficers.forEach { officer ->
            val agentState = newStates[officer.id]
                ?: CharacterAgentRegistry.initialFor(officer.id, officer.ambition, officer.loyalty)
            val (updated, proposals) = tickOfficer(agentState, officer, gameState)
            newStates[officer.id] = updated
            allProposals += proposals
        }
        return newStates to allProposals
    }

    private fun tickOfficer(
        state: CharacterAgentState,
        officer: Officer,
        gameState: GameState
    ): Pair<CharacterAgentState, List<AgentProposal>> {
        if (officer.status in setOf(OfficerStatus.DECEASED, OfficerStatus.HIDDEN))
            return state to emptyList()

        var s = state
        s = maybeUpdateGoal(s, officer, gameState)
        s = maybeUpdatePlan(s, officer, gameState)
        val proposals = generateProposals(s, officer, gameState).take(MAX_PROPOSALS)
        return s to proposals
    }

    // ── 目标评估 ───────────────────────────────────────────────────────────────

    private fun maybeUpdateGoal(
        state: CharacterAgentState, officer: Officer, gameState: GameState
    ): CharacterAgentState {
        if (state.goalPersistTurns < MIN_GOAL_TURNS) {
            return state.copy(goalPersistTurns = state.goalPersistTurns + 1)
        }
        val metrics = buildMetrics(state, officer, gameState)
        val options = AgentGoal.entries.filter { it != AgentGoal.UNDEFINED }.map { goal ->
            UtilityOption(
                id = goal.name, payload = goal,
                baseScore = if (goal == state.longTermGoal) 0.0 else 0.0,
                factors = goalFactors(goal, officer),
                continuityBonus = 0.22
            )
        }
        val best = UtilityDecisionEngine.choose(metrics, options, state.longTermGoal.name)
            ?.option?.payload ?: return state
        if (best == state.longTermGoal) return state.copy(goalPersistTurns = state.goalPersistTurns + 1)

        val entry = AgentMemoryEntry(
            turn = gameState.turn, category = MemoryCategory.POLITICAL_CHANGE,
            summary = "志向转变：「${state.longTermGoal.label}」→「${best.label}」", significance = 1
        )
        return addMemory(state, entry).copy(
            longTermGoal = best, previousGoalId = state.longTermGoal.name, goalPersistTurns = 0
        )
    }

    private fun goalFactors(goal: AgentGoal, officer: Officer): List<UtilityFactor> = when (goal) {
        AgentGoal.NORTHERN_EXPEDITION, AgentGoal.SEEK_BATTLE -> listOf(
            UtilityFactor("jinThreat",    1.2, UtilityCurve.LINEAR),
            UtilityFactor("command",      0.9, UtilityCurve.QUADRATIC),
            UtilityFactor("warBias",      1.0, UtilityCurve.LINEAR),
            UtilityFactor("loyalty",      0.6, UtilityCurve.LINEAR)
        )
        AgentGoal.PEACE_NEGOTIATION -> listOf(
            UtilityFactor("jinThreat",    0.6, UtilityCurve.LINEAR, invert = true),
            UtilityFactor("grain",        0.5, UtilityCurve.LINEAR, invert = true),
            UtilityFactor("warBias",      1.0, UtilityCurve.LINEAR, invert = true)
        )
        AgentGoal.SECURE_SUPPLY, AgentGoal.RESTORE_ECONOMY -> listOf(
            UtilityFactor("grain",        1.0, UtilityCurve.SQRT, invert = true),
            UtilityFactor("politics",     0.8, UtilityCurve.LINEAR)
        )
        AgentGoal.BUILD_INFLUENCE, AgentGoal.SEEK_PROMOTION -> listOf(
            UtilityFactor("ambition",     1.5, UtilityCurve.QUADRATIC),
            UtilityFactor("loyalty",      0.3, UtilityCurve.LINEAR, invert = true)
        )
        AgentGoal.SURVIVE_POLITICAL -> listOf(
            UtilityFactor("ambition",     0.5, UtilityCurve.LINEAR, invert = true),
            UtilityFactor("fear",         1.2, UtilityCurve.QUADRATIC)
        )
        AgentGoal.DEFEND_FRONTLINE -> listOf(
            UtilityFactor("command",      1.0, UtilityCurve.LINEAR),
            UtilityFactor("jinThreat",    0.9, UtilityCurve.SQRT)
        )
        else -> listOf(UtilityFactor("politics", 0.8, UtilityCurve.LINEAR))
    }

    // ── 计划评估 ───────────────────────────────────────────────────────────────

    private fun maybeUpdatePlan(
        state: CharacterAgentState, officer: Officer, gameState: GameState
    ): CharacterAgentState {
        val metrics = buildMetrics(state, officer, gameState)
        val goalBonus = mapOf(
            "PETITION_BATTLE"   to if (state.longTermGoal in setOf(AgentGoal.NORTHERN_EXPEDITION, AgentGoal.SEEK_BATTLE)) 0.25 else 0.0,
            "MILITARY_REQUEST"  to if (state.longTermGoal == AgentGoal.SECURE_SUPPLY) 0.20 else 0.0,
            "OPPOSE_POLICY"     to if (state.longTermGoal == AgentGoal.OPPOSE_FACTIONS) 0.18 else 0.0,
            "WARN_DANGER"       to if (state.longTermGoal == AgentGoal.DEFEND_FRONTLINE) 0.15 else 0.0,
            "RECOMMEND_OFFICER" to if (state.longTermGoal == AgentGoal.RECOMMEND_TALENT) 0.20 else 0.0,
            "PETITION_PEACE"    to if (state.longTermGoal == AgentGoal.PEACE_NEGOTIATION) 0.22 else 0.0
        )
        val options = listOf(
            UtilityOption("PETITION_BATTLE",   AgentPlanType.PETITION_BATTLE,
                goalBonus["PETITION_BATTLE"] ?: 0.0,
                listOf(UtilityFactor("command",1.2), UtilityFactor("jinThreat",0.8), UtilityFactor("warBias",0.9)),
                continuityBonus = 0.15),
            UtilityOption("MILITARY_REQUEST",  AgentPlanType.MILITARY_REQUEST,
                goalBonus["MILITARY_REQUEST"] ?: 0.0,
                listOf(UtilityFactor("grain",1.0,invert=true), UtilityFactor("command",0.5))),
            UtilityOption("OPPOSE_POLICY",     AgentPlanType.OPPOSE_POLICY,
                goalBonus["OPPOSE_POLICY"] ?: 0.0,
                listOf(UtilityFactor("politics",0.8), UtilityFactor("warBias",0.4,invert=true))),
            UtilityOption("WARN_DANGER",       AgentPlanType.WARN_DANGER,
                goalBonus["WARN_DANGER"] ?: 0.0,
                listOf(UtilityFactor("jinThreat",1.2, UtilityCurve.QUADRATIC))),
            UtilityOption("RECOMMEND_OFFICER", AgentPlanType.RECOMMEND_OFFICER,
                goalBonus["RECOMMEND_OFFICER"] ?: 0.0,
                listOf(UtilityFactor("politics",0.9), UtilityFactor("loyalty",0.5))),
            UtilityOption("PETITION_PEACE",    AgentPlanType.PETITION_PEACE,
                goalBonus["PETITION_PEACE"] ?: 0.0,
                listOf(UtilityFactor("warBias",1.0,invert=true), UtilityFactor("jinThreat",0.5,invert=true))),
            UtilityOption("SUGGEST_DIPLOMACY", AgentPlanType.SUGGEST_DIPLOMACY,
                0.0,
                listOf(UtilityFactor("politics",1.0), UtilityFactor("jinThreat",0.5))),
            UtilityOption("OBSERVE",           AgentPlanType.OBSERVE,
                0.0,
                listOf(UtilityFactor("fear",0.7, UtilityCurve.QUADRATIC), UtilityFactor("loyalty",0.4,invert=true)))
        )
        val best = UtilityDecisionEngine.choose(metrics, options, state.currentPlan.name)
            ?.option?.payload ?: return state
        return state.copy(currentPlan = best, currentGoal = mapPlanToGoal(best, state))
    }

    private fun mapPlanToGoal(plan: AgentPlanType, state: CharacterAgentState): AgentGoal = when (plan) {
        AgentPlanType.PETITION_BATTLE   -> AgentGoal.SEEK_BATTLE
        AgentPlanType.PETITION_PEACE    -> AgentGoal.PEACE_NEGOTIATION
        AgentPlanType.MILITARY_REQUEST  -> AgentGoal.SECURE_SUPPLY
        AgentPlanType.WARN_DANGER       -> AgentGoal.DEFEND_FRONTLINE
        AgentPlanType.RECOMMEND_OFFICER -> AgentGoal.RECOMMEND_TALENT
        AgentPlanType.OPPOSE_POLICY     -> AgentGoal.OPPOSE_FACTIONS
        else -> state.currentGoal
    }

    // ── 提案生成 ───────────────────────────────────────────────────────────────

    private fun generateProposals(
        state: CharacterAgentState, officer: Officer, gameState: GameState
    ): List<AgentProposal> {
        if (state.currentPlan == AgentPlanType.OBSERVE ||
            state.currentPlan == AgentPlanType.PRIVATE_ALLIANCE) return emptyList()

        val reason = buildReason(state, officer)
        val edict  = state.currentPlan.edict.ifBlank { "请陛下圣裁。" }

        val targetCityId = when (state.currentPlan) {
            AgentPlanType.PETITION_BATTLE -> {
                gameState.armies.firstOrNull {
                    it.commanderId == officer.id &&
                    it.statusCode.name == "ENGAGEMENT_PENDING"
                }?.targetCityId ?: ""
            }
            else -> ""
        }

        val rival = state.relations.values
            .filter { it.tag in setOf(RelationTag.RIVAL, RelationTag.ENEMY) }
            .minByOrNull { it.score }

        val targetOfficerId = when (state.currentPlan) {
            AgentPlanType.OPPOSE_POLICY  -> rival?.targetOfficerId ?: ""
            AgentPlanType.SUPPORT_ALLY   -> state.relations.values
                .filter { it.tag == RelationTag.ALLY }
                .maxByOrNull { it.score }?.targetOfficerId ?: ""
            AgentPlanType.RECOMMEND_OFFICER -> gameState.officers
                .firstOrNull { it.status == OfficerStatus.HIDDEN && it.id !in gameState.talentLeads }?.id ?: ""
            else -> ""
        }

        return listOf(AgentProposal(
            id = "${officer.id}_${state.currentPlan.name}_${gameState.turn}",
            kind = state.currentPlan,
            targetOfficerId = targetOfficerId,
            targetCityId = targetCityId,
            edictSuggestion = edict,
            reason = reason,
            urgency = urgencyFor(state, gameState),
            score = loyaltyScore(state),
            turn = gameState.turn
        ))
    }

    private fun buildReason(state: CharacterAgentState, officer: Officer): String {
        val intro = when (state.emperorAttitude) {
            EmperorAttitude.DEVOTED    -> "臣${officer.name}斗胆进言："
            EmperorAttitude.SUPPORTIVE -> "臣${officer.name}以为："
            EmperorAttitude.NEUTRAL    -> "臣${officer.name}恭请圣断："
            EmperorAttitude.DISAPPOINTED -> "臣${officer.name}心有忧虑，冒昧上奏："
            EmperorAttitude.ESTRANGED  -> "臣${officer.name}不得不言："
            EmperorAttitude.RESENTFUL  -> "臣${officer.name}忍无可忍，直言进谏："
        }
        return "$intro${state.currentPlan.label}，${state.longTermGoal.description}。"
    }

    private fun urgencyFor(state: CharacterAgentState, gs: GameState): Int {
        val base = when (state.currentPlan) {
            AgentPlanType.WARN_DANGER -> (gs.jinThreat * 0.9).toInt()
            AgentPlanType.PETITION_BATTLE -> 60 + gs.jinThreat / 5
            AgentPlanType.MILITARY_REQUEST -> if (gs.grain < 100000) 75 else 45
            else -> 50
        }
        return base.coerceIn(20, 95)
    }

    private fun loyaltyScore(state: CharacterAgentState): Double =
        state.loyaltyToEmperor / 100.0

    // ── 冲突检测 ───────────────────────────────────────────────────────────────

    fun detectConflicts(
        proposals: List<AgentProposal>,
        states: Map<String, CharacterAgentState>,
        gameState: GameState
    ): List<String> {
        val conflicts = mutableListOf<String>()
        val warProposals   = proposals.filter { it.kind == AgentPlanType.PETITION_BATTLE }
        val peaceProposals = proposals.filter { it.kind == AgentPlanType.PETITION_PEACE }

        if (warProposals.isNotEmpty() && peaceProposals.isNotEmpty()) {
            val wName = warProposals.first().id.split("_").first()
                .let { id -> gameState.officers.find { it.id == id }?.name ?: id }
            val pName = peaceProposals.first().id.split("_").first()
                .let { id -> gameState.officers.find { it.id == id }?.name ?: id }
            conflicts += "【朝堂争议】${wName}力主出兵，${pName}力主休和，双方意见相左，请陛下圣裁。"
        }

        // 敌对人物提出相同类型动议（意外同盟提示）
        proposals.forEachIndexed { i, p1 ->
            proposals.drop(i + 1).forEach { p2 ->
                if (p1.kind == p2.kind) {
                    val id1 = p1.id.split("_").first()
                    val id2 = p2.id.split("_").first()
                    val rel = states[id1]?.relations?.get(id2)
                    if (rel?.tag in setOf(RelationTag.ENEMY, RelationTag.RIVAL)) {
                        val n1 = gameState.officers.find { it.id == id1 }?.name ?: id1
                        val n2 = gameState.officers.find { it.id == id2 }?.name ?: id2
                        conflicts += "【异常同盟】${n1}与${n2}向来不睦，却同提「${p1.kind.label}」，各有盘算。"
                    }
                }
            }
        }
        return conflicts
    }

    // ── 反馈处理 ───────────────────────────────────────────────────────────────

    fun onProposalAccepted(
        states: Map<String, CharacterAgentState>, officerId: String, turn: Int
    ): Map<String, CharacterAgentState> {
        val s = states[officerId] ?: return states
        val entry = AgentMemoryEntry(turn, MemoryCategory.EMPEROR_DECISION,
            "所请之事蒙圣允，甚感圣恩。", significance = 2)
        val updated = addMemory(s, entry).copy(
            loyaltyToEmperor = (s.loyaltyToEmperor + 4).coerceAtMost(100),
            adviceAdoptedCount = s.adviceAdoptedCount + 1
        )
        return states + (officerId to updated)
    }

    fun onProposalRejected(
        states: Map<String, CharacterAgentState>,
        officerId: String, turn: Int, reason: String = "圣意另有考量"
    ): Map<String, CharacterAgentState> {
        val s = states[officerId] ?: return states
        val entry = AgentMemoryEntry(turn, MemoryCategory.EMPEROR_DECISION,
            "所上奏章未获圣纳：$reason", significance = 2)
        val loyaltyDrop = if (s.adviceRejectedCount >= 3) 4 else 1
        val updated = addMemory(s, entry).copy(
            loyaltyToEmperor = (s.loyaltyToEmperor - loyaltyDrop).coerceAtLeast(0),
            adviceRejectedCount = s.adviceRejectedCount + 1,
            emperorAttitude = deriveAttitude(
                (s.loyaltyToEmperor - loyaltyDrop).coerceAtLeast(0),
                s.adviceRejectedCount + 1
            )
        )
        return states + (officerId to updated)
    }

    fun onBattleResult(
        states: Map<String, CharacterAgentState>,
        commanderId: String, won: Boolean, cityName: String, turn: Int
    ): Map<String, CharacterAgentState> {
        val s = states[commanderId] ?: return states
        val entry = AgentMemoryEntry(
            turn, MemoryCategory.BATTLE,
            if (won) "率军克复${cityName}，一雪国耻。" else "战于${cityName}，攻势受挫，折损兵马。",
            significance = if (won) 2 else 3
        )
        val updated = addMemory(s, entry).copy(
            loyaltyToEmperor = if (won) (s.loyaltyToEmperor + 2).coerceAtMost(100) else s.loyaltyToEmperor
        )
        return states + (commanderId to updated)
    }

    fun onRewardOrPunish(
        states: Map<String, CharacterAgentState>,
        officerId: String, isReward: Boolean, turn: Int, desc: String = ""
    ): Map<String, CharacterAgentState> {
        val s = states[officerId] ?: return states
        val entry = AgentMemoryEntry(
            turn, MemoryCategory.REWARD_PUNISHMENT,
            if (isReward) "蒙圣上嘉赏：$desc" else "受圣上责罚：$desc",
            significance = 2
        )
        val updated = addMemory(s, entry).copy(
            loyaltyToEmperor = if (isReward)
                (s.loyaltyToEmperor + 5).coerceAtMost(100)
            else (s.loyaltyToEmperor - 8).coerceAtLeast(0),
            rewardCount  = if (isReward) s.rewardCount + 1 else s.rewardCount,
            punishCount  = if (!isReward) s.punishCount + 1 else s.punishCount,
            emperorAttitude = deriveAttitude(
                if (isReward) (s.loyaltyToEmperor + 5).coerceAtMost(100)
                else (s.loyaltyToEmperor - 8).coerceAtLeast(0),
                s.adviceRejectedCount
            )
        )
        return states + (officerId to updated)
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private fun addMemory(state: CharacterAgentState, entry: AgentMemoryEntry): CharacterAgentState {
        val newMemory = (state.recentMemory + entry).takeLast(MAX_RECENT_MEMORIES)
        val summary = if (newMemory.size >= MAX_RECENT_MEMORIES) {
            val oldest = newMemory.first()
            "第${oldest.turn}旬：${oldest.summary}"
        } else state.compressedMemorySummary
        return state.copy(
            recentMemory = newMemory,
            compressedMemorySummary = summary
        )
    }

    private fun deriveAttitude(loyalty: Int, rejectedCount: Int): EmperorAttitude = when {
        loyalty >= 80 && rejectedCount < 3 -> EmperorAttitude.DEVOTED
        loyalty >= 65 && rejectedCount < 5 -> EmperorAttitude.SUPPORTIVE
        loyalty >= 50                       -> EmperorAttitude.NEUTRAL
        loyalty >= 35                       -> EmperorAttitude.DISAPPOINTED
        loyalty >= 20                       -> EmperorAttitude.ESTRANGED
        else                               -> EmperorAttitude.RESENTFUL
    }

    private fun buildMetrics(
        state: CharacterAgentState, officer: Officer, gs: GameState
    ): Map<String, Double> = mapOf(
        "jinThreat"  to gs.jinThreat / 100.0,
        "grain"      to (gs.grain / 200000.0).coerceIn(0.0, 1.0),
        "command"    to officer.command / 100.0,
        "politics"   to officer.politics / 100.0,
        "loyalty"    to state.loyaltyToEmperor / 100.0,
        "ambition"   to state.ambition / 100.0,
        "fear"       to state.fearLevel / 100.0,
        "warBias"    to state.warBias / 100.0
    )

    private fun priorityScore(officer: Officer, state: CharacterAgentState?): Int =
        (state?.adviceRejectedCount ?: 0) * 5 +
        (if (state?.currentPlan != AgentPlanType.OBSERVE) 20 else 0) +
        officer.politics / 5 + officer.command / 5
}

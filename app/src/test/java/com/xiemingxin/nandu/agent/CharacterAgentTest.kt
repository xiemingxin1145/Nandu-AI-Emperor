package com.xiemingxin.nandu.agent

import com.xiemingxin.nandu.game.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Stage 8 人物 Agent 系统核心测试
 * 覆盖：不变量、目标稳定性、提案反馈、冲突检测、fallback
 */
class CharacterAgentTest {

    private fun mockOfficer(
        id: String, cmd: Int = 80, pol: Int = 70, loy: Int = 80,
        amb: Int = 40, city: String = "linan",
        status: OfficerStatus = OfficerStatus.IN_COURT
    ) = Officer(id, id, "宋廷", cmd, 70, 70, pol, loy, city, status,
                charm = 65, ambition = amb, rankLevel = 3, merit = 0,
                origin = "士族", skills = emptyList(), bio = "")

    private fun baseState(officers: List<Officer> = emptyList()): GameState = GameState(
        cities = listOf(City("linan", "临安", "song", 5000, 50, 100000, 50000, 70, "STABLE")),
        officers = officers, armies = emptyList(), factions = emptyList(),
        turn = 5, troopMorale = 70, courtStability = 60, jinThreat = 70,
        gold = 50000, grain = 150000, prestige = 50,
        season = Season.SPRING, weather = WeatherType.CLEAR,
        calendar = GameCalendar("建炎元年", 1127, 1, 1)
    )

    // ── 测试1：Agent 不能凭空修改权威 GameState ─────────────────────────────
    @Test
    fun `agent proposals cannot modify GameState fields`() {
        val yf = mockOfficer("yue_fei", cmd = 96, loy = 92, amb = 18)
        val state = baseState(listOf(yf))
        val agentState = CharacterAgentRegistry.initialFor("yue_fei", 18, 92)

        val (newStates, proposals) = CharacterAgentSystem.tickAll(
            mapOf("yue_fei" to agentState), state
        )

        // 提案 canModifyState 永远 false
        proposals.forEach { proposal ->
            assertFalse("提案不得声称能修改状态", proposal.canModifyState)
        }

        // GameState 字段未被改变（Agent tick 不改 troops/gold 等）
        assertEquals("gold 未被 Agent 改变", 50000, state.gold)
        assertEquals("grain 未被 Agent 改变", 150000, state.grain)
        assertEquals("troopMorale 未被 Agent 改变", 70, state.troopMorale)
    }

    // ── 测试2：长期目标不会每旬随机翻转 ──────────────────────────────────────
    @Test
    fun `long term goal is stable across consecutive turns`() {
        val yf = mockOfficer("yue_fei", cmd = 96, loy = 92, amb = 18)

        var agentStates = mapOf("yue_fei" to
            CharacterAgentRegistry.initialFor("yue_fei", 18, 92))

        val goals = mutableListOf<CharacterGoalType>()
        for (turn in 1..5) {
            val state = baseState(listOf(yf)).copy(turn = turn)
            val (newStates, _) = CharacterAgentSystem.tickAll(agentStates, state)
            agentStates = newStates
            goals.add(agentStates["yue_fei"]!!.longTermGoal)
        }

        // 岳飞的北伐志向应至少持续3旬（MIN_GOAL_STABILITY_TURNS）
        val uniqueGoals = goals.toSet()
        assertTrue("5旬内目标切换不应超过2次", uniqueGoals.size <= 2)
        // 岳飞初始目标是 NORTHERN_EXPEDITION，不应在前几旬就突变
        assertEquals("岳飞第1旬应坚守北伐志向",
            CharacterGoalType.NORTHERN_EXPEDITION, goals.first())
    }

    // ── 测试3：不同人物对同一局势产生不同评分 ──────────────────────────────────
    @Test
    fun `different officers score same situation differently`() {
        val yueFei = mockOfficer("yue_fei", cmd = 96, pol = 55, loy = 92, amb = 18)
        val qinHui = mockOfficer("qin_hui", cmd = 40, pol = 88, loy = 55, amb = 88)
        val state   = baseState(listOf(yueFei, qinHui)).copy(jinThreat = 80)

        val yfState = CharacterAgentRegistry.initialFor("yue_fei", 18, 92)
        val qhState = CharacterAgentRegistry.initialFor("qin_hui", 88, 55)

        val (newStates, proposals) = CharacterAgentSystem.tickAll(
            mapOf("yue_fei" to yfState, "qin_hui" to qhState), state
        )

        val yfPlan = newStates["yue_fei"]?.currentPlan
        val qhPlan = newStates["qin_hui"]?.currentPlan

        // 岳飞主战 → 倾向 REQUEST_BATTLE / WARN_DANGER
        // 秦桧主和 → 倾向 OPPOSE_POLICY / WAIT_AND_SEE
        assertNotEquals("岳飞与秦桧计划应不同", yfPlan, qhPlan)

        // 岳飞不应选择 WAIT_AND_SEE 作为主要计划（威胁值80时应主动）
        assertNotEquals("岳飞在高威胁时不应静观其变",
            CharacterPlanType.WAIT_AND_SEE, yfPlan)
    }

    // ── 测试4：连续驳回建议后态度/记忆发生变化 ──────────────────────────────────
    @Test
    fun `repeated rejections increase frustration and change attitude`() {
        var state = CharacterAgentRegistry.initialFor("yue_fei", 18, 92)

        assertEquals("初始挫败感为0", 0, state.frustration)
        assertEquals("初始态度为忠心", EmperorAttitude.LOYAL_DEVOTED, state.attitudeToEmperor)

        // 连续7次驳回
        repeat(7) { i ->
            state = state.onProposalRejected(turn = i + 1, reason = "圣意另有考量")
        }

        assertTrue("7次驳回后挫败感应显著增加", state.frustration >= 40)
        assertTrue("7次驳回后记忆条目增加",
            state.recentMemories.count { it.category == "proposal_rejected" } >= 7)
        // 忠心值应下降
        assertTrue("多次驳回后忠心值应下降", state.loyaltyToEmperor < 92)
        // 态度应发生变化
        assertNotEquals("多次驳回后态度应变化",
            EmperorAttitude.LOYAL_DEVOTED, state.attitudeToEmperor)
    }

    // ── 测试5：无 API 时 Fallback 仍能运行 ──────────────────────────────────────
    @Test
    fun `fallback runs without any API or model calls`() {
        val officers = listOf(
            mockOfficer("yue_fei", cmd = 96),
            mockOfficer("qin_hui", cmd = 40, amb = 88),
            mockOfficer("han_shizhong", cmd = 88)
        )
        val state = baseState(officers)
        val initStates = CharacterAgentSystem.ensureInitialized(emptyMap(), officers)

        // 完全本地执行，不依赖任何外部 API
        val (newStates, proposals) = CharacterAgentSystem.tickAll(initStates, state)

        assertEquals("所有人物都应被处理", officers.size, newStates.size)
        assertTrue("应产生至少1个提案", proposals.isNotEmpty())
        // 确保没有抛异常，所有状态有效
        newStates.values.forEach { s ->
            assertTrue("忠心值应在有效范围", s.loyaltyToEmperor in 0..100)
            assertTrue("挫败感应在有效范围", s.frustration in 0..100)
        }
    }

    // ── 测试6：死亡/隐藏人物不能继续行动 ──────────────────────────────────────
    @Test
    fun `deceased and hidden officers do not generate proposals`() {
        val deceased = mockOfficer("dead_general", status = OfficerStatus.DECEASED)
        val hidden   = mockOfficer("hidden_talent", status = OfficerStatus.HIDDEN)
        val active   = mockOfficer("yue_fei", cmd = 96, status = OfficerStatus.IN_COURT)
        val state = baseState(listOf(deceased, hidden, active))

        val initStates = CharacterAgentSystem.ensureInitialized(emptyMap(), listOf(deceased, hidden, active))
        val (newStates, proposals) = CharacterAgentSystem.tickAll(initStates, state)

        // 死亡人物的 agentState 应 isActive=false
        // DECEASED 人物在 ensureInitialized 时不初始化，proposals 不包含其 id
        val deceasedProposals = proposals.filter { it.id.startsWith("dead_general") }
        val hiddenProposals   = proposals.filter { it.id.startsWith("hidden_talent") }
        assertTrue("DECEASED 人物不得产生提案", deceasedProposals.isEmpty())
        assertTrue("HIDDEN 人物不得产生提案", hiddenProposals.isEmpty())
    }

    // ── 测试7：意见冲突检测 ──────────────────────────────────────────────────────
    @Test
    fun `conflict detection identifies opposing proposals`() {
        val yfState = CharacterAgentState(
            officerId = "yue_fei",
            longTermGoal = CharacterGoalType.NORTHERN_EXPEDITION,
            currentPlan = CharacterPlanType.REQUEST_BATTLE,
            loyaltyToEmperor = 92, ambitionLevel = 18, fearLevel = 8, frustration = 0,
            relations = listOf(CharacterRelation("qin_hui", RelationKind.HOSTILE, 85))
        )
        val qhState = CharacterAgentState(
            officerId = "qin_hui",
            longTermGoal = CharacterGoalType.COURT_DOMINANCE,
            currentPlan = CharacterPlanType.OPPOSE_POLICY,
            loyaltyToEmperor = 55, ambitionLevel = 88, fearLevel = 45, frustration = 10
        )

        val battleProposal = AgentProposal(
            id = "yue_fei_battle_5", kind = CharacterPlanType.REQUEST_BATTLE,
            edictSuggestion = "命岳飞率军北上", reason = "请战", turn = 5
        )
        val opposeProposal = AgentProposal(
            id = "qin_hui_oppose_5", kind = CharacterPlanType.OPPOSE_POLICY,
            edictSuggestion = "不可轻动兵戈", reason = "反对", turn = 5
        )

        val state = baseState(listOf(
            mockOfficer("yue_fei"), mockOfficer("qin_hui")
        ))
        val states = mapOf("yue_fei" to yfState, "qin_hui" to qhState)
        val conflicts = CharacterAgentSystem.detectConflicts(
            listOf(battleProposal, opposeProposal), states, state
        )

        // 应检测到冲突（一方请战、另一方反对）
        assertTrue("应检测到朝堂争议", conflicts.isNotEmpty())
    }

    // ── 测试8：记忆不超上限 ──────────────────────────────────────────────────────
    @Test
    fun `memories respect max limits`() {
        var state = CharacterAgentRegistry.initialFor("zong_ze", 10, 96)

        // 添加20条记忆
        repeat(20) { i ->
            state = state.addMemory(AgentMemoryEntry(
                turn = i, category = "test", summary = "测试记忆${i}",
                emotionalImpact = if (i % 3 == 0) 8 else 2  // 每3条1条关键
            ))
        }

        assertTrue("recentMemories 不超过10条", state.recentMemories.size <= CharacterAgentState.MAX_RECENT_MEMORIES)
        assertTrue("keyMemories 不超过5条", state.keyMemories.size <= CharacterAgentState.MAX_KEY_MEMORIES)
    }

    // ── 测试9：采纳建议后状态改善 ──────────────────────────────────────────────
    @Test
    fun `accepting proposal improves officer state`() {
        var state = CharacterAgentRegistry.initialFor("yue_fei", 18, 92)
        // 先驳回3次让挫败感积累
        repeat(3) { i -> state = state.onProposalRejected(i, "不采") }
        val frustrationBefore = state.frustration

        // 采纳一次
        state = state.onProposalAccepted(turn = 10)

        assertTrue("采纳后挫败感应下降", state.frustration < frustrationBefore)
        assertTrue("采纳后忠心应上升", state.loyaltyToEmperor >= 92)
        assertEquals("采纳计数应+1", 1, state.edictAcceptedCount)
    }

    // ── 测试10：UtilityDecisionEngine 积分可重现 ────────────────────────────────
    @Test
    fun `utility scoring is deterministic`() {
        val metrics = mapOf(
            "jinThreat" to 0.8, "command_score" to 0.96, "loyalty_rate" to 0.92,
            "ambition_score" to 0.18, "frustration_rate" to 0.0, "grain_ratio" to 0.75,
            "politics_score" to 0.55, "officer_loyal" to 0.92, "fear_rate" to 0.08
        )
        val option = UtilityOption(
            id = "REQUEST_BATTLE",
            payload = CharacterPlanType.REQUEST_BATTLE,
            baseScore = 0.25,
            factors = listOf(
                UtilityFactor("command_score", 1.2),
                UtilityFactor("jinThreat", 0.8)
            )
        )

        val score1 = UtilityDecisionEngine.rank(metrics, listOf(option)).first().score
        val score2 = UtilityDecisionEngine.rank(metrics, listOf(option)).first().score

        assertEquals("相同输入应产生相同分数", score1, score2, 0.0001)
        assertTrue("REQUEST_BATTLE 在高统率高威胁时应得分较高", score1 > 0.5)
    }
}

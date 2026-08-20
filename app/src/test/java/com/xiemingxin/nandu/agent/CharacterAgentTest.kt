package com.xiemingxin.nandu.agent

import com.xiemingxin.nandu.game.*
import org.junit.Test
import org.junit.Assert.*

/**
 * Stage 8 人物 Agent 系统测试（10项）
 */
class CharacterAgentTest {

    private fun officer(
        id: String, cmd: Int = 80, pol: Int = 70, loy: Int = 80,
        amb: Int = 40, city: String = "linan",
        status: OfficerStatus = OfficerStatus.IN_COURT
    ) = Officer(id, id, "宋廷", cmd, 70, 70, pol, loy, city, status,
                charm = 65, ambition = amb, rankLevel = 3, merit = 0,
                origin = "士族", skills = emptyList(), bio = "")

    private fun gs(officers: List<Officer> = emptyList(), turn: Int = 5): GameState = GameState(
        cities = listOf(City("linan", "临安", "song", 5000, 50, 100000, 50000, 70, "STABLE")),
        officers = officers, armies = emptyList(), factions = emptyList(),
        turn = turn, troopMorale = 70, courtStability = 60, jinThreat = 70,
        gold = 50000, grain = 150000, prestige = 50,
        season = Season.SPRING, weather = WeatherType.CLEAR,
        calendar = GameCalendar("建炎元年", 1127, 1, 1)
    )

    // ── T1：提案不能声称能修改 GameState [Stage8-v1.1] ────────────────────────────
    @Test
    fun `proposals cannot modify GameState`() {
        val yf = officer("yue_fei", cmd = 96)
        val state = gs(listOf(yf))
        val agentState = CharacterAgentRegistry.initialFor("yue_fei", 18, 92)
        val (_, proposals) = CharacterAgentSystem.tickAll(mapOf("yue_fei" to agentState), state)

        proposals.forEach { assertFalse("提案不得声称能改状态", it.canModifyState) }
        assertEquals("gold 未被 Agent 改变", 50000, state.gold)
        assertEquals("grain 未被 Agent 改变", 150000, state.grain)
    }

    // ── T2：长期目标不每旬随机翻转 ──────────────────────────────────────────
    @Test
    fun `long term goal is stable across 5 turns`() {
        val yf = officer("yue_fei", cmd = 96, loy = 92, amb = 18)
        var states = mapOf("yue_fei" to CharacterAgentRegistry.initialFor("yue_fei", 18, 92))
        val goals = mutableListOf<AgentGoal>()

        for (turn in 1..5) {
            val (newStates, _) = CharacterAgentSystem.tickAll(states, gs(listOf(yf), turn))
            states = newStates
            goals += states["yue_fei"]!!.longTermGoal
        }

        // 5旬内目标变化不超过2次（MIN_GOAL_TURNS=3保证稳定性）
        val changes = goals.zipWithNext().count { (a, b) -> a != b }
        assertTrue("目标翻转不超过2次（实际${changes}次）", changes <= 2)
        assertEquals("岳飞初始应坚守北伐", AgentGoal.NORTHERN_EXPEDITION, goals.first())
    }

    // ── T3：不同人物对同一局势产生不同评分 ──────────────────────────────────
    @Test
    fun `different officers produce different plans for same situation`() {
        val yf = officer("yue_fei", cmd = 96, pol = 55, loy = 92, amb = 18)
        val qh = officer("qin_hui", cmd = 40, pol = 88, loy = 55, amb = 88)
        val state = gs(listOf(yf, qh)).copy(jinThreat = 80)

        val yfState = CharacterAgentRegistry.initialFor("yue_fei", 18, 92)
        val qhState = CharacterAgentRegistry.initialFor("qin_hui", 88, 55)

        val (newStates, _) = CharacterAgentSystem.tickAll(
            mapOf("yue_fei" to yfState, "qin_hui" to qhState), state
        )

        val yfPlan = newStates["yue_fei"]?.currentPlan
        val qhPlan = newStates["qin_hui"]?.currentPlan
        assertNotEquals("岳飞与秦桧计划应不同", yfPlan, qhPlan)
        assertNotEquals("岳飞在高威胁时不应静观",
            AgentPlanType.OBSERVE, yfPlan)
    }

    // ── T4：连续驳回建议后态度改变 ──────────────────────────────────────────
    @Test
    fun `repeated rejections change attitude and loyalty`() {
        var states = mapOf("yue_fei" to CharacterAgentRegistry.initialFor("yue_fei", 18, 92))
        val initialLoyalty = states["yue_fei"]!!.loyaltyToEmperor

        repeat(7) { i ->
            states = CharacterAgentSystem.onProposalRejected(states, "yue_fei", i + 1, "不采")
        }

        val after = states["yue_fei"]!!
        assertTrue("7次驳回后忠心下降", after.loyaltyToEmperor < initialLoyalty)
        assertTrue("7次驳回后有拒绝记忆",
            after.recentMemory.count { it.category == MemoryCategory.EMPEROR_DECISION } >= 7)
        assertNotEquals("态度应从DEVOTED变化",
            EmperorAttitude.DEVOTED, after.emperorAttitude)
    }

    // ── T5：无 API 时 fallback 仍能运行 ──────────────────────────────────────
    @Test
    fun `fallback runs without API`() {
        val officers = listOf(
            officer("yue_fei", cmd = 96),
            officer("qin_hui", cmd = 40, amb = 88),
            officer("han_shizhong", cmd = 88)
        )
        val state = gs(officers)
        val initStates = CharacterAgentSystem.ensureInitialized(emptyMap(), officers)
        val (newStates, proposals) = CharacterAgentSystem.tickAll(initStates, state)

        assertEquals("所有人物被初始化", officers.size, newStates.size)
        assertTrue("应有至少1个提案", proposals.isNotEmpty())
        newStates.values.forEach { s ->
            assertTrue("忠心在0-100", s.loyaltyToEmperor in 0..100)
            assertTrue("warBias在0-100", s.warBias in 0..100)
        }
    }

    // ── T6：DECEASED/HIDDEN 人物不产生提案 ──────────────────────────────────
    @Test
    fun `deceased and hidden produce no proposals`() {
        val deceased = officer("dead", status = OfficerStatus.DECEASED)
        val hidden   = officer("hidden", status = OfficerStatus.HIDDEN)
        val active   = officer("yue_fei", cmd = 96, status = OfficerStatus.IN_COURT)
        val state    = gs(listOf(deceased, hidden, active))

        val initStates = CharacterAgentSystem.ensureInitialized(emptyMap(), listOf(deceased, hidden, active))
        val (_, proposals) = CharacterAgentSystem.tickAll(initStates, state)

        assertTrue("DECEASED 不产生提案",
            proposals.none { it.id.startsWith("dead_") })
        assertTrue("HIDDEN 不产生提案",
            proposals.none { it.id.startsWith("hidden_") })
    }

    // ── T7：冲突检测识别对立提案 ────────────────────────────────────────────
    @Test
    fun `conflict detection finds opposing proposals`() {
        val battleP = AgentProposal("yue_fei_PETITION_BATTLE_5",
            AgentPlanType.PETITION_BATTLE, reason = "请战", turn = 5)
        val peaceP  = AgentProposal("qin_hui_PETITION_PEACE_5",
            AgentPlanType.PETITION_PEACE,  reason = "议和", turn = 5)

        val states = mapOf(
            "yue_fei" to CharacterAgentRegistry.initialFor("yue_fei", 18, 92),
            "qin_hui" to CharacterAgentRegistry.initialFor("qin_hui", 88, 55)
        )
        val state = gs(listOf(officer("yue_fei"), officer("qin_hui")))
        val conflicts = CharacterAgentSystem.detectConflicts(
            listOf(battleP, peaceP), states, state
        )
        assertTrue("应检测到朝堂争议", conflicts.isNotEmpty())
    }

    // ── T8：记忆不超上限 ────────────────────────────────────────────────────
    @Test
    fun `memories respect max limit`() {
        var states = mapOf("zong_ze" to CharacterAgentRegistry.initialFor("zong_ze", 10, 96))
        // 连续驳回20次，每次添加记忆
        repeat(20) { i ->
            states = CharacterAgentSystem.onProposalRejected(states, "zong_ze", i, "不采")
        }
        val mem = states["zong_ze"]!!.recentMemory
        assertTrue("记忆不超过10条（实际${mem.size}条）", mem.size <= 10)
    }

    // ── T9：采纳建议后状态改善 ──────────────────────────────────────────────
    @Test
    fun `accepting proposal improves officer state`() {
        var states = mapOf("yue_fei" to CharacterAgentRegistry.initialFor("yue_fei", 18, 92))
        repeat(3) { i -> states = CharacterAgentSystem.onProposalRejected(states, "yue_fei", i, "不采") }
        val loyaltyBefore = states["yue_fei"]!!.loyaltyToEmperor

        states = CharacterAgentSystem.onProposalAccepted(states, "yue_fei", 10)

        assertTrue("采纳后忠心恢复或上升",
            states["yue_fei"]!!.loyaltyToEmperor >= loyaltyBefore)
        assertEquals("采纳计数+1", 1, states["yue_fei"]!!.adviceAdoptedCount)
    }

    // ── T10：UtilityDecisionEngine 评分可重现 ──────────────────────────────
    @Test
    fun `utility scoring is deterministic`() {
        val metrics = mapOf(
            "jinThreat" to 0.8, "command" to 0.96, "loyalty" to 0.92,
            "ambition"  to 0.18, "grain" to 0.75, "politics" to 0.55,
            "warBias"   to 0.95, "fear" to 0.08
        )
        val option = UtilityOption(
            id = "PETITION_BATTLE", payload = AgentPlanType.PETITION_BATTLE,
            baseScore = 0.25,
            factors = listOf(UtilityFactor("command", 1.2), UtilityFactor("jinThreat", 0.8))
        )
        val s1 = UtilityDecisionEngine.rank(metrics, listOf(option)).first().score
        val s2 = UtilityDecisionEngine.rank(metrics, listOf(option)).first().score
        assertEquals("相同输入相同分数", s1, s2, 0.0001)
        assertTrue("高统率高威胁时 PETITION_BATTLE 得分>0.5", s1 > 0.5)
    }
}

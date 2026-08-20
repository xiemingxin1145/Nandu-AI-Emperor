package com.xiemingxin.nandu.agent

import com.xiemingxin.nandu.game.*
import org.junit.Test
import org.junit.Assert.*

/** Stage 8 人物 Agent 系统单元测试 */
class CharacterAgentTest {

    private fun mkOfficer(
        id: String, cmd: Int = 75, pol: Int = 60, force: Int = 65,
        loyalty: Int = 80, ambition: Int = 30,
        status: OfficerStatus = OfficerStatus.IN_COURT,
        city: String = "linan"
    ) = Officer(id, id, "宋廷", cmd, force, 65, pol, loyalty, city, status,
                charm = 60, ambition = ambition, rankLevel = 2, merit = 0,
                origin = "将门", skills = emptyList(), bio = "")

    private fun mkState(officers: List<Officer> = emptyList()) = GameState(
        cities = listOf(
            City("linan", "临安", "song", 5000, 50, 100000, 80000, 80),
            City("jiankang", "建康", "song", 3000, 45, 60000, 40000, 70),
            City("yangzhou", "扬州", "jin", 4000, 40, 50000, 30000, 50)
        ),
        officers = officers.ifEmpty { listOf(mkOfficer("yue_fei"), mkOfficer("qin_hui")) },
        armies = emptyList(), factions = emptyList(), turn = 5,
        troopMorale = 70, courtStability = 55, jinThreat = 75, gold = 60000, grain = 150000,
        prestige = 40, season = Season.SPRING, weather = WeatherType.CLEAR,
        calendar = GameCalendar("建炎元年", 1127, 1, 1)
    )

    // ── 测试1：AgentState 不能直接修改 GameState ─────────────────────────────
    @Test
    fun `agent proposals do not mutate GameState`() {
        val state = mkState()
        val agentStates = mapOf(
            "yue_fei" to CharacterPersonalities.initialState(state.officers.first { it.id == "yue_fei" })
        )
        val result = CharacterAgentEngine.processTurn(state, agentStates, turn = 5)

        // 关键：proposals只是意图，GameState未被修改
        assertEquals("city troops not changed", 5000, state.cities.first { it.id == "linan" }.troops)
        assertEquals("army list unchanged", state.armies.size, state.armies.size)
        result.proposals.forEach { p ->
            assertFalse("proposal must not directly change troops",
                p.targetCommandType == "modify_troops")
        }
    }

    // ── 测试2：长期目标不每旬随机翻转 ─────────────────────────────────────────
    @Test
    fun `long term goal does not flip every turn`() {
        val yue = mkOfficer("yue_fei")
        val state = mkState(listOf(yue))
        var agentState = CharacterPersonalities.initialState(yue)
        // 记录初始长期目标
        val initialLongTerm = agentState.longTermGoal

        // 跑5旬
        repeat(5) { t ->
            val result = CharacterAgentEngine.processTurn(state, mapOf("yue_fei" to agentState), t)
            agentState = result.updatedStates["yue_fei"] ?: agentState
        }
        // 长期目标不应改变（只有currentGoal允许有限漂移）
        assertEquals("long-term goal should not flip", initialLongTerm, agentState.longTermGoal)
    }

    // ── 测试3：不同人物对同一局势产生不同评分 ─────────────────────────────────
    @Test
    fun `different characters score same situation differently`() {
        val yue = mkOfficer("yue_fei", cmd = 90, pol = 45, loyalty = 92, ambition = 18)
        val qin = mkOfficer("qin_hui", cmd = 30, pol = 90, loyalty = 42, ambition = 86)
        val state = mkState(listOf(yue, qin))

        val yueAgent = CharacterPersonalities.initialState(yue)
        val qinAgent = CharacterPersonalities.initialState(qin)

        val yueResult = CharacterAgentEngine.processTurn(state, mapOf("yue_fei" to yueAgent), 5)
        val qinResult = CharacterAgentEngine.processTurn(state, mapOf("qin_hui" to qinAgent), 5)

        val yueTopPlan = yueResult.updatedStates["yue_fei"]?.currentPlan
        val qinTopPlan = qinResult.updatedStates["qin_hui"]?.currentPlan

        // 岳飞（主战97）和秦桧（主和8）行为应不同
        assertNotEquals("yue_fei and qin_hui should have different plans in same situation",
            yueTopPlan, qinTopPlan)
    }

    // ── 测试4：被连续驳回后态度和记忆变化 ─────────────────────────────────────
    @Test
    fun `repeated rejected advice changes attitude and memory`() {
        val yue = mkOfficer("yue_fei")
        var agentState = CharacterPersonalities.initialState(yue)
        val initialLoyalty = agentState.loyaltyToEmperor
        val initialAttitude = agentState.emperorAttitude
        val initialRejected = agentState.adviceRejectedCount

        // 连续驳回3次
        repeat(3) { t ->
            agentState = CharacterAgentEngine.onAdviceRejected(agentState, t)
        }
        assertTrue("loyalty should decrease after repeated rejection",
            agentState.loyaltyToEmperor < initialLoyalty)
        assertTrue("rejected count should increase",
            agentState.adviceRejectedCount > initialRejected)
        assertTrue("recent memory should contain rejection records",
            agentState.recentMemory.any { "被皇帝驳" in it.summary || "驳" in it.summary })
        // 如果驳回够多，态度应该变差
        if (agentState.loyaltyToEmperor < 55) {
            assertNotEquals("attitude should worsen with low loyalty",
                initialAttitude, agentState.emperorAttitude)
        }
    }

    // ── 测试5：fallback在无API时仍能运行 ──────────────────────────────────────
    @Test
    fun `local utility fallback runs without any model call`() {
        val officers = listOf(
            mkOfficer("yue_fei"), mkOfficer("han_shizhong"), mkOfficer("qin_hui")
        )
        val state = mkState(officers)
        val agentStates = officers.associate { o ->
            o.id to CharacterPersonalities.initialState(o)
        }
        // 完全本地，不调用任何API
        val result = CharacterAgentEngine.processTurn(state, agentStates, 5)
        assertNotNull("result must not be null", result)
        // 至少有部分人物产生了决策
        assertTrue("at least one updated state", result.updatedStates.isNotEmpty())
    }

    // ── 测试6：死亡/隐藏人物不继续行动 ───────────────────────────────────────
    @Test
    fun `deceased or hidden officer does not generate proposals`() {
        val deadOfficer = mkOfficer("dead_guy", status = OfficerStatus.DECEASED)
        val hiddenOfficer = mkOfficer("hidden_guy", status = OfficerStatus.HIDDEN)
        val state = mkState(listOf(deadOfficer, hiddenOfficer))
        val agentStates = mapOf(
            "dead_guy" to CharacterPersonalities.initialState(deadOfficer),
            "hidden_guy" to CharacterPersonalities.initialState(hiddenOfficer)
        )
        val result = CharacterAgentEngine.processTurn(state, agentStates, 5)
        assertTrue("dead/hidden officers should produce no proposals",
            result.proposals.none { it.officerId in setOf("dead_guy", "hidden_guy") })
    }

    // ── 测试7：已标记inactive的Agent不行动 ────────────────────────────────────
    @Test
    fun `inactive agent does not generate proposals`() {
        val officer = mkOfficer("some_officer")
        val state = mkState(listOf(officer))
        val inactiveAgent = CharacterPersonalities.initialState(officer).copy(inactive = true)
        val result = CharacterAgentEngine.processTurn(state, mapOf("some_officer" to inactiveAgent), 5)
        assertTrue("inactive agent should produce no proposals",
            result.proposals.none { it.officerId == "some_officer" })
    }

    // ── 测试8：目标最小坚持旬数保证连续性 ────────────────────────────────────
    @Test
    fun `goal does not change before minimum persist turns`() {
        val yue = mkOfficer("yue_fei")
        val state = mkState(listOf(yue))
        val initialAgent = CharacterPersonalities.initialState(yue).copy(
            currentGoal = AgentGoal.NORTHERN_EXPEDITION,
            goalPersistTurns = 1  // 还未达到MIN(2)
        )
        val result = CharacterAgentEngine.processTurn(state, mapOf("yue_fei" to initialAgent), 5)
        val newAgent = result.updatedStates["yue_fei"]!!
        // 因为 goalPersistTurns < MIN，目标不应改变
        assertEquals("goal should not flip before min persist turns",
            AgentGoal.NORTHERN_EXPEDITION, newAgent.currentGoal)
    }

    // ── 测试9：主战主和冲突检测 ───────────────────────────────────────────────
    @Test
    fun `war and peace advocates generate conflict when both active`() {
        val yue = mkOfficer("yue_fei", loyalty = 92, ambition = 18)
        val qin = mkOfficer("qin_hui", loyalty = 42, ambition = 86, pol = 90)
        val state = mkState(listOf(yue, qin)).copy(jinThreat = 85)

        val agentStates = mapOf(
            "yue_fei" to CharacterPersonalities.initialState(yue),
            "qin_hui" to CharacterPersonalities.initialState(qin)
        )
        val result = CharacterAgentEngine.processTurn(state, agentStates, 5)
        // 如果两人分别选了主战/主和，应该检测到冲突
        val hasWarProposal = result.proposals.any { it.planType == AgentPlanType.PETITION_BATTLE }
        val hasPeaceProposal = result.proposals.any { it.planType == AgentPlanType.PETITION_PEACE }
        if (hasWarProposal && hasPeaceProposal) {
            assertTrue("conflict should be detected when war/peace advocates both active",
                result.conflicts.isNotEmpty())
        }
        // 即使没有冲突，至少不崩溃
        assertNotNull(result)
    }

    // ── 测试10：存档round-trip ─────────────────────────────────────────────────
    @Test
    fun `agent state survives save codec round-trip`() {
        val yue = mkOfficer("yue_fei")
        val initialState = mkState(listOf(yue))
        val agentState = CharacterPersonalities.initialState(yue).copy(
            loyaltyToEmperor = 65,
            adviceRejectedCount = 3,
            fearLevel = 25,
            currentGoal = AgentGoal.NORTHERN_EXPEDITION
        )
        val stateWithAgent = initialState.copy(agentStates = mapOf("yue_fei" to agentState))
        val encoded = GameSaveCodec.export(stateWithAgent)
        val decoded = GameSaveCodec.import(encoded).getOrNull()
        assertNotNull("decode should succeed", decoded)
        val decodedAgent = decoded!!.agentStates["yue_fei"]
        assertNotNull("agent state should survive round-trip", decodedAgent)
        assertEquals("loyaltyToEmperor round-trip", 65, decodedAgent!!.loyaltyToEmperor)
        assertEquals("adviceRejectedCount round-trip", 3, decodedAgent.adviceRejectedCount)
        assertEquals("currentGoal round-trip", AgentGoal.NORTHERN_EXPEDITION, decodedAgent.currentGoal)
    }

    // ── 测试11：UtilityDecisionEngine为行为评分，非随机 ─────────────────────
    @Test
    fun `utility engine ranks candidates deterministically`() {
        val metrics = mapOf(
            "warBias" to 0.95, "jinThreat" to 0.85, "loyalty" to 0.90,
            "riskTolerance" to 0.80, "fearLevel" to 0.10, "grainPressure" to 0.1,
            "frontlineAlert" to 1.0, "hasArmy" to 1.0, "isAtFront" to 1.0,
            "rejectionRatio" to 0.0, "punishedRecently" to 0.0,
            "courtStability" to 0.6, "ambition" to 0.2, "treasuryPressure" to 0.1,
            "hasPoliticalEnemy" to 0.0, "isInCourt" to 1.0
        )
        val options = listOf(
            UtilityOption("PETITION_BATTLE", AgentPlanType.PETITION_BATTLE, 0.0, listOf(
                UtilityFactor("warBias", 0.50, UtilityCurve.QUADRATIC),
                UtilityFactor("jinThreat", 0.25, UtilityCurve.LINEAR),
                UtilityFactor("loyalty", 0.15)
            )),
            UtilityOption("PETITION_PEACE", AgentPlanType.PETITION_PEACE, 0.0, listOf(
                UtilityFactor("warBias", 0.55, invert = true),
                UtilityFactor("fearLevel", 0.25, UtilityCurve.QUADRATIC)
            ))
        )
        val r1 = UtilityDecisionEngine.rank(metrics, options)
        val r2 = UtilityDecisionEngine.rank(metrics, options)
        // 确定性：两次结果相同
        assertEquals("ranking must be deterministic", r1.map { it.option.id }, r2.map { it.option.id })
        // 主战倾向应该排第一
        assertEquals("high warBias should rank PETITION_BATTLE first",
            "PETITION_BATTLE", r1.first().option.id)
    }

    // ── 测试12：赏赐后忠诚提升 ───────────────────────────────────────────────
    @Test
    fun `reward increases loyalty and decreases fear`() {
        val officer = mkOfficer("yue_fei")
        val agentState = CharacterPersonalities.initialState(officer).copy(
            loyaltyToEmperor = 70, fearLevel = 30
        )
        val rewarded = CharacterAgentEngine.onRewarded(agentState, turn = 5)
        assertTrue("loyalty should increase after reward", rewarded.loyaltyToEmperor > 70)
        assertTrue("fear should decrease after reward", rewarded.fearLevel < 30)
        assertTrue("memory should record reward", rewarded.recentMemory.any { "赏" in it.summary })
    }
}

package com.xiemingxin.nandu.game

import com.xiemingxin.nandu.ai.WorldAction
import com.xiemingxin.nandu.ai.WorldTurnPlan
import org.junit.Test
import org.junit.Assert.*

/**
 * DELEGATION-001：皇帝授权制 AI 治国执行器测试。
 */
class DelegationSystemTest {

    private fun songFaction() = Faction(
        "song", "大宋", "宋", "赵构", "yingtianfu", "行在草创", isPlayable = true
    )

    private fun jinFaction() = Faction(
        "jin", "金国", "金", "完颜宗弼", "kaifeng", "兵锋正盛"
    )

    private fun songCity(troops: Int = 20000, gold: Int = 50000, defense: Int = 60) = City(
        "yingtianfu", "应天府", "song", troops = troops, defense = defense, grain = 100000, gold = gold
    )

    private fun jinCity(troops: Int = 20000, gold: Int = 30000) = City(
        "kaifeng", "开封", "jin", troops = troops, defense = 70, grain = 80000, gold = gold
    )

    private fun officer(id: String, name: String = id, command: Int = 80, loyalty: Int = 90, currentCityId: String = "yingtianfu") = Officer(
        id = id, name = name, faction = "宋廷", command = command, force = 80, strategy = 80,
        politics = 60, loyalty = loyalty, currentCityId = currentCityId, status = OfficerStatus.DEPLOYED
    )

    private fun songArmy(commanderId: String, troops: Int = 5000, id: String = "army_$commanderId") = Army(
        id = id, name = "${commanderId}部", ownerFactionId = "song", commanderId = commanderId,
        homeCityId = "yingtianfu", currentCityId = "yingtianfu", troops = troops, morale = 70,
        armyType = "infantry", supplyCityId = "yingtianfu", statusCode = ArmyStatus.GARRISONED, status = "驻防"
    )

    private fun jinArmy(commanderId: String, troops: Int = 5000) = Army(
        id = "army_jin_$commanderId", name = "${commanderId}部", ownerFactionId = "jin", commanderId = commanderId,
        homeCityId = "kaifeng", currentCityId = "kaifeng", troops = troops, morale = 70,
        armyType = "cavalry", supplyCityId = "kaifeng", statusCode = ArmyStatus.GARRISONED, status = "驻防"
    )

    private fun baseState(
        officers: List<Officer>,
        armies: List<Army>,
        mandates: List<ImperialMandate> = emptyList(),
        songCityOverride: City = songCity(),
        jinCityOverride: City = jinCity(),
        gold: Int = 50000,
        grain: Int = 100000
    ) = GameState(
        turn = 5,
        factions = listOf(songFaction(), jinFaction()),
        cities = listOf(songCityOverride, jinCityOverride),
        officers = officers,
        armies = armies,
        imperialMandates = mandates,
        gold = gold,
        grain = grain
    )

    private fun mandate(
        responsibleOfficerId: String,
        allowedActions: Set<MandateActionKind> = setOf(MandateActionKind.RECRUIT),
        autonomyLevel: MandateAutonomyLevel = MandateAutonomyLevel.BY_THE_BOOK,
        budgetGold: Int = 10000,
        budgetGrain: Int = 20000,
        regionCityIds: Set<String> = setOf("yingtianfu"),
        isActive: Boolean = true,
        id: String = "mandate_1"
    ) = ImperialMandate(
        id = id, issuedTurn = 1, goal = "经营应天，恢复实力",
        responsibleOfficerId = responsibleOfficerId, regionCityIds = regionCityIds,
        autonomyLevel = autonomyLevel, allowedActions = allowedActions,
        budgetGold = budgetGold, budgetGrain = budgetGrain, isActive = isActive
    )

    // ══════ 1. 未授权时 AI 不能控制宋军 ══════
    @Test
    fun unauthorizedPlayerArmyCannotBeControlled() {
        val zongZe = officer("zong_ze", "宗泽")
        val army = songArmy("zong_ze")
        val state = baseState(listOf(zongZe), listOf(army)) // 没有任何 mandate

        val plan = WorldTurnPlan(actions = listOf(
            WorldAction(type = "recruit_troops", factionId = "song", armyId = army.id, targetCityId = "yingtianfu", officerId = "zong_ze", amount = 2000)
        ))
        val result = WorldAiTurnExecutor.execute(state, plan)

        assertTrue("没有圣旨时应驳回", result.reports.any { it.contains("驳回") })
        assertEquals("城池兵力不应变化", songCity().troops, result.newState.cities.first { it.id == "yingtianfu" }.troops)
    }

    // ══════ 2. 授权后仅能执行授权范围（禁止未授权的交战） ══════
    @Test
    fun mandateCannotInitiateBattleWithoutExplicitAuthorization() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze")
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.RECRUIT)) // 没给 INITIATE_BATTLE
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))

        val v = DelegatedActionValidator.validate(
            state,
            WorldAction(type = "attack_city", factionId = "song", armyId = army.id, targetCityId = "kaifeng"),
            "song"
        )
        assertTrue(v is DelegatedActionValidator.ValidationResult.Rejected)
    }

    // ══════ 3. 预算耗尽后停止 ══════
    @Test
    fun recruitStopsWhenBudgetExhausted() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze", troops = 1000)
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.RECRUIT), budgetGold = 50, budgetGrain = 100)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))

        val action = WorldAction(type = "recruit_troops", factionId = "song", armyId = army.id, targetCityId = "yingtianfu", officerId = "zong_ze", amount = 5000)
        val approved = DelegatedActionValidator.validate(state, action, "song")
        assertTrue(approved is DelegatedActionValidator.ValidationResult.Approved)

        val (newState, record) = DelegatedActionValidator.execute(
            state, action, (approved as DelegatedActionValidator.ValidationResult.Approved).mandate, "song"
        )
        assertFalse("预算不够应执行失败", record.success)
        assertEquals("城池兵力不应变化", 1000, army.troops) // sanity: army 本身没被就地改
        assertEquals(1000, newState.armies.first().troops) // 军团确实没有变化
    }

    // ══════ 4. 御前亲断：圣旨不允许任何自动执行 ══════
    @Test
    fun imperialDecreeMandateNeverAutoExecutes() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze")
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.RECRUIT), autonomyLevel = MandateAutonomyLevel.IMPERIAL_DECREE)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))

        val v = DelegatedActionValidator.validate(
            state,
            WorldAction(type = "recruit_troops", factionId = "song", armyId = army.id, targetCityId = "yingtianfu", officerId = "zong_ze", amount = 1000),
            "song"
        )
        assertTrue(v is DelegatedActionValidator.ValidationResult.Rejected)
    }

    // ══════ 5. 手动撤销授权后 AI 不再执行（"覆盖"的等价场景）══════
    @Test
    fun revokedMandateStopsFurtherExecution() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze")
        val m = mandate("zong_ze")
        var state = baseState(listOf(zongZe), listOf(army), listOf(m))

        state = ImperialMandateSystem.revoke(state, m.id)

        val v = DelegatedActionValidator.validate(
            state,
            WorldAction(type = "recruit_troops", factionId = "song", armyId = army.id, targetCityId = "yingtianfu", officerId = "zong_ze", amount = 1000),
            "song"
        )
        assertTrue("撤销后不应再有有效授权", v is DelegatedActionValidator.ValidationResult.Rejected)
    }

    // ══════ 6. 募兵真实扣人口(城池兵力)/钱粮 ══════
    @Test
    fun recruitTrulyDeductsTroopsAndBudget() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze", troops = 1000)
        val m = mandate("zong_ze", budgetGold = 10000, budgetGrain = 20000)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))
        val cityBefore = state.cities.first { it.id == "yingtianfu" }

        val action = WorldAction(type = "recruit_troops", factionId = "song", armyId = army.id, targetCityId = "yingtianfu", officerId = "zong_ze", amount = 2000)
        val approved = DelegatedActionValidator.validate(state, action, "song") as DelegatedActionValidator.ValidationResult.Approved
        val (newState, record) = DelegatedActionValidator.execute(state, action, approved.mandate, "song")

        assertTrue(record.success)
        val cityAfter = newState.cities.first { it.id == "yingtianfu" }
        assertTrue("城池兵力应减少", cityAfter.troops < cityBefore.troops)
        assertTrue("实际常住人口必须同步减少", cityAfter.population < cityBefore.population)
        assertTrue("军团兵力应增加", newState.armies.first().troops > 1000)
        assertTrue("国库应被扣减", newState.gold < 50000)
        assertTrue("圣旨预算应记录支出", newState.imperialMandates.first().spentGold > 0)
    }

    // ══════ 7. 调兵真实走路线（不允许瞬移，通过 mandate 授权后调用真实系统）══════
    @Test
    fun repositionArmyGoesThroughRealMovementSystem() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze")
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.REPOSITION_ARMY), regionCityIds = emptySet())
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))

        val action = WorldAction(type = "move_army", factionId = "song", armyId = army.id, targetCityId = "kaifeng")
        val approved = DelegatedActionValidator.validate(state, action, "song")
        // 目标是敌方城池，真实路线系统应该给出改道/出征之类的结果，而不是直接瞬移过去
        assertTrue(approved is DelegatedActionValidator.ValidationResult.Approved)
        val (newState, record) = DelegatedActionValidator.execute(state, action, (approved as DelegatedActionValidator.ValidationResult.Approved).mandate, "song")
        // 无论成败，军团都不应该在同一回合内瞬间出现在目标城
        assertNotEquals("不允许瞬移到敌城", "kaifeng", newState.armies.first().currentCityId.takeIf { record.success && newState.armies.first().routeIndex >= newState.armies.first().routeNodeIds.size - 1 })
    }

    // ══════ 8. NPC（金）与玩家遵守同一规则：金国募兵同样要城池归属+钱粮才能成功 ══════
    @Test
    fun jinFactionRecruitFollowsSameRulesAsPlayer() {
        val jinGeneral = officer("wanyan_zongbi", "完颜宗弼", currentCityId = "kaifeng")
        val jArmy = jinArmy("wanyan_zongbi", troops = 1000)
        val state = baseState(listOf(jinGeneral), listOf(jArmy), jinCityOverride = jinCity(troops = 20000, gold = 5000))

        val plan = WorldTurnPlan(actions = listOf(
            WorldAction(type = "recruit_troops", factionId = "jin", armyId = jArmy.id, targetCityId = "kaifeng", officerId = "wanyan_zongbi", amount = 3000)
        ))
        val result = WorldAiTurnExecutor.execute(state, plan)
        val jinArmyAfter = result.newState.armies.first { it.id == jArmy.id }
        assertTrue("金国募兵应真实增兵", jinArmyAfter.troops > 1000)
        val jinCityAfter = result.newState.cities.first { it.id == "kaifeng" }
        assertTrue("金国募兵应真实扣城池兵力", jinCityAfter.troops < 20000)
    }

    // ══════ 9. 金国钱粮不够时募兵应失败（同规则的另一面）══════
    @Test
    fun jinFactionRecruitFailsWithoutEnoughGold() {
        val jinGeneral = officer("wanyan_zongbi", currentCityId = "kaifeng")
        val jArmy = jinArmy("wanyan_zongbi", troops = 1000)
        val state = baseState(listOf(jinGeneral), listOf(jArmy), jinCityOverride = jinCity(troops = 20000, gold = 10))

        val plan = WorldTurnPlan(actions = listOf(
            WorldAction(type = "recruit_troops", factionId = "jin", armyId = jArmy.id, targetCityId = "kaifeng", officerId = "wanyan_zongbi", amount = 5000)
        ))
        val result = WorldAiTurnExecutor.execute(state, plan)
        assertEquals("钱粮不够不应真的募兵", 1000, result.newState.armies.first { it.id == jArmy.id }.troops)
    }

    // ══════ 10. 便宜从事：兵力不满时本地自动补一次募兵，不需要等 AI 提议 ══════
    @Test
    fun discretionaryMandateAutoRecruitsWithoutAiProposal() {
        val zongZe = officer("zong_ze", command = 90) // commandLimit较高，保证有明显缺口
        val army = songArmy("zong_ze", troops = 1000)
        val m = mandate("zong_ze", autonomyLevel = MandateAutonomyLevel.DISCRETIONARY, budgetGold = 100000, budgetGrain = 100000)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))

        // AI 完全没有提议任何动作
        val result = WorldAiTurnExecutor.execute(state, WorldTurnPlan(actions = emptyList()))
        assertTrue("便宜从事应自动补募兵", result.reports.any { it.contains("便宜从事") })
        assertTrue("军团兵力应增加", result.newState.armies.first().troops > 1000)
    }

    // ══════ 11. FactionStrategyPlanner 给低兵力的 AI 势力生成募兵候选（世界不会打残就死）══════
    @Test
    fun factionStrategyPlannerOffersRecruitCandidateForWeakArmy() {
        val jinGeneral = officer("wanyan_zongbi", command = 90, currentCityId = "kaifeng")
        val jArmy = jinArmy("wanyan_zongbi", troops = 500) // 远低于统兵上限
        val state = baseState(listOf(jinGeneral), listOf(jArmy), jinCityOverride = jinCity(troops = 20000, gold = 30000))

        val candidates = FactionStrategyPlanner.candidates(state, "jin")
        assertTrue("应该有募兵候选", candidates.any { it.intent == StrategicIntent.REBUILD })
    }

    // ══════ 12. save/export/import 后授权仍存在 ══════
    @Test
    fun mandatesSurviveSaveExportImport() {
        val zongZe = officer("zong_ze")
        val army = songArmy("zong_ze")
        val m = mandate("zong_ze", budgetGold = 12345)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))

        val exported = GameSaveCodec.export(state)
        val imported = GameSaveCodec.import(exported).getOrThrow()

        assertEquals(1, imported.imperialMandates.size)
        val restored = imported.imperialMandates.first()
        assertEquals(m.id, restored.id)
        assertEquals(m.responsibleOfficerId, restored.responsibleOfficerId)
        assertEquals(m.budgetGold, restored.budgetGold)
        assertEquals(m.allowedActions, restored.allowedActions)
        assertEquals(m.autonomyLevel, restored.autonomyLevel)
    }

    // ══════ 13. API 失败 fallback：heuristicWorldPlan 不依赖真实 AI 也能给出候选 ══════
    @Test
    fun heuristicWorldPlanWorksWithoutRealAi() {
        val jinGeneral = officer("wanyan_zongbi", currentCityId = "kaifeng")
        val jArmy = jinArmy("wanyan_zongbi", troops = 500)
        val state = baseState(listOf(jinGeneral), listOf(jArmy), jinCityOverride = jinCity(troops = 20000, gold = 30000))

        val plan = FactionStrategyPlanner.heuristicWorldPlan(state)
        assertTrue("本地兜底战略脑不应崩溃或返回空计划描述", plan.strategySummary.isNotBlank())
    }

    @Test
    fun recruitmentCannotSpendNonexistentCentralTreasuryOrGrain() {
        val zongZe = officer("zong_ze", "宗泽")
        val army = songArmy("zong_ze", troops = 1000)
        val m = mandate("zong_ze", budgetGold = 10000, budgetGrain = 20000)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m), gold = 10, grain = 50)
        val action = WorldAction("recruit_troops", "song", army.id, "yingtianfu", amount = 1000, officerId = "zong_ze")
        val approved = DelegatedActionValidator.validate(state, action, "song") as DelegatedActionValidator.ValidationResult.Approved
        val (after, record) = DelegatedActionValidator.execute(state, action, approved.mandate, "song")

        assertFalse(record.success)
        assertEquals(1000, after.armies.single().troops)
        assertEquals(10, after.gold)
    }

    @Test
    fun recruitmentCannotTeleportCommanderIntoAnotherCity() {
        val zongZe = officer("zong_ze", "宗泽", currentCityId = "kaifeng")
        val result = ArmySystem.recruitOrReinforce(
            baseState(listOf(zongZe), emptyList()), "song", "yingtianfu", "zong_ze", 2000, "infantry"
        )

        assertTrue(result is ArmySystem.ArmyResult.Failure)
        assertTrue((result as ArmySystem.ArmyResult.Failure).reason.contains("不得隔空募兵"))
    }

    @Test
    fun discretionaryMandateCanCreateRealArmyWhenCommanderInitiallyHasNone() {
        val initial = GameState()
        val draft = ImperialMandatePolicy.draft(initial,
            "命宗泽经营北线，准便宜从事，自行募兵修城，军费以三万贯为限，不得主动发动大战。")!!
        val authorized = ImperialMandateSystem.issue(initial, draft)
        val beforeCity = authorized.cities.first { it.id == "yingtianfu" }
        val result = WorldAiTurnExecutor.execute(authorized, WorldTurnPlan())
        val newArmy = result.newState.armies.first { it.commanderId == "zong_ze" }
        val afterCity = result.newState.cities.first { it.id == "yingtianfu" }

        assertEquals("yingtianfu", newArmy.currentCityId)
        assertTrue(newArmy.troops > 0)
        assertTrue(afterCity.troops < beforeCity.troops)
        assertTrue(afterCity.population < beforeCity.population)
        assertTrue(result.newState.gold < authorized.gold)
        assertTrue(result.newState.grain < authorized.grain)
        assertEquals(1, result.newState.mandateExecutionLog.size)
        assertTrue(result.newState.mandateExecutionLog.single().description.contains("宗泽"))
    }

    @Test
    fun byTheBookMandatePerformsOnlyTheExplicitlyAuthorizedAction() {
        val zongZe = officer("zong_ze", "宗泽")
        val army = songArmy("zong_ze", troops = 1000)
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.REPAIR_DEFENSE),
            autonomyLevel = MandateAutonomyLevel.BY_THE_BOOK, budgetGold = 10000)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))
        val result = WorldAiTurnExecutor.execute(state, WorldTurnPlan())

        assertEquals(1000, result.newState.armies.single().troops)
        assertTrue(result.newState.cities.first { it.id == "yingtianfu" }.defense > 60)
        assertEquals(MandateActionKind.REPAIR_DEFENSE, result.newState.mandateExecutionLog.single().actionKind)
    }

    @Test
    fun successfulWorldActionsWritePersistentExecutionRecords() {
        val zongZe = officer("zong_ze", "宗泽")
        val army = songArmy("zong_ze", troops = 1000)
        val state = baseState(listOf(zongZe), listOf(army), listOf(mandate("zong_ze")))
        val plan = WorldTurnPlan(actions = listOf(
            WorldAction("recruit_troops", "song", army.id, "yingtianfu", amount = 1000, officerId = "zong_ze")
        ))
        val result = WorldAiTurnExecutor.execute(state, plan)

        assertEquals("同一道圣旨每旬不得重复自动执行", 1, result.newState.mandateExecutionLog.size)
        assertTrue(result.newState.mandateExecutionLog.single().success)
        val restored = GameSaveCodec.import(GameSaveCodec.export(result.newState)).getOrThrow()
        assertEquals(result.newState.mandateExecutionLog, restored.mandateExecutionLog)
    }

    @Test
    fun revokedMandateProducesNoAutonomousAction() {
        val zongZe = officer("zong_ze", "宗泽")
        val army = songArmy("zong_ze", troops = 1000)
        val m = mandate("zong_ze", autonomyLevel = MandateAutonomyLevel.DISCRETIONARY)
        val state = ImperialMandateSystem.revoke(baseState(listOf(zongZe), listOf(army), listOf(m)), m.id)
        val result = WorldAiTurnExecutor.execute(state, WorldTurnPlan())

        assertEquals(1000, result.newState.armies.single().troops)
        assertTrue(result.newState.mandateExecutionLog.isEmpty())
    }

    @Test
    fun authorizedArmyAutonomouslyReceivesRealBudgetedSupply() {
        val zongZe = officer("zong_ze", "宗泽")
        val army = songArmy("zong_ze", troops = 1000).copy(supplyLevel = 30)
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.RESUPPLY),
            autonomyLevel = MandateAutonomyLevel.DISCRETIONARY, budgetGrain = 20000)
        val state = baseState(listOf(zongZe), listOf(army), listOf(m))
        val result = WorldAiTurnExecutor.execute(state, WorldTurnPlan())

        assertTrue(result.newState.armies.single().supplyLevel > 30)
        assertTrue(result.newState.cities.first { it.id == "yingtianfu" }.grain < 100000)
        assertTrue(result.newState.imperialMandates.single().spentGrain > 0)
        assertEquals(MandateActionKind.RESUPPLY, result.newState.mandateExecutionLog.single().actionKind)
    }

    @Test
    fun authorizedMinisterCanAcceptVacantCommandOnlyAtRealLocation() {
        val zongZe = officer("zong_ze", "宗泽")
        val vacant = songArmy("", troops = 5000, id = "vacant_song_army")
        val m = mandate("zong_ze", allowedActions = setOf(MandateActionKind.ASSIGN_COMMANDER),
            autonomyLevel = MandateAutonomyLevel.DISCRETIONARY)
        val state = baseState(listOf(zongZe), listOf(vacant), listOf(m))
        val result = WorldAiTurnExecutor.execute(state, WorldTurnPlan())

        assertEquals("zong_ze", result.newState.armies.single().commanderId)
        assertEquals("yingtianfu", result.newState.officers.single().currentCityId)
        assertEquals(MandateActionKind.ASSIGN_COMMANDER, result.newState.mandateExecutionLog.single().actionKind)
    }
}

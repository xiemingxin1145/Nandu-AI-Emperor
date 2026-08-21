package com.xiemingxin.nandu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiemingxin.nandu.ai.AiEngineConfig
import com.xiemingxin.nandu.ai.AiProvider
import com.xiemingxin.nandu.ai.AiProviderType
import com.xiemingxin.nandu.ai.AiSettingsStore
import com.xiemingxin.nandu.ai.CityContext
import com.xiemingxin.nandu.ai.ClaudeProvider
import com.xiemingxin.nandu.ai.CustomApiProvider
import com.xiemingxin.nandu.ai.EdictResult
import com.xiemingxin.nandu.ai.ArmyContext
import com.xiemingxin.nandu.ai.GameContext
import com.xiemingxin.nandu.ai.GeminiProvider
import com.xiemingxin.nandu.ai.MockProvider
import com.xiemingxin.nandu.ai.OfficerContext
import com.xiemingxin.nandu.ai.OpenAiProvider
import com.xiemingxin.nandu.ai.OpenRouterProvider
import com.xiemingxin.nandu.ai.WorldContextFactory
import com.xiemingxin.nandu.ai.WorldPlanningProvider
import com.xiemingxin.nandu.game.AppointmentSystem
import com.xiemingxin.nandu.game.ArmyMovementSystem
import com.xiemingxin.nandu.game.ArmySupplySystem
import com.xiemingxin.nandu.game.ArmySystem
import com.xiemingxin.nandu.game.WarSystem
import com.xiemingxin.nandu.agent.CharacterAgentSystem
import com.xiemingxin.nandu.agent.AgentProposal
import com.xiemingxin.nandu.game.ArmyStatus
import com.xiemingxin.nandu.game.CharacterTravelSystem
import com.xiemingxin.nandu.game.OfficerIntel
import com.xiemingxin.nandu.game.AchievementSystem
import com.xiemingxin.nandu.game.BattleResolver
import com.xiemingxin.nandu.game.BattleUnitCatalog
import com.xiemingxin.nandu.game.BuildingCatalog
import com.xiemingxin.nandu.game.CityVisitAction
import com.xiemingxin.nandu.game.CouncilChoice
import com.xiemingxin.nandu.game.CouncilConsequenceSystem
import com.xiemingxin.nandu.game.CouncilScene
import com.xiemingxin.nandu.game.GameEnding
import com.xiemingxin.nandu.game.GameRuleEngine
import com.xiemingxin.nandu.game.GameSaveCodec
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.ImperialDecision
import com.xiemingxin.nandu.game.ImperialMandatePolicy
import com.xiemingxin.nandu.game.ImperialMandateSystem
import com.xiemingxin.nandu.game.LegacySystem
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.game.TavernSystem
import com.xiemingxin.nandu.game.VictoryJudge
import com.xiemingxin.nandu.game.WeatherSystem
import com.xiemingxin.nandu.game.WorldAiTurnExecutor
import com.xiemingxin.nandu.game.WorldPresentationPolicy
import com.xiemingxin.nandu.game.WorldTurnReplay
import com.xiemingxin.nandu.game.withUpdatedFactionStatus
import com.xiemingxin.nandu.story.EventDirector
import com.xiemingxin.nandu.story.StoryEvent
import com.xiemingxin.nandu.story.StoryEventEffectApplier
import com.xiemingxin.nandu.story.StoryEventLoader
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UiState(
    val gameState: GameState = GameState(),
    val phase: GamePhase = GamePhase.IDLE,
    val lastEdictResult: EdictResult? = null,
    val lastOutcomes: List<String> = emptyList(),
    val lastRejected: List<String> = emptyList(),
    val errorMessage: String? = null,
    val providerType: AiProviderType = AiProviderType.MOCK,
    val apiKey: String = "",
    val customModel: String = "",
    val providerStatusMessage: String = "离线 Mock 推演",
    val isRealAiEnabled: Boolean = false,
    val saveCode: String = "",
    val saveMessage: String = "",
    val currentStoryEvent: StoryEvent? = null,
    val storyOutcomes: List<String> = emptyList(),
    val battleReport: String? = null,
    val ending: GameEnding = GameEnding.ONGOING,
    val earnedAchievements: Set<String> = emptySet(),
    val newAchievement: String? = null,
    val lastVisitNarration: String? = null,
    val lastBattleOutcome: com.xiemingxin.nandu.game.BattleOutcome? = null,  // Stage 5 战报
    // Stage 8 Agent 提案
    val agentProposals: List<AgentProposal> = emptyList(),
    val imperialDecision: ImperialDecision = ImperialDecision(),
    val activeWorldReplay: WorldTurnReplay? = null,
    val lastWorldReplay: WorldTurnReplay? = null
)

enum class GamePhase { IDLE, AI_PROCESSING, AWAITING_CONFIRM, EXECUTING, SHOWING_RESULT }

class EmperorViewModel(application: Application) : AndroidViewModel(application) {

    private val aiSettingsStore = AiSettingsStore(application)
    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var currentProvider: AiProvider = MockProvider()
    private val storyEvents: List<StoryEvent> = StoryEventLoader.loadDefaultEvents(application)

    init {
        restoreAiConfigIntoState(GameState(), keepCurrentGame = false)
    }

    fun updateProviderSettings(type: AiProviderType, apiKey: String, customModel: String = "") {
        val config = AiEngineConfig(type, apiKey.trim(), customModel.trim())
        applyProviderConfig(config, persist = true, statusOverride = "AI 引擎已保存：${providerLabel(config)}")
    }

    fun testProviderConnection() {
        val state = _uiState.value
        val config = AiEngineConfig(state.providerType, state.apiKey, state.customModel)
        val keylessCustomReady = config.providerType == AiProviderType.CUSTOM && config.isRealAiEnabled
        if (config.providerType != AiProviderType.MOCK && config.apiKey.isBlank() && !keylessCustomReady) {
            _uiState.value = state.copy(saveMessage = "请先填写 API Key；若是免鉴权中转站，请选择“自定义API”并填写 Base URL 与模型名。")
            return
        }
        _uiState.value = state.copy(saveMessage = "正在叩问 ${config.providerType.displayName}……")
        viewModelScope.launch {
            val result = currentProvider.parseEdict("测试连接：请解析为无实际命令的朝堂问安。", buildGameContext(_uiState.value.gameState))
            _uiState.value = _uiState.value.copy(
                saveMessage = result.fold(
                    onSuccess = { "接口可用：${it.summary.ifBlank { "AI 已回应" }}" },
                    onFailure = { "接口失败：${it.message ?: "未知错误"}" }
                )
            )
        }
    }

    fun submitEdict(edictText: String) {
        if (edictText.isBlank()) return
        val state = _uiState.value.gameState
        _uiState.value = _uiState.value.copy(phase = GamePhase.AI_PROCESSING, errorMessage = null)
        viewModelScope.launch {
            val context = buildGameContext(state)
            val result = currentProvider.parseEdict(edictText, context)
            result.fold(
                onSuccess = { edictResult ->
                    _uiState.value = _uiState.value.copy(
                        phase = GamePhase.AWAITING_CONFIRM,
                        lastEdictResult = edictResult,
                        imperialDecision = ImperialDecision()
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE, errorMessage = "圣旨传达失败：${error.message}")
                }
            )
        }
    }

    fun confirmEdict(edictText: String) {
        val current = _uiState.value
        val edictResult = current.lastEdictResult ?: return
        val mandate = ImperialMandatePolicy.draft(current.gameState, edictText, current.imperialDecision.selectedOfficerIds)
        if (!current.imperialDecision.canExecute(edictResult, mandate != null)) {
            _uiState.value = current.copy(
                errorMessage = if (edictResult.clarificationNeeded) {
                    "圣意尚待补充，请先明确旨意后再行朱批。"
                } else {
                    "请先择定所采纳的臣议，并核对可执行命令。"
                }
            )
            return
        }
        val adoptedResult = edictResult.copy(
            npcResponses = edictResult.npcResponses.filter {
                it.officerId in current.imperialDecision.selectedOfficerIds
            }
        )
        _uiState.value = current.copy(phase = GamePhase.EXECUTING, errorMessage = null)
        val (imperialState, overrides) = ImperialMandatePolicy.prioritizeManualCommands(current.gameState, adoptedResult.commands)
        val executionResult = GameRuleEngine.executeEdict(imperialState, adoptedResult, edictText)
        val finalState = if (mandate != null) ImperialMandateSystem.issue(executionResult.newState, mandate) else executionResult.newState
        val mandateOutcome = mandate?.let {
            val name = finalState.officers.firstOrNull { officer -> officer.id == it.responsibleOfficerId }?.name ?: "受命之臣"
            "【长期授权】${name}${it.autonomyLevel.label}，可${it.allowedActions.joinToString("、") { action -> action.label }}；" +
                "军费上限${it.budgetGold}贯，${ImperialMandatePolicy.describeRestrictions(finalState, it)}。"
        }
        _uiState.value = _uiState.value.copy(
            gameState = finalState,
            lastOutcomes = overrides + listOfNotNull(mandateOutcome) + executionResult.outcomes,
            lastRejected = executionResult.rejectedCommands,
            phase = GamePhase.SHOWING_RESULT
        )
    }

    fun applyCouncilChoice(scene: CouncilScene, choice: CouncilChoice) {
        val current = _uiState.value
        val result = CouncilConsequenceSystem.apply(current.gameState, scene, choice)
        val newAch = AchievementSystem.checkNewAchievements(result.newState, current.earnedAchievements)
        _uiState.value = current.copy(
            gameState = result.newState,
            storyOutcomes = result.outcomes,
            earnedAchievements = current.earnedAchievements + newAch,
            newAchievement = newAch.firstOrNull() ?: current.newAchievement
        )
    }

    fun cancelEdict() {
        _uiState.value = _uiState.value.copy(
            phase = GamePhase.IDLE,
            lastEdictResult = null,
            imperialDecision = ImperialDecision(),
            errorMessage = null
        )
    }

    fun revokeImperialMandate(mandateId: String) {
        val current = _uiState.value
        val mandate = current.gameState.imperialMandates.firstOrNull { it.id == mandateId && it.isActive } ?: return
        val name = current.gameState.officers.firstOrNull { it.id == mandate.responsibleOfficerId }?.name ?: "受命之臣"
        _uiState.value = current.copy(
            gameState = ImperialMandateSystem.revoke(current.gameState, mandateId),
            saveMessage = "已收回${name}的${mandate.autonomyLevel.label}授权。"
        )
    }

    fun toggleCouncilOpinion(officerId: String) {
        val current = _uiState.value
        val responseExists = current.lastEdictResult?.npcResponses?.any { it.officerId == officerId } == true
        if (responseExists) {
            _uiState.value = current.copy(
                imperialDecision = current.imperialDecision.toggleOfficer(officerId),
                errorMessage = null
            )
        }
    }

    fun synthesizeCouncilOpinions() {
        val current = _uiState.value
        val result = current.lastEdictResult ?: return
        _uiState.value = current.copy(imperialDecision = current.imperialDecision.synthesize(result), errorMessage = null)
    }

    fun amendEdict() {
        val current = _uiState.value
        _uiState.value = current.copy(
            phase = GamePhase.IDLE,
            imperialDecision = current.imperialDecision.requestAmendment(),
            errorMessage = null
        )
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE)
    }

    fun buildInCity(cityId: String, buildingId: String) {
        val state = _uiState.value.gameState
        val city = state.cities.firstOrNull { it.id == cityId } ?: return
        val def = BuildingCatalog.byId(buildingId) ?: return
        val level = city.buildings[buildingId] ?: 0
        if (level >= def.maxLevel) return
        if (def.requireWaterNode && !city.isWaterNode) return
        val (goldCost, grainCost) = BuildingCatalog.upgradeCost(def, level)
        if (city.gold < goldCost || city.grain < grainCost) return

        var newDefense = city.defense
        var newCommerce = city.commerce
        var newAgriculture = city.agriculture
        var newSupport = city.popularSupport
        when (buildingId) {
            "city_wall" -> newDefense = (newDefense + 15).coerceAtMost(100)
            "market" -> newCommerce = (newCommerce + 10).coerceAtMost(100)
            "granary" -> newAgriculture = (newAgriculture + 10).coerceAtMost(100)
            "academy", "temple", "taoist_temple" -> newSupport = (newSupport + 6).coerceAtMost(100)
        }

        val newCity = city.copy(
            gold = city.gold - goldCost,
            grain = city.grain - grainCost,
            defense = newDefense,
            commerce = newCommerce,
            agriculture = newAgriculture,
            popularSupport = newSupport,
            buildings = city.buildings + (buildingId to level + 1)
        )
        val newCities = state.cities.map { if (it.id == cityId) newCity else it }
        _uiState.value = _uiState.value.copy(gameState = state.copy(cities = newCities))
    }

    fun siegeCity(targetCityId: String) {
        // Stage 5: 旧攻城入口，转发给 WarSystem（向后兼容）
        val state = _uiState.value.gameState
        val army = state.armies.firstOrNull {
            it.ownerFactionId == "song" &&
            (it.statusCode == ArmyStatus.ENGAGEMENT_PENDING || it.statusCode == ArmyStatus.GARRISONED)
        } ?: run {
            _uiState.value = _uiState.value.copy(battleReport = "无可用军团，无法发起攻势。")
            return
        }
        executeAttackCity(army.id, targetCityId)
    }

    fun visitCity(cityId: String, action: CityVisitAction) {
        val state = _uiState.value.gameState
        val city = state.cities.firstOrNull { it.id == cityId } ?: return
        if (city.owner != "song") {
            _uiState.value = _uiState.value.copy(lastVisitNarration = "此城尚未归宋，无从从容走访。")
            return
        }
        if (state.cityActionPoints <= 0) {
            _uiState.value = _uiState.value.copy(lastVisitNarration = "本旬精力已尽，城中行动力不足。待下一旬再走访。")
            return
        }
        if (state.gold < action.goldCost) {
            _uiState.value = _uiState.value.copy(lastVisitNarration = "府库不足，连${action.label}的花销都凑不齐。")
            return
        }

        val cityOfficers = state.officers.filter {
            it.currentCityId == cityId &&
                (it.status == OfficerStatus.HIDDEN || it.status == OfficerStatus.WANDERING) &&
                !state.talentLeads.contains(it.id)
        }

        val seed = (state.turn.toLong() * 1_000_003L) +
            (cityId.hashCode().toLong() and 0xFFFFL) * 31L +
            action.ordinal * 7L +
            state.rumors.size.toLong()

        val result = TavernSystem.resolveVisit(
            city = city,
            action = action,
            cityOfficers = cityOfficers,
            allCities = state.cities,
            turn = state.turn,
            seed = seed
        )

        val newGold = (state.gold + result.goldDelta).coerceAtLeast(0)
        val newPrestige = (state.prestige + result.prestigeDelta).coerceIn(0, 200)
        val newRumors = result.rumor?.let { state.rumors + it } ?: state.rumors
        val newLeads = if (result.talentLeadId.isNotBlank()) state.talentLeads + result.talentLeadId else state.talentLeads

        val narration = buildString {
            append(result.narrative)
            if (result.prestigeDelta != 0) append(" 名望${if (result.prestigeDelta > 0) "+" else ""}${result.prestigeDelta}。")
            if (result.goldDelta != 0) append(" 耗银${-result.goldDelta}。")
            if (result.talentLeadId.isNotBlank()) append(" 【觅得贤才线索】")
            result.rumor?.let { append("\n\n“${it.text}”") }
        }

        val newState = state.copy(
            gold = newGold,
            prestige = newPrestige,
            rumors = newRumors,
            talentLeads = newLeads,
            cityActionPoints = state.cityActionPoints - 1
        )
        _uiState.value = _uiState.value.copy(
            gameState = newState,
            lastVisitNarration = narration
        )
    }

    fun dismissVisitNarration() {
        _uiState.value = _uiState.value.copy(lastVisitNarration = null)
    }

    fun recruitInCity(cityId: String, unitId: String) {
        val state = _uiState.value.gameState
        val city = state.cities.firstOrNull { it.id == cityId } ?: return
        val def = BattleUnitCatalog.byId(unitId) ?: return
        if (city.gold < def.recruitGold || city.grain < def.recruitGrain) return
        val recruitSize = 1000
        if (city.population < recruitSize * 5) {
            _uiState.value = _uiState.value.copy(battleReport = "${city.name}丁口不足，民疲难征，无法再募兵。需待人口休养生息。")
            return
        }
        val supportPenalty = if (city.troops > city.population / 10) 3 else 1
        val newCity = city.copy(
            gold = city.gold - def.recruitGold,
            grain = city.grain - def.recruitGrain,
            troops = city.troops + recruitSize,
            population = city.population - recruitSize * 3,
            popularSupport = (city.popularSupport - supportPenalty).coerceAtLeast(0)
        )
        val newCities = state.cities.map { if (it.id == cityId) newCity else it }
        _uiState.value = _uiState.value.copy(gameState = state.copy(cities = newCities))
    }

    // Stage 5 ─────────────────────────────────────────────────────────────────
    fun executeAttackCity(armyId: String, targetCityId: String) {
        val state = _uiState.value.gameState
        val result = WarSystem.executeAttack(state, armyId, targetCityId)
        when (result) {
            is WarSystem.WarResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    gameState = result.newState,
                    lastBattleOutcome = result.outcome,
                    lastOutcomes = listOf(result.message)
                )
            }
            is WarSystem.WarResult.Failure -> {
                _uiState.value = _uiState.value.copy(lastRejected = listOf(result.reason))
            }
        }
    }

    fun executeRetreatArmy(armyId: String) {
        val state = _uiState.value.gameState
        val (newState, msg) = WarSystem.executeRetreat(state, armyId)
        _uiState.value = _uiState.value.copy(
            gameState = newState,
            lastOutcomes = listOf(msg)
        )
    }

    /** V1.0 活朝堂：召回外任/领军人物回京。真实赶路，不瞬移，见 AppointmentSystem.recallToCourt。 */
    fun recallOfficer(officerId: String) {
        val state = _uiState.value.gameState
        when (val result = AppointmentSystem.recallToCourt(state, officerId)) {
            is AppointmentSystem.AppointResult.Success -> {
                _uiState.value = _uiState.value.copy(
                    gameState = result.newState,
                    lastOutcomes = listOf(result.message)
                )
            }
            is AppointmentSystem.AppointResult.Failure -> {
                _uiState.value = _uiState.value.copy(lastRejected = listOf(result.reason))
            }
        }
    }

    fun dismissBattleReport() {
        _uiState.value = _uiState.value.copy(lastBattleOutcome = null, battleReport = null)
    }

    /**
     * Stage 6 唯一世界 Tick：
     *  - 有支持世界推演的真实模型：每旬只调用一次，让它给出非玩家势力行动 + NPC主动上奏；
     *  - 接口失败/超时/坏JSON：立即退回本地战略脑，不阻断游戏；
     *  - AI只提动作，WorldAiTurnExecutor 按本地规则验证和执行；
     *  - 随后再推进玩家军团行军、双方补给、事件与胜负判断。
     */
    fun advanceTurn() {
        val snapshot = _uiState.value
        if (snapshot.phase == GamePhase.AI_PROCESSING) return

        val state = snapshot.gameState
        val planner = currentProvider as? WorldPlanningProvider
        val useRemoteWorldAi = snapshot.isRealAiEnabled && planner != null

        _uiState.value = snapshot.copy(
            phase = GamePhase.AI_PROCESSING,
            storyOutcomes = listOf("【天下推演】诸势力正在议定本旬行动……"),
            errorMessage = null
        )

        viewModelScope.launch {
            val planningResult = if (useRemoteWorldAi) {
                planner!!.planWorldTurn(WorldContextFactory.fromState(state))
            } else {
                Result.failure(IllegalStateException("当前模型通道未启用世界推演"))
            }

            val remoteSucceeded = planningResult.isSuccess
            val plan = planningResult.getOrElse { WorldAiTurnExecutor.heuristicPlan(state) }
            val worldResult = WorldAiTurnExecutor.execute(state, plan)
            var working = worldResult.newState

            val worldReports = mutableListOf<String>()
            if (plan.strategySummary.isNotBlank()) {
                worldReports += if (remoteSucceeded) {
                    "【AI世界推演】${plan.strategySummary}"
                } else {
                    "【本地战略脑】${plan.strategySummary}"
                }
            }
            if (snapshot.isRealAiEnabled && !remoteSucceeded) {
                val reason = planningResult.exceptionOrNull()?.message.orEmpty().take(160)
                worldReports += "【AI自动降级】本旬世界模型未能完成推演，已无缝切换本地战略脑${if (reason.isBlank()) "。" else "：$reason"}"
            }
            worldReports += worldResult.reports
            worldResult.npcInitiatives.forEach { initiative ->
                val officerName = working.officers.firstOrNull { it.id == initiative.officerId }?.name ?: initiative.officerId
                val label = when (initiative.kind) {
                    "warning" -> "警奏"
                    "request" -> "请命"
                    "advice" -> "进言"
                    else -> "奏对"
                }
                worldReports += "【$label·$officerName】${initiative.text}"
            }

            // Stage 4 玩家军团战略 Tick：世界AI不越权替玩家行动。
            val marchResult = ArmyMovementSystem.tickAllArmies(working, tickDays = 10)
            working = marchResult.first
            val marchReports = marchResult.second

            val supplyResult = ArmySupplySystem.tickAllSupply(working)
            working = supplyResult.first
            val supplyReports = supplyResult.second

            val clearedFlags = working.storyFlags - "sieged_this_turn"
            val nextCalendar = working.calendar.advance()
            val advancedState = working.copy(
                turn = working.turn + 1,
                era = nextCalendar.eraName,
                calendar = nextCalendar,
                season = nextCalendar.season(),
                weather = WeatherSystem.generate(nextCalendar, working.turn + 1),
                storyFlags = clearedFlags,
                cityActionPoints = TavernSystem.MAX_ACTION_POINTS
            ).withUpdatedFactionStatus()

            // 活朝堂：召回入朝的人物在此检查是否已抵达临安，抵达才真正转 IN_COURT，不瞬移。
            val travelResult = CharacterTravelSystem.tickArrivals(advancedState)
            val stateAfterTravel = travelResult.first
            val travelReports = travelResult.second

            // V1.1 历史 Canon：处理预定状态迁移（如宗泽开局入对、随后外任转东京留守）。
            val scheduledResult = CharacterTravelSystem.tickScheduledTransitions(stateAfterTravel)
            val nextState = scheduledResult.first
            val scheduledReports = scheduledResult.second

            val event = EventDirector.selectForTurn(
                state = nextState,
                events = storyEvents,
                firedEventIds = nextState.firedEventIds,
                flags = nextState.storyFlags
            ).firstOrNull()
            val ending = VictoryJudge.judgeDefeat(nextState)
            val earned = _uiState.value.earnedAchievements
            val newAch = AchievementSystem.checkNewAchievements(nextState, earned)

            val reports = (worldReports + marchReports + supplyReports + travelReports + scheduledReports)
                .map { WorldPresentationPolicy.humanizeReport(nextState, it) }
            val replay = WorldPresentationPolicy.replay(state, nextState, reports)

            _uiState.value = _uiState.value.copy(
                gameState = nextState,
                phase = GamePhase.IDLE,
                lastOutcomes = emptyList(),
                lastRejected = emptyList(),
                currentStoryEvent = event,
                storyOutcomes = reports,
                activeWorldReplay = replay,
                lastWorldReplay = replay,
                ending = ending,
                earnedAchievements = earned + newAch,
                newAchievement = newAch.firstOrNull() ?: _uiState.value.newAchievement,
                providerStatusMessage = if (remoteSucceeded) {
                    "${snapshot.providerType.displayName} 世界AI已接管：每旬一次低成本推演"
                } else {
                    buildProviderStatus(AiEngineConfig(snapshot.providerType, snapshot.apiKey, snapshot.customModel))
                }
            )
        }
    }

    fun dismissAchievement() {
        _uiState.value = _uiState.value.copy(newAchievement = null)
    }

    fun abdicate() {
        val ending = VictoryJudge.judgeAbdication(_uiState.value.gameState)
        _uiState.value = _uiState.value.copy(ending = ending)
    }

    fun restartGame() {
        restoreAiConfigIntoState(GameState(), keepCurrentGame = false)
    }

    fun recordAndRestart(context: android.content.Context) {
        val cur = _uiState.value
        val songCities = cur.gameState.cities.count { it.owner == "song" }
        LegacySystem.recordReign(context, cur.earnedAchievements, songCities)
        val legacy = LegacySystem.load(context)
        val freshState = LegacySystem.applyLegacyBonus(GameState(), legacy)
        restoreAiConfigIntoState(freshState, keepCurrentGame = false)
    }

    fun chooseStoryOption(choiceId: String) {
        val event = _uiState.value.currentStoryEvent ?: return
        val state = _uiState.value.gameState
        val result = StoryEventEffectApplier.applyChoice(state, event, choiceId)
        // 可重复事件不进入已触发集合
        val newFired = if (event.repeatable) state.firedEventIds else state.firedEventIds + event.eventId
        val newFlags = state.storyFlags + result.flags
        val finalState = result.newState.copy(
            firedEventIds = newFired,
            storyFlags = newFlags
        )
        // 连锁事件：玩家做出选择后立刻触发后续事件
        val chainEvent = EventDirector.chainCandidates(event, storyEvents, newFired).firstOrNull()
        _uiState.value = _uiState.value.copy(
            gameState = finalState,
            currentStoryEvent = chainEvent,
            storyOutcomes = if (chainEvent == null) result.outcomes else emptyList()
        )
    }

    fun dismissStoryOutcome() {
        _uiState.value = _uiState.value.copy(storyOutcomes = emptyList())
    }

    fun dismissWorldReplay() {
        _uiState.value = _uiState.value.copy(activeWorldReplay = null, storyOutcomes = emptyList())
    }

    fun reopenWorldReplay() {
        val current = _uiState.value
        current.lastWorldReplay?.let { _uiState.value = current.copy(activeWorldReplay = it) }
    }

    fun exportSaveCode() {
        val code = GameSaveCodec.export(_uiState.value.gameState)
        _uiState.value = _uiState.value.copy(
            saveCode = code,
            saveMessage = "存档码已生成，复制保存即可。"
        )
    }

    fun importSaveCode(code: String) {
        GameSaveCodec.import(code).fold(
            onSuccess = { loaded ->
                _uiState.value = _uiState.value.copy(
                    gameState = loaded,
                    saveCode = code.trim(),
                    saveMessage = "读档成功：${loaded.calendar.displayText()}。",
                    phase = GamePhase.IDLE,
                    lastEdictResult = null,
                    lastOutcomes = emptyList(),
                    lastRejected = emptyList()
                )
            },
            onFailure = { error ->
                _uiState.value = _uiState.value.copy(saveMessage = "读档失败：${error.message ?: "存档码损坏"}")
            }
        )
    }

    private fun restoreAiConfigIntoState(gameState: GameState, keepCurrentGame: Boolean) {
        val saved = aiSettingsStore.load()
        currentProvider = createProvider(saved.providerType, saved.apiKey, saved.customModel)
        val current = _uiState.value
        _uiState.value = UiState(
            gameState = if (keepCurrentGame) current.gameState else gameState,
            providerType = saved.providerType,
            apiKey = saved.apiKey,
            customModel = saved.customModel,
            providerStatusMessage = buildProviderStatus(saved),
            isRealAiEnabled = saved.isRealAiEnabled
        )
    }

    private fun applyProviderConfig(config: AiEngineConfig, persist: Boolean, statusOverride: String? = null) {
        if (persist) aiSettingsStore.save(config)
        currentProvider = createProvider(config.providerType, config.apiKey, config.customModel)
        _uiState.value = _uiState.value.copy(
            providerType = config.providerType,
            apiKey = config.apiKey,
            customModel = config.customModel,
            providerStatusMessage = statusOverride ?: buildProviderStatus(config),
            isRealAiEnabled = config.isRealAiEnabled,
            saveMessage = statusOverride ?: buildProviderStatus(config)
        )
    }

    private fun createProvider(type: AiProviderType, apiKey: String, customModel: String): AiProvider {
        val customParts = parseCustomConfig(customModel)
        return when (type) {
            AiProviderType.CLAUDE -> ClaudeProvider(apiKey)
            AiProviderType.OPENAI -> OpenAiProvider(apiKey, customModel.ifEmpty { "gpt-4o-mini" })
            AiProviderType.GEMINI -> GeminiProvider(apiKey)
            AiProviderType.OPENROUTER -> OpenRouterProvider(apiKey, customModel.ifEmpty { "deepseek/deepseek-chat" })
            AiProviderType.CUSTOM -> CustomApiProvider(
                baseUrl = customParts.first.ifBlank { "https://api.example.com/v1" },
                apiKey = apiKey,
                model = customParts.second.ifBlank { "deepseek-chat" }
            )
            AiProviderType.MOCK -> MockProvider()
        }
    }

    private fun buildProviderStatus(config: AiEngineConfig): String = when {
        config.providerType == AiProviderType.MOCK -> "离线 Mock：圣旨和世界行动使用本地规则脑"
        config.providerType == AiProviderType.CUSTOM && config.isRealAiEnabled ->
            "自定义 OpenAI-compatible 引擎已启用：圣旨 + 每旬世界推演；API Key 可为空"
        config.apiKey.isBlank() -> "${config.providerType.displayName} 未填 Key，世界推演将使用本地战略脑"
        else -> "${config.providerType.displayName} 已启用：圣旨交给真实模型；支持时每旬驱动世界AI"
    }

    private fun providerLabel(config: AiEngineConfig): String = when {
        config.providerType == AiProviderType.MOCK -> "Mock 离线"
        config.customModel.isNotBlank() -> "${config.providerType.displayName} / ${config.customModel}"
        else -> config.providerType.displayName
    }

    private fun parseCustomConfig(value: String): Pair<String, String> {
        val parts = value.split("|", limit = 2)
        return if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to value.trim()
    }

    private fun buildGameContext(state: GameState): GameContext {
        // Stage 3：只把已知（非HIDDEN）人物暴露给AI，防止泄露隐藏人才
        val visibleStatuses = setOf(
            OfficerStatus.IN_COURT, OfficerStatus.DEPLOYED,
            OfficerStatus.WANDERING, OfficerStatus.SOLDIER,
            OfficerStatus.DISMISSED
        )
        // talentLeads 中的人物：玩家已发现但未征辟，也让AI知道（但只显示摸糊信息）
        val leadIds = state.talentLeads
        val officerContexts = state.officers
            .filter { o ->
                o.status in visibleStatuses ||
                (o.status == OfficerStatus.HIDDEN && o.id in leadIds)
            }
            .map { o ->
                val isLead = o.id in leadIds && o.status in setOf(
                    OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING
                )
                val role = AppointmentSystem.currentRole(state, o.id)
                val cmdSummary = when {
                    o.force >= 85 && o.command >= 85 -> "猛将(武${o.force}/统${o.command})"
                    o.command >= 85 -> "统帅(统${o.command}/谋${o.strategy})"
                    o.politics >= 85 -> "文臣(政${o.politics}/谋${o.strategy})"
                    o.strategy >= 85 -> "谋士(谋${o.strategy}/统${o.command})"
                    else -> "将吏(武${o.force}/政${o.politics})"
                }
                OfficerContext(
                    id = o.id,
                    name = o.name,
                    faction = o.faction,
                    currentCityId = o.currentCityId,
                    status = if (isLead) "LEAD_PENDING" else o.status.name,
                    currentRole = role,
                    commandSummary = cmdSummary,
                    loyaltyLabel = OfficerIntel.loyaltyLabel(o.loyalty),
                    isRecruitLead = isLead
                )
            }

        val pendingLeads = state.officers
            .filter { it.id in leadIds && it.status in setOf(OfficerStatus.HIDDEN, OfficerStatus.SOLDIER, OfficerStatus.WANDERING) }
            .map { o -> "${o.name}（${state.cities.find { c -> c.id == o.currentCityId }?.name ?: o.currentCityId}，待征辟）" }

        val armyContexts = state.armies
            .filter { it.ownerFactionId == "song" && it.statusCode != ArmyStatus.DISBANDED }
            .map { a ->
                val cmdName = state.officers.find { it.id == a.commanderId }?.name ?: a.name
                ArmyContext(
                    id = a.id,
                    name = a.name,
                    commanderName = cmdName,
                    commanderId = a.commanderId,
                    currentCityId = a.currentCityId,
                    troops = a.troops,
                    morale = a.morale,
                    supplyLevel = a.supplyLevel,
                    statusLabel = a.status,
                    targetCityId = a.targetCityId
                )
            }

        return GameContext(
            currentTurn = state.turn,
            era = "${state.calendar.displayText()} / ${state.season.label} / 天气${state.weather.label}",
            gold = state.gold,
            grain = state.grain,
            troopMorale = state.troopMorale,
            courtStability = state.courtStability,
            jinThreat = state.jinThreat,
            activeCities = state.cities.map { CityContext(it.id, it.name, it.owner, it.troops, it.defense) },
            availableOfficers = officerContexts,
            pendingRecruitLeads = pendingLeads,
            songArmies = armyContexts
        )
    }
}

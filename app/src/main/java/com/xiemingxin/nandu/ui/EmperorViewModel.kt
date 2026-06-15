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
import com.xiemingxin.nandu.ai.GameContext
import com.xiemingxin.nandu.ai.GeminiProvider
import com.xiemingxin.nandu.ai.MockProvider
import com.xiemingxin.nandu.ai.OfficerContext
import com.xiemingxin.nandu.ai.OpenAiProvider
import com.xiemingxin.nandu.ai.OpenRouterProvider
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
import com.xiemingxin.nandu.game.JinAI
import com.xiemingxin.nandu.game.LegacySystem
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.game.TavernSystem
import com.xiemingxin.nandu.game.VictoryJudge
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
    val lastVisitNarration: String? = null
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
        if (config.providerType != AiProviderType.MOCK && config.apiKey.isBlank()) {
            _uiState.value = state.copy(saveMessage = "请先填写 API Key，再叩问接口。")
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
                    _uiState.value = _uiState.value.copy(phase = GamePhase.AWAITING_CONFIRM, lastEdictResult = edictResult)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE, errorMessage = "圣旨传达失败：${error.message}")
                }
            )
        }
    }

    fun confirmEdict(edictText: String) {
        val edictResult = _uiState.value.lastEdictResult ?: return
        _uiState.value = _uiState.value.copy(phase = GamePhase.EXECUTING)
        val executionResult = GameRuleEngine.executeEdict(_uiState.value.gameState, edictResult, edictText)
        _uiState.value = _uiState.value.copy(
            gameState = executionResult.newState,
            lastOutcomes = executionResult.outcomes,
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
        _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE, lastEdictResult = null)
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
        val state = _uiState.value.gameState
        val target = state.cities.firstOrNull { it.id == targetCityId } ?: return
        if (target.owner == "song" && target.controlState == "STABLE") return

        if (state.storyFlags.contains("sieged_this_turn")) {
            _uiState.value = _uiState.value.copy(battleReport = "大军一旬之内已发动攻势，将士疲惫、粮道未稳，须待下一旬整备后再战。")
            return
        }
        val songGrain = state.cities.filter { it.owner == "song" }.sumOf { it.grain }
        val warGrainCost = 20000
        if (songGrain < warGrainCost) {
            _uiState.value = _uiState.value.copy(battleReport = "粮草不足两万石，大军无法出征。当务之急是屯田筹粮。")
            return
        }

        val songArmies = state.armies.filter { it.ownerFactionId == "song" }
        val attackerTroops = songArmies.sumOf { it.troops }.coerceAtLeast(5000)
        val avgMorale = if (songArmies.isNotEmpty()) songArmies.sumOf { it.morale } / songArmies.size else 50
        val commander = state.officers
            .filter { it.faction == "song" || it.faction.contains("战") }
            .maxByOrNull { it.command }
        val command = commander?.command ?: 60

        val outcome = BattleResolver.resolveSiege(
            attackerTroops = attackerTroops,
            attackerMorale = avgMorale,
            commanderCommand = command,
            city = target,
            season = state.season,
            weather = state.weather
        )

        val newOwner = if (outcome.newControlState == "STABLE" || outcome.newControlState == "FRONTLINE") {
            if (outcome.attackerWins && target.owner == "jin") "song" else target.owner
        } else target.owner
        val newTarget = target.copy(
            controlState = outcome.newControlState,
            owner = newOwner,
            troops = (target.troops - outcome.defenderLosses).coerceAtLeast(0)
        )

        val totalAtk = attackerTroops.coerceAtLeast(1)
        val moraleShift = if (outcome.attackerWins) 8 else -12
        val newArmies = state.armies.map { army ->
            if (army.ownerFactionId == "song") {
                val share = (outcome.attackerLosses.toDouble() * army.troops / totalAtk).toInt()
                army.copy(
                    troops = (army.troops - share).coerceAtLeast(0),
                    morale = (army.morale + moraleShift).coerceIn(10, 100)
                )
            } else army
        }

        var remainingCost = warGrainCost
        val citiesAfterGrain = state.cities.map { c ->
            if (c.owner == "song" && remainingCost > 0) {
                val deduct = minOf(c.grain, remainingCost)
                remainingCost -= deduct
                if (c.id == targetCityId) newTarget.copy(grain = (newTarget.grain - deduct).coerceAtLeast(0))
                else c.copy(grain = c.grain - deduct)
            } else if (c.id == targetCityId) newTarget else c
        }
        val newGameState = state.copy(
            cities = citiesAfterGrain,
            armies = newArmies,
            storyFlags = state.storyFlags + "sieged_this_turn"
        )
        val earned = _uiState.value.earnedAchievements
        val newAch = AchievementSystem.checkNewAchievements(newGameState, earned)
        _uiState.value = _uiState.value.copy(
            gameState = newGameState,
            battleReport = outcome.report,
            ending = VictoryJudge.judgeDefeat(newGameState),
            earnedAchievements = earned + newAch,
            newAchievement = newAch.firstOrNull() ?: _uiState.value.newAchievement
        )
    }

    fun dismissBattleReport() {
        _uiState.value = _uiState.value.copy(battleReport = null)
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

    fun advanceTurn() {
        val state = _uiState.value.gameState
        val jinResult = JinAI.executeTurn(state, state.jinThreat)
        var working = jinResult.newState
        val clearedFlags = working.storyFlags - "sieged_this_turn"
        val nextState = working.copy(
            turn = working.turn + 1,
            calendar = working.calendar.advance(),
            storyFlags = clearedFlags,
            cityActionPoints = TavernSystem.MAX_ACTION_POINTS
        )
        val event = EventDirector.selectForTurn(
            state = nextState,
            events = storyEvents,
            firedEventIds = nextState.firedEventIds,
            flags = nextState.storyFlags
        ).firstOrNull()
        val ending = VictoryJudge.judgeDefeat(nextState)
        val earned = _uiState.value.earnedAchievements
        val newAch = AchievementSystem.checkNewAchievements(nextState, earned)
        _uiState.value = _uiState.value.copy(
            gameState = nextState,
            phase = GamePhase.IDLE,
            lastOutcomes = emptyList(),
            lastRejected = emptyList(),
            currentStoryEvent = event,
            storyOutcomes = jinResult.reports,
            ending = ending,
            earnedAchievements = earned + newAch,
            newAchievement = newAch.firstOrNull() ?: _uiState.value.newAchievement
        )
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
            AiProviderType.OPENAI -> OpenAiProvider(apiKey, customModel.ifEmpty { "gpt-4o" })
            AiProviderType.GEMINI -> GeminiProvider(apiKey)
            AiProviderType.OPENROUTER -> OpenRouterProvider(apiKey, customModel.ifEmpty { "anthropic/claude-3.5-sonnet" })
            AiProviderType.CUSTOM -> CustomApiProvider(
                baseUrl = customParts.first.ifBlank { "https://api.example.com/v1" },
                apiKey = apiKey,
                model = customParts.second.ifBlank { "gpt-4o" }
            )
            AiProviderType.MOCK -> MockProvider()
        }
    }

    private fun buildProviderStatus(config: AiEngineConfig): String = when {
        config.providerType == AiProviderType.MOCK -> "离线 Mock 推演：可试玩，但不是真 AI 理解"
        config.apiKey.isBlank() -> "${config.providerType.displayName} 未填 Key，仍无法叩问真 AI"
        else -> "${config.providerType.displayName} 已启用：圣旨将交给真实模型解析"
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

    private fun buildGameContext(state: GameState) = GameContext(
        currentTurn = state.turn,
        era = "${state.calendar.displayText()} / ${state.season.label} / 天气${state.weather.label}",
        gold = state.gold,
        grain = state.grain,
        troopMorale = state.troopMorale,
        courtStability = state.courtStability,
        jinThreat = state.jinThreat,
        activeCities = state.cities.map { CityContext(it.id, it.name, it.owner, it.troops, it.defense) },
        availableOfficers = state.officers
            .filter { it.status == OfficerStatus.IN_COURT || it.status == OfficerStatus.DEPLOYED }
            .map { OfficerContext(it.id, it.name, it.faction, it.currentCityId, it.status.name) }
    )
}

package com.xiemingxin.nandu.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xiemingxin.nandu.ai.AiProvider
import com.xiemingxin.nandu.ai.AiProviderType
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
import com.xiemingxin.nandu.game.GameRuleEngine
import com.xiemingxin.nandu.game.GameSaveCodec
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.OfficerStatus
import com.xiemingxin.nandu.game.BuildingCatalog
import com.xiemingxin.nandu.game.BattleResolver
import com.xiemingxin.nandu.game.BattleUnitCatalog
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
    val saveCode: String = "",
    val saveMessage: String = "",
    val currentStoryEvent: StoryEvent? = null,
    val storyOutcomes: List<String> = emptyList(),
    val battleReport: String? = null
)

enum class GamePhase { IDLE, AI_PROCESSING, AWAITING_CONFIRM, EXECUTING, SHOWING_RESULT }

class EmperorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var currentProvider: AiProvider = MockProvider()

    // V0.7.1 剧情事件库（开局一次性加载）
    private val storyEvents: List<StoryEvent> = StoryEventLoader.loadJianyan01(application)

    fun updateProviderSettings(type: AiProviderType, apiKey: String, customModel: String = "") {
        val customParts = parseCustomConfig(customModel)
        currentProvider = when (type) {
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
        _uiState.value = _uiState.value.copy(providerType = type, apiKey = apiKey, customModel = customModel)
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

    fun cancelEdict() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE, lastEdictResult = null)
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE)
    }

    /** V0.9 城池营建：即时扣钱粮、升级建筑、应用效果 */
    fun buildInCity(cityId: String, buildingId: String) {
        val state = _uiState.value.gameState
        val city = state.cities.firstOrNull { it.id == cityId } ?: return
        val def = BuildingCatalog.byId(buildingId) ?: return
        val level = city.buildings[buildingId] ?: 0
        if (level >= def.maxLevel) return
        if (def.requireWaterNode && !city.isWaterNode) return
        val (goldCost, grainCost) = BuildingCatalog.upgradeCost(def, level)
        if (city.gold < goldCost || city.grain < grainCost) return

        // 应用建筑效果（直接调整城池属性）
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
        _uiState.value = _uiState.value.copy(
            gameState = state.copy(cities = newCities)
        )
    }

    /** V0.9 攻城战役：调集周边宋军攻打目标城，结算控制状态 */
    fun siegeCity(targetCityId: String) {
        val state = _uiState.value.gameState
        val target = state.cities.firstOrNull { it.id == targetCityId } ?: return
        if (target.owner == "song" && target.controlState == "STABLE") return

        // 集结：目标城周边的宋军 + 宋城驻军
        val songArmies = state.armies.filter { it.ownerFactionId == "song" }
        val attackerTroops = songArmies.sumOf { it.troops }.coerceAtLeast(5000)
        val avgMorale = if (songArmies.isNotEmpty()) songArmies.sumOf { it.morale } / songArmies.size else 50

        // 主将统率：取忠诚最高的在外武将
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

        // 更新目标城：控制状态、兵力损失、若克复则归宋
        val newOwner = if (outcome.newControlState == "STABLE" || outcome.newControlState == "FRONTLINE") {
            if (outcome.attackerWins && target.owner == "jin") "song" else target.owner
        } else target.owner
        val newTarget = target.copy(
            controlState = outcome.newControlState,
            owner = newOwner,
            troops = (target.troops - outcome.defenderLosses).coerceAtLeast(0)
        )

        // 攻方损失按比例分摊到各宋军
        val totalAtk = attackerTroops.coerceAtLeast(1)
        val newArmies = state.armies.map { army ->
            if (army.ownerFactionId == "song") {
                val share = (outcome.attackerLosses.toDouble() * army.troops / totalAtk).toInt()
                army.copy(troops = (army.troops - share).coerceAtLeast(0))
            } else army
        }

        val newCities = state.cities.map { if (it.id == targetCityId) newTarget else it }
        _uiState.value = _uiState.value.copy(
            gameState = state.copy(cities = newCities, armies = newArmies),
            battleReport = outcome.report
        )
    }

    fun dismissBattleReport() {
        _uiState.value = _uiState.value.copy(battleReport = null)
    }

    /** V0.9 募兵：在城池招募1000名指定兵种，扣金粮、增驻军 */
    fun recruitInCity(cityId: String, unitId: String) {
        val state = _uiState.value.gameState
        val city = state.cities.firstOrNull { it.id == cityId } ?: return
        val def = BattleUnitCatalog.byId(unitId) ?: return
        if (city.gold < def.recruitGold || city.grain < def.recruitGrain) return
        val newCity = city.copy(
            gold = city.gold - def.recruitGold,
            grain = city.grain - def.recruitGrain,
            troops = city.troops + 1000
        )
        val newCities = state.cities.map { if (it.id == cityId) newCity else it }
        _uiState.value = _uiState.value.copy(gameState = state.copy(cities = newCities))
    }

    /** V0.7.1 推进一旬：日历前进，并检查是否触发剧情事件 */
    fun advanceTurn() {
        val state = _uiState.value.gameState
        val nextState = state.copy(
            turn = state.turn + 1,
            calendar = state.calendar.advance()
        )
        // 用EventDirector筛出本旬可触发的第一个事件
        val event = EventDirector.firstCandidate(
            state = nextState,
            events = storyEvents,
            firedEventIds = nextState.firedEventIds,
            flags = nextState.storyFlags
        )
        _uiState.value = _uiState.value.copy(
            gameState = nextState,
            phase = GamePhase.IDLE,
            lastOutcomes = emptyList(),
            lastRejected = emptyList(),
            currentStoryEvent = event,
            storyOutcomes = emptyList()
        )
    }

    /** V0.7.1 玩家在剧情弹窗中做出选择 */
    fun chooseStoryOption(choiceId: String) {
        val event = _uiState.value.currentStoryEvent ?: return
        val state = _uiState.value.gameState
        val result = StoryEventEffectApplier.applyChoice(state, event, choiceId)
        // 记录已触发事件 + 累积flag
        val newFired = state.firedEventIds + event.eventId
        val newFlags = state.storyFlags + result.flags
        val finalState = result.newState.copy(
            firedEventIds = newFired,
            storyFlags = newFlags
        )
        _uiState.value = _uiState.value.copy(
            gameState = finalState,
            currentStoryEvent = null,
            storyOutcomes = result.outcomes
        )
    }

    /** 关闭剧情结果提示 */
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

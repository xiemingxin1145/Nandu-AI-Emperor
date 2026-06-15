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
import com.xiemingxin.nandu.game.JinAI
import com.xiemingxin.nandu.game.VictoryJudge
import com.xiemingxin.nandu.game.GameEnding
import com.xiemingxin.nandu.game.AchievementSystem
import com.xiemingxin.nandu.game.LegacySystem
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
    val battleReport: String? = null,
    val ending: GameEnding = GameEnding.ONGOING,
    val earnedAchievements: Set<String> = emptySet(),
    val newAchievement: String? = null
)

enum class GamePhase { IDLE, AI_PROCESSING, AWAITING_CONFIRM, EXECUTING, SHOWING_RESULT }

class EmperorViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var currentProvider: AiProvider = MockProvider()

    // V1.3.1：旧剧情库 + assets/data/events/history_events_v1.json 数据包
    private val storyEvents: List<StoryEvent> = StoryEventLoader.loadDefaultEvents(application)

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
        var newCity = city.copy(
            gold = city.gold - goldCost,
            grain = city.grain - grainCost,
            buildings = city.buildings + (buildingId to level + 1)
        )
        when (buildingId) {
            "city_wall" -> newCity = newCity.copy(defense = (newCity.defense + 15).coerceAtMost(100))
            "market" -> newCity = newCity.copy(commerce = (newCity.commerce + 10).coerceAtMost(100))
            "granary" -> newCity = newCity.copy(agriculture = (newCity.agriculture + 10).coerceAtMost(100))
            "academy", "temple", "taoist_temple" -> newCity = newCity.copy(popularSupport = (newCity.popularSupport + 5).coerceAtMost(100))
        }
        val newCities = state.cities.map { if (it.id == cityId) newCity else it }
        _uiState.value = _uiState.value.copy(gameState = state.copy(cities = newCities))
    }

    fun siegeCity(cityId: String) {
        val state = _uiState.value.gameState
        val result = BattleResolver.siege(state, cityId)
        _uiState.value = _uiState.value.copy(
            gameState = result.newState,
            battleReport = result.report
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

    /** V0.7.1 推进一旬：日历前进，并检查是否触发剧情事件 */
    fun advanceTurn() {
        val state = _uiState.value.gameState

        // V0.7 金军每旬战略行动（反攻/增兵/袭扰）
        val jinResult = JinAI.executeTurn(state, state.jinThreat)
        var working = jinResult.newState

        // 清除本旬攻势标记，新一旬可再次出征
        val clearedFlags = working.storyFlags - "sieged_this_turn"

        val nextState = working.copy(
            turn = working.turn + 1,
            calendar = working.calendar.advance(),
            storyFlags = clearedFlags
        )

        // EventDirector筛剧情事件
        val event = EventDirector.firstCandidate(
            state = nextState,
            events = storyEvents,
            firedEventIds = nextState.firedEventIds,
            flags = nextState.storyFlags
        )

        // V1.2 亡国判定（成就不结束游戏）
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

    /** V1.2 关闭成就庆祝弹窗 */
    fun dismissAchievement() {
        _uiState.value = _uiState.value.copy(newAchievement = null)
    }

    /** V1.2 主动禅位归隐（体面收场，按功业评庙号） */
    fun abdicate() {
        val ending = VictoryJudge.judgeAbdication(_uiState.value.gameState)
        _uiState.value = _uiState.value.copy(ending = ending)
    }

    /** V1.2 重开一局：记录传承后重置 */
    fun restartGame() {
        _uiState.value = UiState()
    }

    /** V1.2 带传承的重开：记录本局功业，应用先辈余荫 */
    fun recordAndRestart(context: android.content.Context) {
        val cur = _uiState.value
        val songCities = cur.gameState.cities.count { it.owner == "song" }
        LegacySystem.recordReign(context, cur.earnedAchievements, songCities)
        val legacy = LegacySystem.load(context)
        val freshState = LegacySystem.applyLegacyBonus(GameState(), legacy)
        _uiState.value = UiState(gameState = freshState)
    }

    /** V0.7.1 玩家在剧情弹窗中做出选择 */
    fun chooseStoryOption(choiceId: String) {
        val event = _uiState.value.currentStoryEvent ?: return
        val state = _uiState.value.gameState
        val result = StoryEventEffectApplier.applyChoice(state, event, choiceId)
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
                    errorMessage = null
                )
            },
            onFailure = { err ->
                _uiState.value = _uiState.value.copy(saveMessage = "读档失败：${err.message ?: "存档码无效"}")
            }
        )
    }

    private fun parseCustomConfig(raw: String): Pair<String, String> {
        val parts = raw.split("|", limit = 2)
        return if (parts.size == 2) parts[0].trim() to parts[1].trim() else "" to raw.trim()
    }

    private fun buildGameContext(state: GameState): GameContext {
        return GameContext(
            year = state.year,
            month = state.month,
            gold = state.gold,
            grain = state.grain,
            troopMorale = state.troopMorale,
            popularSupport = state.popularSupport,
            courtStability = state.courtStability,
            jinThreat = state.jinThreat,
            warFactionPower = state.warFactionPower,
            peaceFactionPower = state.peaceFactionPower,
            cities = state.cities.map { city ->
                CityContext(
                    id = city.id,
                    name = city.name,
                    owner = city.owner,
                    troops = city.troops,
                    defense = city.defense,
                    grain = city.grain,
                    gold = city.gold,
                    popularSupport = city.popularSupport
                )
            },
            officers = state.officers
                .filter { it.status != OfficerStatus.HIDDEN }
                .map { officer ->
                    OfficerContext(
                        id = officer.id,
                        name = officer.name,
                        position = officer.position,
                        faction = officer.faction,
                        loyalty = officer.loyalty,
                        command = officer.command,
                        politics = officer.politics,
                        stance = officer.stance,
                        status = officer.status.name
                    )
                },
            storyFlags = state.storyFlags.toList(),
            recentEvents = state.recentEvents
        )
    }
}

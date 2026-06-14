package com.xiemingxin.nandu.ui

import androidx.lifecycle.ViewModel
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
import com.xiemingxin.nandu.game.GameState
import com.xiemingxin.nandu.game.OfficerStatus
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
    val customModel: String = ""
)

enum class GamePhase {
    IDLE,
    AI_PROCESSING,
    AWAITING_CONFIRM,
    EXECUTING,
    SHOWING_RESULT
}

class EmperorViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState = _uiState.asStateFlow()

    private var currentProvider: AiProvider = MockProvider()

    fun updateProviderSettings(type: AiProviderType, apiKey: String, customModel: String = "") {
        currentProvider = when (type) {
            AiProviderType.CLAUDE -> ClaudeProvider(apiKey)
            AiProviderType.OPENAI -> OpenAiProvider(apiKey, customModel.ifEmpty { "gpt-4o" })
            AiProviderType.GEMINI -> GeminiProvider(apiKey)
            AiProviderType.OPENROUTER -> OpenRouterProvider(apiKey, customModel.ifEmpty { "anthropic/claude-sonnet-4-6" })
            AiProviderType.CUSTOM -> CustomApiProvider(customModel, apiKey, "")
            AiProviderType.MOCK -> MockProvider()
        }
        _uiState.value = _uiState.value.copy(
            providerType = type,
            apiKey = apiKey,
            customModel = customModel
        )
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
                        lastEdictResult = edictResult
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        phase = GamePhase.IDLE,
                        errorMessage = "圣旨传达失败：${error.message}"
                    )
                }
            )
        }
    }

    fun confirmEdict(edictText: String) {
        val edictResult = _uiState.value.lastEdictResult ?: return
        _uiState.value = _uiState.value.copy(phase = GamePhase.EXECUTING)

        val executionResult = GameRuleEngine.executeEdict(
            state = _uiState.value.gameState,
            edictResult = edictResult,
            edictText = edictText
        )

        _uiState.value = _uiState.value.copy(
            gameState = executionResult.newState,
            lastOutcomes = executionResult.outcomes,
            lastRejected = executionResult.rejectedCommands,
            phase = GamePhase.SHOWING_RESULT
        )
    }

    fun cancelEdict() {
        _uiState.value = _uiState.value.copy(
            phase = GamePhase.IDLE,
            lastEdictResult = null
        )
    }

    fun dismissResult() {
        _uiState.value = _uiState.value.copy(phase = GamePhase.IDLE)
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

package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.weight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiemingxin.nandu.game.CouncilChoice
import com.xiemingxin.nandu.game.CouncilConsequenceSystem
import com.xiemingxin.nandu.game.CouncilScene
import com.xiemingxin.nandu.game.ShunchangDirective
import com.xiemingxin.nandu.ui.EmperorViewModel

private val shunchangDirectiveScene = CouncilScene(
    id = "battle_shunchang_command",
    palaceId = CouncilConsequenceSystem.SHUNCHANG_BATTLE_COUNCIL_ID,
    title = "顺昌方向御前军议",
    summary = "皇帝对当前真实战区下达正式军令。",
    lines = emptyList(),
    choices = emptyList()
)

private fun directiveChoice(directive: ShunchangDirective): CouncilChoice = when (directive) {
    ShunchangDirective.HOLD -> CouncilChoice(
        id = "hold",
        label = directive.label,
        edictDraft = "着顺昌方向诸军固守城池，核实粮储，加固城垣。",
        preview = "动用真实粮草、城防与战区士气。"
    )
    ShunchangDirective.REINFORCE -> CouncilChoice(
        id = "reinforce",
        label = directive.label,
        edictDraft = "检点可调军团，择近路驰援顺昌，不得虚报兵马。",
        preview = "真实军团沿地图道路行军，不瞬移。"
    )
    ShunchangDirective.DELIBERATE -> CouncilChoice(
        id = "deliberate",
        label = directive.label,
        edictDraft = "命枢密院复核军情，暂缓定策。",
        preview = "不伪造调兵，但承担等待造成的军心与敌势代价。"
    )
}

/**
 * V1.6.2 STAB-001~003 顺昌战役正式入口。
 *
 * - STAB-001：入口由当前世界条件门控；
 * - STAB-002：日期、人物、官职、位置、兵力全部读取实时 GameState；
 * - STAB-003：固守/驰援/再议通过 BattleDirectiveSystem 真实回写世界并进入纪年存档。
 */
@Composable
fun ShunchangBattleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emperorViewModel: EmperorViewModel = viewModel()
    val uiState by emperorViewModel.uiState.collectAsState()

    Column(modifier = modifier.fillMaxSize()) {
        DynamicShunchangBriefingScreen(
            state = uiState.gameState,
            onBack = onBack,
            modifier = Modifier.weight(1f)
        )
        ShunchangDirectivePanel(
            state = uiState.gameState,
            feedback = uiState.storyOutcomes.lastOrNull(),
            onDirective = { directive ->
                emperorViewModel.applyCouncilChoice(
                    shunchangDirectiveScene,
                    directiveChoice(directive)
                )
            }
        )
    }
}

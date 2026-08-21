package com.xiemingxin.nandu.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiemingxin.nandu.ui.EmperorViewModel

/**
 * V1.6.2 STAB-001 + STAB-002 integration bridge.
 *
 * The old screen was an art-asset demo with a fixed "Jianyan 4" date, fixed Liu Qi / Yue Fei /
 * Han Shizhong roster, fixed troop counts and local-only decision buttons.  The production entry is
 * now gated by HistoricalBattleAvailability in PalaceHallScreen/MainActivity, while this screen
 * reads the same live EmperorViewModel state and delegates all presentation to the dynamic battle
 * briefing system.
 *
 * Keeping this thin compatibility wrapper avoids changing MainActivity's navigation API while
 * ensuring no direct call can fall back to the historical demo data.
 */
@Composable
fun ShunchangBattleScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emperorViewModel: EmperorViewModel = viewModel()
    val uiState by emperorViewModel.uiState.collectAsState()

    DynamicShunchangBriefingScreen(
        state = uiState.gameState,
        onBack = onBack,
        modifier = modifier
    )
}

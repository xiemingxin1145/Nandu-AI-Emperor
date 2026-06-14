package com.xiemingxin.nandu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiemingxin.nandu.ui.EmperorViewModel
import com.xiemingxin.nandu.ui.screens.*
import com.xiemingxin.nandu.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = InkBlack) {
                    NanduApp()
                }
            }
        }
    }
}

@Composable
fun NanduApp() {
    val viewModel: EmperorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()

    var showSettings by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }
    var edictText by remember { mutableStateOf("") }

    if (showSettings) {
        SettingsScreen(
            currentProvider = uiState.providerType,
            currentApiKey = uiState.apiKey,
            currentModel = uiState.customModel,
            onSave = { t, k, m -> viewModel.updateProviderSettings(t, k, m); showSettings = false },
            onBack = { showSettings = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(InkBlack)) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                0 -> EmperorMainScreen(
                    uiState = uiState,
                    onSubmitEdict = { text -> edictText = text; viewModel.submitEdict(text) },
                    onConfirmEdict = { viewModel.confirmEdict(edictText) },
                    onCancelEdict = { viewModel.cancelEdict() },
                    onDismissResult = { viewModel.dismissResult() },
                    onOpenSettings = { showSettings = true }
                )
                1 -> MapScreen(gameState = uiState.gameState, onCitySelected = { })
                2 -> StateScreen(gameState = uiState.gameState)
                3 -> MilitaryScreen(gameState = uiState.gameState)
            }
        }

        NavigationBar(containerColor = DeepBlack, contentColor = ImperialGold, tonalElevation = 0.dp) {
            listOf("朝议" to 0, "山河" to 1, "国政" to 2, "军务" to 3).forEach { (label, idx) ->
                NavigationBarItem(
                    selected = currentTab == idx,
                    onClick = { currentTab = idx },
                    icon = {},
                    label = {
                        Text(label, fontSize = 12.sp, fontWeight = if (currentTab == idx) FontWeight.Bold else FontWeight.Normal)
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = ImperialGold,
                        unselectedTextColor = Color(0xFF5A5A5A),
                        indicatorColor = Color(0xFF1E1508)
                    )
                )
            }
        }
    }
}

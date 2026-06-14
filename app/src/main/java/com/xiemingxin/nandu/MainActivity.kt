package com.xiemingxin.nandu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.xiemingxin.nandu.ai.AiProviderType
import com.xiemingxin.nandu.ui.EmperorViewModel
import com.xiemingxin.nandu.ui.GamePhase
import com.xiemingxin.nandu.ui.screens.EmperorMainScreen
import com.xiemingxin.nandu.ui.screens.InkBlack
import com.xiemingxin.nandu.ui.screens.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NanduTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = InkBlack
                ) {
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
    var currentEdictText by remember { mutableStateOf("") }

    if (showSettings) {
        SettingsScreen(
            currentProvider = uiState.providerType,
            currentApiKey = uiState.apiKey,
            currentModel = uiState.customModel,
            onSave = { type, key, model ->
                viewModel.updateProviderSettings(type, key, model)
                showSettings = false
            },
            onBack = { showSettings = false }
        )
    } else {
        EmperorMainScreen(
            uiState = uiState,
            onSubmitEdict = { text ->
                currentEdictText = text
                viewModel.submitEdict(text)
            },
            onConfirmEdict = { viewModel.confirmEdict(currentEdictText) },
            onCancelEdict = { viewModel.cancelEdict() },
            onDismissResult = { viewModel.dismissResult() },
            onOpenSettings = { showSettings = true }
        )
    }
}

@Composable
fun NanduTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

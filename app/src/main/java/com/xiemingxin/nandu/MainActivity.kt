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
import com.xiemingxin.nandu.game.City
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

    fun draftFromCity(payload: String) {
        val parts = payload.split("|", limit = 2)
        val cityId = parts.getOrNull(0).orEmpty()
        val action = parts.getOrNull(1) ?: "auto"
        val city = uiState.gameState.cities.firstOrNull { it.id == cityId } ?: return
        edictText = buildCityDraft(city, action)
        currentTab = 0
    }

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
                    draftEdictText = edictText,
                    onSubmitEdict = { text -> edictText = text; viewModel.submitEdict(text) },
                    onConfirmEdict = { viewModel.confirmEdict(edictText) },
                    onCancelEdict = { viewModel.cancelEdict() },
                    onDismissResult = { viewModel.dismissResult() },
                    onOpenSettings = { showSettings = true }
                )
                1 -> MapScreen(gameState = uiState.gameState, onCitySelected = { payload -> draftFromCity(payload) })
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

private fun buildCityDraft(city: City, action: String): String {
    return when (action) {
        "repair" -> "传朕旨意：拨钱粮修缮${city.name}城防，增筑敌楼，整备弓弩，命守臣十日内具报修城进度。"
        "dispatch" -> "传朕旨意：命附近诸军向${city.name}方向集结，择忠勇将领统辖，严禁扰民，粮道未明不得轻进。"
        "grain" -> "传朕旨意：令转运司优先向${city.name}调拨粮草五万石，沿途州县护送，不得盘剥百姓。"
        "attack" -> if (city.owner == "jin") {
            "传朕旨意：命岳飞与韩世忠会同诸军，详查${city.name}敌情，先断其粮道，再择机进取。赵鼎筹措军粮，李纲整饬沿途城防，不得轻敌冒进。"
        } else {
            "传朕旨意：令${city.name}守臣整军备战，清点兵粮，修整军械，作为江防与北伐转运之基。"
        }
        else -> when {
            city.owner == "jin" -> "传朕旨意：命岳飞与韩世忠会同诸军，详查${city.name}敌情，先断其粮道，再择机进取。赵鼎筹措军粮，李纲整饬沿途城防，不得轻敌冒进。"
            city.controlState == "FRONTLINE" || city.controlState == "CONTESTED" -> "传朕旨意：即刻加固${city.name}城防，调拨粮草五万石，命附近将领整军待命。若金军南犯，坚壁清野，以守为攻。"
            city.defense < 65 -> "传朕旨意：拨钱粮修缮${city.name}城防，增筑敌楼，整备弓弩，十日内报修城进度。"
            city.troops < 15000 -> "传朕旨意：为${city.name}增补守军一万，责令地方筹粮，不得扰民，三旬内稳住城防。"
            else -> "传朕旨意：令${city.name}守臣清点兵粮，安抚百姓，整备军械，作为后续北伐与江防转运之基。"
        }
    }
}

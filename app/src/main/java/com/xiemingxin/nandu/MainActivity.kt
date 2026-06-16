package com.xiemingxin.nandu

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.xiemingxin.nandu.game.GameEnding
import com.xiemingxin.nandu.game.AchievementSystem
import androidx.compose.ui.platform.LocalContext
import com.xiemingxin.nandu.game.AudioResourceRegistry
import com.xiemingxin.nandu.ui.EmperorViewModel
import com.xiemingxin.nandu.ui.screens.*
import com.xiemingxin.nandu.ui.theme.*

private const val AUDIO_PREFS = "nandu_audio_settings"
private const val AUDIO_ENABLED_KEY = "audio_enabled"
private const val BGM_VOLUME_KEY = "bgm_volume"
private const val SFX_VOLUME_KEY = "sfx_volume"

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
private fun GameAudioController(
    showIntro: Boolean,
    ending: GameEnding,
    currentTab: Int,
    inCity: Boolean,
    inPalaceTask: Boolean,
    battleSignal: String?,
    sfxSignal: String?,
    audioEnabled: Boolean,
    bgmVolume: Float,
    sfxVolume: Float
) {
    val player = com.xiemingxin.nandu.ui.components.rememberGameAudioPlayer()
    val safeBgmVolume = if (audioEnabled) bgmVolume.coerceIn(0f, 1f) else 0f
    val safeSfxVolume = if (audioEnabled) sfxVolume.coerceIn(0f, 1f) else 0f
    val scene = when {
        ending != GameEnding.ONGOING && ending.rank == "亡" -> "defeat"
        ending != GameEnding.ONGOING -> "victory"
        showIntro -> "main_menu"
        inCity -> "city"
        inPalaceTask -> "council"
        else -> when (currentTab) {
            0 -> "palace"
            1 -> "court"
            2 -> "map"
            3 -> "court"
            4 -> "military"
            else -> "court"
        }
    }
    com.xiemingxin.nandu.ui.components.PlayBgmEffect(
        path = AudioResourceRegistry.bgmForScene(scene),
        sceneKey = scene,
        volume = safeBgmVolume,
        player = player
    )
    com.xiemingxin.nandu.ui.components.PlaySfxEffect(
        path = AudioResourceRegistry.Sfx.drumWar,
        triggerKey = battleSignal,
        volume = safeSfxVolume * 0.85f,
        variant = true,
        player = player
    )
    com.xiemingxin.nandu.ui.components.PlaySfxEffect(
        path = AudioResourceRegistry.Sfx.battleStart,
        triggerKey = battleSignal,
        volume = safeSfxVolume * 0.7f,
        player = player
    )
    com.xiemingxin.nandu.ui.components.PlaySfxEffect(
        path = AudioResourceRegistry.sfxForGameEvent(sfxSignal?.substringBefore(":").orEmpty()),
        triggerKey = sfxSignal,
        volume = safeSfxVolume,
        variant = true,
        player = player
    )
}

@Composable
fun NanduApp() {
    val viewModel: EmperorViewModel = viewModel()
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val audioPrefs = remember(context) { context.getSharedPreferences(AUDIO_PREFS, Context.MODE_PRIVATE) }

    var showIntro by remember { mutableStateOf(true) }
    var showPrologue by remember { mutableStateOf(false) }
    var interiorCityId by remember { mutableStateOf<String?>(null) }
    var activePalaceId by remember { mutableStateOf<String?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var currentTab by remember { mutableStateOf(0) }
    var edictText by remember { mutableStateOf("") }
    var sfxSignal by remember { mutableStateOf<String?>(null) }
    var audioEnabled by remember { mutableStateOf(audioPrefs.getBoolean(AUDIO_ENABLED_KEY, true)) }
    var bgmVolume by remember { mutableStateOf(audioPrefs.getFloat(BGM_VOLUME_KEY, 0.7f).coerceIn(0f, 1f)) }
    var sfxVolume by remember { mutableStateOf(audioPrefs.getFloat(SFX_VOLUME_KEY, 0.74f).coerceIn(0f, 1f)) }

    fun saveAudioSettings(enabled: Boolean, bgm: Float, sfx: Float) {
        audioPrefs.edit()
            .putBoolean(AUDIO_ENABLED_KEY, enabled)
            .putFloat(BGM_VOLUME_KEY, bgm.coerceIn(0f, 1f))
            .putFloat(SFX_VOLUME_KEY, sfx.coerceIn(0f, 1f))
            .apply()
    }

    fun playSfx(event: String) {
        sfxSignal = "$event:${System.nanoTime()}"
    }

    GameAudioController(
        showIntro = showIntro,
        ending = uiState.ending,
        currentTab = currentTab,
        inCity = interiorCityId != null,
        inPalaceTask = activePalaceId != null,
        battleSignal = uiState.battleReport,
        sfxSignal = sfxSignal,
        audioEnabled = audioEnabled,
        bgmVolume = bgmVolume,
        sfxVolume = sfxVolume
    )

    if (uiState.ending != GameEnding.ONGOING) {
        val songCities = uiState.gameState.cities.count { it.owner == "song" }
        EndingScreen(
            ending = uiState.ending,
            controlledCities = songCities,
            onRestart = {
                playSfx("confirm")
                viewModel.recordAndRestart(context)
                showIntro = true
                showPrologue = false
                interiorCityId = null
                activePalaceId = null
                currentTab = 0
            }
        )
        return
    }

    // ── 主菜单 ──────────────────────────────────────────
    if (showIntro && !showPrologue) {
        MainMenuScreen(
            onNewGame  = { playSfx("confirm"); showPrologue = true },
            onContinue = { playSfx("confirm"); showIntro = false },
            onSettings = { playSfx("open_panel"); showSettings = true },
            onExit     = { finish() }
        )
        return
    }

    // ── 序章（开辟新局后播放）───────────────────────────
    if (showPrologue) {
        IntroScreen(onStart = {
            playSfx("confirm")
            showPrologue = false
            showIntro = false
        })
        return
    }

    interiorCityId?.let { cid ->
        val city = uiState.gameState.cities.firstOrNull { it.id == cid }
        if (city != null) {
            CityInteriorScreen(
                city = city,
                actionPoints = uiState.gameState.cityActionPoints,
                prestige = uiState.gameState.prestige,
                rumors = uiState.gameState.rumors,
                lastVisitNarration = uiState.lastVisitNarration,
                onBuild = { buildingId -> playSfx("city_build"); viewModel.buildInCity(cid, buildingId) },
                onRecruit = { unitId -> playSfx("city_recruit"); viewModel.recruitInCity(cid, unitId) },
                onVisit = { action -> playSfx("open_panel"); viewModel.visitCity(cid, action) },
                onDismissVisitNarration = { playSfx("close_panel"); viewModel.dismissVisitNarration() },
                onBack = { playSfx("close_panel"); interiorCityId = null; viewModel.dismissVisitNarration() }
            )
            return
        }
    }

    fun draftFromCity(payload: String) {
        val parts = payload.split("|", limit = 2)
        val cityId = parts.getOrNull(0).orEmpty()
        val action = parts.getOrNull(1) ?: "auto"
        if (action.startsWith("build:")) {
            playSfx("city_build")
            viewModel.buildInCity(cityId, action.removePrefix("build:"))
            return
        }
        if (action == "siege") {
            playSfx("military_start")
            viewModel.siegeCity(cityId)
            return
        }
        if (action.startsWith("recruit:")) {
            playSfx("city_recruit")
            viewModel.recruitInCity(cityId, action.removePrefix("recruit:"))
            return
        }
        if (action == "enter") {
            playSfx("open_panel")
            interiorCityId = cityId
            return
        }
        val city = uiState.gameState.cities.firstOrNull { it.id == cityId } ?: return
        playSfx("edict_submitted")
        edictText = buildCityDraft(city, action)
        activePalaceId = null
        currentTab = 1
    }

    if (showSettings) {
        SettingsScreen(
            currentProvider = uiState.providerType,
            currentApiKey = uiState.apiKey,
            currentModel = uiState.customModel,
            saveCode = uiState.saveCode,
            saveMessage = uiState.saveMessage,
            audioEnabled = audioEnabled,
            bgmVolume = bgmVolume,
            sfxVolume = sfxVolume,
            onAudioSettingsChanged = { enabled, bgm, sfx ->
                audioEnabled = enabled
                bgmVolume = bgm.coerceIn(0f, 1f)
                sfxVolume = sfx.coerceIn(0f, 1f)
                saveAudioSettings(audioEnabled, bgmVolume, sfxVolume)
                playSfx(if (enabled) "confirm" else "cancel")
            },
            onSave = { t, k, m -> playSfx("confirm"); viewModel.updateProviderSettings(t, k, m) },
            onTestConnection = { playSfx("open_panel"); viewModel.testProviderConnection() },
            onExportSave = { playSfx("confirm"); viewModel.exportSaveCode() },
            onImportSave = { code -> playSfx("confirm"); viewModel.importSaveCode(code) },
            onBack = { playSfx("close_panel"); showSettings = false }
        )
        return
    }

    activePalaceId?.let { palaceId ->
        PalaceTasksScreen(
            state = uiState.gameState,
            palaceId = palaceId,
            onBack = { playSfx("close_panel"); activePalaceId = null },
            onCouncilChoice = { scene, choice -> playSfx("council_choice"); viewModel.applyCouncilChoice(scene, choice) },
            onDraftEdict = { draft ->
                playSfx("edict_submitted")
                edictText = draft
                activePalaceId = null
                currentTab = 1
            },
            onOpenTab = { tab ->
                playSfx("switch_tab")
                activePalaceId = null
                currentTab = tab
            }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().background(InkBlack)) {
        Box(modifier = Modifier.weight(1f)) {
            when (currentTab) {
                0 -> PalaceHallScreen(
                    state = uiState.gameState,
                    aiStatus = uiState.providerStatusMessage,
                    isRealAiEnabled = uiState.isRealAiEnabled,
                    onOpenSettings = { playSfx("open_panel"); showSettings = true },
                    onOpenPalace = { palaceId -> playSfx("council_open"); activePalaceId = palaceId },
                    onNavigate = { tab -> playSfx("switch_tab"); currentTab = tab + 1 }
                )
                1 -> EmperorMainScreen(
                    uiState = uiState,
                    draftEdictText = edictText,
                    onSubmitEdict = { text -> playSfx("edict_submitted"); edictText = text; viewModel.submitEdict(text) },
                    onConfirmEdict = { playSfx("edict_confirmed"); viewModel.confirmEdict(edictText) },
                    onCancelEdict = { playSfx("edict_cancelled"); viewModel.cancelEdict() },
                    onDismissResult = { playSfx("turn_advance"); viewModel.dismissResult() },
                    onAdvanceTurn = { playSfx("turn_advance"); viewModel.advanceTurn() },
                    onStoryChoice = { choiceId -> playSfx("story_event"); viewModel.chooseStoryOption(choiceId) },
                    onDismissStoryOutcome = { playSfx("story_outcome"); viewModel.dismissStoryOutcome() },
                    onOpenSettings = { playSfx("open_panel"); showSettings = true }
                )
                2 -> MapScreen(gameState = uiState.gameState, onCitySelected = { payload -> draftFromCity(payload) })
                3 -> StateScreen(gameState = uiState.gameState)
                4 -> MilitaryScreen(gameState = uiState.gameState)
            }
        }

        NavigationBar(containerColor = DeepBlack, contentColor = ImperialGold, tonalElevation = 0.dp) {
            listOf("皇宫" to 0, "朝议" to 1, "山河" to 2, "国政" to 3, "军务" to 4).forEach { (label, idx) ->
                NavigationBarItem(
                    selected = currentTab == idx,
                    onClick = { playSfx("switch_tab"); activePalaceId = null; currentTab = idx },
                    icon = {},
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (currentTab == idx) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedTextColor = ImperialGold,
                        unselectedTextColor = Color(0xFF5A5A5A),
                        indicatorColor = Color(0xFF1E1508)
                    )
                )
            }
        }

        uiState.newAchievement?.let { achId ->
            val ach = AchievementSystem.byId(achId)
            if (ach != null) {
                Dialog(onDismissRequest = { playSfx("close_panel"); viewModel.dismissAchievement() }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1508)),
                        border = BorderStroke(2.dp, Color(0xFFFFD700)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                        ) {
                            Text("✦ 功 业 达 成 ✦", color = Color(0xFFFFD700), fontSize = 14.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(ach.title, color = Color(0xFFFFD700), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(10.dp))
                            Text(ach.desc, color = Color(0xFFE8DCC0), fontSize = 13.sp)
                            Spacer(Modifier.height(18.dp))
                            Button(
                                onClick = { playSfx("confirm"); viewModel.dismissAchievement() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD700))
                            ) { Text("铭 记 此 功", color = InkBlack, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }
        }

        uiState.battleReport?.let { report ->
            Dialog(onDismissRequest = { playSfx("close_panel"); viewModel.dismissBattleReport() }) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1508)),
                    border = BorderStroke(1.dp, ImperialGold),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("【军 报】", color = ImperialGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(10.dp))
                        Text(report, color = Color(0xFFE8DCC0), fontSize = 13.sp)
                        Spacer(Modifier.height(14.dp))
                        Button(
                            onClick = { playSfx("confirm"); viewModel.dismissBattleReport() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = ImperialGold)
                        ) { Text("朕已阅", color = InkBlack, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

private fun buildCityDraft(city: City, action: String): String {
    return when (action) {
        "repair" -> "传朕旨意：修缮${city.name}城防，整备守具，十日内具报。"
        "dispatch" -> "传朕旨意：命附近诸军向${city.name}集结，择将统辖，先稳粮道。"
        "grain" -> "传朕旨意：令转运司向${city.name}调粮，沿途州县护送。"
        "attack" -> "传朕旨意：详查${city.name}敌情，集诸军择机进取，粮道未稳不得轻进。"
        else -> when {
            city.owner == "jin" -> "传朕旨意：详查${city.name}敌情，先断粮道，再议进取。"
            city.controlState == "FRONTLINE" || city.controlState == "CONTESTED" -> "传朕旨意：加固${city.name}城防，调拨粮草，令诸军整备。"
            city.defense < 65 -> "传朕旨意：拨钱粮修缮${city.name}城防，十日内报进度。"
            city.troops < 15000 -> "传朕旨意：为${city.name}增补守军，先稳城防。"
            else -> "传朕旨意：令${city.name}清点兵粮，安抚百姓，整备军械。"
        }
    }
}
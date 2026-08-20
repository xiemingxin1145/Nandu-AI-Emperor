package com.xiemingxin.nandu.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xiemingxin.nandu.ai.AiProviderType
import com.xiemingxin.nandu.ui.theme.*

@Composable
fun SettingsScreen(
    currentProvider: AiProviderType,
    currentApiKey: String,
    currentModel: String,
    saveCode: String,
    saveMessage: String,
    audioEnabled: Boolean = true,
    bgmVolume: Float = 0.7f,
    sfxVolume: Float = 0.74f,
    onAudioSettingsChanged: (Boolean, Float, Float) -> Unit = { _, _, _ -> },
    onSave: (AiProviderType, String, String) -> Unit,
    onTestConnection: () -> Unit,
    onExportSave: () -> Unit,
    onImportSave: (String) -> Unit,
    onBack: () -> Unit
) {
    val initialBase = if (currentModel.contains("|")) currentModel.substringBefore("|") else ""
    val initialModel = if (currentModel.contains("|")) currentModel.substringAfter("|") else currentModel

    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var baseUrl by remember { mutableStateOf(initialBase) }
    var modelName by remember { mutableStateOf(initialModel) }
    var importCode by remember(saveCode) { mutableStateOf(saveCode) }
    var localAudioEnabled by remember { mutableStateOf(audioEnabled) }
    var localBgmVolume by remember { mutableStateOf(bgmVolume.coerceIn(0f, 1f)) }
    var localSfxVolume by remember { mutableStateOf(sfxVolume.coerceIn(0f, 1f)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← ", color = ImperialGold, fontSize = 22.sp, modifier = Modifier.clickable { onBack() })
            Column {
                Text("AI 引擎中枢", color = ImperialGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("圣旨解析 / 天下推演 / 任意中转站 / 存档 / 音频", color = Color(0xFF8B7355), fontSize = 11.sp)
            }
        }

        InfoBox(
            title = "Stage 6 · AI 原生天下",
            body = "同一个便宜模型既能理解圣旨，也会在每旬替非玩家势力做一次战略决策。AI只负责‘想’，兵力、行军、补给、城池和胜负仍由本地规则引擎裁决，所以不需要昂贵模型硬算整个世界。"
        )
        InfoBox(
            title = "省钱策略",
            body = "每旬世界AI最多调用模型1次、最多提出4个行动。接口超时、坏JSON或余额不足时，会自动切到本地战略脑继续游戏，不会卡死在等待模型。"
        )

        SectionTitle("一、选择模型通道")
        AiProviderType.entries.forEach { provider ->
            ProviderCard(
                provider = provider,
                selected = selectedProvider == provider,
                onClick = {
                    selectedProvider = provider
                    when (provider) {
                        AiProviderType.OPENAI -> if (modelName.isBlank()) modelName = "gpt-4o-mini"
                        AiProviderType.OPENROUTER -> if (modelName.isBlank()) modelName = "deepseek/deepseek-chat"
                        AiProviderType.CUSTOM -> {
                            if (baseUrl.isBlank()) baseUrl = "https://你的中转站域名/v1"
                            if (modelName.isBlank()) modelName = "deepseek-chat"
                        }
                        else -> Unit
                    }
                }
            )
        }

        if (selectedProvider != AiProviderType.MOCK) {
            SectionTitle("二、接口参数")
            StyledTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = if (selectedProvider == AiProviderType.CUSTOM) "API Key（免鉴权中转可留空）" else "API Key",
                placeholder = if (selectedProvider == AiProviderType.CUSTOM) "sk-... / 可留空" else "sk-...",
                isPassword = true
            )

            when (selectedProvider) {
                AiProviderType.OPENAI -> {
                    StyledTextField(modelName, { modelName = it }, "模型名", "gpt-4o-mini / 你的可用小模型")
                    HintText("官方地址固定；默认选小模型，世界推演不要求旗舰模型。")
                }
                AiProviderType.OPENROUTER -> {
                    StyledTextField(modelName, { modelName = it }, "模型名", "deepseek/deepseek-chat / qwen/... / 其他便宜模型")
                    HintText("OpenRouter 可自由换低价模型；只要能稳定返回 JSON 即可。")
                }
                AiProviderType.CUSTOM -> {
                    StyledTextField(baseUrl, { baseUrl = it }, "Base URL", "https://你的中转站域名/v1")
                    StyledTextField(modelName, { modelName = it }, "模型名", "deepseek-chat / qwen-plus / 站内任意模型名")
                    HintText("兼容 OpenAI /chat/completions。Base URL 可填到 /v1，也可直接填完整 /chat/completions；免鉴权服务的 Key 可以留空。")
                    HintText("适合各种模型中转站、自建网关、局域网 OpenAI-compatible 服务。")
                }
                AiProviderType.CLAUDE -> HintText("Claude 官方通道可继续解析圣旨；若想让 Claude 也驱动天下，可经 OpenAI-compatible 中转站接入。")
                AiProviderType.GEMINI -> HintText("Gemini 官方协议仍在排期；现在可用支持 Gemini 的 OpenAI-compatible 中转站。")
                AiProviderType.MOCK -> Unit
            }

            Text(
                "Key 优先由 Android Keystore / EncryptedSharedPreferences 本机保存，不写入 GitHub。",
                color = Color(0xFF5A8A5A),
                fontSize = 11.sp
            )
        }

        SectionTitle("三、低成本快速预设")
        PresetRow(
            onMock = { selectedProvider = AiProviderType.MOCK },
            onOpenAi = {
                selectedProvider = AiProviderType.OPENAI
                modelName = "gpt-4o-mini"
            },
            onOpenRouter = {
                selectedProvider = AiProviderType.OPENROUTER
                modelName = "deepseek/deepseek-chat"
            },
            onCustom = {
                selectedProvider = AiProviderType.CUSTOM
                baseUrl = "https://你的中转站域名/v1"
                modelName = "deepseek-chat"
            }
        )

        val savedModel = if (selectedProvider == AiProviderType.CUSTOM) {
            "${baseUrl.trim()}|${modelName.trim()}"
        } else {
            modelName.trim()
        }

        Button(
            onClick = { onSave(selectedProvider, apiKey.trim(), savedModel) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ImperialRed),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text("保存并启用 AI 天下", color = Color.White, fontWeight = FontWeight.Bold)
        }

        OutlinedButton(
            onClick = {
                onSave(selectedProvider, apiKey.trim(), savedModel)
                onTestConnection()
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold)
        ) {
            Text("测试接口", fontWeight = FontWeight.Bold)
        }

        if (saveMessage.isNotBlank()) {
            Text(saveMessage, color = ImperialGold, fontSize = 12.sp, lineHeight = 17.sp)
        }

        SectionTitle("四、存档码")
        InfoBox(
            "手机存档说明",
            "点导出后复制整段 NANDU_SAVE_V1 开头的存档码。换手机、重装后，把存档码粘回来即可恢复天下状态。"
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onExportSave,
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4A2D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("导出存档", color = Color.White, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = { onImportSave(importCode) },
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold)
            ) {
                Text("导入存档", fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = importCode,
            onValueChange = { importCode = it },
            label = { Text("存档码", color = Color(0xFF8B7355)) },
            placeholder = { Text("导出后这里会出现存档码，也可粘贴旧存档码导入", color = Color(0xFF5A4A38), fontSize = 12.sp) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp),
            minLines = 5,
            maxLines = 8,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ImperialGold,
                unfocusedBorderColor = Color(0xFF4A3728),
                focusedTextColor = XuanCream,
                unfocusedTextColor = XuanCream,
                cursorColor = ImperialGold
            )
        )

        SectionTitle("五、音频设置")
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF120D07)),
            border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("开启音频", color = ImperialGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("关闭后 BGM 与音效都会静音。", color = Color(0xFF8B7355), fontSize = 11.sp)
                    }
                    Switch(
                        checked = localAudioEnabled,
                        onCheckedChange = {
                            localAudioEnabled = it
                            onAudioSettingsChanged(localAudioEnabled, localBgmVolume, localSfxVolume)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = ImperialGold)
                    )
                }
                AudioSliderRow("BGM 音量", localBgmVolume, enabled = localAudioEnabled) {
                    localBgmVolume = it
                    onAudioSettingsChanged(localAudioEnabled, localBgmVolume, localSfxVolume)
                }
                AudioSliderRow("音效音量", localSfxVolume, enabled = localAudioEnabled) {
                    localSfxVolume = it
                    onAudioSettingsChanged(localAudioEnabled, localBgmVolume, localSfxVolume)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = ImperialGold, fontSize = 14.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun InfoBox(title: String, body: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF120D07)),
        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = ImperialGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(body, color = XuanCream, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun AudioSliderRow(label: String, value: Float, enabled: Boolean, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = XuanCream, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("${(value * 100).toInt()}%", color = ImperialGold, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = ImperialGold, activeTrackColor = ImperialGold)
        )
    }
}

@Composable
private fun ProviderCard(provider: AiProviderType, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) ImperialGold else Color(0xFF3A2A1A)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .border(if (selected) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1E1508) else Color(0xFF0D0A04), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(selectedColor = ImperialGold)
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(provider.displayName, color = XuanCream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(providerDescription(provider), color = Color(0xFF8B7355), fontSize = 11.sp)
        }
    }
}

private fun providerDescription(provider: AiProviderType): String = when (provider) {
    AiProviderType.CLAUDE -> "官方 Anthropic：圣旨解析；世界AI建议经中转兼容层接入"
    AiProviderType.OPENAI -> "官方 OpenAI：默认小模型，可驱动圣旨与每旬天下推演"
    AiProviderType.GEMINI -> "官方协议排期中；可通过兼容中转接 Gemini"
    AiProviderType.OPENROUTER -> "聚合路由：优先选择便宜模型，支持世界AI"
    AiProviderType.CUSTOM -> "任意 OpenAI-compatible 中转站 / 自建网关，最灵活"
    AiProviderType.MOCK -> "无需 Key：本地规则脑，断网也能继续玩"
}

@Composable
private fun StyledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color(0xFF8B7355)) },
        placeholder = { Text(placeholder, color = Color(0xFF5A4A38), fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ImperialGold,
            unfocusedBorderColor = Color(0xFF4A3728),
            focusedTextColor = XuanCream,
            unfocusedTextColor = XuanCream,
            cursorColor = ImperialGold
        ),
        singleLine = true
    )
}

@Composable
private fun HintText(text: String) {
    Text(text, color = Color(0xFF8B7355), fontSize = 11.sp, lineHeight = 16.sp)
}

@Composable
private fun PresetRow(
    onMock: () -> Unit,
    onOpenAi: () -> Unit,
    onOpenRouter: () -> Unit,
    onCustom: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetButton("Mock离线", Modifier.weight(1f), onMock)
            PresetButton("OpenAI Mini", Modifier.weight(1f), onOpenAi)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetButton("OpenRouter低价", Modifier.weight(1f), onOpenRouter)
            PresetButton("自定义中转站", Modifier.weight(1f), onCustom)
        }
    }
}

@Composable
private fun PresetButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold)
    ) {
        Text(text, fontSize = 12.sp)
    }
}

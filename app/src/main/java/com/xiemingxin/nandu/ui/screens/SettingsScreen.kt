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

    Column(
        modifier = Modifier.fillMaxSize().background(InkBlack).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← ", color = ImperialGold, fontSize = 22.sp, modifier = Modifier.clickable { onBack() })
            Column {
                Text("AI 引擎中枢", color = ImperialGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("模型通道 / API Key / 存档码 / 手机端调试", color = Color(0xFF8B7355), fontSize = 11.sp)
            }
        }

        InfoBox(
            title = "当前策略",
            body = "Mock 可离线测试；真实模型会把圣旨解析为 JSON，再交给本地规则引擎裁决。API Key 经本机加密保存，不进入 GitHub。"
        )

        SectionTitle("一、选择模型通道")
        AiProviderType.entries.forEach { provider ->
            ProviderCard(
                provider = provider,
                selected = selectedProvider == provider,
                onClick = {
                    selectedProvider = provider
                    if (provider == AiProviderType.OPENAI && modelName.isBlank()) modelName = "gpt-4o"
                    if (provider == AiProviderType.OPENROUTER && modelName.isBlank()) modelName = "anthropic/claude-3.5-sonnet"
                    if (provider == AiProviderType.CUSTOM && baseUrl.isBlank()) baseUrl = "https://你的中转站域名/v1"
                }
            )
        }

        if (selectedProvider != AiProviderType.MOCK) {
            SectionTitle("二、接口参数")
            StyledTextField(apiKey, { apiKey = it }, "API Key", "sk-...", isPassword = true)
            when (selectedProvider) {
                AiProviderType.OPENAI -> {
                    StyledTextField(modelName, { modelName = it }, "模型名", "gpt-4o / gpt-4.1 / 你的可用模型")
                    HintText("官方地址固定为 https://api.openai.com/v1/chat/completions")
                }
                AiProviderType.OPENROUTER -> {
                    StyledTextField(modelName, { modelName = it }, "模型名", "anthropic/claude-3.5-sonnet / openai/gpt-4o / deepseek/deepseek-chat")
                    HintText("OpenRouter 地址固定为 https://openrouter.ai/api/v1/chat/completions")
                }
                AiProviderType.CUSTOM -> {
                    StyledTextField(baseUrl, { baseUrl = it }, "Base URL", "https://你的中转站域名/v1")
                    StyledTextField(modelName, { modelName = it }, "模型名", "中转站给你的模型名，如 gpt-4o / claude-3-5-sonnet / deepseek-chat")
                    HintText("系统会自动请求：Base URL + /chat/completions。")
                }
                AiProviderType.CLAUDE -> HintText("Claude 官方通道当前使用 Anthropic Messages 接口。模型名暂固定，后续会开放选择。")
                AiProviderType.GEMINI -> HintText("Gemini 官方接口还在排期；现在可用自定义 OpenAI-compatible 中转接 Gemini。")
                AiProviderType.MOCK -> Unit
            }
            Text("API Key 由 Android Keystore/EncryptedSharedPreferences 本机保存；失败机型会降级但不上传。", color = Color(0xFF5A8A5A), fontSize = 11.sp)
        }

        SectionTitle("三、快速预设")
        PresetRow(
            onMock = { selectedProvider = AiProviderType.MOCK },
            onOpenAi = { selectedProvider = AiProviderType.OPENAI; modelName = "gpt-4o" },
            onOpenRouter = { selectedProvider = AiProviderType.OPENROUTER; modelName = "anthropic/claude-3.5-sonnet" },
            onCustom = { selectedProvider = AiProviderType.CUSTOM; baseUrl = "https://你的中转站域名/v1"; modelName = "gpt-4o" }
        )

        val savedModel = if (selectedProvider == AiProviderType.CUSTOM) "${baseUrl.trim()}|${modelName.trim()}" else modelName.trim()
        Button(
            onClick = { onSave(selectedProvider, apiKey.trim(), savedModel) },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ImperialRed),
            shape = RoundedCornerShape(8.dp)
        ) { Text("保存并启用", color = Color.White, fontWeight = FontWeight.Bold) }

        OutlinedButton(
            onClick = {
                onSave(selectedProvider, apiKey.trim(), savedModel)
                onTestConnection()
            },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.65f)),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold)
        ) { Text("叩问接口 / 测试连接", fontWeight = FontWeight.Bold) }

        if (saveMessage.isNotBlank()) Text(saveMessage, color = ImperialGold, fontSize = 12.sp, lineHeight = 17.sp)

        SectionTitle("四、存档码")
        InfoBox("手机存档说明", "点导出后复制整段 NANDU_SAVE_V1 开头的存档码。换手机、重装、刷新后，把存档码粘回来点导入即可。")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = onExportSave,
                modifier = Modifier.weight(1f).height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D4A2D)),
                shape = RoundedCornerShape(8.dp)
            ) { Text("导出存档", color = Color.White, fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { onImportSave(importCode) },
                modifier = Modifier.weight(1f).height(44.dp),
                border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.55f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold)
            ) { Text("导入存档", fontWeight = FontWeight.Bold) }
        }
        OutlinedTextField(
            value = importCode,
            onValueChange = { importCode = it },
            label = { Text("存档码", color = Color(0xFF8B7355)) },
            placeholder = { Text("点击导出后这里会出现存档码，也可粘贴旧存档码导入", color = Color(0xFF5A4A38), fontSize = 12.sp) },
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
    }
}

@Composable
private fun SectionTitle(text: String) { Text(text, color = ImperialGold, fontSize = 14.sp, fontWeight = FontWeight.Bold) }

@Composable
private fun InfoBox(title: String, body: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF120D07)), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.35f)), shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = ImperialGold, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(body, color = XuanCream, fontSize = 12.sp, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun ProviderCard(provider: AiProviderType, selected: Boolean, onClick: () -> Unit) {
    val borderColor = if (selected) ImperialGold else Color(0xFF3A2A1A)
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
            .border(if (selected) 1.dp else 0.5.dp, borderColor, RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFF1E1508) else Color(0xFF0D0A04), RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = ImperialGold))
        Spacer(Modifier.width(8.dp))
        Column {
            Text(provider.displayName, color = XuanCream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(providerDescription(provider), color = Color(0xFF8B7355), fontSize = 11.sp)
        }
    }
}

private fun providerDescription(provider: AiProviderType): String = when (provider) {
    AiProviderType.CLAUDE -> "官方 Anthropic，适合圣旨理解和群臣辩论"
    AiProviderType.OPENAI -> "官方 OpenAI-compatible，已接入 /chat/completions"
    AiProviderType.GEMINI -> "官方 Gemini 排期中，可先用中转站"
    AiProviderType.OPENROUTER -> "聚合路由，便宜模型和多模型可选"
    AiProviderType.CUSTOM -> "自定义 OpenAI-compatible 中转站，最适合便宜接口"
    AiProviderType.MOCK -> "无需 Key，本地模拟，开发测试最稳"
}

@Composable
private fun StyledTextField(value: String, onValueChange: (String) -> Unit, label: String, placeholder: String, isPassword: Boolean = false) {
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
private fun HintText(text: String) { Text(text, color = Color(0xFF8B7355), fontSize = 11.sp, lineHeight = 16.sp) }

@Composable
private fun PresetRow(onMock: () -> Unit, onOpenAi: () -> Unit, onOpenRouter: () -> Unit, onCustom: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetButton("Mock离线", Modifier.weight(1f), onMock)
            PresetButton("OpenAI", Modifier.weight(1f), onOpenAi)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PresetButton("OpenRouter", Modifier.weight(1f), onOpenRouter)
            PresetButton("中转站", Modifier.weight(1f), onCustom)
        }
    }
}

@Composable
private fun PresetButton(text: String, modifier: Modifier, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = modifier.height(40.dp), border = BorderStroke(1.dp, ImperialGold.copy(alpha = 0.5f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = ImperialGold)) {
        Text(text, fontSize = 12.sp)
    }
}

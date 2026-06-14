package com.xiemingxin.nandu.ui.screens

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

@Composable
fun SettingsScreen(
    currentProvider: AiProviderType,
    currentApiKey: String,
    currentModel: String,
    onSave: (AiProviderType, String, String) -> Unit,
    onBack: () -> Unit
) {
    var selectedProvider by remember { mutableStateOf(currentProvider) }
    var apiKey by remember { mutableStateOf(currentApiKey) }
    var customModel by remember { mutableStateOf(currentModel) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(InkBlack)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("← ", color = ImperialGold, fontSize = 20.sp,
                modifier = Modifier.clickable { onBack() })
            Text("AI引擎设置", color = ImperialGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        // Provider选择
        Text("选择AI模型", color = XuanCream, fontSize = 14.sp)

        AiProviderType.entries.forEach { provider ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { selectedProvider = provider }
                    .border(
                        width = if (selectedProvider == provider) 1.dp else 0.5.dp,
                        color = if (selectedProvider == provider) ImperialGold else Color(0xFF3A2A1A),
                        shape = RoundedCornerShape(4.dp)
                    )
                    .background(
                        if (selectedProvider == provider) Color(0xFF1E1508) else Color(0xFF0D0A04),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedProvider == provider,
                    onClick = { selectedProvider = provider },
                    colors = RadioButtonDefaults.colors(selectedColor = ImperialGold)
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(provider.displayName, color = XuanCream, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Text(
                        when (provider) {
                            AiProviderType.CLAUDE -> "推荐。最准确的圣旨理解"
                            AiProviderType.OPENAI -> "GPT-4o，效果优秀"
                            AiProviderType.GEMINI -> "Google模型，免费额度较多"
                            AiProviderType.OPENROUTER -> "聚合路由，可选便宜模型"
                            AiProviderType.CUSTOM -> "自定义API地址"
                            AiProviderType.MOCK -> "无需API Key，本地离线模拟"
                        },
                        color = Color(0xFF8B7355),
                        fontSize = 11.sp
                    )
                }
            }
        }

        // API Key输入（MOCK不需要）
        if (selectedProvider != AiProviderType.MOCK) {
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it },
                label = { Text("API Key", color = Color(0xFF8B7355)) },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ImperialGold,
                    unfocusedBorderColor = Color(0xFF4A3728),
                    focusedTextColor = XuanCream,
                    unfocusedTextColor = XuanCream
                ),
                singleLine = true
            )

            // 自定义模型名（OpenRouter/Custom需要）
            if (selectedProvider == AiProviderType.OPENROUTER || selectedProvider == AiProviderType.CUSTOM) {
                OutlinedTextField(
                    value = customModel,
                    onValueChange = { customModel = it },
                    label = { Text("模型名 / Base URL", color = Color(0xFF8B7355)) },
                    placeholder = { Text("如：anthropic/claude-sonnet-4-6", color = Color(0xFF5A4A38)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImperialGold,
                        unfocusedBorderColor = Color(0xFF4A3728),
                        focusedTextColor = XuanCream,
                        unfocusedTextColor = XuanCream
                    ),
                    singleLine = true
                )
            }

            Text(
                "⚠ API Key仅存储于本机，不上传服务器",
                color = Color(0xFF5A8A5A),
                fontSize = 11.sp
            )
        }

        Button(
            onClick = { onSave(selectedProvider, apiKey, customModel) },
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ImperialRed)
        ) {
            Text("保存设置", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

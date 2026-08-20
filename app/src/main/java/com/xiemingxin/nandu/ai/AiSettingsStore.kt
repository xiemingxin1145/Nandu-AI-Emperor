package com.xiemingxin.nandu.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * AI 引擎本机配置库。
 *
 * 自定义中转仍沿用 "baseUrl|model" 存在 customModel 中，保持旧版本设置兼容；
 * Stage 6 起 CUSTOM 允许 API Key 为空，方便接 Ollama/LM Studio/局域网网关/免鉴权中转。
 */
data class AiEngineConfig(
    val providerType: AiProviderType = AiProviderType.MOCK,
    val apiKey: String = "",
    val customModel: String = ""
) {
    val isRealAiEnabled: Boolean
        get() = when (providerType) {
            AiProviderType.MOCK -> false
            AiProviderType.CUSTOM -> {
                val parts = customModel.split("|", limit = 2)
                parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()
            }
            else -> apiKey.isNotBlank()
        }
}

class AiSettingsStore(context: Context) {
    private val appContext = context.applicationContext

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(appContext)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            EncryptedSharedPreferences.create(
                appContext,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (_: Exception) {
            // 部分国产 ROM / 旧机型 Keystore 异常时，至少不让玩家丢设置入口。
            appContext.getSharedPreferences(FALLBACK_PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun load(): AiEngineConfig {
        val provider = runCatching {
            AiProviderType.valueOf(
                prefs.getString(KEY_PROVIDER, AiProviderType.MOCK.name)
                    ?: AiProviderType.MOCK.name
            )
        }.getOrDefault(AiProviderType.MOCK)
        return AiEngineConfig(
            providerType = provider,
            apiKey = prefs.getString(KEY_API_KEY, "").orEmpty(),
            customModel = prefs.getString(KEY_MODEL, "").orEmpty()
        )
    }

    fun save(config: AiEngineConfig) {
        prefs.edit()
            .putString(KEY_PROVIDER, config.providerType.name)
            .putString(KEY_API_KEY, config.apiKey)
            .putString(KEY_MODEL, config.customModel)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "nandu_ai_engine_secure"
        private const val FALLBACK_PREFS_NAME = "nandu_ai_engine_fallback"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_MODEL = "model"
    }
}

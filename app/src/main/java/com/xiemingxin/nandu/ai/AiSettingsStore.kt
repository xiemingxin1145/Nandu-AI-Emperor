package com.xiemingxin.nandu.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * AI 引擎本机配置库。
 *
 * 目标：像「玄机阁」一样由玩家自己填 Key / Base URL / 模型名，配置只保存在本机，
 * 优先走 Android Keystore + EncryptedSharedPreferences；极端机型失败时降级到普通 SharedPreferences，
 * 避免设置页崩溃。
 */
data class AiEngineConfig(
    val providerType: AiProviderType = AiProviderType.MOCK,
    val apiKey: String = "",
    val customModel: String = ""
) {
    val isRealAiEnabled: Boolean
        get() = providerType != AiProviderType.MOCK && apiKey.isNotBlank()
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
            AiProviderType.valueOf(prefs.getString(KEY_PROVIDER, AiProviderType.MOCK.name) ?: AiProviderType.MOCK.name)
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

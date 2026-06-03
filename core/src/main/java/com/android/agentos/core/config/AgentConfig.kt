package com.android.agentos.core.config

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class AgentConfig(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val securePrefs: SharedPreferences = EncryptedSharedPreferences.create(
        "secure_agent_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val prefs: SharedPreferences = context.getSharedPreferences("agent_prefs", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = securePrefs.getString("api_key", null)
        set(value) = securePrefs.edit().putString("api_key", value).apply()

    var apiBaseUrl: String?
        get() = prefs.getString("api_base_url", "https://api.openai.com/v1")
        set(value) = prefs.edit().putString("api_base_url", value).apply()

    var modelPath: String?
        get() = prefs.getString("model_path", null)
        set(value) = prefs.edit().putString("model_path", value).apply()

    var useLocalModel: Boolean
        get() = prefs.getBoolean("use_local_model", false)
        set(value) = prefs.edit().putBoolean("use_local_model", value).apply()

    var providerType: String
        get() = prefs.getString("provider_type", "OPENAI") ?: "OPENAI"
        set(value) = prefs.edit().putString("provider_type", value).apply()

    var routingPolicy: String
        get() = prefs.getString("routing_policy", "PRIVACY_FIRST") ?: "PRIVACY_FIRST"
        set(value) = prefs.edit().putString("routing_policy", value).apply()

    /**
     * Validates an API endpoint URL.
     */
    fun validateEndpoint(url: String): Boolean {
        return try {
            val uri = java.net.URI(url)
            uri.isAbsolute && (uri.scheme == "http" || uri.scheme == "https")
        } catch (e: Exception) {
            false
        }
    }
}

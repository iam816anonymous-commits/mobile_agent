package com.android.agentos.core.config

import android.content.Context
import android.content.SharedPreferences

class AgentConfig(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agent_prefs", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = prefs.getString("api_key", null)
        set(value) = prefs.edit().putString("api_key", value).apply()

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
}

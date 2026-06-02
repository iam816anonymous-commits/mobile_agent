package com.android.agentos.ai

import android.content.Context
import com.android.agentos.core.config.AgentConfig
import com.android.agentos.core.models.LLMProvider

class LLMProviderFactory {
    companion object {
        fun create(context: Context, config: AgentConfig): LLMProvider {
            return when {
                config.useLocalModel && config.modelPath != null -> {
                    MediapipeLLMProvider(context, config.modelPath!!)
                }
                config.apiKey != null && config.apiKey!!.isNotEmpty() -> {
                    OpenAILLMProvider(config.apiKey!!)
                }
                else -> {
                    // Rule-based fallback for Phase 3.6 demo
                    RuleBasedLLMProvider()
                }
            }
        }
    }
}

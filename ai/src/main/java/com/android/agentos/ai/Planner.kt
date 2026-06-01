package com.android.agentos.ai

import com.android.agentos.core.models.LLMProvider
import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.ScreenElement

class Planner(private val llmProvider: LLMProvider) {

    /**
     * Generates a multi-step plan based on user input and current screen state.
     */
    suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        return llmProvider.generatePlan(userInput, screenContext)
    }

    suspend fun verifyExecution(action: com.android.agentos.core.models.AgentAction, screenContext: List<ScreenElement>): Boolean {
        return llmProvider.verifyAction(action, screenContext)
    }
}

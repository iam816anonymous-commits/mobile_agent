package com.android.agentos.ai

import com.android.agentos.core.models.*

class Planner(private val llmProvider: LLMProvider) {

    /**
     * Generates a multi-step plan based on user input and current screen state.
     */
    suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        return llmProvider.generatePlan(userInput, screenContext)
    }

    suspend fun verifyExecution(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        return llmProvider.verifyAction(action, screenContext)
    }
}

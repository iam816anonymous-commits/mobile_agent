package com.android.agentos.core.models

interface LLMProvider {
    val name: String
    suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan
    suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): Boolean
}

package com.android.agentos.ai

import com.android.agentos.core.models.*
import kotlinx.serialization.json.Json

/**
 * Fallback LLM provider using OpenAI API.
 */
class OpenAILLMProvider(private val apiKey: String) : LLMProvider {
    override val name: String = "OpenAI-Cloud"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        // In a real implementation, this would use a network client (e.g., Retrofit/Ktor)
        // For the Phase 3.6 demo, we return a mock plan if API key is present
        return if (apiKey.isNotEmpty()) {
            Plan(goal = userInput, steps = listOf(AgentAction(ActionType.WAIT)))
        } else {
            Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)
        }
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        return VerificationResult(true, 0.9f, "Cloud verification simulated")
    }
}

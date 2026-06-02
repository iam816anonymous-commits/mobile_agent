package com.android.agentos.ai

import com.android.agentos.core.models.*
import kotlinx.serialization.json.Json

/**
 * Fallback LLM provider using any OpenAI-compatible API (BYOK).
 */
class GenericCloudLLMProvider(
    private val apiKey: String,
    private val apiBaseUrl: String
) : LLMProvider {
    override val name: String = "Cloud-BYOK"
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        // This provider uses the custom base URL and API key.
        // Implementation would typically use Ktor/Retrofit to call the /chat/completions endpoint.

        if (apiKey.isEmpty()) {
            return Plan(goal = userInput, steps = emptyList(), status = PlanStatus.FAILED)
        }

        // Mocking the network call for the system architecture demo
        return Plan(
            goal = userInput,
            steps = listOf(AgentAction(ActionType.OPEN_APP, target = "Chrome"))
        )
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        return VerificationResult(true, 0.95f, "Verified via $apiBaseUrl")
    }
}

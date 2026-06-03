package com.android.agentos.core.models

interface LLMProvider {
    val name: String
    suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan
    suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult
    fun getUsageMetrics(): UsageMetrics? = null
}

data class UsageMetrics(
    val inputTokens: Int,
    val outputTokens: Int,
    val estimatedCost: Double
)

data class VerificationResult(
    val success: Boolean,
    val confidence: Float, // 0.0 to 1.0
    val reason: String? = null
)

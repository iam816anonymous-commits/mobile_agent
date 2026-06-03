package com.android.agentos.ai

import com.android.agentos.core.models.LLMProvider
import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.ScreenElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Framework for comparing performance across multiple providers on the same task.
 */
class ComparativeEvaluator(
    private val providers: List<LLMProvider>
) {
    suspend fun evaluate(userInput: String, screenContext: List<ScreenElement>): Map<String, EvaluationResult> = coroutineScope {
        providers.associate { provider ->
            val startTime = System.currentTimeMillis()
            val deferred = async {
                try {
                    val plan = provider.generatePlan(userInput, screenContext)
                    val duration = System.currentTimeMillis() - startTime
                    EvaluationResult.Success(plan, duration, provider.getUsageMetrics())
                } catch (e: Exception) {
                    EvaluationResult.Failure(e.message ?: "Unknown error")
                }
            }
            provider.name to deferred.await()
        }
    }
}

sealed class EvaluationResult {
    data class Success(
        val plan: Plan,
        val latencyMs: Long,
        val usage: com.android.agentos.core.models.UsageMetrics?
    ) : EvaluationResult()

    data class Failure(val error: String) : EvaluationResult()
}

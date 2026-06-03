package com.android.agentos.ai

import com.android.agentos.ai.classification.TaskClassifier
import com.android.agentos.ai.classification.TaskComplexity
import com.android.agentos.core.config.AgentConfig
import com.android.agentos.core.engine.UtilityScorer
import com.android.agentos.core.models.LLMProvider

/**
 * Routing policies for provider selection.
 */
enum class RoutingPolicy {
    PRIVACY_FIRST,  // Prefer local LLM for sensitive tasks
    PERFORMANCE_FIRST, // Prefer cloud LLM for complex tasks
    COST_BALANCED, // Use local for simple, cloud for high complexity
    UTILITY_OPTIMIZED, // Use historical performance to maximize utility
    LOCAL_ONLY,
    CLOUD_ONLY
}

/**
 * Routes intelligence requests to the most appropriate provider.
 */
class IntelligenceRouter(
    private val config: AgentConfig,
    private val localProvider: LLMProvider,
    private val cloudProvider: LLMProvider,
    private val taskClassifier: TaskClassifier = TaskClassifier(),
    private val utilityScorer: UtilityScorer = UtilityScorer()
) {
    fun selectProvider(userInput: String): LLMProvider {
        val policyStr = config.routingPolicy
        val policy = try { RoutingPolicy.valueOf(policyStr) } catch(e: Exception) { RoutingPolicy.PRIVACY_FIRST }
        val classification = taskClassifier.classify(userInput)

        return when (policy) {
            RoutingPolicy.LOCAL_ONLY -> localProvider
            RoutingPolicy.CLOUD_ONLY -> cloudProvider
            RoutingPolicy.UTILITY_OPTIMIZED -> {
                // Heuristic for now: Cloud has higher utility for HIGH complexity
                if (classification.complexity == TaskComplexity.HIGH) cloudProvider else localProvider
            }
            RoutingPolicy.PRIVACY_FIRST -> {
                if (classification.requiresPrivacy) localProvider else {
                    if (classification.complexity == TaskComplexity.HIGH) cloudProvider else localProvider
                }
            }
            RoutingPolicy.PERFORMANCE_FIRST -> {
                if (classification.complexity == TaskComplexity.LOW) localProvider else cloudProvider
            }
            RoutingPolicy.COST_BALANCED -> {
                if (classification.complexity == TaskComplexity.HIGH) cloudProvider else localProvider
            }
        }
    }
}

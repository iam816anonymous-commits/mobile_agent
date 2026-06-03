package com.android.agentos.core.engine

import com.android.agentos.core.models.UsageMetrics

/**
 * Calculates user value metrics like time saved.
 */
class ValueEstimator {
    fun estimateManualEffort(goal: String, steps: Int): Int {
        // Basic heuristic: 0.5 mins per step for simple tasks, more for complex research
        val complexityFactor = if (goal.contains("research", true) || goal.contains("summarize", true)) 3 else 1
        return steps * complexityFactor / 2 + 1
    }

    fun calculateTimeSaved(manualMinutes: Int, executionMs: Long): Double {
        val executionMinutes = executionMs / 60000.0
        return (manualMinutes - executionMinutes).coerceAtLeast(0.0)
    }
}

/**
 * Scores provider utility based on multi-objective optimization.
 */
class UtilityScorer {
    fun calculateUtility(
        successRate: Float,
        latencyMs: Long,
        cost: Double,
        isLocal: Boolean,
        requiresPrivacy: Boolean
    ): Double {
        // Weights
        val wSR = 100.0
        val wLatency = -0.01
        val wCost = -500.0
        val wPrivacy = if (requiresPrivacy && isLocal) 50.0 else 0.0

        return (successRate * wSR) + (latencyMs * wLatency) + (cost * wCost) + wPrivacy
    }
}

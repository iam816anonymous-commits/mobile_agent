package com.android.agentos.ai

import com.android.agentos.core.models.Plan

/**
 * Performs self-evaluation of agent output quality and usefulness.
 */
class QualityEvaluator {
    fun evaluate(goal: String, plan: Plan, observedOutcome: String?): Float {
        var score = 0.5f

        // Positive indicators
        if (plan.steps.isNotEmpty()) score += 0.1f
        if (observedOutcome != null && observedOutcome.length > 20) score += 0.2f

        // Complexity check
        if (goal.contains("research", true) && plan.steps.size > 3) score += 0.2f

        return score.coerceIn(0f, 1f)
    }
}

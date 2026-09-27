package com.android.agentos.ai

import android.util.Log
import com.android.agentos.core.models.*
import com.android.agentos.memory.AgentDatabase

/**
 * Local dynamic fallback path adapter.
 * Uses Room persistence action and failure histories to adapt plan paths dynamically at runtime
 * without any cloud API or external LLM dependencies.
 */
class AdaptiveFallbackPlanner(
    private val db: AgentDatabase? = null,
    private val failureThreshold: Float = 0.40f
) {
    companion object {
        private const val TAG = "AdaptiveFallbackPlanner"
    }

    /**
     * Dynamically inspects and modifies a plan's steps based on historical success/failure
     * metrics in the local Room database.
     */
    suspend fun adaptPlan(originalPlan: Plan): Plan {
        if (db == null) return originalPlan

        val adaptedSteps = mutableListOf<AgentAction>()

        for (step in originalPlan.steps) {
            val target = step.target
            if (target != null && step.type == ActionType.CLICK) {
                val history = db.agentDao().getActionsForTarget(target)
                if (history.isNotEmpty()) {
                    val failures = history.count { !it.success }
                    val failureRate = failures.toFloat() / history.size

                    if (failureRate >= failureThreshold) {
                        Log.w(TAG, "Target '$target' has high failure rate ($failureRate). Adapting fallback path dynamically.")
                        // Dynamic Fallback Path 1: Insert SCROLL_TO_ELEMENT before CLICK
                        adaptedSteps.add(AgentAction(ActionType.SCROLL_TO_ELEMENT, target = target))

                        // Dynamic Fallback Path 2: Convert click to ID or secondary selector if available
                        val fallbackTarget = getFallbackSelectorFor(target)
                        adaptedSteps.add(step.copy(target = fallbackTarget))
                        continue
                    }
                }
            }
            adaptedSteps.add(step)
        }

        return originalPlan.copy(steps = adaptedSteps)
    }

    /**
     * Provides deterministic local fallback selector mapping for known UI components.
     */
    private fun getFallbackSelectorFor(target: String): String {
        return when (target.lowercase()) {
            "search or type web address" -> "url_bar"
            "search" -> "search_box"
            "new text note" -> "add_note_button"
            else -> target
        }
    }
}

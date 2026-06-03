package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase

/**
 * Detects divergence between user stated goals and actual agent activity patterns.
 */
class GoalDriftDetector(private val db: AgentDatabase) {

    suspend fun detectDrift(): DriftResult {
        val activeGoals = db.agentDao().getActiveGoals()
        val recentActions = db.agentDao().getActionHistory().take(100)

        if (activeGoals.isEmpty()) return DriftResult(0f, "No active goals")

        // Count actions aligned with goal titles
        var alignedCount = 0
        recentActions.forEach { action ->
            if (activeGoals.any { goal -> action.target?.contains(goal.title, true) == true }) {
                alignedCount++
            }
        }

        val alignment = if (recentActions.isNotEmpty()) alignedCount.toFloat() / recentActions.size else 1f
        val drift = (1f - alignment)

        return DriftResult(
            driftScore = drift,
            status = if (drift > 0.4f) "HIGH DRIFT" else "ALIGNED"
        )
    }
}

data class DriftResult(val driftScore: Float, val status: String)

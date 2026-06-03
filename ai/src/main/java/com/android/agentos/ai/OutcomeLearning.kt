package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.OutcomeEntity

/**
 * Compares planned benefits with observed results and learns effectiveness.
 */
class OutcomeLearner(private val db: AgentDatabase) {

    suspend fun processOutcome(outcome: OutcomeEntity) {
        val quality = outcome.qualityScore

        if (outcome.success && quality > 0.7f) {
            // Learn successful approach
            StrategyLearner(db).learnSuccessfulStrategy(
                goalTitle = outcome.goal,
                steps = "SUCCESSFUL_ACTION_SEQUENCE", // Placeholder
                quality = quality
            )
        }

        // Update goal progress based on outcome success
        val goals = db.agentDao().getActiveGoals()
        goals.filter { outcome.goal.contains(it.title, true) }.forEach { goal ->
            GoalManager(db).updateProgress(goal.id, goal.progress + 0.1f)
        }
    }
}

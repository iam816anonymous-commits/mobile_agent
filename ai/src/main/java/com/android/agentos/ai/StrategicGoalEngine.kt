package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.GoalEntity
import com.android.agentos.memory.StrategyRecord

/**
 * Manages goal relationships, priorities, and term-levels.
 */
class GoalManager(private val db: AgentDatabase) {

    suspend fun createGoal(title: String, term: String, parentId: Long? = null) {
        val goal = GoalEntity(
            title = title,
            term = term,
            parentGoalId = parentId,
            description = "User goal: $title",
            targetOutcome = "Achieved $title"
        )
        db.agentDao().insertGoal(goal)
    }

    suspend fun updateProgress(goalId: Long, progress: Float) {
        val goal = db.agentDao().getActiveGoals().find { it.id == goalId }
        goal?.let {
            db.agentDao().insertGoal(it.copy(progress = progress.coerceIn(0f, 1f)))
        }
    }
}

/**
 * Extracts and persists successful interaction strategies.
 */
class StrategyLearner(private val db: AgentDatabase) {
    suspend fun learnSuccessfulStrategy(goalTitle: String, steps: String, quality: Float) {
        val existing = db.agentDao().getStrategiesForGoal(goalTitle).firstOrNull()

        val updated = if (existing != null) {
            existing.copy(
                successCount = existing.successCount + 1,
                avgQualityScore = (existing.avgQualityScore + quality) / 2
            )
        } else {
            StrategyRecord(
                goalTitle = goalTitle,
                stepsJson = steps,
                successCount = 1,
                avgQualityScore = quality
            )
        }
        db.agentDao().insertStrategy(updated)
    }
}

package com.android.agentos.ai.reasoning

import com.android.agentos.core.models.Subgoal
import com.android.agentos.core.models.LLMProvider
import com.android.agentos.core.models.ScreenElement

class GoalDecomposer(private val llmProvider: LLMProvider) {

    suspend fun decomposeGoal(goal: String, screenContext: List<ScreenElement>): List<Subgoal> {
        val subgoals = llmProvider.decomposeGoal(goal)
        return if (subgoals.isNotEmpty()) {
            subgoals
        } else {
            // Fallback
            listOf(
                Subgoal(description = "Initialize task for $goal"),
                Subgoal(description = "Execute core steps"),
                Subgoal(description = "Verify final outcome")
            )
        }
    }
}

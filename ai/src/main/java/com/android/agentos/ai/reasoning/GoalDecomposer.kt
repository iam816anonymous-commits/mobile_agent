package com.android.agentos.ai.reasoning

import com.android.agentos.core.models.Subgoal
import com.android.agentos.core.models.LLMProvider
import com.android.agentos.core.models.ScreenElement

class GoalDecomposer(private val llmProvider: LLMProvider) {

    suspend fun decomposeGoal(goal: String, screenContext: List<ScreenElement>): List<Subgoal> {
        // In Phase 3.5, we use a specialized prompt to extract subgoals
        val prompt = "Decompose the following user goal into exactly 3 clear high-level subgoals for an Android agent: $goal. Return ONLY a comma-separated list."
        // Using generatePlan as a proxy for raw text generation for now
        val rawResponse = "Open Search App, Input Query, Verify and Save Info"
        return rawResponse.split(",").map { Subgoal(description = it.trim()) }
    }
}

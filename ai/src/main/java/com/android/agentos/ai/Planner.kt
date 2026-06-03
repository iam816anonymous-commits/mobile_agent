package com.android.agentos.ai

import com.android.agentos.core.models.*
import com.android.agentos.core.memory.EpisodicMemory
import com.android.agentos.ai.reasoning.UtilityEvaluator
import com.android.agentos.ai.reasoning.GoalDecomposer

class Planner(
    private var llmProvider: LLMProvider,
    private val episodicMemory: EpisodicMemory? = null,
    private val utilityEvaluator: UtilityEvaluator = UtilityEvaluator(),
    private var goalDecomposer: GoalDecomposer = GoalDecomposer(llmProvider),
    private val router: IntelligenceRouter? = null
) {

    private val actionHistory = mutableListOf<ActionHistory>()

    /**
     * Generates a hierarchical multi-step plan based on user input and current screen state.
     */
    suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        // Dynamic provider selection
        val selectedProvider = router?.selectProvider(userInput) ?: llmProvider

        val similarEpisodes = episodicMemory?.retrieveSimilar(userInput) ?: emptyList()

        // Enhance prompt with episodic context if available
        val enhancedInput = if (similarEpisodes.isNotEmpty()) {
            "Goal: $userInput\nContext from past success: ${similarEpisodes[0].plan.steps.take(3)}"
        } else userInput

        val subgoals = goalDecomposer.decomposeGoal(userInput, screenContext)
        val plan = selectedProvider.generatePlan(enhancedInput, screenContext)

        // Utility-based ranking of steps if multiple options were implied (simplified)
        val rankedSteps = utilityEvaluator.rankActions(plan.steps)

        return plan.copy(steps = rankedSteps, subgoals = subgoals)
    }

    suspend fun verifyExecution(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        return llmProvider.verifyAction(action, screenContext)
    }

    fun addHistory(action: AgentAction, success: Boolean) {
        actionHistory.add(ActionHistory(action, success))
    }

    private data class ActionHistory(val action: AgentAction, val success: Boolean)
}

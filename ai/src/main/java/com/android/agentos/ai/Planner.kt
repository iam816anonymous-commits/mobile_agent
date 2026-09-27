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
    private val router: IntelligenceRouter? = null,
    private val adaptiveFallbackPlanner: AdaptiveFallbackPlanner? = null
) {

    private val actionHistory = mutableListOf<ActionHistory>()

    /**
     * Generates a hierarchical multi-step plan based on user input and current screen state,
     * applying dynamic fallback adaptation using local persistence metrics.
     */
    suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        val selectedProvider = router?.selectProvider(userInput) ?: llmProvider

        val similarEpisodes = episodicMemory?.retrieveSimilar(userInput) ?: emptyList()

        val enhancedInput = if (similarEpisodes.isNotEmpty()) {
            "Goal: $userInput\nContext from past success: ${similarEpisodes[0].plan.steps.take(3)}"
        } else userInput

        val subgoals = goalDecomposer.decomposeGoal(userInput, screenContext)
        val rawPlan = selectedProvider.generatePlan(enhancedInput, screenContext)

        val rankedSteps = utilityEvaluator.rankActions(rawPlan.steps)
        val basePlan = rawPlan.copy(steps = rankedSteps, subgoals = subgoals)

        return adaptiveFallbackPlanner?.adaptPlan(basePlan) ?: basePlan
    }

    suspend fun verifyExecution(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        return llmProvider.verifyAction(action, screenContext)
    }

    fun addHistory(action: AgentAction, success: Boolean) {
        actionHistory.add(ActionHistory(action, success))
    }

    private data class ActionHistory(val action: AgentAction, val success: Boolean)
}

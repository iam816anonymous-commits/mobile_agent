package com.android.agentos.ai.reflection

import com.android.agentos.core.models.*
import com.android.agentos.core.engine.ReflectionProvider

class ReflectionEngine(private val llmProvider: LLMProvider) : ReflectionProvider {

    override suspend fun reflectAndReplan(
        originalGoal: String,
        failure: FailureLog,
        screenContext: List<ScreenElement>
    ): Plan? {
        val repairPrompt = """
            Original Goal: $originalGoal
            Failure: ${failure.errorType} - ${failure.errorMessage}
            Current Screen Elements: ${screenContext.take(15).map { it.text }}
            The last action failed. Generate a REPAIR PLAN to recover and continue toward the goal.
        """.trimIndent()

        return llmProvider.generatePlan(repairPrompt, screenContext)
    }
}

package com.android.agentos.ai.outcome

import com.android.agentos.core.models.ScreenElement
import com.android.agentos.core.models.LLMProvider
import com.android.agentos.core.models.VerificationResult
import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ActionType

class OutcomeVerifier(private val llmProvider: LLMProvider) {

    suspend fun verifyGoalAchievement(goal: String, screenContext: List<ScreenElement>): Boolean {
        val verificationAction = AgentAction(ActionType.VERIFY_ELEMENT, target = goal) // Using goal as target for reasoning
        val result = llmProvider.verifyAction(verificationAction, screenContext)
        return result.success && result.confidence > 0.7f
    }
}

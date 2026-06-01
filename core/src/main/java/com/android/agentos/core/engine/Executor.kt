package com.android.agentos.core.engine

import android.util.Log
import com.android.agentos.core.models.*
import kotlinx.coroutines.delay

class Executor(
    private val accessibilityProvider: AccessibilityProvider,
    private val verificationProvider: VerificationProvider,
    private val onActionStarted: (AgentAction) -> Unit,
    private val onActionFinished: (ExecutionResult) -> Unit,
    private val onFailure: (FailureLog) -> Unit
) {
    companion object {
        private const val TAG = "AgentExecutor"
        private const val MAX_RETRIES = 3
        private const val ACTION_DELAY = 1000L
    }

    suspend fun execute(plan: Plan) {
        plan.status = PlanStatus.EXECUTING

        for (step in plan.steps) {
            var success = false
            var retries = 0

            while (!success && retries < MAX_RETRIES) {
                onActionStarted(step)
                Log.d(TAG, "Executing action: ${step.type} (Attempt ${retries + 1})")

                // 1. Execute
                val performed = accessibilityProvider.performAction(step)

                if (performed) {
                    delay(ACTION_DELAY) // Wait for UI transition

                    // 2. Observe & Verify
                    val screenContext = accessibilityProvider.getCurrentScreenHierarchy()
                    success = verificationProvider.verifyAction(step, screenContext)

                    if (success) {
                        onActionFinished(ExecutionResult(step.id, true, "Action verified successfully"))
                    } else {
                        Log.w(TAG, "Action verification failed: ${step.type}")
                    }
                } else {
                    Log.e(TAG, "Action performance failed: ${step.type}")
                }

                if (!success) {
                    retries++
                    if (retries < MAX_RETRIES) {
                        Log.i(TAG, "Retrying action: ${step.type}")
                        delay(2000L) // Wait longer before retry
                    } else {
                        onFailure(FailureLog(step.id, "MAX_RETRIES", "Failed after $MAX_RETRIES attempts"))
                        plan.status = PlanStatus.FAILED
                        return
                    }
                }
            }
        }
        plan.status = PlanStatus.COMPLETED
    }
}

interface AccessibilityProvider {
    fun performAction(action: AgentAction): Boolean
    fun getCurrentScreenHierarchy(): List<ScreenElement>
}

interface VerificationProvider {
    suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): Boolean
}

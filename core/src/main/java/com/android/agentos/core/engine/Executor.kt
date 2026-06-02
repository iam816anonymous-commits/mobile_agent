package com.android.agentos.core.engine

import android.util.Log
import com.android.agentos.core.models.*
import kotlinx.coroutines.delay

class Executor(
    private val accessibilityProvider: AccessibilityProvider,
    private val verificationProvider: VerificationProvider,
    private val memoryProvider: MemoryProvider,
    private val onActionStarted: (AgentAction) -> Unit,
    private val onActionFinished: (ExecutionResult) -> Unit,
    private val onFailure: (FailureLog) -> Unit
) {
    companion object {
        private const val TAG = "AgentExecutor"
        private const val MAX_RETRIES = 3
        private const val ACTION_DELAY = 1000L
    }

    var stressTestMode: Boolean = false

    suspend fun execute(plan: Plan) {
        plan.status = PlanStatus.EXECUTING

        for (step in plan.steps) {
            if (stressTestMode) {
                val delayTime = (500..3000).random().toLong()
                Log.i(TAG, "STRESS TEST: Introducing artificial delay of ${delayTime}ms")
                delay(delayTime)

                if ((1..10).random() > 8) {
                    Log.w(TAG, "STRESS TEST: Simulating unexpected interruption")
                    delay(1000)
                }
            }
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
                    val verification = verificationProvider.verifyAction(step, screenContext)
                    success = verification.success

                    val result = ExecutionResult(step.id, success, verification.reason ?: (if (success) "Action verified" else "Verification failed"))
                    memoryProvider.logAction(plan.id, step, result)

                    if (success) {
                        onActionFinished(result)
                    } else {
                        Log.w(TAG, "Action verification failed: ${step.type}")
                    }
                } else {
                    Log.e(TAG, "Action performance failed: ${step.type}")
                    memoryProvider.logAction(plan.id, step, ExecutionResult(step.id, false, "Performance failed"))
                }

                if (!success) {
                    val failure = FailureLog(step.id, "EXECUTION_FAILURE", "Failed at attempt ${retries + 1}")
                    memoryProvider.logFailure(failure)

                    retries++
                    if (retries < MAX_RETRIES) {
                        Log.i(TAG, "Retrying action: ${step.type}")
                        delay(2000L) // Wait longer before retry
                    } else {
                        onFailure(failure)
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
    fun getCurrentScreenState(): ScreenState
}

interface VerificationProvider {
    suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult
}

interface MemoryProvider {
    suspend fun logAction(planId: String, action: AgentAction, result: ExecutionResult)
    suspend fun logFailure(failure: FailureLog)
}

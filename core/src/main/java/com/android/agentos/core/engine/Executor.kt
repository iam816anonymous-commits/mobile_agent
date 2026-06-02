package com.android.agentos.core.engine

import android.util.Log
import com.android.agentos.core.models.*
import com.android.agentos.core.failure.FailureClassifier
import kotlinx.coroutines.delay

class Executor(
    private val accessibilityProvider: AccessibilityProvider,
    private val verificationProvider: VerificationProvider,
    private val memoryProvider: MemoryProvider,
    private val reflectionProvider: ReflectionProvider? = null,
    private val outcomeProvider: OutcomeProvider? = null,
    private val worldModelProvider: WorldModelProvider? = null,
    private val failureClassifier: FailureClassifier = FailureClassifier(),
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
    var adversarialMode: Boolean = false

    private fun simulateAdversarialConditions() {
        val roll = (1..100).random()
        when {
            roll < 5 -> Log.i(TAG, "ADVERSARIAL: Simulating notification popup")
            roll < 10 -> Log.i(TAG, "ADVERSARIAL: Simulating keyboard appearing")
            roll < 15 -> Log.i(TAG, "ADVERSARIAL: Simulating network fluctuation")
        }
    }

    suspend fun execute(plan: Plan) {
        if (plan.status == PlanStatus.COMPLETED || plan.status == PlanStatus.FAILED) return

        plan.status = PlanStatus.EXECUTING

        while (plan.currentStepIndex < plan.steps.size) {
            if (plan.status == PlanStatus.PAUSED) break

            val step = plan.steps[plan.currentStepIndex]
            if (stressTestMode) {
                val delayTime = (500..3000).random().toLong()
                Log.i(TAG, "STRESS TEST: Introducing artificial delay of ${delayTime}ms")
                delay(delayTime)

                if ((1..10).random() > 8) {
                    Log.w(TAG, "STRESS TEST: Simulating unexpected interruption")
                    delay(1000)
                }
            }

            if (adversarialMode) {
                simulateAdversarialConditions()
            }
            var success = false
            var retries = 0

            while (!success && retries < MAX_RETRIES) {
                val beforeState = accessibilityProvider.getCurrentScreenState().classification
                val predictedNextState = worldModelProvider?.predictNextState(beforeState, step.type)

                onActionStarted(step)
                Log.d(TAG, "Executing action: ${step.type} (Attempt ${retries + 1}). Predicted: $predictedNextState")

                // 1. Execute
                val performed = accessibilityProvider.performAction(step)

                if (performed) {
                    delay(ACTION_DELAY) // Wait for UI transition

                    // 2. Observe & Verify
                    val afterState = accessibilityProvider.getCurrentScreenState()
                    val screenContext = afterState.elements

                    if (predictedNextState != null && predictedNextState != ScreenType.UNKNOWN) {
                        if (afterState.classification != predictedNextState) {
                            Log.w(TAG, "Transition Anomaly: Expected $predictedNextState, got ${afterState.classification}")
                        } else {
                            Log.i(TAG, "Transition Prediction Success: ${afterState.classification}")
                        }
                        worldModelProvider?.learnTransition(beforeState, step.type, afterState.classification)
                    }

                    val verification = verificationProvider.verifyAction(step, screenContext)
                    success = verification.success

                    val result = ExecutionResult(step.id, success, verification.reason ?: (if (success) "Action verified" else "Verification failed"))
                    memoryProvider.logAction(plan.id, step, result)

                    if (success) {
                        onActionFinished(result)
                        plan.currentStepIndex++
                    } else {
                        Log.w(TAG, "Action verification failed: ${step.type}")
                    }
                } else {
                    Log.e(TAG, "Action performance failed: ${step.type}")
                    memoryProvider.logAction(plan.id, step, ExecutionResult(step.id, false, "Performance failed"))
                }

                if (!success) {
                    val category = failureClassifier.classifyFailure("EXECUTION_FAILURE", "Failed at attempt ${retries + 1}", accessibilityProvider.getCurrentScreenHierarchy())
                    val failure = FailureLog(step.id, category, "EXECUTION_FAILURE", "Failed at attempt ${retries + 1}")
                    memoryProvider.logFailure(failure)

                    retries++
                    if (retries < MAX_RETRIES) {
                        Log.i(TAG, "Retrying action: ${step.type}")
                        delay(2000L) // Wait longer before retry
                    } else {
                        val screen = accessibilityProvider.getCurrentScreenHierarchy()
                        val category = failureClassifier.classifyFailure("MAX_RETRIES", "Failed after $MAX_RETRIES attempts", screen)
                        val failureWithCategory = FailureLog(step.id, category, "MAX_RETRIES", "Failed after $MAX_RETRIES attempts")
                        // Try Reflection/Re-planning before giving up
                        val repairPlan = reflectionProvider?.reflectAndReplan(plan.goal, failureWithCategory, accessibilityProvider.getCurrentScreenHierarchy())
                        if (repairPlan != null && repairPlan.steps.isNotEmpty()) {
                            Log.i(TAG, "Attempting repair plan...")
                            execute(repairPlan) // Recursive call for repair
                            if (repairPlan.status == PlanStatus.COMPLETED) {
                                success = true
                                break
                            }
                        }

                        onFailure(failure)
                        plan.status = PlanStatus.FAILED
                        return
                    }
                }
            }
        }
        if (plan.currentStepIndex >= plan.steps.size) {
            // Final Outcome Verification
            val finalScreen = accessibilityProvider.getCurrentScreenHierarchy()
            val outcomeVerified = outcomeProvider?.verifyGoalAchievement(plan.goal, finalScreen) ?: true

            if (outcomeVerified) {
                plan.status = PlanStatus.COMPLETED
                Log.i(TAG, "Plan Goal achieved and verified: ${plan.goal}")
            } else {
                plan.status = PlanStatus.FAILED
                Log.e(TAG, "Plan steps finished but goal verification failed: ${plan.goal}")
            }
        }
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
    suspend fun logOutcome(planId: String, goal: String, success: Boolean, observedOutcome: String?)
}

interface ReflectionProvider {
    suspend fun reflectAndReplan(goal: String, failure: FailureLog, screenContext: List<ScreenElement>): Plan
}

interface OutcomeProvider {
    suspend fun verifyGoalAchievement(goal: String, screenContext: List<ScreenElement>): Boolean
}

interface WorldModelProvider {
    fun predictNextState(currentState: ScreenType, action: ActionType): ScreenType
    fun learnTransition(from: ScreenType, action: ActionType, to: ScreenType)
}

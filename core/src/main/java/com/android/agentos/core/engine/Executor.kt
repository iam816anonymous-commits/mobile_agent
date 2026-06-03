package com.android.agentos.core.engine

import android.util.Log
import com.android.agentos.core.models.*
import com.android.agentos.core.failure.FailureClassifier
import kotlinx.coroutines.delay

class Executor(
    private val accessibilityProvider: AccessibilityProvider,
    private val verificationProvider: VerificationProvider,
    private val llmProvider: LLMProvider? = null,
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
    }

    suspend fun execute(plan: Plan, depth: Int = 0) {
        if (depth > 3) {
            plan.status = PlanStatus.FAILED
            return
        }
        if (plan.status == PlanStatus.COMPLETED || plan.status == PlanStatus.FAILED) return

        plan.status = PlanStatus.EXECUTING

        val startTime = System.currentTimeMillis()
        while (plan.currentStepIndex < plan.steps.size) {
            if (plan.status == PlanStatus.PAUSED) break

            val step = plan.steps[plan.currentStepIndex]
            if (stressTestMode) {
                val delayTime = (500..3000).random().toLong()
                delay(delayTime)

                if ((1..10).random() > 8) {
                    delay(1000)
                }
            }

            if (adversarialMode) {
                simulateAdversarialConditions()
            }
            var success = false
            var retries = 0

            while (!success && retries < MAX_RETRIES) {
                val beforeStateObj = accessibilityProvider.getCurrentScreenState()
                val beforeState = beforeStateObj.classification
                val predictedNextState = worldModelProvider?.predictNextState(beforeState, step.type)

                onActionStarted(step)

                // 1. Execute
                val performed = accessibilityProvider.performAction(step)

                if (performed) {
                    delay(ACTION_DELAY) // Wait for UI transition

                    // Dynamic Adaptation: check if layout changed radically
                    val currentElements = accessibilityProvider.getCurrentScreenHierarchy()
                    if (currentElements.size != beforeStateObj.elements.size) {
                        // Add extra delay for dynamic layouts (list to grid etc)
                        delay(1000)
                    }

                    // 2. Observe & Verify
                    val afterState = accessibilityProvider.getCurrentScreenState()
                    val screenContext = afterState.elements

                    if (predictedNextState != null && predictedNextState != ScreenType.UNKNOWN) {
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
                    }
                } else {
                    memoryProvider.logAction(plan.id, step, ExecutionResult(step.id, false, "Performance failed"))
                }

                if (!success) {
                    val category = failureClassifier.classifyFailure("EXECUTION_FAILURE", "Failed at attempt ${retries + 1}", accessibilityProvider.getCurrentScreenHierarchy())
                    val failure = FailureLog(step.id, category, "EXECUTION_FAILURE", "Failed at attempt ${retries + 1}")
                    memoryProvider.logFailure(failure)

                    retries++
                    if (retries < MAX_RETRIES) {
                        delay(2000L) // Wait longer before retry
                    } else {
                        val screen = accessibilityProvider.getCurrentScreenHierarchy()
                        val category = failureClassifier.classifyFailure("MAX_RETRIES", "Failed after $MAX_RETRIES attempts", screen)
                        val failureWithCategory = FailureLog(step.id, category, "MAX_RETRIES", "Failed after $MAX_RETRIES attempts")
                        // Try Reflection/Re-planning before giving up
                        val repairPlan = reflectionProvider?.reflectAndReplan(plan.goal, failureWithCategory, accessibilityProvider.getCurrentScreenHierarchy())
                        if (repairPlan != null && repairPlan.steps.isNotEmpty()) {
                            execute(repairPlan, depth + 1) // Recursive call for repair
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

            val duration = System.currentTimeMillis() - startTime
            llmProvider?.let {
                memoryProvider.logProviderMetric(
                    it.name,
                    plan.id,
                    duration,
                    it.getUsageMetrics(),
                    outcomeVerified
                )
            }

            if (outcomeVerified) {
                plan.status = PlanStatus.COMPLETED
            } else {
                plan.status = PlanStatus.FAILED
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
    suspend fun logProviderMetric(providerName: String, planId: String, latency: Long, usage: UsageMetrics?, success: Boolean)
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

package com.android.agentos.core.engine

import com.android.agentos.core.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class ExecutorTest {

    class MockAccessibilityProvider : AccessibilityProvider {
        var actionsPerformed = mutableListOf<AgentAction>()
        override fun performAction(action: AgentAction): Boolean {
            actionsPerformed.add(action)
            return true
        }
        override fun getCurrentScreenHierarchy(): List<ScreenElement> = emptyList()
        override fun getCurrentScreenState(): ScreenState = ScreenState(null, null, emptyList())
    }

    class MockVerificationProvider : VerificationProvider {
        override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
            return VerificationResult(true, 1.0f)
        }
    }

    class MockMemoryProvider : MemoryProvider {
        override suspend fun logAction(planId: String, action: AgentAction, result: ExecutionResult) {}
        override suspend fun logFailure(failure: FailureLog) {}
        override suspend fun logOutcome(planId: String, goal: String, success: Boolean, observedOutcome: String?) {}
        override suspend fun logProviderMetric(providerName: String, planId: String, latency: Long, usage: UsageMetrics?, success: Boolean) {}
    }

    @Test
    fun testRecursionLimit() = runBlocking {
        val accessibility = MockAccessibilityProvider()
        val verification = MockVerificationProvider()
        val memory = MockMemoryProvider()

        val executor = Executor(
            accessibilityProvider = accessibility,
            verificationProvider = verification,
            memoryProvider = memory,
            onActionStarted = {},
            onActionFinished = {},
            onFailure = {}
        )

        val plan = Plan(goal = "Test", steps = listOf(AgentAction(ActionType.WAIT)))

        executor.execute(plan, depth = 4)
        assertEquals(PlanStatus.FAILED, plan.status)

        val plan2 = Plan(goal = "Test", steps = listOf(AgentAction(ActionType.WAIT)))
        executor.execute(plan2, depth = 0)
        assertEquals(PlanStatus.COMPLETED, plan2.status)
    }
}

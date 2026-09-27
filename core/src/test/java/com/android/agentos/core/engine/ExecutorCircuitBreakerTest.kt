package com.android.agentos.core.engine

import com.android.agentos.core.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ExecutorCircuitBreakerTest {

    private class MockAccessibilityProvider : AccessibilityProvider {
        override fun performAction(action: AgentAction): Boolean = true
        override fun getCurrentScreenHierarchy(): List<ScreenElement> = emptyList()
        override fun getCurrentScreenState(): ScreenState = ScreenState(null, ScreenType.UNKNOWN, emptyList())
    }

    private class MockVerificationProvider : VerificationProvider {
        override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
            return VerificationResult(true, 1.0f, "Success")
        }
    }

    private class MockMemoryProvider : MemoryProvider {
        var failureLogged: FailureLog? = null
        override suspend fun logAction(planId: String, action: AgentAction, result: ExecutionResult) {}
        override suspend fun logFailure(failure: FailureLog) { failureLogged = failure }
        override suspend fun logOutcome(planId: String, goal: String, success: Boolean, observedOutcome: String?) {}
        override suspend fun logProviderMetric(providerName: String, planId: String, latency: Long, usage: UsageMetrics?, success: Boolean) {}
    }

    @Test
    fun testCircuitBreakerPreventsInfiniteLoopsInExecutor() = runBlocking {
        val stateMachine = LocalAgentStateMachine(maxSteps = 2) // Strict 2-step limit
        val memory = MockMemoryProvider()

        val executor = Executor(
            accessibilityProvider = MockAccessibilityProvider(),
            verificationProvider = MockVerificationProvider(),
            memoryProvider = memory,
            stateMachine = stateMachine,
            onActionStarted = {},
            onActionFinished = {},
            onFailure = {}
        )

        // Plan with 5 steps, but circuit breaker limit is 2
        val plan = Plan(
            goal = "Test long plan",
            steps = listOf(
                AgentAction(ActionType.WAIT),
                AgentAction(ActionType.WAIT),
                AgentAction(ActionType.WAIT),
                AgentAction(ActionType.WAIT),
                AgentAction(ActionType.WAIT)
            )
        )

        executor.execute(plan)

        assertEquals(PlanStatus.FAILED, plan.status)
        assertEquals(AgentState.CIRCUIT_BROKEN, stateMachine.currentState)
        assertNotNull(memory.failureLogged)
        assertEquals("CIRCUIT_BREAKER_TRIPPED", memory.failureLogged?.errorType)
    }
}

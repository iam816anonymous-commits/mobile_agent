package com.android.agentos.core.engine

import com.android.agentos.core.models.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SafetyKillSwitchTest {

    @Before
    fun setUp() {
        AgentBridge.instance.resetKillSwitch()
    }

    @After
    fun tearDown() {
        AgentBridge.instance.resetKillSwitch()
    }

    @Test
    fun testKillSwitchPreventsExecution() = runBlocking {
        AgentBridge.instance.triggerKillSwitch("Emergency test trigger")
        assertTrue(AgentBridge.instance.isKilled.get())

        val stateMachine = LocalAgentStateMachine(maxSteps = 10)
        var failureLogged: FailureLog? = null

        val executor = Executor(
            accessibilityProvider = AgentBridge.instance,
            verificationProvider = AgentBridge.instance,
            memoryProvider = object : MemoryProvider {
                override suspend fun logAction(planId: String, action: AgentAction, result: ExecutionResult) {}
                override suspend fun logFailure(failure: FailureLog) { failureLogged = failure }
                override suspend fun logOutcome(planId: String, goal: String, success: Boolean, observedOutcome: String?) {}
                override suspend fun logProviderMetric(providerName: String, planId: String, latency: Long, usage: UsageMetrics?, success: Boolean) {}
            },
            stateMachine = stateMachine,
            onActionStarted = {},
            onActionFinished = {},
            onFailure = { failureLogged = it }
        )

        val plan = Plan(
            goal = "Test kill switch",
            steps = listOf(AgentAction(ActionType.CLICK, target = "button"))
        )

        executor.execute(plan)

        assertEquals(PlanStatus.FAILED, plan.status)
        assertEquals(AgentState.KILLED, stateMachine.currentState)
        assertNotNull(failureLogged)
        assertEquals("KILLED", failureLogged?.errorType)
    }
}

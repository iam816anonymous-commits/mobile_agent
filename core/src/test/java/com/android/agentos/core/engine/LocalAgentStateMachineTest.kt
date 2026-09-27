package com.android.agentos.core.engine

import org.junit.Assert.*
import org.junit.Test

class LocalAgentStateMachineTest {

    @Test
    fun testInitialStateIsIdle() {
        val stateMachine = LocalAgentStateMachine(maxSteps = 5)
        assertEquals(AgentState.IDLE, stateMachine.currentState)
        assertEquals(0, stateMachine.stepCounter.get())
        assertFalse(stateMachine.isTerminated())
    }

    @Test
    fun testTransitions() {
        val stateMachine = LocalAgentStateMachine(maxSteps = 5)
        stateMachine.transitionTo(AgentState.PLANNING, "Starting planning")
        assertEquals(AgentState.PLANNING, stateMachine.currentState)

        stateMachine.transitionTo(AgentState.EXECUTING, "Executing steps")
        assertEquals(AgentState.EXECUTING, stateMachine.currentState)

        stateMachine.transitionTo(AgentState.COMPLETED, "Goal finished")
        assertEquals(AgentState.COMPLETED, stateMachine.currentState)
        assertTrue(stateMachine.isTerminated())
    }

    @Test
    fun testCircuitBreakerTripsWhenMaxStepsExceeded() {
        val stateMachine = LocalAgentStateMachine(maxSteps = 3)

        assertTrue(stateMachine.incrementAndCheckCircuitBreaker()) // 1
        assertTrue(stateMachine.incrementAndCheckCircuitBreaker()) // 2
        assertTrue(stateMachine.incrementAndCheckCircuitBreaker()) // 3
        assertFalse(stateMachine.incrementAndCheckCircuitBreaker()) // 4 -> Trips circuit breaker

        assertEquals(AgentState.CIRCUIT_BROKEN, stateMachine.currentState)
        assertTrue(stateMachine.isTerminated())
    }
}

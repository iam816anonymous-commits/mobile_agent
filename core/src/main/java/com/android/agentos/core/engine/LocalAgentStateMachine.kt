package com.android.agentos.core.engine

import android.util.Log
import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.PlanStatus
import java.util.concurrent.atomic.AtomicInteger

/**
 * State enum for the Local Agent State Machine.
 */
enum class AgentState {
    IDLE,
    PLANNING,
    EXECUTING,
    VERIFYING,
    RECOVERING,
    CIRCUIT_BROKEN,
    KILLED,
    COMPLETED,
    FAILED
}

/**
 * Core State Machine Loop equipped with a strict step-counter circuit breaker
 * to prevent infinite loops during automation execution and repair loops.
 */
class LocalAgentStateMachine(
    val maxSteps: Int = DEFAULT_MAX_STEPS
) {
    companion object {
        const val DEFAULT_MAX_STEPS = 25
        private const val TAG = "AgentStateMachine"
    }

    var currentState: AgentState = AgentState.IDLE
        private set

    val stepCounter = AtomicInteger(0)

    fun reset() {
        currentState = AgentState.IDLE
        stepCounter.set(0)
    }

    fun transitionTo(newState: AgentState, reason: String? = null) {
        val oldState = currentState
        currentState = newState
        Log.i(TAG, "State transition: $oldState -> $newState ${reason?.let { "($it)" } ?: ""}")
    }

    /**
     * Increments the step counter and checks against circuit breaker threshold.
     * Returns true if execution can continue, or false if circuit breaker tripped.
     */
    fun incrementAndCheckCircuitBreaker(): Boolean {
        val count = stepCounter.incrementAndGet()
        if (count > maxSteps) {
            transitionTo(AgentState.CIRCUIT_BROKEN, "Step-counter circuit breaker tripped ($count > $maxSteps steps)")
            return false
        }
        return true
    }

    fun isTerminated(): Boolean {
        return currentState == AgentState.CIRCUIT_BROKEN ||
               currentState == AgentState.KILLED ||
               currentState == AgentState.COMPLETED ||
               currentState == AgentState.FAILED
    }
}

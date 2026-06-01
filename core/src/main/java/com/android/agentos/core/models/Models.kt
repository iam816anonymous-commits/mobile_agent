package com.android.agentos.core.models

import kotlinx.serialization.Serializable

/**
 * Represents a single action performed by the agent.
 */
@Serializable
data class AgentAction(
    val type: ActionType,
    val target: String? = null,
    val text: String? = null,
    val x: Int? = null,
    val y: Int? = null,
    val id: String = java.util.UUID.randomUUID().toString()
)

/**
 * Types of actions the agent can perform.
 */
enum class ActionType {
    OPEN_APP,
    CLICK,
    TYPE_TEXT,
    SCROLL_UP,
    SCROLL_DOWN,
    GO_BACK,
    SCREENSHOT,
    WAIT,
    VERIFY_ELEMENT
}

/**
 * A sequence of actions to achieve a specific goal.
 */
@Serializable
data class Plan(
    val id: String = java.util.UUID.randomUUID().toString(),
    val goal: String,
    val steps: List<AgentAction>,
    var status: PlanStatus = PlanStatus.PENDING
)

enum class PlanStatus {
    PENDING,
    EXECUTING,
    COMPLETED,
    FAILED
}

@Serializable
data class ExecutionResult(
    val actionId: String,
    val success: Boolean,
    val message: String,
    val observedState: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class FailureLog(
    val actionId: String,
    val errorType: String,
    val errorMessage: String,
    val screenshotPath: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a UI element on the screen.
 */
@Serializable
data class ScreenElement(
    val text: String?,
    val contentDescription: String?,
    val className: String,
    val bounds: Rect,
    val isClickable: Boolean,
    val id: String? = null
)

/**
 * Represents screen coordinates.
 */
@Serializable
data class Rect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)

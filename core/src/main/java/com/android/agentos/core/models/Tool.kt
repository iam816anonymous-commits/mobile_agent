package com.android.agentos.core.models

interface Tool {
    val name: String
    val description: String
    val supportedActions: List<ActionType>
    fun canHandle(action: AgentAction): Boolean
    fun execute(action: AgentAction): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val message: String,
    val observedState: String? = null
)

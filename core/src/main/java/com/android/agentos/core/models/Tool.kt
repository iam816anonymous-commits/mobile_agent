package com.android.agentos.core.models

interface Tool {
    val name: String
    fun canHandle(action: AgentAction): Boolean
    fun execute(action: AgentAction): ToolResult
}

data class ToolResult(
    val success: Boolean,
    val message: String,
    val observedState: String? = null
)

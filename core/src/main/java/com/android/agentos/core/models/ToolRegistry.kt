package com.android.agentos.core.models

import com.android.agentos.core.models.Tool
import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ToolResult

class ToolRegistry {
    private val tools = mutableListOf<Tool>()

    fun registerTool(tool: Tool) {
        tools.add(tool)
    }

    fun findToolForAction(action: AgentAction): Tool? {
        return tools.find { it.canHandle(action) }
    }

    fun getAllTools(): List<Tool> = tools

    fun getToolMetadata(): String {
        return tools.joinToString("\n") {
            "- ${it.name}: ${it.description} (Actions: ${it.supportedActions.joinToString(", ")})"
        }
    }
}

package com.android.agentos.tools

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ActionType
import com.android.agentos.core.models.Tool
import com.android.agentos.core.models.ToolResult

class ChromeTool : Tool {
    override val name: String = "Chrome"

    override fun canHandle(action: AgentAction): Boolean {
        return action.target?.contains("chrome", ignoreCase = true) == true
    }

    override fun execute(action: AgentAction): ToolResult {
        return when (action.type) {
            ActionType.OPEN_APP -> ToolResult(true, "Opening Chrome")
            ActionType.TYPE_TEXT -> ToolResult(true, "Searching in Chrome: ${action.text}")
            ActionType.CLICK -> ToolResult(true, "Clicking in Chrome: ${action.target}")
            ActionType.VERIFY_ELEMENT -> ToolResult(true, "Verifying element in Chrome: ${action.target}")
            else -> ToolResult(false, "Unsupported action for Chrome")
        }
    }
}

package com.android.agentos.tools

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ActionType
import com.android.agentos.core.models.Tool
import com.android.agentos.core.models.ToolResult

class YouTubeTool : Tool {
    override val name: String = "YouTube"
    override val description: String = "Video platform for searching and watching videos"
    override val supportedActions: List<ActionType> = listOf(ActionType.OPEN_APP, ActionType.TYPE_TEXT, ActionType.CLICK)

    override fun canHandle(action: AgentAction): Boolean {
        return action.target?.contains("youtube") == true
    }

    override fun execute(action: AgentAction): ToolResult {
        return when (action.type) {
            ActionType.OPEN_APP -> ToolResult(true, "Opening YouTube")
            ActionType.TYPE_TEXT -> ToolResult(true, "Searching in YouTube: ${action.text}")
            ActionType.CLICK -> ToolResult(true, "Clicking in YouTube: ${action.target}")
            else -> ToolResult(false, "Unsupported action for YouTube")
        }
    }
}

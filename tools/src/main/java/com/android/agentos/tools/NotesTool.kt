package com.android.agentos.tools

import com.android.agentos.core.models.*

class NotesTool : Tool {
    override val name: String = "Notes"
    override val description: String = "Note-taking app for creating and managing notes"
    override val supportedActions: List<ActionType> = listOf(ActionType.OPEN_APP, ActionType.TYPE_TEXT, ActionType.CLICK)

    override fun canHandle(action: AgentAction): Boolean {
        return action.target?.contains("notes", ignoreCase = true) == true ||
               action.target?.contains("keep", ignoreCase = true) == true
    }

    override fun execute(action: AgentAction): ToolResult {
        return when (action.type) {
            ActionType.OPEN_APP -> ToolResult(true, "Opening Notes")
            ActionType.TYPE_TEXT -> ToolResult(true, "Writing note: ${action.text}")
            ActionType.CLICK -> ToolResult(true, "Clicking in Notes: ${action.target}")
            else -> ToolResult(false, "Unsupported action for Notes")
        }
    }
}

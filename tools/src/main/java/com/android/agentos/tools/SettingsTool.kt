package com.android.agentos.tools

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ActionType
import com.android.agentos.core.models.Tool
import com.android.agentos.core.models.ToolResult

class SettingsTool : Tool {
    override val name: String = "Settings"
    override val description: String = "System settings for Wi-Fi, Bluetooth, and device configuration"
    override val supportedActions: List<ActionType> = listOf(ActionType.OPEN_APP, ActionType.CLICK)

    override fun canHandle(action: AgentAction): Boolean {
        return action.target?.contains("settings", ignoreCase = true) == true
    }

    override fun execute(action: AgentAction): ToolResult {
        return when (action.type) {
            ActionType.OPEN_APP -> ToolResult(true, "Opening Settings")
            ActionType.CLICK -> ToolResult(true, "Clicking in Settings: ${action.target}")
            else -> ToolResult(false, "Unsupported action for Settings")
        }
    }
}

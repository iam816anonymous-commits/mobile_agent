package com.android.agentos.ai

import com.android.agentos.core.models.*

class RuleBasedLLMProvider : LLMProvider {
    override val name: String = "RuleBased-Phase1"

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        val steps = mutableListOf<AgentAction>()
        val normalizedInput = userInput.lowercase()

        when {
            normalizedInput.contains("chrome") -> {
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.android.chrome"))
                if (normalizedInput.contains("search")) {
                    val query = userInput.substring(userInput.lowercase().indexOf("search") + "search".length).trim()
                    steps.add(AgentAction(ActionType.CLICK, target = "Search or type web address"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = query))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = query)) // Verify results page contains query
                }
            }
            normalizedInput.contains("youtube") -> {
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.google.android.youtube"))
                if (normalizedInput.contains("find") || normalizedInput.contains("search")) {
                    val keyword = if (normalizedInput.contains("find")) "find" else "search"
                    val query = userInput.substring(userInput.lowercase().indexOf(keyword) + keyword.length).trim()
                    steps.add(AgentAction(ActionType.CLICK, target = "Search"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = query))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = query))
                }
            }
            else -> {
                steps.add(AgentAction(ActionType.WAIT))
            }
        }
        return Plan(goal = userInput, steps = steps)
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): Boolean {
        // Simple heuristic verification based on screen context
        return when (action.type) {
            ActionType.OPEN_APP -> screenContext.any { it.className.contains("MainActivity", ignoreCase = true) } // Very rough
            ActionType.VERIFY_ELEMENT -> screenContext.any {
                it.text?.contains(action.target ?: "", ignoreCase = true) == true ||
                it.contentDescription?.contains(action.target ?: "", ignoreCase = true) == true
            }
            else -> true // Assume success for others in Phase 1
        }
    }
}

package com.android.agentos.ai

import com.android.agentos.core.models.*

class RuleBasedLLMProvider : LLMProvider {
    override val name: String = "RuleBased-Phase1"

    override suspend fun generatePlan(userInput: String, screenContext: List<ScreenElement>): Plan {
        val steps = mutableListOf<AgentAction>()
        val normalizedInput = userInput.lowercase()

        when {
            normalizedInput.contains("chrome") && (normalizedInput.contains("keep") || normalizedInput.contains("note")) -> {
                // Requested Demo Workflow: Chrome Search -> Keep Note
                val query = "Android AI agents"
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.android.chrome"))
                steps.add(AgentAction(ActionType.CLICK, target = "Search or type web address"))
                steps.add(AgentAction(ActionType.TYPE_TEXT, text = query))
                steps.add(AgentAction(ActionType.CLICK, target = "Enter"))
                steps.add(AgentAction(ActionType.WAIT))
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.google.android.keep"))
                steps.add(AgentAction(ActionType.CLICK, target = "New text note"))
                steps.add(AgentAction(ActionType.TYPE_TEXT, text = "Research Result for $query: Local Agent OS is functional."))
                steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = "Research Result"))
            }
            normalizedInput.contains("chrome") -> {
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.android.chrome"))
                if (normalizedInput.contains("search")) {
                    val query = userInput.substring(userInput.lowercase().indexOf("search") + "search".length).trim()
                    steps.add(AgentAction(ActionType.CLICK, target = "Search or type web address"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = query))
                    steps.add(AgentAction(ActionType.CLICK, target = "Enter")) // Submit search
                    steps.add(AgentAction(ActionType.WAIT))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = query))
                }
            }
            normalizedInput.contains("youtube") -> {
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.google.android.youtube"))
                if (normalizedInput.contains("find") || normalizedInput.contains("search")) {
                    val keyword = if (normalizedInput.contains("find")) "find" else "search"
                    val query = userInput.substring(userInput.lowercase().indexOf(keyword) + keyword.length).trim()
                    steps.add(AgentAction(ActionType.CLICK, target = "Search"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = query))
                    steps.add(AgentAction(ActionType.CLICK, target = "Enter"))
                    steps.add(AgentAction(ActionType.WAIT))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = query))
                }
            }
            normalizedInput.contains("settings") -> {
                steps.add(AgentAction(ActionType.OPEN_APP, target = "com.android.settings"))
                if (normalizedInput.contains("wi-fi") || normalizedInput.contains("wifi")) {
                    steps.add(AgentAction(ActionType.CLICK, target = "Network & internet"))
                    steps.add(AgentAction(ActionType.CLICK, target = "Internet"))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = "Wi-Fi"))
                }
            }
            normalizedInput.contains("notes") || normalizedInput.contains("note") -> {
                if (normalizedInput.contains("weather") || normalizedInput.contains("summarize")) {
                    // Knowledge task: Research & Summarize
                    steps.add(AgentAction(ActionType.OPEN_APP, target = "com.android.chrome"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = userInput))
                    steps.add(AgentAction(ActionType.CLICK, target = "Enter"))
                    steps.add(AgentAction(ActionType.MONITOR_FOR_ELEMENT, target = "Results"))
                    steps.add(AgentAction(ActionType.OPEN_APP, target = "com.google.android.keep"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = "Summary of: $userInput"))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = "Summary"))
                } else if (normalizedInput.contains("chrome") && normalizedInput.contains("search")) {
                    // Multi-step: Search Chrome then Note
                    val query = userInput.substring(userInput.lowercase().indexOf("search") + "search".length).split("then").first().trim()
                    steps.add(AgentAction(ActionType.OPEN_APP, target = "com.android.chrome"))
                    steps.add(AgentAction(ActionType.CLICK, target = "Search or type web address"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = query))
                    steps.add(AgentAction(ActionType.CLICK, target = "Enter"))
                    steps.add(AgentAction(ActionType.VERIFY_ELEMENT, target = query))
                    steps.add(AgentAction(ActionType.OPEN_APP, target = "com.google.android.keep")) // Using Keep as default Notes
                    steps.add(AgentAction(ActionType.CLICK, target = "New text note"))
                    steps.add(AgentAction(ActionType.TYPE_TEXT, text = "Research for $query: Success"))
                } else {
                    steps.add(AgentAction(ActionType.OPEN_APP, target = "com.google.android.keep"))
                    if (normalizedInput.contains("create") || normalizedInput.contains("write")) {
                        val note = userInput.substring(userInput.lowercase().indexOf("note") + "note".length).trim()
                        steps.add(AgentAction(ActionType.CLICK, target = "New text note"))
                        steps.add(AgentAction(ActionType.TYPE_TEXT, text = note))
                    }
                }
            }
            else -> {
                steps.add(AgentAction(ActionType.WAIT))
            }
        }
        return Plan(goal = userInput, steps = steps)
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        // Simple heuristic verification based on screen context
        return when (action.type) {
            ActionType.OPEN_APP -> {
                val success = screenContext.isNotEmpty() // Very rough
                VerificationResult(success, if (success) 0.7f else 0.0f, if (success) "App visible" else "App not found")
            }
            ActionType.VERIFY_ELEMENT -> {
                val found = screenContext.any {
                    it.text?.contains(action.target ?: "", ignoreCase = true) == true ||
                    it.contentDescription?.contains(action.target ?: "", ignoreCase = true) == true
                }
                VerificationResult(found, if (found) 0.9f else 0.0f, if (found) "Element ${action.target} found" else "Element not found")
            }
            else -> VerificationResult(true, 1.0f, "Assumed success")
        }
    }
}

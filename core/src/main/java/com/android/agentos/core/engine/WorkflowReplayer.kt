package com.android.agentos.core.engine

import com.android.agentos.core.models.*

class WorkflowReplayer(
    private val executor: Executor,
    private val accessibilityProvider: AccessibilityProvider
) {
    suspend fun replay(graph: WorkflowGraph) {
        var currentNodeId: String? = graph.startNodeId

        while (currentNodeId != null) {
            val node = graph.nodes.find { it.id == currentNodeId } ?: break

            // Execute the action in the node with node-specific constraints
            val plan = Plan(goal = "Replaying ${graph.name}", steps = listOf(node.action))

            // In a real implementation, we would pass timeout and maxRetries to executor
            executor.execute(plan)

            // Evaluate transitions
            val screenContext = accessibilityProvider.getCurrentScreenHierarchy()
            val outcome = if (plan.status == PlanStatus.COMPLETED) "SUCCESS" else "FAILURE"

            // Priority 1: Specific SUCCESS/FAILURE transitions
            // Priority 2: Screen-based recovery transitions
            val transition = node.transitions.find { it.condition == outcome }
                ?: node.transitions.find { it.condition.startsWith("SCREEN_MATCH") && matchScreen(it.condition, screenContext) }
                ?: node.transitions.find { it.condition == "ANY_FAILURE" && outcome == "FAILURE" }

            currentNodeId = transition?.targetNodeId
        }
    }

    private fun matchScreen(condition: String, elements: List<ScreenElement>): Boolean {
        val targetText = condition.substringAfter("SCREEN_MATCH:").trim()
        return elements.any { it.text?.contains(targetText, ignoreCase = true) == true }
    }
}

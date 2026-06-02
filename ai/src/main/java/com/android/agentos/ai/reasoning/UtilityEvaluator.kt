package com.android.agentos.ai.reasoning

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ActionType

class UtilityEvaluator {

    fun rankActions(actions: List<AgentAction>): List<AgentAction> {
        return actions.sortedByDescending { calculateUtility(it) }
    }

    private fun calculateUtility(action: AgentAction): Float {
        var score = 0.5f

        // Prefer specific targets over generic ones
        if (action.target != null) score += 0.2f

        // Prefer destructive or high-value actions if they are explicit
        if (action.type == ActionType.TYPE_TEXT && action.text?.isNotEmpty() == true) score += 0.1f

        // Penalize WAIT if it's the only action
        if (action.type == ActionType.WAIT) score -= 0.1f

        return score.coerceIn(0.0f, 1.0f)
    }
}

package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.CognitiveBudgetRecord
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enforces limits on cognitive resources (Reasoning, Verification, Storage).
 */
class CognitiveBudgeter(private val db: AgentDatabase) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    suspend fun canAfford(category: BudgetCategory, cost: Double): Boolean {
        val date = dateFormat.format(Date())
        val budget = db.agentDao().getBudget(date) ?: CognitiveBudgetRecord(date, 0.0, 0.0, 0L)

        return when (category) {
            BudgetCategory.VERIFICATION -> (budget.verificationSpent + cost) <= budget.verificationLimit
            BudgetCategory.REASONING -> (budget.reasoningSpent + cost) <= budget.reasoningLimit
        }
    }

    suspend fun recordSpend(category: BudgetCategory, cost: Double) {
        val date = dateFormat.format(Date())
        val budget = db.agentDao().getBudget(date) ?: CognitiveBudgetRecord(date, 0.0, 0.0, 0L)

        val updated = when (category) {
            BudgetCategory.VERIFICATION -> budget.copy(verificationSpent = budget.verificationSpent + cost)
            BudgetCategory.REASONING -> budget.copy(reasoningSpent = budget.reasoningSpent + cost)
        }
        db.agentDao().insertBudget(updated)
    }
}

enum class BudgetCategory { VERIFICATION, REASONING }

/**
 * Prioritizes reasoning effort based on knowledge utility and goals.
 */
class AttentionEngine(private val db: AgentDatabase) {
    suspend fun calculateAttentionWeight(knowledgeId: Long, currentGoal: String): Float {
        val fact = db.agentDao().getAllKnowledge().find { it.id == knowledgeId } ?: return 0f

        // Boost if matches goal
        val goalBoost = if (currentGoal.contains(fact.title, true)) 0.5f else 0f
        val utilityWeight = fact.trustScore * 0.3f + (fact.usefulnessCount.toFloat() / 10f) * 0.2f

        return (utilityWeight + goalBoost).coerceIn(0f, 1f)
    }
}

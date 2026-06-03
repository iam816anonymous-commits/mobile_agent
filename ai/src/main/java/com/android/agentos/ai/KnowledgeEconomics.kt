package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase

/**
 * Calculates long-term knowledge utility and economic value.
 */
class KnowledgeEconomics(private val db: AgentDatabase) {

    suspend fun calculateUtility(knowledgeId: Long): Double {
        val fact = db.agentDao().getAllKnowledge().find { it.id == knowledgeId } ?: return 0.0

        // Multi-factor utility formula
        val reuseFactor = fact.usefulnessCount * 2.0
        val trustFactor = fact.trustScore * 10.0
        val reasoningWeight = fact.reasoningContribution * 5.0
        val costPenalty = fact.verificationCost * 1.5

        return reuseFactor + trustFactor + reasoningWeight - costPenalty
    }

    suspend fun getIntelligenceDividendTotal(): Double {
        val allKnowledge = db.agentDao().getAllKnowledge()
        return allKnowledge.sumOf { calculateUtility(it.id) }
    }
}

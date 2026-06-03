package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase

/**
 * Tracks the exact contribution of retrieved knowledge to reasoning.
 */
class KnowledgeUtilityTracker(private val db: AgentDatabase) {

    suspend fun trackReuse(knowledgeId: Long, contributionScore: Float) {
        val knowledge = db.agentDao().getAllKnowledge().find { it.id == knowledgeId }
        knowledge?.let {
            val newUsefulness = it.usefulnessCount + 1
            // We increase trust score when an item is reused successfully
            val newTrust = (it.trustScore + contributionScore * 0.1f).coerceAtMost(1.0f)
            db.agentDao().insertKnowledge(it.copy(
                usefulnessCount = newUsefulness,
                trustScore = newTrust,
                lastVerifiedTimestamp = System.currentTimeMillis()
            ))
        }
    }

    suspend fun calculateIntelligenceDividend(): Float {
        val knowledgeCount = db.agentDao().getAllKnowledge().size
        val reuseCount = db.agentDao().getAllKnowledge().sumOf { it.usefulnessCount }

        if (knowledgeCount == 0) return 0f
        return (reuseCount.toFloat() / knowledgeCount) * 0.5f // Simplified formula
    }
}

package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity

/**
 * Manages knowledge freshness and applies confidence decay.
 */
class FreshnessEngine(private val db: AgentDatabase) {

    suspend fun applyDecay() {
        val now = System.currentTimeMillis()
        val knowledge = db.agentDao().getAllKnowledge()

        knowledge.forEach { item ->
            val ageDays = (now - item.lastVerifiedTimestamp) / (1000 * 60 * 60 * 24)
            if (ageDays > 0) {
                val newTrust = (item.trustScore - (ageDays * item.decayRate)).coerceAtLeast(0.1f)
                db.agentDao().insertKnowledge(item.copy(trustScore = newTrust))
            }
        }
    }
}

/**
 * Detects conflicting facts between different sources.
 */
class ContradictionDetector {
    fun detect(item1: KnowledgeEntity, item2: KnowledgeEntity): Boolean {
        // Simplified conflict detection based on title similarity but content difference
        if (item1.title == item2.title && item1.content != item2.content) {
            return true
        }
        return false
    }
}

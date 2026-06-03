package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity
import com.android.agentos.memory.EvidenceRecord

/**
 * Handles dynamic trust updates and contradiction resolution.
 */
class BeliefRevisionEngine(private val db: AgentDatabase) {

    suspend fun reviseBelief(knowledgeId: Long, newEvidence: EvidenceRecord) {
        val fact = db.agentDao().getAllKnowledge().find { it.id == knowledgeId }
        fact?.let {
            // Update trust based on evidence confidence
            val weight = 0.3f
            val revisedTrust = (it.trustScore * (1 - weight) + newEvidence.confidence * weight).coerceIn(0f, 1f)

            db.agentDao().insertKnowledge(it.copy(
                trustScore = revisedTrust,
                lastVerifiedTimestamp = System.currentTimeMillis()
            ))

            // Trigger cascading updates
            CascadingUpdateManager(db).triggerUpdate(knowledgeId)
        }
    }
}

/**
 * Manages cascading updates when base knowledge changes.
 */
class CascadingUpdateManager(private val db: AgentDatabase) {
    suspend fun triggerUpdate(knowledgeId: Long) {
        val dependents = db.agentDao().getDependentsOf(knowledgeId)
        dependents.forEach { dep ->
            // Mark conclusion for re-computation
            val conclusion = db.agentDao().getAllKnowledge().find { it.id == dep.dependentKnowledgeId }
            conclusion?.let {
                // Heuristic: lower confidence of conclusions when sources change
                val newTrust = it.trustScore * 0.9f
                db.agentDao().insertKnowledge(it.copy(trustScore = newTrust))
            }
        }
    }
}

package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity
import com.android.agentos.memory.EvidenceRecord

/**
 * Tracks the origin and lineage of knowledge entries.
 */
class ProvenanceTracker(private val db: AgentDatabase) {

    suspend fun logProvenance(
        knowledgeId: Long,
        sourceApp: String?,
        sourceUrl: String?,
        modelName: String,
        workflowId: String?
    ) {
        val evidence = EvidenceRecord(
            knowledgeId = knowledgeId,
            claim = "Origin Verification",
            evidenceContent = "Captured from $sourceApp via $modelName",
            sourceApp = sourceApp,
            sourceUrl = sourceUrl,
            modelName = modelName,
            confidence = 1.0f
        )
        db.agentDao().insertEvidence(evidence)
    }

    fun isReliableSource(source: String): Boolean {
        // Source priority logic
        return source.contains("manual", true) || source.contains("official", true)
    }
}

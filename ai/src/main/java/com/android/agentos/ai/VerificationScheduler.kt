package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity
import com.android.agentos.memory.VerificationLog

/**
 * Schedules and executes active revalidation of knowledge claims.
 */
class VerificationScheduler(private val db: AgentDatabase) {

    suspend fun findClaimsToVerify(): List<KnowledgeEntity> {
        val now = System.currentTimeMillis()
        val knowledge = db.agentDao().getAllKnowledge()

        // Priority: low trust + old verification
        return knowledge.filter {
            it.trustScore < 0.7f || (now - it.lastVerifiedTimestamp) > (1000 * 60 * 60 * 24 * 7) // 7 days
        }
    }

    suspend fun logVerification(knowledgeId: Long, success: Boolean, method: String) {
        val knowledge = db.agentDao().getAllKnowledge().find { it.id == knowledgeId }
        knowledge?.let {
            val newTrust = if (success) (it.trustScore + 0.1f).coerceAtMost(1.0f) else (it.trustScore - 0.2f).coerceAtMost(0.0f)

            db.agentDao().insertVerification(VerificationLog(
                knowledgeId = knowledgeId,
                verifiedBy = "VerificationScheduler",
                method = method,
                outcome = success,
                updatedTrust = newTrust
            ))

            db.agentDao().insertKnowledge(it.copy(
                trustScore = newTrust,
                lastVerifiedTimestamp = System.currentTimeMillis()
            ))
        }
    }
}

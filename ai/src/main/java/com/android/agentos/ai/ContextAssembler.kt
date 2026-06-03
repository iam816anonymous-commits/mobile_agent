package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity
import com.android.agentos.core.models.Plan

/**
 * Assembles high-density reasoning context from multiple memory sources.
 */
class ContextAssembler(private val db: AgentDatabase) {

    suspend fun assembleContext(goal: String): String {
        val facts = db.agentDao().searchKnowledge("%$goal%")
        val entities = db.agentDao().searchGraphEntities("%$goal%")

        val contextBuilder = StringBuilder()
        contextBuilder.append("Relevant Evidence-Based Context:\n")

        facts.take(5).forEach { fact ->
            val evidence = db.agentDao().getEvidenceForKnowledge(fact.id)
            val trustPrefix = if (fact.trustScore > 0.8f) "[Verified]" else if (fact.trustScore < 0.4f) "[Low Confidence]" else "[Unverified]"

            contextBuilder.append("- $trustPrefix ${fact.content}\n")
            if (evidence.isNotEmpty()) {
                contextBuilder.append("  (Source: ${evidence.first().sourceApp ?: evidence.first().modelName})\n")
            }
        }

        if (entities.isNotEmpty()) {
            contextBuilder.append("- Key Entities: ${entities.joinToString { it.name }}\n")
        }

        return contextBuilder.toString()
    }
}

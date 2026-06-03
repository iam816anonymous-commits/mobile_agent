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
        contextBuilder.append("Relevant Context:\n")

        if (facts.isNotEmpty()) {
            contextBuilder.append("- Facts: ${facts.take(3).joinToString { it.content }}\n")
        }

        if (entities.isNotEmpty()) {
            contextBuilder.append("- Entities: ${entities.joinToString { it.name }}\n")
        }

        return contextBuilder.toString()
    }
}

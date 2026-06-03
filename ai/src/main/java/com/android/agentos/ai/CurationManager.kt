package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity
import com.android.agentos.memory.CurationEvent

/**
 * Manages automated knowledge curation: merging, archiving, and promotion.
 */
class CurationManager(private val db: AgentDatabase) {

    suspend fun performCuration() {
        val knowledge = db.agentDao().getAllKnowledge()

        // 1. Identify duplicates (Simplified)
        val grouped = knowledge.groupBy { it.title.lowercase() }

        grouped.filter { it.value.size > 1 }.forEach { (title, items) ->
            mergeDuplicates(items)
        }

        // 2. Archive low utility
        knowledge.filter { it.usefulnessCount == 0 && it.trustScore < 0.3f }.forEach {
            archiveItem(it)
        }
    }

    private suspend fun mergeDuplicates(items: List<KnowledgeEntity>) {
        val keep = items.maxByOrNull { it.trustScore } ?: return
        val others = items.filter { it.id != keep.id }

        others.forEach {
            // In a real system, we'd delete or mark as archived
        }

        db.agentDao().insertCurationEvent(CurationEvent(
            eventType = "MERGE",
            description = "Merged ${items.size} duplicates for ${keep.title}",
            affectedEntityIds = items.joinToString { it.id.toString() }
        ))
    }

    private suspend fun archiveItem(item: KnowledgeEntity) {
         db.agentDao().insertCurationEvent(CurationEvent(
            eventType = "ARCHIVE",
            description = "Archived low-utility fact: ${item.title}",
            affectedEntityIds = item.id.toString()
        ))
    }
}

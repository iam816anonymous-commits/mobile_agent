package com.android.agentos.core.memory

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ExecutionResult

class SemanticMemory {

    private val memoryStore = mutableListOf<MemoryEntry>()
    var totalRetrievals: Int = 0
    var successfulHits: Int = 0

    fun store(goal: String, actions: List<AgentAction>, success: Boolean) {
        memoryStore.add(MemoryEntry(goal, actions, success))
    }

    fun retrieveRelevant(query: String): List<MemoryEntry> {
        totalRetrievals++
        val matches = memoryStore.filter { entry ->
            entry.goal.contains(query, ignoreCase = true) ||
            entry.actions.any { it.target?.contains(query, ignoreCase = true) == true }
        }
        if (matches.isNotEmpty()) successfulHits++
        return matches.take(5)
    }

    fun getHitRate(): Float = if (totalRetrievals > 0) successfulHits.toFloat() / totalRetrievals else 0f

    data class MemoryEntry(
        val goal: String,
        val actions: List<AgentAction>,
        val success: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
}

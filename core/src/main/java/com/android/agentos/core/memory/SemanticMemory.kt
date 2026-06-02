package com.android.agentos.core.memory

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ExecutionResult

class SemanticMemory {

    private val memoryStore = mutableListOf<MemoryEntry>()

    fun store(goal: String, actions: List<AgentAction>, success: Boolean) {
        memoryStore.add(MemoryEntry(goal, actions, success))
    }

    fun retrieveRelevant(query: String): List<MemoryEntry> {
        return memoryStore.filter { entry ->
            entry.goal.contains(query, ignoreCase = true) ||
            entry.actions.any { it.target?.contains(query, ignoreCase = true) == true }
        }.take(5)
    }

    data class MemoryEntry(
        val goal: String,
        val actions: List<AgentAction>,
        val success: Boolean,
        val timestamp: Long = System.currentTimeMillis()
    )
}

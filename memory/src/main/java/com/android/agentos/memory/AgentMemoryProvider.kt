package com.android.agentos.memory

import com.android.agentos.core.engine.MemoryProvider
import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.ExecutionResult
import com.android.agentos.core.models.FailureLog

class AgentMemoryProvider(private val db: AgentDatabase) : MemoryProvider {
    override suspend fun logAction(planId: String, action: AgentAction, result: ExecutionResult) {
        db.agentDao().insertActionHistory(
            ActionHistoryEntity(
                id = action.id,
                planId = planId,
                subgoalId = null, // In a real setup, we'd pass this from the executor
                type = action.type.name,
                target = action.target,
                text = action.text,
                success = result.success,
                timestamp = result.timestamp,
                depth = 0 // Placeholder for depth analysis
            )
        )
    }

    override suspend fun logFailure(failure: FailureLog) {
        db.agentDao().insertFailure(
            FailureHistoryEntity(
                actionId = failure.actionId,
                category = failure.category.name,
                errorType = failure.errorType,
                errorMessage = failure.errorMessage,
                timestamp = failure.timestamp
            )
        )
    }
}

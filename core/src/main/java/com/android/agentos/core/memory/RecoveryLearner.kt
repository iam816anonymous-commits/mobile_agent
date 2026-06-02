package com.android.agentos.core.memory

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.FailureLog

class RecoveryLearner {

    private val recoveryLog = mutableListOf<RecoverySequence>()

    fun recordSuccessfulRecovery(failure: FailureLog, recoveryActions: List<AgentAction>) {
        recoveryLog.add(RecoverySequence(failure.errorType, failure.errorMessage, recoveryActions))
    }

    fun suggestRecovery(failure: FailureLog): List<AgentAction>? {
        return recoveryLog.find { it.errorType == failure.errorType && it.errorMessage == failure.errorMessage }?.recoveryActions
    }

    data class RecoverySequence(
        val errorType: String,
        val errorMessage: String,
        val recoveryActions: List<AgentAction>
    )
}

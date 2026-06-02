package com.android.agentos.core.memory

import com.android.agentos.core.models.AgentAction
import com.android.agentos.core.models.FailureLog

class RecoveryLearner {

    private val recoveryLog = mutableListOf<RecoverySequence>()

    fun recordSuccessfulRecovery(failure: FailureLog, recoveryActions: List<AgentAction>) {
        val existing = recoveryLog.find { it.matches(failure) && it.recoveryActions == recoveryActions }
        if (existing != null) {
            existing.successCount++
        } else {
            recoveryLog.add(RecoverySequence(failure.errorType, failure.errorMessage, recoveryActions, 1))
        }
    }

    fun suggestRecovery(failure: FailureLog): List<AgentAction>? {
        return recoveryLog
            .filter { it.matches(failure) }
            .maxByOrNull { it.successCount }
            ?.recoveryActions
    }

    fun getRecoveryStats(): String {
        return recoveryLog.joinToString("\n") {
            "${it.errorType}: ${it.successCount} successes"
        }
    }

    data class RecoverySequence(
        val errorType: String,
        val errorMessage: String,
        val recoveryActions: List<AgentAction>,
        var successCount: Int = 0
    ) {
        fun matches(failure: FailureLog): Boolean {
            return errorType == failure.errorType &&
                   (errorMessage == failure.errorMessage || failure.errorMessage.contains(errorMessage))
        }
    }
}

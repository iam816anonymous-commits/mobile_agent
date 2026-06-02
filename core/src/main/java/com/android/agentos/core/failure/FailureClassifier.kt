package com.android.agentos.core.failure

import com.android.agentos.core.models.FailureCategory
import com.android.agentos.core.models.FailureLog
import com.android.agentos.core.models.ScreenElement

class FailureClassifier {

    fun classifyFailure(errorType: String, errorMessage: String, screenContext: List<ScreenElement>): FailureCategory {
        return when {
            errorMessage.contains("not found", ignoreCase = true) || errorMessage.contains("not visible", ignoreCase = true) -> FailureCategory.UI_NOT_FOUND
            errorMessage.contains("timeout", ignoreCase = true) -> FailureCategory.TIMEOUT
            errorMessage.contains("llm", ignoreCase = true) || errorMessage.contains("json", ignoreCase = true) -> FailureCategory.LLM_ERROR
            screenContext.isEmpty() -> FailureCategory.APP_CRASH
            else -> FailureCategory.UNKNOWN
        }
    }
}

package com.android.agentos.core.models

enum class SemanticRole {
    SEARCH_BAR,
    CONTENT_LIST,
    LIST_ITEM,
    ACTION_BUTTON,
    NAVIGATION_BACK,
    INPUT_FIELD,
    CONFIRMATION_DIALOG,
    TAB_ITEM,
    UNKNOWN
}

data class SemanticElement(
    val role: SemanticRole,
    val element: ScreenElement,
    val confidence: Float
)

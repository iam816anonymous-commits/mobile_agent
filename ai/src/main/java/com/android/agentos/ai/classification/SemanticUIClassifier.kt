package com.android.agentos.ai.classification

import com.android.agentos.core.models.ScreenElement
import com.android.agentos.core.models.SemanticRole
import com.android.agentos.core.models.SemanticElement

class SemanticUIClassifier {

    fun classifyElements(elements: List<ScreenElement>): List<SemanticElement> {
        return elements.map { element ->
            val role = when {
                element.className.contains("EditText", ignoreCase = true) -> {
                    if (element.text?.contains("search", ignoreCase = true) == true ||
                        element.id?.contains("search", ignoreCase = true) == true) SemanticRole.SEARCH_BAR
                    else SemanticRole.INPUT_FIELD
                }
                element.className.contains("Button", ignoreCase = true) || element.isClickable -> {
                    if (element.text?.contains("back", ignoreCase = true) == true ||
                        element.contentDescription?.contains("back", ignoreCase = true) == true) SemanticRole.NAVIGATION_BACK
                    else SemanticRole.ACTION_BUTTON
                }
                element.className.contains("RecyclerView", ignoreCase = true) ||
                element.className.contains("ListView", ignoreCase = true) -> SemanticRole.CONTENT_LIST
                else -> SemanticRole.UNKNOWN
            }
            SemanticElement(role, element, 0.8f)
        }
    }
}

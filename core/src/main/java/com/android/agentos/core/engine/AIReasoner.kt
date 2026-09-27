package com.android.agentos.core.engine

import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.ScreenElement

/**
 * Optional AI Bridge Interface for optional external reasoning.
 * When disabled, the agent relies strictly on fast, local deterministic logic.
 */
interface AIReasoner {
    suspend fun reason(screenSummary: String, goal: String): Plan?
}

/**
 * Utility to format active screen node hierarchy into a compact text summary
 * suitable for low-RAM devices and lightweight reasoning payloads.
 */
object CompactHierarchyFormatter {

    /**
     * Converts a list of ScreenElement into a concise, compact string summary.
     * Example output: "[Button: 'Search' (id: url_bar)], [TextField: 'Enter text']"
     */
    fun formatCompactSummary(elements: List<ScreenElement>, maxElements: Int = 30): String {
        if (elements.isEmpty()) return "Screen: Empty"

        val sb = StringBuilder()
        sb.append("Screen Elements (").append(elements.size).append(" total):\n")

        val interactiveElements = elements.filter {
            it.isClickable || !it.text.isNullOrBlank() || !it.contentDescription.isNullOrBlank()
        }.take(maxElements)

        for (el in interactiveElements) {
            val type = el.className.substringAfterLast('.')
            val label = el.text ?: el.contentDescription ?: ""
            val id = el.id?.substringAfterLast(":id/") ?: ""

            sb.append("- [").append(type)
            if (label.isNotBlank()) sb.append(": '").append(label).append("'")
            if (id.isNotBlank()) sb.append(" (id: ").append(id).append(")")
            if (el.isClickable) sb.append(" (clickable)")
            sb.append("]\n")
        }

        return sb.toString().trim()
    }
}

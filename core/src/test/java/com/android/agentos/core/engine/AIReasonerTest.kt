package com.android.agentos.core.engine

import com.android.agentos.core.models.Rect
import com.android.agentos.core.models.ScreenElement
import org.junit.Assert.*
import org.junit.Test

class AIReasonerTest {

    @Test
    fun testCompactHierarchyFormatterFormatsInteractiveElements() {
        val elements = listOf(
            ScreenElement(
                text = "Search or type web address",
                contentDescription = null,
                className = "android.widget.EditText",
                bounds = Rect(0, 0, 100, 100),
                isClickable = true,
                id = "com.android.chrome:id/url_bar"
            ),
            ScreenElement(
                text = null,
                contentDescription = null,
                className = "android.view.View",
                bounds = Rect(0, 0, 10, 10),
                isClickable = false,
                id = null
            ),
            ScreenElement(
                text = "Submit",
                contentDescription = "Submit search",
                className = "android.widget.Button",
                bounds = Rect(100, 100, 200, 200),
                isClickable = true,
                id = "com.android.chrome:id/submit_button"
            )
        )

        val summary = CompactHierarchyFormatter.formatCompactSummary(elements)

        assertTrue(summary.contains("EditText: 'Search or type web address'"))
        assertTrue(summary.contains("(id: url_bar)"))
        assertTrue(summary.contains("Button: 'Submit'"))
        assertFalse(summary.contains("android.view.View")) // Non-interactive element without text skipped
    }
}

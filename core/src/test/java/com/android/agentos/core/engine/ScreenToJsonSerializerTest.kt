package com.android.agentos.core.engine

import com.android.agentos.core.models.Rect
import com.android.agentos.core.models.ScreenElement
import org.junit.Assert.*
import org.junit.Test

class ScreenToJsonSerializerTest {

    @Test
    fun testSerializeCreatesCompactJsonString() {
        val elements = listOf(
            ScreenElement(
                text = "Search",
                contentDescription = null,
                className = "android.widget.Button",
                bounds = Rect(0, 0, 50, 50),
                isClickable = true,
                id = "com.app:id/search_btn"
            )
        )

        val jsonOutput = ScreenToJsonSerializer.serialize(elements)
        assertTrue(jsonOutput.contains("\"type\":\"Button\""))
        assertTrue(jsonOutput.contains("\"text\":\"Search\""))
        assertTrue(jsonOutput.contains("\"id\":\"search_btn\""))
    }
}

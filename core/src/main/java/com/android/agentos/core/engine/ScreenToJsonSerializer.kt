package com.android.agentos.core.engine

import com.android.agentos.core.models.ScreenElement
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class CompactNode(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val clickable: Boolean = false
)

/**
 * Converts screen hierarchy into a compact text-only JSON summary representing active interactive components.
 */
object ScreenToJsonSerializer {
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }

    fun serialize(elements: List<ScreenElement>, maxElements: Int = 30): String {
        val compactNodes = elements.filter {
            it.isClickable || !it.text.isNullOrBlank() || !it.contentDescription.isNullOrBlank()
        }.take(maxElements).map { el ->
            CompactNode(
                type = el.className.substringAfterLast('.'),
                text = el.text ?: el.contentDescription,
                id = el.id?.substringAfterLast(":id/"),
                clickable = el.isClickable
            )
        }
        return json.encodeToString(compactNodes)
    }
}

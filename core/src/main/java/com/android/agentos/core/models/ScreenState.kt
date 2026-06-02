package com.android.agentos.core.models

import kotlinx.serialization.Serializable

@Serializable
data class ScreenState(
    val packageName: String?,
    val activityName: String?,
    val elements: List<ScreenElement>,
    val classification: ScreenType = ScreenType.UNKNOWN,
    val confidence: Float = 1.0f
)

enum class ScreenType {
    UNKNOWN,
    HOME_SCREEN,
    CHROME_HOME,
    CHROME_SEARCH_RESULTS,
    YOUTUBE_HOME,
    YOUTUBE_SEARCH_RESULTS,
    SETTINGS_MAIN,
    SETTINGS_WIFI,
    WHATSAPP_CONVERSATION_LIST,
    WHATSAPP_CHAT
}

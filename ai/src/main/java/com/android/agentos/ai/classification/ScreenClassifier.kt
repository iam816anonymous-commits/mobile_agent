package com.android.agentos.ai.classification

import com.android.agentos.core.models.*

class ScreenClassifier {

    private val semanticClassifier = SemanticUIClassifier()

    fun classify(packageName: String?, elements: List<ScreenElement>): ScreenState {
        val semanticElements = semanticClassifier.classifyElements(elements)
        val candidates = mutableListOf<ClassificationCandidate>()

        // Semantic based (App independent)
        if (semanticElements.any { it.role == SemanticRole.SEARCH_BAR }) {
            candidates.add(ClassificationCandidate(ScreenType.UNKNOWN, 0.6f)) // Generalized Search state
        }

        // Chrome
        if (packageName == "com.android.chrome") {
            if (elements.any { it.text?.contains("google.com/search") == true }) {
                candidates.add(ClassificationCandidate(ScreenType.CHROME_SEARCH_RESULTS, 0.9f))
            } else {
                candidates.add(ClassificationCandidate(ScreenType.CHROME_HOME, 0.8f))
            }
        }

        // YouTube
        if (packageName == "com.google.android.youtube") {
            if (elements.any { it.id?.contains("search_results") == true }) {
                candidates.add(ClassificationCandidate(ScreenType.YOUTUBE_SEARCH_RESULTS, 0.9f))
            } else {
                candidates.add(ClassificationCandidate(ScreenType.YOUTUBE_HOME, 0.8f))
            }
        }

        // Settings
        if (packageName == "com.android.settings") {
            val hasWifiText = elements.any { it.text?.contains("Wi-Fi", ignoreCase = true) == true }
            val hasNetworkCategory = elements.any { it.text == "Network & internet" }

            if (hasWifiText && !hasNetworkCategory) {
                candidates.add(ClassificationCandidate(ScreenType.SETTINGS_WIFI, 0.9f))
            } else {
                candidates.add(ClassificationCandidate(ScreenType.SETTINGS_MAIN, 0.8f))
            }
        }

        val bestMatch = candidates.maxByOrNull { it.confidence } ?: ClassificationCandidate(ScreenType.UNKNOWN, 0.5f)

        return ScreenState(
            packageName = packageName,
            activityName = null,
            elements = elements,
            classification = bestMatch.type,
            confidence = bestMatch.confidence
        )
    }

    private data class ClassificationCandidate(val type: ScreenType, val confidence: Float)
}

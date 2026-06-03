package com.android.agentos.ai.classification

/**
 * Task complexity level.
 */
enum class TaskComplexity {
    LOW,    // Simple app launch, single click
    MEDIUM, // Multi-step within one app
    HIGH    // Cross-app, data synthesis, complex reasoning
}

/**
 * Result of task classification for routing.
 */
data class TaskClassification(
    val complexity: TaskComplexity,
    val requiresPrivacy: Boolean,
    val estimatedSteps: Int,
    val suggestedProvider: String? = null
)

/**
 * Classifies user intent to inform provider selection.
 */
class TaskClassifier {
    fun classify(userInput: String): TaskClassification {
        val input = userInput.lowercase()

        // Basic heuristic-based classification
        val requiresPrivacy = input.contains("message") ||
                             input.contains("mail") ||
                             input.contains("contact") ||
                             input.contains("password")

        val complexity = when {
            input.contains("then") || input.contains("and") || input.contains("research") -> TaskComplexity.HIGH
            input.length > 50 -> TaskComplexity.MEDIUM
            else -> TaskComplexity.LOW
        }

        return TaskClassification(
            complexity = complexity,
            requiresPrivacy = requiresPrivacy,
            estimatedSteps = when(complexity) {
                TaskComplexity.LOW -> 2
                TaskComplexity.MEDIUM -> 5
                TaskComplexity.HIGH -> 10
            }
        )
    }
}

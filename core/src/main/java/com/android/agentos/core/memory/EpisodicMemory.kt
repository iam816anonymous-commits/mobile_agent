package com.android.agentos.core.memory

import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.ExecutionResult
import kotlinx.serialization.Serializable

@Serializable
data class Episode(
    val plan: Plan,
    val results: List<ExecutionResult>,
    val timestamp: Long = System.currentTimeMillis()
)

class EpisodicMemory {
    private val episodes = mutableListOf<Episode>()

    fun storeEpisode(episode: Episode) {
        // Protection against memory poisoning: only store high-quality successful episodes
        val isHighConfidence = episode.results.all { it.success }
        if (episode.plan.status == com.android.agentos.core.models.PlanStatus.COMPLETED && isHighConfidence) {
            episodes.add(episode)
        }
    }

    fun retrieveSimilar(goal: String): List<Episode> {
        return episodes
            .map { it to calculateSimilarity(goal, it) }
            .filter { it.second > 0.3f }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(3)
    }

    private fun calculateSimilarity(query: String, episode: Episode): Float {
        val queryWords = query.lowercase().split(" ").toSet()
        val goalWords = episode.plan.goal.lowercase().split(" ").toSet()
        val intersect = queryWords.intersect(goalWords).size
        return intersect.toFloat() / (queryWords.size + goalWords.size - intersect)
    }

    fun getSuccessCount(): Int = episodes.size
}

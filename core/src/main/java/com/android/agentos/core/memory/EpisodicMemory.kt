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
        if (episode.plan.status == com.android.agentos.core.models.PlanStatus.COMPLETED) {
            episodes.add(episode)
        }
    }

    fun retrieveSimilar(goal: String): List<Episode> {
        return episodes.filter { it.plan.goal.contains(goal, ignoreCase = true) || goal.contains(it.plan.goal, ignoreCase = true) }.take(3)
    }

    fun getSuccessCount(): Int = episodes.size
}

package com.android.agentos.ai

import com.android.agentos.memory.AgentDatabase
import com.android.agentos.memory.KnowledgeEntity

/**
 * Promotes knowledge relevant to recurring goals and user interests.
 */
class GoalAwarePrioritizer(private val db: AgentDatabase) {

    suspend fun prioritize(knowledge: List<KnowledgeEntity>, activeGoal: String): List<KnowledgeEntity> {
        val interests = db.agentDao().getAllInterests()

        return knowledge.map { fact ->
            var score = fact.attentionWeight

            // Check against interests
            interests.forEach { interest ->
                if (fact.content.contains(interest.interest, true)) {
                    score += interest.strength * 0.2f
                }
            }

            // Check against current goal
            if (activeGoal.contains(fact.title, true)) {
                score += 0.4f
            }

            fact.copy(attentionWeight = score.coerceIn(0f, 1f))
        }.sortedByDescending { it.attentionWeight }
    }
}

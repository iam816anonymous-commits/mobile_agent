package com.android.agentos.memory

import com.android.agentos.core.models.LLMProvider

/**
 * Calculates real-time performance analytics for providers.
 */
class PerformanceAnalytics(private val db: AgentDatabase) {

    suspend fun getProviderStats(): List<ProviderStats> {
        val metrics = db.agentDao().getProviderMetrics()
        return metrics.groupBy { it.providerName }.map { (name, list) ->
            val totalLatency = list.sumOf { it.latencyMs }
            val totalCost = list.sumOf { it.estimatedCost }
            val successCount = list.count { it.success }
            val totalCount = list.size

            ProviderStats(
                name = name,
                avgLatencyMs = if (totalCount > 0) totalLatency / totalCount else 0,
                totalCost = totalCost,
                successRate = if (totalCount > 0) successCount.toFloat() / totalCount else 0f,
                utilityPerCost = if (totalCost > 0) successCount.toDouble() / totalCost else 0.0
            )
        }
    }
}

data class ProviderStats(
    val name: String,
    val avgLatencyMs: Long,
    val totalCost: Double,
    val successRate: Float,
    val utilityPerCost: Double
)

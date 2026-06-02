package com.android.agentos.core.benchmark

import com.android.agentos.core.engine.Executor
import com.android.agentos.core.models.Plan
import com.android.agentos.core.models.PlanStatus
import com.android.agentos.core.models.LLMProvider

class BenchmarkSuite(
    private val executor: Executor,
    private val llmProvider: LLMProvider
) {
    private val results = mutableListOf<BenchmarkResult>()

    suspend fun runNovelTask(command: String) {
        val startTime = System.currentTimeMillis()
        val plan = llmProvider.generatePlan(command, emptyList())
        executor.execute(plan)
        val duration = System.currentTimeMillis() - startTime

        results.add(BenchmarkResult(
            command = command,
            success = plan.status == PlanStatus.COMPLETED,
            stepsCount = plan.steps.size,
            durationMs = duration
        ))
    }

    fun getSummary(): String {
        val total = results.size
        val successful = results.count { it.success }
        val avgDuration = if (total > 0) results.map { it.durationMs }.average() else 0.0

        return """
            Benchmark Summary:
            Total Tasks: $total
            Successful: $successful (${if(total>0) (successful.toFloat()/total*100).toInt() else 0}%)
            Average Duration: ${avgDuration.toInt()}ms
        """.trimIndent()
    }

    data class BenchmarkResult(
        val command: String,
        val success: Boolean,
        val stepsCount: Int,
        val durationMs: Long,
        val app: String? = null
    )

    fun getPerAppStats(): Map<String, Int> {
        return results.filter { it.app != null }.groupBy { it.app!! }.mapValues { entry ->
            (entry.value.count { it.success }.toFloat() / entry.value.size * 100).toInt()
        }
    }
}

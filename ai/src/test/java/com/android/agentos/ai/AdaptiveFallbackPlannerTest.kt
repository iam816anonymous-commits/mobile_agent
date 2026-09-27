package com.android.agentos.ai

import com.android.agentos.core.models.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class AdaptiveFallbackPlannerTest {

    @Test
    fun testFallbackPlannerWithoutDBReturnsOriginalPlan() = runBlocking {
        val planner = AdaptiveFallbackPlanner(db = null)
        val originalPlan = Plan(
            goal = "Test search",
            steps = listOf(
                AgentAction(ActionType.CLICK, target = "Search or type web address")
            )
        )

        val adaptedPlan = planner.adaptPlan(originalPlan)
        assertEquals(originalPlan.steps.size, adaptedPlan.steps.size)
        assertEquals(originalPlan.steps[0].target, adaptedPlan.steps[0].target)
    }
}

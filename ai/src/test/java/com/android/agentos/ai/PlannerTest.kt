package com.android.agentos.ai

import com.android.agentos.core.models.ActionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PlannerTest {
    @Test
    fun testChromePlanGeneration() = runBlocking {
        val planner = Planner(RuleBasedLLMProvider())
        val plan = planner.generatePlan("Open Chrome and search for kittens", emptyList())

        assertEquals("Open Chrome and search for kittens", plan.goal)
        assertTrue(plan.steps.any { it.type == ActionType.OPEN_APP && it.target == "com.android.chrome" })
        assertTrue(plan.steps.any { it.type == ActionType.TYPE_TEXT && it.text == "kittens" })
    }
}

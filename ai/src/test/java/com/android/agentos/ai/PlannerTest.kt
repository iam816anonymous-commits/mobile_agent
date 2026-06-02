package com.android.agentos.ai

import com.android.agentos.core.models.ActionType
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannerTest {
    @Test
    fun testChromePlanGeneration() = runBlocking {
        val planner = Planner(RuleBasedLLMProvider())
        val goal = "Open Chrome and search kittens"
        val plan = planner.generatePlan(goal, emptyList())

        assertEquals(goal, plan.goal)
        assertTrue(plan.steps.any { it.type == ActionType.OPEN_APP && it.target == "com.android.chrome" })
        assertTrue(plan.steps.any { it.type == ActionType.TYPE_TEXT && it.text == "kittens" })
    }
}

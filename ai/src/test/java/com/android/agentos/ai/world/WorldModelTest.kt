package com.android.agentos.ai.world

import com.android.agentos.core.models.ActionType
import com.android.agentos.core.models.ScreenType
import org.junit.Assert.assertEquals
import org.junit.Test

class WorldModelTest {

    @Test
    fun testTransitionLearning() {
        val worldModel = WorldModel()

        // Initial state: Unknown transition
        val prediction = worldModel.predictNextState(ScreenType.HOME_SCREEN, ActionType.OPEN_APP)
        assertEquals(ScreenType.UNKNOWN, prediction)

        // Learn a transition
        worldModel.learnTransition(ScreenType.HOME_SCREEN, ActionType.OPEN_APP, ScreenType.CHROME_SEARCH_RESULTS)

        // Verify prediction
        val newPrediction = worldModel.predictNextState(ScreenType.HOME_SCREEN, ActionType.OPEN_APP)
        assertEquals(ScreenType.CHROME_SEARCH_RESULTS, newPrediction)
    }
}

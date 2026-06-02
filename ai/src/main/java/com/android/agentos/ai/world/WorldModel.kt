package com.android.agentos.ai.world

import com.android.agentos.core.models.ActionType
import com.android.agentos.core.models.ScreenType
import com.android.agentos.core.engine.WorldModelProvider

class WorldModel : WorldModelProvider {
    private val transitions = mutableMapOf<Pair<ScreenType, ActionType>, ScreenType>()

    init {
        // Seed with common transitions
        transitions[Pair(ScreenType.UNKNOWN, ActionType.OPEN_APP)] = ScreenType.UNKNOWN // App specific
        transitions[Pair(ScreenType.CHROME_HOME, ActionType.TYPE_TEXT)] = ScreenType.CHROME_HOME
        transitions[Pair(ScreenType.CHROME_HOME, ActionType.CLICK)] = ScreenType.CHROME_SEARCH_RESULTS
        transitions[Pair(ScreenType.SETTINGS_MAIN, ActionType.CLICK)] = ScreenType.SETTINGS_WIFI
    }

    override fun predictNextState(currentState: ScreenType, action: ActionType): ScreenType {
        return transitions[Pair(currentState, action)] ?: ScreenType.UNKNOWN
    }

    override fun learnTransition(from: ScreenType, action: ActionType, to: ScreenType) {
        if (from != ScreenType.UNKNOWN && to != ScreenType.UNKNOWN) {
            transitions[Pair(from, action)] = to
        }
    }
}

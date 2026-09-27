package com.android.agentos.core.engine

import com.android.agentos.core.models.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Robust bridge for communication between the Accessibility Service and the Executor.
 */
class AgentBridge : AccessibilityProvider, VerificationProvider {

    private var internalProvider: AccessibilityProvider? = null
    private var internalVerification: VerificationProvider? = null

    val isKilled = AtomicBoolean(false)
    var killReason: String? = null
        private set

    private val _events = MutableSharedFlow<BridgeEvent>(replay = 0)
    val events: SharedFlow<BridgeEvent> = _events

    fun triggerKillSwitch(reason: String = "Kill-switch activated") {
        isKilled.set(true)
        killReason = reason
    }

    fun resetKillSwitch() {
        isKilled.set(false)
        killReason = null
    }

    fun registerProvider(provider: AccessibilityProvider, verifier: VerificationProvider) {
        internalProvider = provider
        internalVerification = verifier
    }

    fun unregisterProvider() {
        internalProvider = null
        internalVerification = null
    }

    override fun performAction(action: AgentAction): Boolean {
        if (isKilled.get()) return false
        return internalProvider?.performAction(action) ?: false
    }

    override fun getCurrentScreenHierarchy(): List<ScreenElement> {
        if (isKilled.get()) return emptyList()
        return internalProvider?.getCurrentScreenHierarchy() ?: emptyList()
    }

    override fun getCurrentScreenState(): ScreenState {
        if (isKilled.get()) return ScreenState(null, ScreenType.UNKNOWN, emptyList())
        return internalProvider?.getCurrentScreenState() ?: ScreenState(null, null, emptyList())
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        if (isKilled.get()) {
            return VerificationResult(false, 0f, "Execution terminated by safety kill-switch: $killReason")
        }
        return internalVerification?.verifyAction(action, screenContext) ?: VerificationResult(false, 0f, "No provider registered")
    }

    suspend fun emitEvent(event: BridgeEvent) {
        _events.emit(event)
    }

    companion object {
        val instance = AgentBridge()
    }
}

sealed class BridgeEvent {
    data class ActionExecuted(val action: AgentAction, val result: Boolean) : BridgeEvent()
    data class ScreenChanged(val state: ScreenState) : BridgeEvent()
    data class KillSwitchTriggered(val reason: String) : BridgeEvent()
}

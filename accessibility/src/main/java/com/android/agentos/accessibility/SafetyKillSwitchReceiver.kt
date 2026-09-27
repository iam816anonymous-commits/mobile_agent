package com.android.agentos.accessibility

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.android.agentos.core.engine.AgentBridge

/**
 * BroadcastReceiver safety kill-switch to immediately terminate execution if triggered
 * by broadcast intent or screen power events.
 */
class SafetyKillSwitchReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SafetyKillSwitch"
        const val ACTION_KILL_SWITCH = "com.android.agentos.KILL_SWITCH"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val action = intent?.action
        Log.w(TAG, "Safety kill-switch broadcast received with action: $action")
        if (action == ACTION_KILL_SWITCH || action == Intent.ACTION_SCREEN_OFF) {
            AgentBridge.instance.triggerKillSwitch("Safety kill-switch broadcast received: $action")
            Log.w(TAG, "Execution state killed via SafetyKillSwitchReceiver")
        }
    }
}

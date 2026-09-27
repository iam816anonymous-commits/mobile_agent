package com.android.agentos.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.android.agentos.core.models.*
import com.android.agentos.core.models.Rect as ModelRect

import com.android.agentos.core.engine.AccessibilityProvider
import com.android.agentos.core.engine.VerificationProvider
import com.android.agentos.core.engine.AgentBridge
import com.android.agentos.ai.classification.ScreenClassifier

class AgentAccessibilityService : AccessibilityService(), AccessibilityProvider, VerificationProvider {

    private val classifier = ScreenClassifier()

    companion object {
        private const val TAG = "AgentAccessibility"
        const val ACTION_KILL_SWITCH = "com.android.agentos.KILL_SWITCH"
    }

    private var killSwitchReceiver: BroadcastReceiver? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        AgentBridge.instance.registerProvider(this, this)
        registerKillSwitchReceiver()
        Log.i(TAG, "AgentAccessibilityService connected & registered with AgentBridge")
    }

    private fun registerKillSwitchReceiver() {
        killSwitchReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == ACTION_KILL_SWITCH || intent?.action == Intent.ACTION_SCREEN_OFF) {
                    Log.w(TAG, "Safety kill-switch received via broadcast! Triggering emergency stop.")
                    AgentBridge.instance.triggerKillSwitch("Kill-switch broadcast received: ${intent?.action}")
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(ACTION_KILL_SWITCH)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(killSwitchReceiver, filter)
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.action == KeyEvent.ACTION_DOWN) {
            // Hardware kill-switch: Volume Down button triggers immediate execution termination
            if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                Log.w(TAG, "Hardware Kill-Switch triggered via Volume Down key press!")
                AgentBridge.instance.triggerKillSwitch("Hardware Volume Down button pressed")
                return true
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
        Log.w(TAG, "AgentAccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        killSwitchReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (e: Exception) {
                Log.e(TAG, "Error unregistering kill-switch receiver", e)
            }
        }
        AgentBridge.instance.unregisterProvider()
    }

    override fun getCurrentScreenState(): ScreenState {
        if (AgentBridge.instance.isKilled.get()) {
            Log.w(TAG, "Kill-switch is active. Skipping state retrieval.")
            return ScreenState(null, ScreenType.UNKNOWN, emptyList())
        }
        val root = rootInActiveWindow
        val packageName = root?.packageName?.toString()
        val elements = getCurrentScreenHierarchy()
        root?.recycle()
        return classifier.classify(packageName, elements)
    }

    /**
     * Captures the current screen hierarchy and flattens it into a list of ScreenElements.
     */
    override fun getCurrentScreenHierarchy(): List<ScreenElement> {
        if (AgentBridge.instance.isKilled.get()) return emptyList()
        val root = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ScreenElement>()
        flattenHierarchy(root, elements)
        root.recycle()
        return elements
    }

    private fun flattenHierarchy(node: AccessibilityNodeInfo, elements: MutableList<ScreenElement>) {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        elements.add(ScreenElement(
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            className = node.className?.toString() ?: "",
            bounds = ModelRect(bounds.left, bounds.top, bounds.right, bounds.bottom),
            isClickable = node.isClickable,
            id = node.viewIdResourceName
        ))

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                flattenHierarchy(child, elements)
                child.recycle()
            }
        }
    }

    /**
     * Executes a given AgentAction.
     */
    override fun performAction(action: AgentAction): Boolean {
        if (AgentBridge.instance.isKilled.get()) {
            Log.w(TAG, "Kill-switch active. Refusing to perform action: ${action.type}")
            return false
        }
        Log.d(TAG, "Performing action: ${action.type}")
        return when (action.type) {
            ActionType.CLICK -> {
                val x = action.x
                val y = action.y
                if (x != null && y != null) {
                    clickAt(x.toFloat(), y.toFloat())
                } else {
                    findAndClick(action.target)
                }
            }
            ActionType.TYPE_TEXT -> {
                typeText(action.text ?: "")
            }
            ActionType.GO_BACK -> {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            ActionType.OPEN_APP -> {
                action.target?.let { packageName ->
                    val intent = packageManager.getLaunchIntentForPackage(packageName)
                    if (intent != null) {
                        startActivity(intent)
                        true
                    } else false
                } ?: false
            }
            ActionType.SCROLL_DOWN -> scroll(true)
            ActionType.SCROLL_UP -> scroll(false)
            ActionType.VERIFY_ELEMENT -> verifyElementVisible(action.target)
            ActionType.MONITOR_FOR_ELEMENT -> monitorForElement(action.target)
            ActionType.SCROLL_TO_ELEMENT -> scrollToElement(action.target)
            else -> false
        }
    }

    private fun monitorForElement(target: String?): Boolean {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 30000) {
            if (AgentBridge.instance.isKilled.get()) return false
            if (verifyElementVisible(target)) return true
            Thread.sleep(1000)
        }
        return false
    }

    private fun scrollToElement(target: String?): Boolean {
        if (target == null) return false
        return AccessibilityNodeInfoTraverser.performScrollAndRetry(this, target, maxScrolls = 5)
    }

    private fun clickAt(x: Float, y: Float): Boolean {
        val path = Path()
        path.moveTo(x, y)
        val builder = GestureDescription.Builder()
        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
        return dispatchGesture(builder.build(), null, null)
    }

    private fun findAndClick(target: String?): Boolean {
        if (target == null) return false
        val root = rootInActiveWindow ?: return false

        // First attempt direct find
        var targetNode = AccessibilityNodeInfoTraverser.findNode(root, target)

        if (targetNode == null) {
            // Scroll and retry recovery
            root.recycle()
            val recovered = AccessibilityNodeInfoTraverser.performScrollAndRetry(this, target, maxScrolls = 3)
            if (!recovered) return false
            val newRoot = rootInActiveWindow ?: return false
            targetNode = AccessibilityNodeInfoTraverser.findNode(newRoot, target)
            newRoot.recycle()
        } else {
            root.recycle()
        }

        if (targetNode != null) {
            val result = performClickOnNode(targetNode)
            targetNode.recycle()
            return result
        }

        return false
    }

    private fun performClickOnNode(node: AccessibilityNodeInfo): Boolean {
        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }
        val parent = node.parent
        return if (parent != null) {
            val result = performClickOnNode(parent)
            parent.recycle()
            result
        } else {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            clickAt(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    private fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        var focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focus == null) {
            // Fallback: locate editable node in tree
            focus = findFirstEditableNode(root)
        }

        if (focus != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val result = focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focus.recycle()
            root.recycle()
            return result
        }

        root.recycle()
        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) {
            return node
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val editable = findFirstEditableNode(child)
                if (editable != null) {
                    if (editable != child) child.recycle()
                    return editable
                }
                child.recycle()
            }
        }
        return null
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
        if (AgentBridge.instance.isKilled.get()) {
            return VerificationResult(false, 0f, "Execution halted by safety kill-switch")
        }
        return when (action.type) {
            ActionType.VERIFY_ELEMENT -> {
                val success = verifyElementVisible(action.target)
                VerificationResult(success, if (success) 1.0f else 0.0f, if (success) "Element found" else "Element not found")
            }
            ActionType.OPEN_APP -> {
                val success = screenContext.isNotEmpty()
                VerificationResult(success, if (success) 0.8f else 0.0f)
            }
            else -> VerificationResult(true, 1.0f)
        }
    }

    private fun verifyElementVisible(target: String?): Boolean {
        if (target == null) return false
        val root = rootInActiveWindow ?: return false
        val node = AccessibilityNodeInfoTraverser.findNode(root, target)
        val visible = node != null
        node?.recycle()
        root.recycle()
        return visible
    }

    private fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val result = AccessibilityNodeInfoTraverser.scrollContainer(root, forward = down)
        root.recycle()
        return result
    }
}

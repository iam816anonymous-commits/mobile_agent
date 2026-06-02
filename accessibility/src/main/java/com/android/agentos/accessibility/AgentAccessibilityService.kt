package com.android.agentos.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
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
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AgentBridge.instance.registerProvider(this, this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    }

    override fun onInterrupt() {
    }

    override fun onDestroy() {
        super.onDestroy()
        AgentBridge.instance.unregisterProvider()
    }

    override fun getCurrentScreenState(): ScreenState {
        val packageName = rootInActiveWindow?.packageName?.toString()
        val elements = getCurrentScreenHierarchy()
        return classifier.classify(packageName, elements)
    }

    /**
     * Captures the current screen hierarchy and flattens it into a list of ScreenElements.
     */
    override fun getCurrentScreenHierarchy(): List<ScreenElement> {
        val root = rootInActiveWindow ?: return emptyList()
        val elements = mutableListOf<ScreenElement>()
        flattenHierarchy(root, elements)
        return elements
    }

    private fun flattenHierarchy(node: AccessibilityNodeInfo, elements: MutableList<ScreenElement>) {
        if (!node.isVisibleToUser) return

        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val text = node.text?.toString()
        val contentDescription = node.contentDescription?.toString()

        // Only add elements that have some identifying info or are clickable
        if (!text.isNullOrEmpty() || !contentDescription.isNullOrEmpty() || node.isClickable) {
            elements.add(ScreenElement(
                text = text,
                contentDescription = contentDescription,
                className = node.className?.toString() ?: "",
                bounds = ModelRect(bounds.left, bounds.top, bounds.right, bounds.bottom),
                isClickable = node.isClickable,
                id = node.viewIdResourceName
            ))
        }

        if (elements.size > 100) return // Safety cap for deep trees

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
        // Long-horizon: retry verification for up to 30 seconds
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < 30000) {
            if (verifyElementVisible(target)) return true
            Thread.sleep(2000)
        }
        return false
    }

    private fun scrollToElement(target: String?): Boolean {
        if (target == null) return false
        for (i in 1..5) {
            if (verifyElementVisible(target)) return true
            scroll(true)
            Thread.sleep(1000)
        }
        return false
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

        // Strategy 1: Find by text
        var nodes = root.findAccessibilityNodeInfosByText(target)
        if (nodes.isEmpty()) {
            // Strategy 2: Find by ID
            nodes = root.findAccessibilityNodeInfosByViewId(target)
        }

        if (nodes.isNotEmpty()) {
            val node = nodes[0]
            val result = performClickOnNode(node)
            nodes.forEach { it.recycle() }
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
            // Fallback to clicking center of bounds
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            clickAt(bounds.centerX().toFloat(), bounds.centerY().toFloat())
        }
    }

    private fun typeText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        var focus = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (focus == null) {
            // Try to find first editable node
            focus = findFirstEditable(root)
            focus?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

        if (focus != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val result = focus.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            focus.recycle()
            return result
        }
        return false
    }

    private fun findFirstEditable(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findFirstEditable(child)
                if (found != null) return found
                child.recycle()
            }
        }
        return null
    }

    override suspend fun verifyAction(action: AgentAction, screenContext: List<ScreenElement>): VerificationResult {
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
        val nodesByText = root.findAccessibilityNodeInfosByText(target)
        if (nodesByText.isNotEmpty()) {
            nodesByText.forEach { it.recycle() }
            return true
        }
        val nodesById = root.findAccessibilityNodeInfosByViewId(target)
        if (nodesById.isNotEmpty()) {
            nodesById.forEach { it.recycle() }
            return true
        }
        return false
    }

    private fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return performScrollOnFirstScrollable(root, action)
    }

    private fun performScrollOnFirstScrollable(node: AccessibilityNodeInfo, action: Int): Boolean {
        if (node.isScrollable) {
            return node.performAction(action)
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                if (performScrollOnFirstScrollable(child, action)) {
                    child.recycle()
                    return true
                }
                child.recycle()
            }
        }
        return false
    }
}

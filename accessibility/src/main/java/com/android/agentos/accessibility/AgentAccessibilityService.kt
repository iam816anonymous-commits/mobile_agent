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
            root.recycle()
            return result
        }
        root.recycle()
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
        var targetNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)

        if (targetNode == null) {
            Log.d(TAG, "No node has FOCUS_INPUT, searching for first editable node")
            targetNode = findFirstEditableNode(root)
            targetNode?.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        }

        if (targetNode != null) {
            val arguments = Bundle()
            arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            val result = targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments)
            targetNode.recycle()
            root.recycle()
            return result
        }

        root.recycle()
        return false
    }

    private fun findFirstEditableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isEditable) return node
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val found = findFirstEditableNode(child)
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
            root.recycle()
            return true
        }
        val nodesById = root.findAccessibilityNodeInfosByViewId(target)
        if (nodesById.isNotEmpty()) {
            nodesById.forEach { it.recycle() }
            root.recycle()
            return true
        }
        root.recycle()
        return false
    }

    private fun scroll(down: Boolean): Boolean {
        val root = rootInActiveWindow ?: return false
        val action = if (down) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        val result = performScrollOnFirstScrollable(root, action)
        root.recycle()
        return result
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

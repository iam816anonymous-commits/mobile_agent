package com.android.agentos.accessibility

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.ArrayDeque

/**
 * Utility for safe, robust AccessibilityNodeInfo traversal and scroll-recovery.
 */
object AccessibilityNodeInfoTraverser {

    private const val TAG = "NodeTraverser"
    private const val MAX_DEPTH = 50

    /**
     * Safely searches for a node matching target string via text, view ID, or content description.
     * Note: The caller is responsible for recycling the returned AccessibilityNodeInfo when done.
     */
    fun findNode(root: AccessibilityNodeInfo?, target: String?): AccessibilityNodeInfo? {
        if (root == null || target.isNullOrBlank()) return null

        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.add(root to 0)

        var matchedNode: AccessibilityNodeInfo? = null

        while (queue.isNotEmpty()) {
            val (node, depth) = queue.removeFirst()

            if (matchesTarget(node, target)) {
                matchedNode = node
                // Recycle remaining nodes in queue
                while (queue.isNotEmpty()) {
                    val (remainingNode, _) = queue.removeFirst()
                    if (remainingNode != root && remainingNode != matchedNode) {
                        remainingNode.recycle()
                    }
                }
                break
            }

            if (depth < MAX_DEPTH) {
                for (i in 0 until node.childCount) {
                    val child = node.getChild(i)
                    if (child != null) {
                        queue.add(child to (depth + 1))
                    }
                }
            }

            if (node != root && node != matchedNode) {
                node.recycle()
            }
        }

        return matchedNode
    }

    /**
     * Checks if a node matches the target query using multiple strategies:
     * 1. Resource View ID
     * 2. Text (case-insensitive substring)
     * 3. Content Description (case-insensitive substring)
     */
    fun matchesTarget(node: AccessibilityNodeInfo, target: String): Boolean {
        val targetLower = target.lowercase()

        // Strategy 1: View ID match
        node.viewIdResourceName?.let { id ->
            if (id.equals(target, ignoreCase = true) || id.endsWith(":id/$target", ignoreCase = true)) {
                return true
            }
        }

        // Strategy 2: Text match
        node.text?.toString()?.let { text ->
            if (text.lowercase().contains(targetLower)) {
                return true
            }
        }

        // Strategy 3: Content Description match
        node.contentDescription?.toString()?.let { desc ->
            if (desc.lowercase().contains(targetLower)) {
                return true
            }
        }

        return false
    }

    /**
     * Attempts to find a node. If missing, automatically performs scroll-down recovery on the first
     * scrollable parent/container and retries node discovery up to maxScrolls times.
     */
    fun performScrollAndRetry(
        service: AccessibilityService,
        target: String?,
        maxScrolls: Int = 3
    ): Boolean {
        if (target == null) return false

        for (attempt in 0..maxScrolls) {
            val root = service.rootInActiveWindow
            if (root != null) {
                val found = findNode(root, target)
                if (found != null) {
                    Log.i(TAG, "Target '$target' found on attempt $attempt")
                    found.recycle()
                    root.recycle()
                    return true
                }

                // If not found and attempts remaining, scroll down
                if (attempt < maxScrolls) {
                    Log.i(TAG, "Target '$target' not visible. Performing scroll-and-retry (Attempt ${attempt + 1}/$maxScrolls)")
                    val scrolled = scrollContainer(root, forward = true)
                    root.recycle()
                    if (!scrolled) {
                        Log.w(TAG, "No scrollable container found for recovery")
                        break
                    }
                    try {
                        Thread.sleep(800) // Wait for UI transition
                    } catch (e: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return false
                    }
                } else {
                    root.recycle()
                }
            } else {
                Log.w(TAG, "rootInActiveWindow was null on scroll attempt $attempt")
            }
        }
        return false
    }

    /**
     * Finds the first scrollable node in hierarchy and performs scroll forward/backward action.
     */
    fun scrollContainer(root: AccessibilityNodeInfo, forward: Boolean = true): Boolean {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()

            if (node.isScrollable) {
                val scrolled = node.performAction(action)
                while (queue.isNotEmpty()) {
                    val remaining = queue.removeFirst()
                    if (remaining != root) remaining.recycle()
                }
                if (node != root) node.recycle()
                return scrolled
            }

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) {
                    queue.add(child)
                }
            }

            if (node != root) {
                node.recycle()
            }
        }
        return false
    }
}

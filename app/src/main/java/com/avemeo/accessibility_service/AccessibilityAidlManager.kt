package com.avemeo.accessibility_service

import android.os.RemoteCallbackList

object AccessibilityAidlManager {
    private var callbacks: RemoteCallbackList<IAccessibilityCallback>? = null
    private const val TAG = "AccessibilityAidlManager"
    private var isBroadcasting = false
    private var isDebugLoggingEnabled = false

    fun setCallbacks(callbacks: RemoteCallbackList<IAccessibilityCallback>) {
        this.callbacks = callbacks
    }

    fun setDebugLogging(enabled: Boolean) {
        isDebugLoggingEnabled = enabled
        if (enabled) {
            safeBroadcast { callback ->
                callback.onLogDebugReceived(TAG, "Debug logging enabled")
            }
        }
    }

    private fun safeBroadcast(block: (IAccessibilityCallback) -> Unit) {
        if (isBroadcasting) {
            return
        }

        callbacks?.let {
            try {
                isBroadcasting = true
                val n = it.beginBroadcast()
                try {
                    for (i in 0 until n) {
                        try {
                            block(it.getBroadcastItem(i))
                        } catch (_: Exception) {
                        }
                    }
                } finally {
                    it.finishBroadcast()
                }
            } finally {
                isBroadcasting = false
            }
        }
    }

    fun notifyKeyEvent(keyCode: Int) {
        safeBroadcast { callback ->
            callback.onKeyEventReceived(keyCode)
            if (isDebugLoggingEnabled) {
                callback.onLogDebugReceived("Key event received: $keyCode", TAG)
            }
        }
    }

    fun notifyAccessibilityEvent(eventType: Int, packageName: String?) {
        safeBroadcast { callback ->
            callback.onAccessibilityEventTypeReceived(eventType)
            if (packageName != null) {
                callback.onAccessibilityEventPackageNameReceived(packageName)
            }
            if (isDebugLoggingEnabled) {
                callback.onLogDebugReceived("Accessibility event received: type=$eventType, package=$packageName", TAG)
            }
        }
    }

    fun logDebug(message: String, tag: String) {
        if (isDebugLoggingEnabled) {
            safeBroadcast { callback ->
                callback.onLogDebugReceived(message, tag)
            }
        }
    }

    fun logError(message: String, tag: String) {
        if (isDebugLoggingEnabled) {
            safeBroadcast { callback ->
                callback.onLogErrorReceived(message, tag)
            }
        }
    }
} 
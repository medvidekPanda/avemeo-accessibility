package com.avemeo.accessibility_service

import android.os.RemoteCallbackList
import android.util.Log

object AccessibilityAidlManager {
    private var callbacks: RemoteCallbackList<IAccessibilityCallback>? = null
    private const val TAG = "AccessibilityAidlManager"
    private var isBroadcasting = false

    fun setCallbacks(callbacks: RemoteCallbackList<IAccessibilityCallback>) {
        this.callbacks = callbacks
    }

    private fun safeBroadcast(block: (IAccessibilityCallback) -> Unit) {
        if (isBroadcasting) {
            Log.w(TAG, "Broadcast already in progress, skipping")
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
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to execute broadcast block", e)
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
        }
    }

    fun notifyAccessibilityEvent(eventType: Int, packageName: String?) {
        safeBroadcast { callback ->
            callback.onAccessibilityEventTypeReceived(eventType)
            if (packageName != null) {
                callback.onAccessibilityEventPackageNameReceived(packageName)
            }
        }
    }

    fun logDebug(tag: String, message: String) {
        Log.d(tag, message)
        safeBroadcast { callback ->
            callback.onLogDebugReceived(message, tag)
        }
    }

    fun logError(tag: String, message: String) {
        Log.e(tag, message)
        safeBroadcast { callback ->
            callback.onLogErrorReceived(message, tag)
        }
    }
} 
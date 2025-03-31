package com.avemeo.accessibility_service

import android.os.RemoteCallbackList
import android.util.Log

class AvemeoLogger(private val callbacks: RemoteCallbackList<IAccessibilityCallback>) {
    private val logTag = "AvemeoAccessibility"

    fun logDebug(message: String) {
        //Log.d(logTag, message)
        sendDebugLog(message)
    }

    fun logError(message: String, exception: Exception) {
        Log.e(logTag, message, exception)
    }

    private fun sendDebugLog(message: String) {
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                callbacks.getBroadcastItem(i).onLogDebugReceived("Debug log", logTag)
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }
}
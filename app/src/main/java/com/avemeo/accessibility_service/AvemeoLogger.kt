package com.avemeo.accessibility_service

class AvemeoLogger {
    private val logTag = "AvemeoAccessibility"

    fun logDebug(message: String) {
        AccessibilityAidlManager.logDebug(logTag, message)
    }

    fun logError(message: String, exception: Exception) {
        AccessibilityAidlManager.logError(logTag, message)
    }
}
package com.avemeo.accessibility_service

class AvemeoLogger {
    private val logTag = "AvemeoAccessibility"

    fun logDebug(message: String) {
        AccessibilityAidlManager.logDebug(message, logTag)
    }

    fun logError(message: String, exception: Exception) {
        AccessibilityAidlManager.logError(message, exception, logTag)
    }
}
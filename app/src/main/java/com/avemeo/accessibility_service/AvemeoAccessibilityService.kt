package com.avemeo.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class AvemeoAccessibilityService : AccessibilityService() {
    private val avemeoLogger = AvemeoLogger();

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            avemeoLogger.logDebug("AccessibilityService connected")
            val accessibilityService = IAccessibilityService.Stub.asInterface(service)

            try {
                accessibilityService?.registerCallback(callback)
                avemeoLogger.logDebug("Callback registered successfully")
            } catch (e: Exception) {
                avemeoLogger.logError("Failed to register callback", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            avemeoLogger.logDebug("AccessibilityService disconnected")
        }
    }

    private val callback = object : IAccessibilityCallback.Stub() {
        override fun onLogDebugReceived(tag: String, message: String, ) {}
        override fun onLogErrorReceived(tag: String, message: String) {}
        override fun onKeyEventReceived(keyCode: Int) {}
        override fun onAccessibilityEventTypeReceived(eventType: Int) {}
        override fun onAccessibilityEventPackageNameReceived(packageName: String) {}
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        avemeoLogger.logDebug("AccessibilityService onServiceConnected called")
        
        val info = AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info
        avemeoLogger.logDebug("AccessibilityServiceInfo configured")

        val intent = Intent("com.avemeo.accessibility_service.BIND")
        intent.`package` = "com.avemeo.accessibility_service"
        try {
            bindService(intent, serviceConnection, BIND_AUTO_CREATE)
            avemeoLogger.logDebug("Bound to accessibility service")
        } catch (e: Exception) {
            avemeoLogger.logError("Failed to bind to accessibility service", e)
        }
    }

    override fun onInterrupt() {
        avemeoLogger.logDebug("Service Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event?.let {
            if (it.action == KeyEvent.ACTION_DOWN) {
                avemeoLogger.logDebug("Key event received: ${it.keyCode}")
                val intent = Intent(this, AccessibilityBindingService::class.java)
                intent.putExtra("keyCode", it.keyCode)
                startService(intent)
                avemeoLogger.logDebug("Key event sent to AccessibilityBindingService")
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            avemeoLogger.logDebug("Accessibility event received: type=${it.eventType}, package=${it.packageName}")
            val intent = Intent(this, AccessibilityBindingService::class.java)
            intent.putExtra("eventType", it.eventType)
            intent.putExtra("packageName", it.packageName?.toString())
            startService(intent)
            avemeoLogger.logDebug("Accessibility event sent to AccessibilityBindingService")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        avemeoLogger.logDebug("onStartCommand called with action: ${intent?.action}")
        if (intent?.action == "LAUNCH_APP_ACTION") {
            val packageName = intent.getStringExtra("launch_package_name")
            packageName?.let {
                avemeoLogger.logDebug("Launching app from onStartCommand: $it")
                onLaunchApp(it)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onLaunchApp(packageName: String) {
        try {
            avemeoLogger.logDebug("Launching app: $packageName")
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                avemeoLogger.logDebug("App launch intent sent")
            } ?: run {
                avemeoLogger.logError(
                    "Launch intent is null for package: $packageName",
                    exception = TODO()
                )
            }
        } catch (e: Exception) {
            avemeoLogger.logError("Failed to launch app: $packageName", e)
        }
    }
}
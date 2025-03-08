package com.avemeo.accessibility_service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class AvemeoAccessibilityService : AccessibilityService() {
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("AvemeoAccessibility", "AccessibilityService connected")
            val accessibilityService = IAccessibilityService.Stub.asInterface(service)
            try {
                accessibilityService?.registerCallback(callback)
            } catch (e: Exception) {
                Log.e("AvemeoAccessibility", "Failed to register callback", e)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("AvemeoAccessibility", "AccessibilityService disconnected")
        }
    }

    private val callback = object : IAccessibilityCallback.Stub() {
        override fun onKeyEventReceived(keyCode: Int) {}
        override fun onAccessibilityEventTypeReceived(eventType: Int) {}
        override fun onAccessibilityEventPackageNameReceived(packageName: String) {}
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val info = AccessibilityServiceInfo()
        info.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 0
            flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        serviceInfo = info

        val intent = Intent("com.avemeo.accessibility_service.BIND")
        intent.`package` = "com.avemeo.accessibility_service"
        try {
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            Log.d("AvemeoAccessibility", "Bound to accessibility service")
        } catch (e: Exception) {
            Log.e("AvemeoAccessibility", "Failed to bind to accessibility service", e)
        }
    }

    override fun onInterrupt() {
        Log.d("AvemeoAccessibility", "Service Interrupted")
    }

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        if (event != null && event.action == KeyEvent.ACTION_DOWN) {
            Log.d("AvemeoAccessibility", "Key event received: $event")
        }

        event?.let {
            if (it.action == KeyEvent.ACTION_DOWN) {
                val intent = Intent(this, AccessibilityBindingService::class.java)
                intent.putExtra("keyCode", it.keyCode)
                startService(intent)
            }
        }
        return super.onKeyEvent(event)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
       Log.d("AvemeoAccessibility", "onAccessibilityEvent" + event.toString())
        event?.let {
            val intent = Intent(this, AccessibilityBindingService::class.java)
            intent.putExtra("eventType", it.eventType)
            intent.putExtra("packageName", it.packageName?.toString())
            startService(intent)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "LAUNCH_APP_ACTION") {
            val packageName = intent.getStringExtra("launch_package_name")
            packageName?.let {
                onLaunchApp(it)
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun onLaunchApp(packageName: String) {
        Log.d("AvemeoAccessibility", "onLaunchApp: $packageName")
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
            launchIntent?.let { intent ->
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
        } catch (e: Exception) {
            Log.e("AvemeoAccessibility", "Failed to launch app in AccessibilityService", e)
        }
    }
}
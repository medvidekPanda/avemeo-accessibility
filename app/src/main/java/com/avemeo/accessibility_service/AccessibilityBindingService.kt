package com.avemeo.accessibility_service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.os.Handler
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.HandlerThread

class AccessibilityBindingService : Service() {
    private val callbacks = RemoteCallbackList<IAccessibilityCallback>()
    private val avemeoLogger = AvemeoLogger()
    private var shouldReconnect = true

    private companion object {
        private const val NOTIFICATION_ID = 1
    }

    private val binder = object : IAccessibilityService.Stub() {
        override fun registerCallback(callback: IAccessibilityCallback) {
            avemeoLogger.logDebug("Registering new callback")
            callbacks.register(callback)
            avemeoLogger.logDebug("Callback registered successfully")
        }

        override fun unregisterCallback(callback: IAccessibilityCallback) {
            avemeoLogger.logDebug("Unregistering callback")
            callbacks.unregister(callback)
            avemeoLogger.logDebug("Callback unregistered successfully")
        }

        override fun launchApp(packageName: String) {
            try {
                avemeoLogger.logDebug("Received launch app request for package: $packageName")
                val intent = Intent(applicationContext, AvemeoAccessibilityService::class.java)
                intent.action = "LAUNCH_APP_ACTION"
                intent.putExtra("launch_package_name", packageName)
                startService(intent)
                avemeoLogger.logDebug("Launch app intent sent to AccessibilityService")
            } catch (e: Exception) {
                avemeoLogger.logError("Failed to launch app: $packageName", e)
            }
        }
    }

    private val handlerThread = HandlerThread("AccessibilityEventThread").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private val keyEventHandlerThread = HandlerThread("KeyEventThread").apply { start() }
    private val keyEventHandler = Handler(keyEventHandlerThread.looper)

    override fun onBind(intent: Intent): IBinder {
        avemeoLogger.logDebug("Service bound with intent: ${intent.action}")
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        avemeoLogger.logDebug("onStartCommand called with action: ${intent?.action}")
        intent?.let {
            when {
                it.hasExtra("keyCode") -> {
                    val keyCode = it.getIntExtra("keyCode", 0)
                    avemeoLogger.logDebug("Processing key event: $keyCode")
                    keyEventHandler.post {
                        AccessibilityAidlManager.notifyKeyEvent(keyCode)
                        avemeoLogger.logDebug("Key event processed and broadcasted")
                    }
                }
                it.hasExtra("eventType") -> {
                    val eventType = it.getIntExtra("eventType", 0)
                    val packageName = it.getStringExtra("packageName")
                    avemeoLogger.logDebug("Processing accessibility event: type=$eventType, package=$packageName")
                    handler.post {
                        AccessibilityAidlManager.notifyAccessibilityEvent(eventType, packageName)
                        avemeoLogger.logDebug("Accessibility event processed and broadcasted")
                    }
                }
                else -> {
                    avemeoLogger.logDebug("onStartCommand: Unknown intent without expected extra data")
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        avemeoLogger.logDebug("AccessibilityBindingService onCreate called")
        AccessibilityAidlManager.setCallbacks(callbacks)
        avemeoLogger.logDebug("Callbacks set in AccessibilityAidlManager")
        startForeground(NOTIFICATION_ID, createNotification())
        avemeoLogger.logDebug("Service started in foreground")
    }

    private fun createNotification(): Notification {
        avemeoLogger.logDebug("Creating notification channel")
        val channelId = "accessibility_service_channel"
        val channel = NotificationChannel(
            channelId, "Accessibility Service", NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        avemeoLogger.logDebug("Notification channel created")

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Avemeo accessibility Service")
            .setContentText("Avemeo accessibility service is running").build()
    }

    override fun onDestroy() {
        avemeoLogger.logDebug("AccessibilityBindingService onDestroy called")
        shouldReconnect = false
        super.onDestroy()
    }
} 
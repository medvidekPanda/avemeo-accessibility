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
            avemeoLogger.logDebug("registerCallback binding")
            callbacks.register(callback)
        }

        override fun unregisterCallback(callback: IAccessibilityCallback) {
            callbacks.unregister(callback)
        }

        override fun launchApp(packageName: String) {
            try {
                avemeoLogger.logDebug("Successfully launched app pub: $packageName")
                val intent = Intent(applicationContext, AvemeoAccessibilityService::class.java)
                intent.action = "LAUNCH_APP_ACTION"
                intent.putExtra("launch_package_name", packageName)
                startService(intent)
            } catch (e: Exception) {
                avemeoLogger.logError("Failed to launch app: $packageName", e)
            }
        }
    }

    private val broadcastLock = Object()
    private val handlerThread = HandlerThread("AccessibilityEventThread").apply { start() }
    private val handler = Handler(handlerThread.looper)
    private var isBroadcasting = false
    private val keyEventHandlerThread = HandlerThread("KeyEventThread").apply { start() }
    private val keyEventHandler = Handler(keyEventHandlerThread.looper)

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when {
                it.hasExtra("keyCode") -> {
                    val keyCode = it.getIntExtra("keyCode", 0)
                    keyEventHandler.post {
                        AccessibilityAidlManager.notifyKeyEvent(keyCode)
                    }
                }
                it.hasExtra("eventType") -> {
                    val eventType = it.getIntExtra("eventType", 0)
                    val packageName = it.getStringExtra("packageName")
                    handler.post {
                        AccessibilityAidlManager.notifyAccessibilityEvent(eventType, packageName)
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
        AccessibilityAidlManager.setCallbacks(callbacks)
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val channelId = "accessibility_service_channel"
        val channel = NotificationChannel(
            channelId, "Accessibility Service", NotificationManager.IMPORTANCE_LOW
        )
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Avemeo accessibility Service")
            .setContentText("Avemeo accessibility service is running").build()
    }

    override fun onDestroy() {
        shouldReconnect = false
        super.onDestroy()
    }
} 
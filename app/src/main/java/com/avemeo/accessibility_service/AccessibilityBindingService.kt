package com.avemeo.accessibility_service

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList
import android.util.Log
import android.os.Handler
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.os.HandlerThread

class AccessibilityBindingService : Service() {
    private val callbacks = RemoteCallbackList<IAccessibilityCallback>()
    private var shouldReconnect = true

    private companion object {
        private const val NOTIFICATION_ID = 1
    }

    private val binder = object : IAccessibilityService.Stub() {
        override fun registerCallback(callback: IAccessibilityCallback) {
            Log.d("AvemeoAccessibility", "registerCallback bindingd")
            callbacks.register(callback)
        }

        override fun unregisterCallback(callback: IAccessibilityCallback) {
            callbacks.unregister(callback)
        }


        override fun launchApp(packageName: String) {
            try {
                //Log.d("AvemeoAccessibility", "Successfully launched app pub: $packageName")
                val intent = Intent(applicationContext, AvemeoAccessibilityService::class.java)
                intent.action = "LAUNCH_APP_ACTION"
                intent.putExtra("launch_package_name", packageName)
                startService(intent)
            } catch (e: Exception) {
                Log.e("AvemeoAccessibility", "Failed to launch app: $packageName", e)
            }
        }
    }


    private val handlerThread = HandlerThread("AccessibilityEventThread").apply { start() }
    private val handler = Handler(handlerThread.looper)

    private val keyEventHandlerThread = HandlerThread("KeyEventThread").apply { start() }
    private val keyEventHandler = Handler(keyEventHandlerThread.looper)

    private val broadcastLock = Object()
    private var isBroadcasting = false

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when {
                it.hasExtra("keyCode") -> {
                    val keyCode = it.getIntExtra("keyCode", 0)
                    keyEventHandler.post {
                        notifyKeyEvent(keyCode)
                    }
                }

                it.hasExtra("eventType") -> {
                    val eventType = it.getIntExtra("eventType", 0)
                    val packageName = it.getStringExtra("packageName")
                    handler.post {
                        notifyAccessibilityEvent(eventType, packageName)
                    }
                }
                
                else -> {
                    // Žádná známá extra data, nic neděláme
                    Log.d("AvemeoAccessibility", "onStartCommand: Neznámý intent bez očekávaných extra dat")
                }
            }
        }
        return START_REDELIVER_INTENT
    }

    @SuppressLint("ForegroundServiceType")
    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun createNotification(): Notification {
        val channelId = "accessibility_service_channel"

        val channel = NotificationChannel(
            channelId,
            "Accessibility Service",
            NotificationManager.IMPORTANCE_LOW
        )

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Avemeo accessibility Service")
            .setContentText("Avemeo accessibility service is running")
            .build()
    }

    private fun notifyAccessibilityEvent(eventType: Int, packageName: String?) {
        synchronized(broadcastLock) {
            if (isBroadcasting) {
                handler.postDelayed({ notifyAccessibilityEvent(eventType, packageName) }, 100)
                return
            }
            
            try {
                isBroadcasting = true
                val n = callbacks.beginBroadcast()
                try {
                    for (i in 0 until n) {
                        try {
                            val callback = callbacks.getBroadcastItem(i)
                            callback.onAccessibilityEventTypeReceived(eventType)
                            if (packageName != null) {
                                callback.onAccessibilityEventPackageNameReceived(packageName)
                            }
                        } catch (e: Exception) {
                            Log.e("AvemeoAccessibility", "Failed to notify accessibility event", e)
                        }
                    }
                } finally {
                    callbacks.finishBroadcast()
                }
            } catch (e: IllegalStateException) {
                Log.e("AvemeoAccessibility", "Error in notifyAccessibilityEvent: ${e.message}")
            } finally {
                isBroadcasting = false
            }
        }
    }

    private fun notifyKeyEvent(keyCode: Int) {
        synchronized(broadcastLock) {
            if (isBroadcasting) {
                keyEventHandler.postDelayed({ notifyKeyEvent(keyCode) }, 100)
                return
            }
            
            try {
                isBroadcasting = true
                val n = callbacks.beginBroadcast()
                try {
                    for (i in 0 until n) {
                        try {
                            callbacks.getBroadcastItem(i).onKeyEventReceived(keyCode)
                        } catch (e: Exception) {
                            Log.e("AvemeoAccessibility", "Failed to notify key event", e)
                        }
                    }
                } finally {
                    callbacks.finishBroadcast()
                }
            } catch (e: IllegalStateException) {
                Log.e("AvemeoAccessibility", "Error in notifyKeyEvent: ${e.message}")
            } finally {
                isBroadcasting = false
            }
        }
    }

    override fun onDestroy() {
        shouldReconnect = false
        super.onDestroy()
    }
} 
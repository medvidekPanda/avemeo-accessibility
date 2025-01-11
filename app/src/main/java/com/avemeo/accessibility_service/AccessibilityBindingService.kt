package com.avemeo.accessibility_service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.RemoteCallbackList

class AccessibilityBindingService : Service() {
    private val callbacks = RemoteCallbackList<IAccessibilityCallback>()

    private val binder = object : IAccessibilityService.Stub() {
        override fun registerCallback(callback: IAccessibilityCallback) {
            callbacks.register(callback)
        }

        override fun unregisterCallback(callback: IAccessibilityCallback) {
            callbacks.unregister(callback)
        }
    }

    private fun notifyKeyEvent(keyCode: Int) {
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                callbacks.getBroadcastItem(i).onKeyEventReceived(keyCode)
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    private fun notifyAccessibilityEvent(eventType: Int, packageName: String?) {
        val n = callbacks.beginBroadcast()
        try {
            for (i in 0 until n) {
                val callback = callbacks.getBroadcastItem(i)
                callback.onAccessibilityEventTypeReceived(eventType)
                if (packageName != null) {
                    callback.onAccessibilityEventPackageNameReceived(packageName)
                }
            }
        } finally {
            callbacks.finishBroadcast()
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            when {
                it.hasExtra("keyCode") -> {
                    notifyKeyEvent(it.getIntExtra("keyCode", 0))
                }
                it.hasExtra("eventType") -> {
                    val eventType = it.getIntExtra("eventType", 0)
                    val packageName = it.getStringExtra("packageName")
                    notifyAccessibilityEvent(eventType, packageName)
                }
            }
        }
        return START_NOT_STICKY
    }
} 
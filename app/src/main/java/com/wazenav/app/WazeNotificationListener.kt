package com.wazenav.app

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class WazeNotificationListener : NotificationListenerService() {

    companion object {
        const val TAG = "WazeNav"
        var lastNotification: String? = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName
        if (packageName != "com.waze") return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(android.app.Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val msg = "$title $text $bigText".trim()
        if (msg.isEmpty()) return

        lastNotification = msg
        Log.d(TAG, "Waze: $msg")

        // Envia para o ESP32
        BleService.send(msg)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {}
}

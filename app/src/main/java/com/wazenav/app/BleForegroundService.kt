package com.wazenav.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat

class BleForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(
            "wazenav_ble",
            "Waze Nav BLE",
            NotificationManager.IMPORTANCE_LOW
        )
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, "wazenav_ble")
            .setContentTitle("Waze Nav")
            .setContentText("A enviar navegação para ESP32")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

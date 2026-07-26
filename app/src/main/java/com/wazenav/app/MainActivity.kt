package com.wazenav.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvLastMsg: TextView
    private lateinit var btnPerm: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvLastMsg = findViewById(R.id.tvLastMsg)
        btnPerm = findViewById(R.id.btnPerm)

        btnPerm.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        requestBlePermissions()
        startBleService()
    }

    override fun onResume() {
        super.onResume()
        val listenerEnabled = isNotificationListenerEnabled()
        tvStatus.text = if (listenerEnabled) "Ativo" else "Acesso a Notificações não ativado"
        tvLastMsg.text = WazeNotificationListener.lastNotification ?: ""
    }

    private fun isNotificationListenerEnabled(): Boolean {
        val cn = componentName
        val flat = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        )
        return flat?.contains(cn.flattenToString()) == true
    }

    private fun requestBlePermissions() {
        val perms = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS
        )
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed, 100)
        }
    }

    private fun startBleService() {
        val intent = Intent(this, BleForegroundService::class.java)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE)
            == PackageManager.PERMISSION_GRANTED) {
            startForegroundService(intent)
        }
    }
}

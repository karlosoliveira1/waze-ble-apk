package com.wazenav.app

import android.Manifest
import android.app.Service
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat

class BleService : Service() {

    companion object {
        const val TAG = "WazeNav-BLE"
        private const val DEVICE_NAME = "NavIndiESP"
        private const val SERVICE_UUID = "4fafc201-1fb1-459e-8fcc-c5c9c331914b"
        private const val CHAR_UUID = "beb5483e-16e1-4688-b7f5-ea07361b26a8"

        private var characteristic: BluetoothGattCharacteristic? = null
        private var gatt: BluetoothGatt? = null
        private var bluetoothAdapter: BluetoothAdapter? = null
        private var scanner: BluetoothLeScanner? = null
        private var serviceInstance: BleService? = null
        private var sendQueue = mutableListOf<String>()
        private var isConnected = false

        fun send(data: String) {
            if (isConnected && characteristic != null && gatt != null) {
                try {
                    characteristic?.value = data.toByteArray()
                    gatt?.writeCharacteristic(characteristic)
                    Log.d(TAG, "Enviado: $data")
                } catch (e: Exception) {
                    Log.e(TAG, "Erro ao enviar", e)
                }
            } else {
                synchronized(sendQueue) {
                    sendQueue.add(data)
                }
            }
        }

        fun isBleConnected(): Boolean = isConnected
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (device.name == DEVICE_NAME) {
                Log.d(TAG, "Encontrado: ${device.address}")
                scanner?.stopScan(this)
                connect(device)
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Conectado")
                isConnected = true
                gatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Desconectado")
                isConnected = false
                characteristic = null
                scan()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) return
            val service = gatt?.getService(java.util.UUID.fromString(SERVICE_UUID))
            characteristic = service?.getCharacteristic(java.util.UUID.fromString(CHAR_UUID))
            Log.d(TAG, "Service encontrado: ${characteristic != null}")

            // Envia fila pendente
            synchronized(sendQueue) {
                for (msg in sendQueue) {
                    send(msg)
                }
                sendQueue.clear()
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Escrita OK")
            } else {
                Log.e(TAG, "Falha na escrita: $status")
            }
        }
    }

    private fun getAdapter(): BluetoothAdapter? {
        val manager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    private fun scan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
            != PackageManager.PERMISSION_GRANTED) return
        bluetoothAdapter = getAdapter()
        scanner = bluetoothAdapter?.bluetoothLeScanner
        scanner?.startScan(scanCallback)
        Log.d(TAG, "Scanning...")
    }

    private fun connect(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT)
            != PackageManager.PERMISSION_GRANTED) return
        gatt = device.connectGatt(this, false, gattCallback)
    }

    override fun onCreate() {
        super.onCreate()
        serviceInstance = this
        scan()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        scanner?.stopScan(scanCallback)
        gatt?.close()
    }
}

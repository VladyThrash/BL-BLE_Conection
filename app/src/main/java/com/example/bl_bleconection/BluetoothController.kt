package com.example.bl_bleconection

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import java.util.UUID

class BluetoothController(private val context: Context, private val listener: BluetoothListener) {

    // Estos UUIDs son genéricos. Los definiremos igual en el código del ESP32 más adelante.
    private val SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")
    private val CHAR_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb")

    private val bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanner = bluetoothAdapter?.bluetoothLeScanner

    // Interfaz para avisarle a la UI qué está pasando
    interface BluetoothListener {
        fun onDeviceFound(device: BluetoothDevice)
        fun onConnected()
        fun onDisconnected()
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // Aquí cumplimos tu regla: filtramos y priorizamos dispositivos ESP32
            if (device.name?.contains("ESP32", ignoreCase = true) == true) {
                listener.onDeviceFound(device)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        scanner?.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun connect(device: BluetoothDevice) {
        stopScan()
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    // --- Estructura de Datos (Traducción de tus requerimientos) ---

    // 1. Encendido (Boolean) -> Se envía 1 byte
    fun sendPowerCommand(isOn: Boolean) {
        val byteValue: Byte = if (isOn) 0x01 else 0x00
        sendBytes(byteArrayOf(0xA1.toByte(), byteValue)) // 0xA1 es el identificador de comando
    }

    // 2. Intensidad (0 - 4096) -> Requiere 2 bytes para caber
    fun sendIntensityCommand(pwmValue: Int) {
        val highByte = (pwmValue shr 8).toByte()
        val lowByte = pwmValue.toByte()
        sendBytes(byteArrayOf(0xA2.toByte(), highByte, lowByte)) // 0xA2 es el identificador de PWM
    }

    // 3. Color RGB -> Se envían 3 bytes directos
    fun sendColorCommand(r: Int, g: Int, b: Int) {
        sendBytes(byteArrayOf(0xA3.toByte(), r.toByte(), g.toByte(), b.toByte())) // 0xA3 identificador de color
    }

    @SuppressLint("MissingPermission")
    private fun sendBytes(data: ByteArray) {
        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_UUID)

        if (characteristic != null) {
            characteristic.value = data
            bluetoothGatt?.writeCharacteristic(characteristic)
        }
    }

    // Callback que maneja los cambios de estado de la conexión BLE
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices() // Obligatorio en BLE para leer las características
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                listener.onDisconnected()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                listener.onConnected()
            }
        }
    }
}
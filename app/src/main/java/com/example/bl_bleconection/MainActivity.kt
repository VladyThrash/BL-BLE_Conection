package com.example.bl_bleconection

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity(), BluetoothController.BluetoothListener {

    private lateinit var btnScan: Button
    private lateinit var btnConnect: Button
    private lateinit var deviceListView: ListView
    private lateinit var radioGroup: RadioGroup

    private lateinit var bluetoothController: BluetoothController
    private val deviceList = ArrayList<BluetoothDevice>()
    private val deviceNames = ArrayList<String>()
    private lateinit var adapter: ArrayAdapter<String>

    private var selectedDevice: BluetoothDevice? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnScan = findViewById(R.id.btnScan)
        btnConnect = findViewById(R.id.btnConnect)
        deviceListView = findViewById(R.id.deviceListView)
        radioGroup = findViewById(R.id.radioGroupConnectionType)

        // Inicializar el puente de comunicación BLE
        bluetoothController = BluetoothController(this, this)

        // Configurar la lista visual
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, deviceNames)
        deviceListView.adapter = adapter
        deviceListView.choiceMode = ListView.CHOICE_MODE_SINGLE

        btnScan.setOnClickListener {
            if (checkPermissions()) {
                deviceList.clear()
                deviceNames.clear()
                adapter.notifyDataSetChanged()
                bluetoothController.startScan()
                Toast.makeText(this, "Escaneando dispositivos BLE...", Toast.LENGTH_SHORT).show()
            } else {
                requestPermissions()
            }
        }

        deviceListView.setOnItemClickListener { _, _, position, _ ->
            selectedDevice = deviceList[position]
            btnConnect.isEnabled = true // Activar botón cuando se elige un dispositivo
        }

        btnConnect.setOnClickListener {
            bluetoothController.stopScan()
            if (selectedDevice != null) {
                // Pasar el dispositivo a la pantalla de acciones
                val intent = Intent(this, ActionsActivity::class.java)
                intent.putExtra("DEVICE_ADDRESS", selectedDevice!!.address)
                startActivity(intent)
            }
        }
    }

    // --- Callbacks de la interfaz BluetoothListener ---

    override fun onDeviceFound(device: BluetoothDevice) {
        // Actualizar la interfaz gráfica desde el hilo principal
        runOnUiThread {
            @SuppressLint("MissingPermission")
            val name = device.name ?: "Dispositivo Desconocido"
            val address = device.address
            val displayText = "$name\n$address"

            if (!deviceNames.contains(displayText)) {
                deviceList.add(device)
                deviceNames.add(displayText)
                adapter.notifyDataSetChanged()
            }
        }
    }

    override fun onConnected() {
        // La conexión se manejará en ActionsActivity
    }

    override fun onDisconnected() {
        // La desconexión se manejará en ActionsActivity
    }

    // --- Gestión de Permisos Dinámicos ---

    private fun checkPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT),
                1
            )
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN),
                1
            )
        }
    }
}
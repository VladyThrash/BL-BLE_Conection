package com.example.bl_bleconection

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ActionsActivity : AppCompatActivity(), BluetoothController.BluetoothListener {

    private lateinit var tvStatus: TextView
    private lateinit var switchPower: Switch
    private lateinit var seekBarIntensity: SeekBar
    private lateinit var tvIntensityLabel: TextView
    private lateinit var btnDisconnect: Button

    private lateinit var bluetoothController: BluetoothController
    private var isConnected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_actions)

        tvStatus = findViewById(R.id.tvStatus)
        switchPower = findViewById(R.id.switchPower)
        seekBarIntensity = findViewById(R.id.seekBarIntensity)
        tvIntensityLabel = findViewById(R.id.tvIntensityLabel)
        btnDisconnect = findViewById(R.id.btnDisconnect)

        // Configurar botones de colores
        val btnRed = findViewById<Button>(R.id.btnColorRed)
        val btnGreen = findViewById<Button>(R.id.btnColorGreen)
        val btnBlue = findViewById<Button>(R.id.btnColorBlue)
        val btnYellow = findViewById<Button>(R.id.btnColorYellow)
        val btnPurple = findViewById<Button>(R.id.btnColorPurple)
        val btnWhite = findViewById<Button>(R.id.btnColorWhite)

        bluetoothController = BluetoothController(this, this)

        // Deshabilitar UI hasta que se confirme la conexión
        setUiEnabled(false)
        tvStatus.text = "Conectando..."
        tvStatus.setTextColor(Color.YELLOW)

        // Obtener la MAC que mandó MainActivity e iniciar conexión
        val deviceAddress = intent.getStringExtra("DEVICE_ADDRESS")
        if (deviceAddress != null) {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            val device = bluetoothManager.adapter.getRemoteDevice(deviceAddress)
            bluetoothController.connect(device)
        } else {
            returnToLogin("Error: No se recibió dispositivo")
        }

        // --- Lógica de envío de datos ---

        switchPower.setOnCheckedChangeListener { _, isChecked ->
            if (isConnected) bluetoothController.sendPowerCommand(isChecked)
        }

        seekBarIntensity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvIntensityLabel.text = "Intensidad: $progress%"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                // Escalar el porcentaje (0-100) al rango PWM del ESP32 (0-4096)
                if (isConnected && seekBar != null) {
                    val pwmValue = (seekBar.progress * 4096) / 100
                    bluetoothController.sendIntensityCommand(pwmValue)
                }
            }
        })

        // Configurar envío de colores RGB (Bytes directos)
        btnRed.setOnClickListener { if (isConnected) bluetoothController.sendColorCommand(255, 0, 0) }
        btnGreen.setOnClickListener { if (isConnected) bluetoothController.sendColorCommand(0, 255, 0) }
        btnBlue.setOnClickListener { if (isConnected) bluetoothController.sendColorCommand(0, 0, 255) }
        btnYellow.setOnClickListener { if (isConnected) bluetoothController.sendColorCommand(255, 255, 0) }
        btnPurple.setOnClickListener { if (isConnected) bluetoothController.sendColorCommand(128, 0, 128) }
        btnWhite.setOnClickListener { if (isConnected) bluetoothController.sendColorCommand(255, 255, 255) }

        btnDisconnect.setOnClickListener {
            bluetoothController.disconnect()
            returnToLogin("Desconectado por el usuario")
        }
    }

    // --- Callbacks de Bluetooth ---

    override fun onDeviceFound(device: BluetoothDevice) {} // No se usa aquí

    override fun onConnected() {
        isConnected = true
        runOnUiThread {
            tvStatus.text = "Conectado a ESP32"
            tvStatus.setTextColor(Color.GREEN)
            setUiEnabled(true)
        }
    }

    override fun onDisconnected() {
        isConnected = false
        runOnUiThread {
            returnToLogin("Conexión perdida con el ESP32")
        }
    }

    private fun setUiEnabled(enabled: Boolean) {
        switchPower.isEnabled = enabled
        seekBarIntensity.isEnabled = enabled
    }

    private fun returnToLogin(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        finish() // Cierra esta Activity y automáticamente vuelve al MainActivity
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothController.disconnect() // Seguridad: liberar recursos si se cierra la app
    }
}
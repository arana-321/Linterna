package com.example.linterna_bun

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private var isTorchOn = false // Controla el estado de la linterna
    private var flashMode = 0 // Controla los modos de parpadeo (0: continuo, 1: lento, 2: rápido, 3: ráfaga)
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private val handler = Handler(Looper.getMainLooper()) // Handler para manejar el parpadeo
    private var isFlashing = false
    private val cameraPermissionCode = 1 // Código para identificar la solicitud de permiso

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Verificar y solicitar el permiso de la cámara
        checkCameraPermission()

        // Inicializa CameraManager para controlar la linterna
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]

        // Referencias de los elementos UI
        val imageButtonPower: ImageButton = findViewById(R.id.imageButtonPower)
        val imageButtonRayo: ImageButton = findViewById(R.id.imageButtonRayo)
        val textViewCounter: TextView = findViewById(R.id.textViewCounter)

        // Evento del botón de encendido/apagado de la linterna
        imageButtonPower.setOnClickListener {
            isTorchOn = !isTorchOn
            toggleTorch(isTorchOn)

            // Cambiar color del botón y el TextView cuando se enciende/apaga la linterna
            if (isTorchOn) {
                imageButtonPower.setBackgroundColor(0xFF00BFFF.toInt()) // Color celeste
                imageButtonRayo.setBackgroundColor(0xFF00BFFF.toInt()) // Color celeste
                textViewCounter.setTextColor(0xFF00BFFF.toInt()) // Color celeste
            } else {
                imageButtonPower.setBackgroundColor(0xFFFFFFFF.toInt()) // Color blanco
                imageButtonRayo.setBackgroundColor(0xFFFFFFFF.toInt()) // Color blanco
                textViewCounter.setTextColor(0xFF000000.toInt()) // Color negro
                isFlashing = false
                handler.removeCallbacksAndMessages(null) // Detener cualquier parpadeo activo
                flashMode = 0 // Resetear el modo de parpadeo
                textViewCounter.text = flashMode.toString()
            }
        }

        // Evento del botón de rayo (controla el parpadeo)
        imageButtonRayo.setOnClickListener {
            if (isTorchOn) {
                // Cambiar el modo de parpadeo solo si la linterna está encendida
                flashMode = (flashMode + 1) % 4
                textViewCounter.text = flashMode.toString()

                if (flashMode == 0) {
                    // Modo de encendido continuo
                    isFlashing = false
                    toggleTorch(true)
                } else {
                    // Controla el parpadeo según el modo
                    isFlashing = true
                    flashLight(flashMode)
                }
            } else {
                // Mostrar mensaje si se intenta parpadear con la linterna apagada
                Toast.makeText(this, "La linterna debe estar encendida para parpadear", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Verifica si el permiso de la cámara ya ha sido concedido
    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), cameraPermissionCode)
        } else {
            Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
        }
    }

    // Maneja el resultado de la solicitud de permisos
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permiso de cámara concedido", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Método para encender/apagar la linterna
    private fun toggleTorch(turnOn: Boolean) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraManager.setTorchMode(cameraId, turnOn)
        } else {
            Toast.makeText(this, "No tienes permiso para usar la cámara", Toast.LENGTH_SHORT).show()
        }
    }

    // Método para controlar los modos de parpadeo
    private fun flashLight(mode: Int) {
        handler.removeCallbacksAndMessages(null) // Detiene cualquier parpadeo previo
        val delay = when (mode) {
            1 -> 1000L // Modo 1: parpadeo lento (1 segundo)
            2 -> 500L  // Modo 2: parpadeo rápido (0.5 segundos)
            3 -> 100L  // Modo 3: ráfaga (0.1 segundos)
            else -> 0L
        }

        if (delay > 0) {
            handler.post(object : Runnable {
                override fun run() {
                    if (isFlashing) {
                        isTorchOn = !isTorchOn
                        toggleTorch(isTorchOn)
                        handler.postDelayed(this, delay)
                    }
                }
            })
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Apaga la linterna cuando la actividad se destruye
        toggleTorch(false)
        handler.removeCallbacksAndMessages(null)
    }
}

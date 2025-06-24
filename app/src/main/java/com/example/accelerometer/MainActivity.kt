package com.example.accelerometer

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var stepCountTextView: TextView

    private var stepCount = 0
    private var previousMagnitude = 0.0
    private var threshold = 10.5  // Umbral para detectar paso (ajustable)
    private var cooldown = 0L     // Evita pasos dobles muy rápidos

    private lateinit var tvX: TextView
    private lateinit var tvY: TextView
    private lateinit var tvZ: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvX = findViewById(R.id.tvX)
        tvY = findViewById(R.id.tvY)
        tvZ = findViewById(R.id.tvZ)

        stepCountTextView = findViewById(R.id.tvSteps)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (accelerometer == null) {
            Log.e("Sensor", "Accelerometer not available on this device")
        }
    }

    override fun onResume() {
        super.onResume()
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (it.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                Log.d("Accelerometer", "X: $x, Y: $y, Z: $z")

                tvX.text = "X: %.2f".format(x)
                tvY.text = "Y: %.2f".format(y)
                tvZ.text = "Z: %.2f".format(z)

                // Magnitud sin gravedad constante (restamos 9.81)
                val magnitude = sqrt((x * x + y * y + z * z).toDouble())
                val magnitudeDelta = magnitude - previousMagnitude

                val now = System.currentTimeMillis()

                // Umbral ajustado más sensible + protección por tiempo
                if (magnitudeDelta > 1.2 && now - cooldown > 300) {
                    stepCount++
                    stepCountTextView.text = "Pasos: $stepCount"
                    cooldown = now
                }

                previousMagnitude = magnitude
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }
}
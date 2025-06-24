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
import androidx.core.content.edit

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var stepCountTextView: TextView

    private var stepCount = 0
    private var previousMagnitude = 0.0
    private var smoothedMagnitude = 0.0
    private val alpha = 0.6 // Cuanto más cerca a 1, más suave
    private var cooldown = 0L     // Evita pasos dobles muy rápidos

    private lateinit var tvX: TextView
    private lateinit var tvY: TextView
    private lateinit var tvZ: TextView

    private var currentDate = getCurrentDate()

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

        currentDate = getCurrentDate()
        stepCount = getStepsForToday()
        stepCountTextView.text = "Pasos: $stepCount"

        logAllStoredSteps()

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

                // Calcular magnitud total (aceleración combinada en 3D)
                val rawMagnitude = sqrt((x * x + y * y + z * z).toDouble())

                // Aplicar filtro de paso bajo
                smoothedMagnitude = alpha * smoothedMagnitude + (1 - alpha) * rawMagnitude

                val magnitudeDelta = smoothedMagnitude - previousMagnitude
                previousMagnitude = smoothedMagnitude

                val now = System.currentTimeMillis()

                if (magnitudeDelta > 1.0 && now - cooldown > 300) {
                    // Si el día cambió, reiniciamos
                    val today = getCurrentDate()
                    if (today != currentDate) {
                        currentDate = today
                        stepCount = 0
                    }

                    stepCount++
                    stepCountTextView.text = "Pasos: $stepCount"
                    cooldown = now

                    // Guardar en SharedPreferences
                    saveStepsForToday(stepCount)
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    // Lógica para guardar el conteo de pasos por día

    // Obtener la fecha actual
    private fun getCurrentDate(): String {
        // Simular fechas diferentes para probar varios registros
        val simulatedDaysAgo = 0 // Cambiar este valor para simular diferentes días
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, -simulatedDaysAgo)
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return formatter.format(calendar.time)
    }

    // Guardar y leer los pasos diarios
    private fun saveStepsForToday(steps: Int) {
        val date = getCurrentDate()
        val prefs = getSharedPreferences("step_data", MODE_PRIVATE)
        prefs.edit { putInt(date, steps) }
    }

    private fun getStepsForToday(): Int {
        val date = getCurrentDate()
        val prefs = getSharedPreferences("step_data", MODE_PRIVATE)
        return prefs.getInt(date, 0)
    }

    // Mostrar todos los pasos almacenados en el log
    private fun logAllStoredSteps() {
        val prefs = getSharedPreferences("step_data", MODE_PRIVATE)
        val allEntries = prefs.all
        for ((key, value) in allEntries) {
            Log.d("StepHistory", "Date: $key -> Steps: $value")
        }
    }
}
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
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import kotlin.math.sqrt

class MainActivity : AppCompatActivity(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private lateinit var stepCountTextView: TextView

    private var stepCount = 0
    private var previousMagnitude = 0.0
    private var smoothedMagnitude = 0.0
    private val alpha = 0.6 // Cuanto más cerca a 1, más suave
    private var cooldown = 0L     // Evita pasos dobles muy rápidos

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

        stepCountTextView = findViewById(R.id.textViewStepCount)

        currentDate = getCurrentDate()
        stepCount = getStepsForToday()
        stepCountTextView.text = stepCount.toString()

        logAllStoredSteps()
        showStepHistoryChart()

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

    private fun showStepHistoryChart() {
        val prefs = getSharedPreferences("step_data", MODE_PRIVATE)
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()

        val sorted = prefs.all.toSortedMap() // Ordenar por fecha

        var index = 0f
        for ((date, value) in sorted) {
            if (value is Int) {
                entries.add(BarEntry(index, value.toFloat()))
                labels.add(date)
                index += 1f
            }
        }

        val dataSet = BarDataSet(entries, "Pasos por día")
        dataSet.color = ContextCompat.getColor(this, R.color.purple_500)

        // Habilitar la visualización de valores en cada barra
        dataSet.setDrawValues(true)

        // Configurar formato para mostrar valores enteros (sin decimales)
        dataSet.valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return value.toInt().toString()
            }
        }

        val barData = BarData(dataSet)
        barData.barWidth = 0.9f

        val chart = findViewById<BarChart>(R.id.barChart)
        chart.data = barData
        chart.setFitBars(true)
        chart.description.isEnabled = false
        chart.animateY(1000)

        // Eje X con etiquetas de fecha
        val xAxis = chart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.setDrawLabels(true)
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f

        chart.invalidate() // Redibujar gráfico
    }
}
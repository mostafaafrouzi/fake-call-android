package com.afrouzi.fakecall.scheduling

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

class ShakeDetector(
    var sensitivityThreshold: Float = 13.0f,
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastShakeTimestamp: Long = 0
    private var shakeCount: Int = 0

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        val gX = x / SensorManager.GRAVITY_EARTH
        val gY = y / SensorManager.GRAVITY_EARTH
        val gZ = z / SensorManager.GRAVITY_EARTH

        val gForce = Math.sqrt((gX * gX + gY * gY + gZ * gZ).toDouble()).toFloat()

        if (gForce > (sensitivityThreshold / SensorManager.GRAVITY_EARTH)) {
            val now = System.currentTimeMillis()
            // Ignore events too close together (within 220ms)
            if (lastShakeTimestamp + 220 > now) {
                return
            }

            // Reset shake count after 1.4 seconds of inactivity
            if (lastShakeTimestamp + 1400 < now) {
                shakeCount = 0
            }

            lastShakeTimestamp = now
            shakeCount++

            if (shakeCount >= 2) {
                shakeCount = 0
                onShake()
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}

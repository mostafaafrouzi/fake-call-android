package com.afrouzi.fakecall.scheduling

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.ScheduledCall
import com.afrouzi.fakecall.ui.MainActivity
import com.afrouzi.fakecall.widget.FakeCallWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShakeService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var shakeDetector: ShakeDetector? = null

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val app = FakeCallApplication.from(this)

        shakeDetector = ShakeDetector(sensitivityThreshold = 13.0f) {
            handleShakeTriggered(app)
        }

        accelerometer?.let {
            sensorManager?.registerListener(shakeDetector, it, SensorManager.SENSOR_DELAY_UI)
        }

        startInForeground()
    }

    private fun startInForeground() {
        val channelId = "shake_detection_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                channelId,
                getString(R.string.settings_shake_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.settings_shake_desc)
                setShowBadge(false)
            }
            nm?.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.settings_shake_title))
            .setContentText(getString(R.string.settings_shake_desc))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun handleShakeTriggered(app: FakeCallApplication) {
        serviceScope.launch(Dispatchers.IO) {
            // Check if a call is already pending to avoid spamming
            val futureCalls = app.scheduleRepository.getFutureScheduledCallsList()
            if (futureCalls.isNotEmpty()) {
                return@launch
            }

            val settings = app.settingsRepository.settings.firstOrNull()
            val delaySeconds = settings?.shakeDelaySeconds ?: 5
            val triggerAt = System.currentTimeMillis() + (delaySeconds * 1000L)

            // Use first profile if available, otherwise default
            val profile = app.profileRepository.profiles.firstOrNull()?.firstOrNull()

            val scheduledCall = ScheduledCall(
                profileId = profile?.id,
                callerName = profile?.name ?: getString(R.string.home_default_caller_name),
                callerNumber = profile?.number ?: getString(R.string.home_default_caller_number),
                photoPath = profile?.photoPath,
                voicePath = profile?.voicePath,
                ringtoneUri = profile?.ringtoneUri,
                triggerAtMillis = triggerAt,
                createdAt = System.currentTimeMillis()
            )

            // Subtle haptic confirmation (two short buzzes)
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 80), -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 80, 80, 80), -1)
                }
            }

            val id = app.scheduleRepository.scheduleCall(scheduledCall)
            app.callScheduler.schedule(scheduledCall.copy(id = id))

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@ShakeService,
                    getString(R.string.settings_shake_toast_triggered, delaySeconds),
                    Toast.LENGTH_SHORT
                ).show()
            }

            FakeCallWidgetProvider.updateAllWidgets(this@ShakeService)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        shakeDetector?.let { sensorManager?.unregisterListener(it) }
        sensorManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            runCatching {
                val intent = Intent(context, ShakeService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }
        }

        fun stop(context: Context) {
            runCatching {
                val intent = Intent(context, ShakeService::class.java)
                context.stopService(intent)
            }
        }
    }
}

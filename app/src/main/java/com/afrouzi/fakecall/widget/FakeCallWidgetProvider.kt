package com.afrouzi.fakecall.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.RemoteViews
import android.widget.Toast
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.data.model.ScheduledCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FakeCallWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_SCHEDULE_WIDGET_CALL) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) return

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_NAME_PREFIX + appWidgetId, context.getString(R.string.home_default_caller_name))
                ?: context.getString(R.string.home_default_caller_name)
            val number = prefs.getString(KEY_NUMBER_PREFIX + appWidgetId, context.getString(R.string.home_default_caller_number))
                ?: context.getString(R.string.home_default_caller_number)
            val photoPath = prefs.getString(KEY_PHOTO_PREFIX + appWidgetId, null)
            val voicePath = prefs.getString(KEY_VOICE_PREFIX + appWidgetId, null)
            val ringtoneUri = prefs.getString(KEY_RINGTONE_PREFIX + appWidgetId, null)
            val delaySec = prefs.getLong(KEY_DELAY_PREFIX + appWidgetId, 10L)
            val profileId = if (prefs.contains(KEY_PROFILE_ID_PREFIX + appWidgetId)) {
                prefs.getLong(KEY_PROFILE_ID_PREFIX + appWidgetId, -1L).takeIf { it != -1L }
            } else null

            val app = FakeCallApplication.from(context)
            val triggerAt = System.currentTimeMillis() + (delaySec * 1000L)

            val scheduledCall = ScheduledCall(
                profileId = profileId,
                callerName = name,
                callerNumber = number,
                photoPath = photoPath,
                voicePath = voicePath,
                ringtoneUri = ringtoneUri,
                triggerAtMillis = triggerAt,
                createdAt = System.currentTimeMillis(),
            )

            // Vibrate for feedback
            runCatching {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    manager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(100)
                }
            }

            app.applicationScope.launch(Dispatchers.IO) {
                val insertedId = app.scheduleRepository.scheduleCall(scheduledCall)
                val callToSchedule = scheduledCall.copy(id = insertedId)
                app.callScheduler.schedule(callToSchedule)

                withContext(Dispatchers.Main) {
                    val delayLabel = formatDelayString(context, delaySec)
                    val toastMsg = context.getString(R.string.widget_scheduled_toast, name, delayLabel)
                    Toast.makeText(context, toastMsg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        for (id in appWidgetIds) {
            prefs.remove(KEY_NAME_PREFIX + id)
            prefs.remove(KEY_NUMBER_PREFIX + id)
            prefs.remove(KEY_PHOTO_PREFIX + id)
            prefs.remove(KEY_VOICE_PREFIX + id)
            prefs.remove(KEY_RINGTONE_PREFIX + id)
            prefs.remove(KEY_DELAY_PREFIX + id)
            prefs.remove(KEY_PROFILE_ID_PREFIX + id)
        }
        prefs.apply()
    }

    companion object {
        const val ACTION_SCHEDULE_WIDGET_CALL = "com.afrouzi.fakecall.widget.ACTION_SCHEDULE_WIDGET_CALL"
        const val PREFS_NAME = "fake_call_widgets"
        const val KEY_NAME_PREFIX = "widget_name_"
        const val KEY_NUMBER_PREFIX = "widget_number_"
        const val KEY_PHOTO_PREFIX = "widget_photo_"
        const val KEY_VOICE_PREFIX = "widget_voice_"
        const val KEY_RINGTONE_PREFIX = "widget_ringtone_"
        const val KEY_DELAY_PREFIX = "widget_delay_"
        const val KEY_PROFILE_ID_PREFIX = "widget_profile_id_"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val name = prefs.getString(KEY_NAME_PREFIX + appWidgetId, context.getString(R.string.app_name))
                ?: context.getString(R.string.app_name)
            val delaySec = prefs.getLong(KEY_DELAY_PREFIX + appWidgetId, 10L)

            val views = RemoteViews(context.packageName, R.layout.widget_fake_call)
            views.setTextViewText(R.id.widget_caller_name, name)
            views.setTextViewText(R.id.widget_call_delay, formatDelayString(context, delaySec))

            val intent = Intent(context, FakeCallWidgetProvider::class.java).apply {
                action = ACTION_SCHEDULE_WIDGET_CALL
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                appWidgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_container, pendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun formatDelayString(context: Context, delaySec: Long): String {
            return when (delaySec) {
                10L -> context.getString(R.string.home_delay_10s)
                30L -> context.getString(R.string.home_delay_30s)
                60L -> context.getString(R.string.home_delay_1m)
                300L -> context.getString(R.string.home_delay_5m)
                else -> "${delaySec}s"
            }
        }
    }
}

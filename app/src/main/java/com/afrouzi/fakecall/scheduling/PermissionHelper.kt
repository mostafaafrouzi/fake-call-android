package com.afrouzi.fakecall.scheduling

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

object PermissionHelper {

    fun canPostNotifications(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    fun canUseFullScreenIntent(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.canUseFullScreenIntent() ?: true
        } else {
            true
        }
    }

    fun getNotificationSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    fun getExactAlarmSettingsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }

    fun getFullScreenIntentSettingsIntent(context: Context): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        } else {
            null
        }
    }

    fun openNotificationSettings(context: Context) {
        val intent = getNotificationSettingsIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(context, intent)
    }

    fun openExactAlarmSettings(context: Context) {
        val intent = getExactAlarmSettingsIntent(context)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent != null) {
            safeStartActivity(context, intent)
        } else {
            openAppDetailsSettings(context)
        }
    }

    fun openFullScreenIntentSettings(context: Context) {
        val intent = getFullScreenIntentSettingsIntent(context)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent != null) {
            safeStartActivity(context, intent)
        } else {
            openAppDetailsSettings(context)
        }
    }

    fun openAppDetailsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        safeStartActivity(context, intent)
    }

    private fun safeStartActivity(context: Context, intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
            } catch (e2: Exception) {
                // Ignore fallback failure
            }
        }
    }

    data class EssentialPermissionsStatus(
        val hasNotifications: Boolean,
        val hasExactAlarm: Boolean,
        val hasFullScreenIntent: Boolean,
    ) {
        val allGranted: Boolean
            get() = hasNotifications && hasExactAlarm && hasFullScreenIntent
    }

    fun getEssentialPermissionsStatus(context: Context): EssentialPermissionsStatus {
        return EssentialPermissionsStatus(
            hasNotifications = canPostNotifications(context),
            hasExactAlarm = canScheduleExactAlarms(context),
            hasFullScreenIntent = canUseFullScreenIntent(context),
        )
    }

    fun areAllEssentialPermissionsGranted(context: Context): Boolean {
        return getEssentialPermissionsStatus(context).allGranted
    }

    fun canRecordAudio(context: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun canReadContacts(context: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

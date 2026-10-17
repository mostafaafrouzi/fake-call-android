package com.afrouzi.fakecall.scheduling

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.R
import com.afrouzi.fakecall.call.IncomingCallActivity
import com.afrouzi.fakecall.data.model.ScheduledCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CallAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val scheduleId = intent.getLongExtra(EXTRA_SCHEDULED_CALL_ID, -1L)
        if (scheduleId == -1L) return

        val pendingResult = goAsync()
        val app = FakeCallApplication.from(context)

        app.applicationScope.launch(Dispatchers.IO) {
            try {
                val scheduledCall = app.scheduleRepository.getScheduledCall(scheduleId)
                if (scheduledCall != null) {
                    showIncomingCall(context, scheduledCall)
                    // Delete schedule row after firing
                    app.scheduleRepository.deleteScheduledCall(scheduleId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun showIncomingCall(context: Context, call: ScheduledCall) {
        ensureNotificationChannel(context)

        val callIntent = Intent(context, IncomingCallActivity::class.java).apply {
            action = IncomingCallActivity.ACTION_INCOMING_CALL
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(IncomingCallActivity.EXTRA_CALL_ID, call.id)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NAME, call.callerName)
            putExtra(IncomingCallActivity.EXTRA_CALLER_NUMBER, call.callerNumber)
            putExtra(IncomingCallActivity.EXTRA_PHOTO_PATH, call.photoPath)
            putExtra(IncomingCallActivity.EXTRA_VOICE_PATH, call.voicePath)
            putExtra(IncomingCallActivity.EXTRA_RINGTONE_URI, call.ringtoneUri)
            putExtra(IncomingCallActivity.EXTRA_SCHEDULED_FOR, call.triggerAtMillis)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            call.id.toInt(),
            callIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(call.callerName)
            .setContentText(context.getString(R.string.incoming_call_title))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOngoing(true)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        // Direct launch fallback when permitted
        runCatching {
            context.startActivity(callIntent)
        }
    }

    private fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val channelName = context.getString(R.string.notification_channel_name)
            val channelDesc = context.getString(R.string.notification_channel_desc)
            val channel = NotificationChannel(
                CHANNEL_ID,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = channelDesc
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val ACTION_TRIGGER_CALL = "com.afrouzi.fakecall.ACTION_TRIGGER_CALL"
        const val EXTRA_SCHEDULED_CALL_ID = "extra_scheduled_call_id"
        const val CHANNEL_ID = "incoming_fake_call"
        const val NOTIFICATION_ID = 2001
    }
}

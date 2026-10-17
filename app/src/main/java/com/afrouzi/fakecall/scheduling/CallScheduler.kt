package com.afrouzi.fakecall.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.afrouzi.fakecall.data.model.ScheduledCall

class CallScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun canScheduleExact(): Boolean {
        return PermissionHelper.canScheduleExactAlarms(context)
    }

    private fun createPendingIntent(callId: Long, flags: Int): PendingIntent? {
        val intent = Intent(context, CallAlarmReceiver::class.java).apply {
            action = CallAlarmReceiver.ACTION_TRIGGER_CALL
            putExtra(CallAlarmReceiver.EXTRA_SCHEDULED_CALL_ID, callId)
        }
        return PendingIntent.getBroadcast(
            context,
            callId.toInt(),
            intent,
            flags
        )
    }

    fun schedule(call: ScheduledCall) {
        val manager = alarmManager ?: return
        val pendingIntent = createPendingIntent(
            call.id,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        ) ?: return

        val triggerAtMillis = call.triggerAtMillis

        if (canScheduleExact()) {
            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        } else {
            manager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    fun cancel(callId: Long) {
        val manager = alarmManager ?: return
        val pendingIntent = createPendingIntent(
            callId,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            manager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}

package com.afrouzi.fakecall.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.afrouzi.fakecall.FakeCallApplication
import com.afrouzi.fakecall.data.model.CallHistoryItem
import com.afrouzi.fakecall.data.model.CallOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        val app = FakeCallApplication.from(context)

        app.applicationScope.launch(Dispatchers.IO) {
            try {
                val now = System.currentTimeMillis()
                val allPending = app.scheduleRepository.getFutureScheduledCallsList(0L)
                for (call in allPending) {
                    if (call.triggerAtMillis > now) {
                        app.callScheduler.schedule(call)
                    } else {
                        // Call expired while phone was turned off
                        app.historyRepository.addHistory(
                            CallHistoryItem(
                                callerName = call.callerName,
                                callerNumber = call.callerNumber,
                                scheduledFor = call.triggerAtMillis,
                                firedAt = now,
                                outcome = CallOutcome.MISSED,
                                durationSec = 0
                            )
                        )
                        app.scheduleRepository.deleteScheduledCall(call.id)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

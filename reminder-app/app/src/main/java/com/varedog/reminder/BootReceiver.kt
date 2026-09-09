package com.varedog.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).reminderDao()
                val upcoming = dao.getUpcoming(System.currentTimeMillis())
                AlarmScheduler.rescheduleAll(context, upcoming)
                AlarmScheduler.scheduleHeartbeat(context)
            } finally {
                pending.finish()
            }
        }
    }
}

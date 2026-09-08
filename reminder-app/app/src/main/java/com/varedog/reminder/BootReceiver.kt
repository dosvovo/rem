package com.varedog.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pending = goAsync()
        Thread {
            try {
                val dao = AppDatabase.get(context).reminderDao()
                val upcoming = dao.getUpcoming(System.currentTimeMillis())
                AlarmScheduler.rescheduleAll(context, upcoming)
            } finally {
                pending.finish()
            }
        }.start()
    }
}

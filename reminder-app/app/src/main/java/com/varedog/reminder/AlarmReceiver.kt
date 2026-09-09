package com.varedog.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (intent.getBooleanExtra(EXTRA_HEARTBEAT, false)) {
                    // 心跳：补发所有已过期但未触发的提醒
                    OverdueChecker.fireOverdue(context)
                } else {
                    val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
                    if (id > 0L) {
                        OverdueChecker.fireOne(context, id)
                    }
                }
                // 心跳自我续约，保证检查链不断
                AlarmScheduler.scheduleHeartbeat(context)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
        const val EXTRA_HEARTBEAT = "heartbeat"
    }
}

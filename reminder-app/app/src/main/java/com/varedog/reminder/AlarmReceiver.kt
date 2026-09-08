package com.varedog.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(EXTRA_REMINDER_ID, -1)
        if (id <= 0L) return

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).reminderDao()
                val reminder = dao.getById(id) ?: return@launch
                if (!reminder.enabled) return@launch

                // 弹出悬浮提醒（服务保活窗口）
                val service = Intent(context, OverlayService::class.java)
                    .putExtra(OverlayService.EXTRA_CONTENT, reminder.content)
                context.startForegroundService(service)

                // 重复提醒：计算下一次并重新注册
                val next = nextTriggerMillis(reminder)
                if (next != null) {
                    dao.updateTriggerAt(id, next)
                    AlarmScheduler.schedule(context, reminder.copy(triggerAtMillis = next))
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun nextTriggerMillis(reminder: Reminder): Long? {
        val cal = Calendar.getInstance()
        cal.timeInMillis = reminder.triggerAtMillis
        when (reminder.repeatType) {
            RepeatType.NONE -> return null
            RepeatType.DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
            RepeatType.WEEKLY -> {
                val target = reminder.weekday ?: 1
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (dayOfWeekIndex(cal) != target)
            }
            RepeatType.MONTHLY -> {
                val day = reminder.monthday ?: 1
                do {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                } while (cal.get(Calendar.DAY_OF_MONTH) != day)
            }
        }
        return cal.timeInMillis
    }

    private fun dayOfWeekIndex(cal: Calendar): Int {
        val value = cal.get(Calendar.DAY_OF_WEEK)
        return when (value) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 7
        }
    }

    companion object {
        const val EXTRA_REMINDER_ID = "reminder_id"
    }
}

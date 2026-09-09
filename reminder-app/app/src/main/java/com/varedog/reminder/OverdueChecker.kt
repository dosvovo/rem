package com.varedog.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.app.NotificationCompat
import java.util.Calendar

object OverdueChecker {

    suspend fun fireOne(context: Context, id: Long) {
        val dao = AppDatabase.get(context).reminderDao()
        val reminder = dao.getById(id) ?: return
        if (!reminder.enabled) return
        fire(context, dao, reminder)
    }

    suspend fun fireOverdue(context: Context) {
        val dao = AppDatabase.get(context).reminderDao()
        val overdue = dao.getOverdue(System.currentTimeMillis())
        for (reminder in overdue) {
            fire(context, dao, reminder)
        }
    }

    private suspend fun fire(context: Context, dao: ReminderDao, reminder: Reminder) {
        // 有悬浮窗权限弹悬浮窗，否则发高优先级通知
        if (Settings.canDrawOverlays(context)) {
            val service = Intent(context, OverlayService::class.java)
                .putExtra(OverlayService.EXTRA_CONTENT, reminder.content)
            try {
                context.startForegroundService(service)
            } catch (_: Throwable) {
                notifyFallback(context, reminder)
            }
        } else {
            notifyFallback(context, reminder)
        }

        val next = nextTriggerMillis(reminder)
        if (next != null) {
            dao.updateTriggerAt(reminder.id, next)
            AlarmScheduler.schedule(context, reminder.copy(triggerAtMillis = next))
        } else {
            // 单次提醒已送达，标记禁用避免重复补弹
            dao.update(reminder.copy(enabled = false))
            AlarmScheduler.cancel(context, reminder.id)
        }
    }

    fun notifyFallback(context: Context, reminder: Reminder) {
        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                FALLBACK_CHANNEL,
                "提醒通知",
                NotificationManager.IMPORTANCE_HIGH
            )
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            reminder.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, FALLBACK_CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("提醒时间到")
            .setContentText(reminder.content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(reminder.id.toInt(), notification)
    }

    fun nextTriggerMillis(reminder: Reminder): Long? {
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

    private const val FALLBACK_CHANNEL = "reminder_fallback"
}

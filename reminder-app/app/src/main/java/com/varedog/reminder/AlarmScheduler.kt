package com.varedog.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AlarmScheduler {

    fun schedule(context: Context, reminder: Reminder) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, reminder.id)
        alarm.cancel(pending)
        if (!reminder.enabled) return
        try {
            // setAlarmClock 最精确、不受 SCHEDULE_EXACT_ALARM 限制，
            // 且触发时进程处于活跃状态，允许启动前台服务
            val show = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarm.setAlarmClock(AlarmManager.AlarmClockInfo(reminder.triggerAtMillis, show), pending)
        } catch (_: Throwable) {
            try {
                alarm.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pending
                )
            } catch (_: Throwable) {
                alarm.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pending
                )
            }
        }
    }

    fun cancel(context: Context, reminderId: Long) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pendingIntent(context, reminderId))
    }

    /**
     * 每小时一次心跳检查：进程存活期间即使精确闹钟被 ROM 拦截，
     * 心跳也会补发过期提醒。心跳用非精确闹钟，省电合规。
     */
    fun scheduleHeartbeat(context: Context) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(AlarmReceiver.EXTRA_HEARTBEAT, true)
        val pending = PendingIntent.getBroadcast(
            context,
            HEARTBEAT_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarm.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + HEARTBEAT_INTERVAL_MILLIS,
            pending
        )
    }

    fun rescheduleAll(context: Context, reminders: List<Reminder>) {
        reminders.forEach { schedule(context, it) }
    }

    private fun pendingIntent(context: Context, reminderId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_REMINDER_ID, reminderId)
        }
        return PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private const val HEARTBEAT_REQUEST_CODE = 1999999997
    private const val HEARTBEAT_INTERVAL_MILLIS = 60L * 60 * 1000
}

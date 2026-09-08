package com.varedog.reminder

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class RepeatType { NONE, DAILY, WEEKLY, MONTHLY }

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val triggerAtMillis: Long,
    val repeatType: RepeatType = RepeatType.NONE,
    val weekday: Int? = null,
    val monthday: Int? = null,
    val enabled: Boolean = true
) {
    fun repeatLabel(): String = when (repeatType) {
        RepeatType.NONE -> "单次"
        RepeatType.DAILY -> "每天"
        RepeatType.WEEKLY -> "每周" + WEEKDAY_NAMES[(weekday ?: 1) - 1]
        RepeatType.MONTHLY -> "每月 ${monthday ?: 1} 号"
    }

    companion object {
        val WEEKDAY_NAMES = arrayOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }
}

package com.varedog.reminder

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ReminderDao {

    @Query("SELECT * FROM reminders ORDER BY triggerAtMillis ASC")
    fun observeAll(): Flow<List<Reminder>>

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): Reminder?

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND triggerAtMillis > :now ORDER BY triggerAtMillis ASC")
    suspend fun getUpcoming(now: Long): List<Reminder>

    @Query("SELECT * FROM reminders WHERE enabled = 1 AND triggerAtMillis < :now ORDER BY triggerAtMillis ASC")
    suspend fun getOverdue(now: Long): List<Reminder>

    @Insert
    suspend fun insert(reminder: Reminder): Long

    @Update
    suspend fun update(reminder: Reminder)

    @Query("UPDATE reminders SET triggerAtMillis = :nextMillis WHERE id = :id")
    suspend fun updateTriggerAt(id: Long, nextMillis: Long)

    @Query("DELETE FROM reminders WHERE id = :id")
    suspend fun deleteById(id: Long)
}

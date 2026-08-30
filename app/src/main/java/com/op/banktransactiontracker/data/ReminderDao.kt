package com.op.banktransactiontracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.time.LocalDate

@Dao
interface ReminderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reminder: ReminderEntity): Long

    @Update
    suspend fun update(reminder: ReminderEntity)

    @Delete
    suspend fun delete(reminder: ReminderEntity)

    @Query("SELECT * FROM reminders WHERE type = :type")
    suspend fun getAllByType(type: ReminderType): List<ReminderEntity>

    @Query("SELECT * FROM reminders WHERE reminderDate >= :start AND reminderDate <= :end")
    suspend fun getAllByDateRange(start: LocalDate, end: LocalDate): List<ReminderEntity>

    @Query("SELECT SUM(amount) FROM reminders WHERE type = :type AND status = 'ACTIVE'")
    suspend fun getTotalAmountByType(type: ReminderType): Double?

    @Query("SELECT * FROM reminders WHERE id = :id")
    suspend fun getById(id: Long): ReminderEntity?

    @Query("SELECT * FROM reminders WHERE status = 'ACTIVE' ORDER BY reminderDate")
    suspend fun getAllActive(): List<ReminderEntity>
}
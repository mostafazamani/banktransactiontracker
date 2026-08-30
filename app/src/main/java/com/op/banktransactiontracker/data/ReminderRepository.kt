package com.op.banktransactiontracker.data

import java.time.LocalDate

class ReminderRepository(private val dao: ReminderDao) {

    suspend fun insert(reminder: ReminderEntity): Long = dao.insert(reminder)

    suspend fun update(reminder: ReminderEntity) = dao.update(reminder)

    suspend fun delete(reminder: ReminderEntity) = dao.delete(reminder)

    suspend fun getById(id: Long) = dao.getById(id)

    suspend fun getAllActive() = dao.getAllActive()

    suspend fun getAllByType(type: ReminderType) = dao.getAllByType(type)

    suspend fun getAllByDateRange(start: LocalDate, end: LocalDate) = dao.getAllByDateRange(start, end)

    suspend fun getTotalAmountByType(type: ReminderType): Double = dao.getTotalAmountByType(type) ?: 0.0

    suspend fun getUpcoming(today: LocalDate = LocalDate.now()) = dao.getUpcoming(today)

    suspend fun getTotals(): Map<ReminderType, Double> {
        return ReminderType.entries.associateWith { getTotalAmountByType(it) }
    }
}
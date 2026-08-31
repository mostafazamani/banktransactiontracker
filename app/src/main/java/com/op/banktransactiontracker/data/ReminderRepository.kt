package com.op.banktransactiontracker.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

class ReminderRepository(private val dao: ReminderDao) {

    private val _totalByType = MutableStateFlow<Map<ReminderType, Double>>(emptyMap())
    val totalByType: StateFlow<Map<ReminderType, Double>> = _totalByType.asStateFlow()

    suspend fun insert(reminder: ReminderEntity): Long {
        val id = dao.insert(reminder)
        loadTotals()
        return id
    }

    suspend fun update(reminder: ReminderEntity) {
        dao.update(reminder)
        loadTotals()
    }

    suspend fun delete(reminder: ReminderEntity) {
        dao.delete(reminder)
        loadTotals()
    }

    suspend fun getById(id: Long): ReminderEntity? = dao.getById(id)

    suspend fun getAllByType(type: ReminderType): List<ReminderEntity> = dao.getAllByType(type)

    suspend fun getAllByDateRange(start: LocalDate, end: LocalDate): List<ReminderEntity> =
        dao.getAllByDateRange(start, end)

    suspend fun getTotalByType(type: ReminderType): Double? = dao.getTotalAmountByType(type)

    suspend fun getAllActive(): List<ReminderEntity> = dao.getAllActive()

    suspend fun loadTotals() {
        val totals = mutableMapOf<ReminderType, Double>()
        ReminderType.entries.forEach { type ->
            totals[type] = dao.getTotalAmountByType(type) ?: 0.0
        }
        _totalByType.value = totals
    }
}
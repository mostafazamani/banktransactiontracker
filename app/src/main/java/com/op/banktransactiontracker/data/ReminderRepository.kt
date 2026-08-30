package com.op.banktransactiontracker.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.op.banktransactiontracker.data.ReminderEntity.ReminderType
import com.op.banktransactiontracker.data.ReminderEntity.ReminderStatus
import java.time.LocalDate

class ReminderRepository(private val dao: ReminderDao) {

    private val _totalByType = MutableLiveData<Map<ReminderType, Double>>()
    val totalByType: LiveData<Map<ReminderType, Double>> = _totalByType

    suspend fun insert(reminder: ReminderEntity) {
        dao.insert(reminder)
        loadTotals()
    }

    suspend fun update(reminder: ReminderEntity) {
        dao.update(reminder)
        loadTotals()
    }

    suspend fun delete(reminder: ReminderEntity) {
        dao.delete(reminder)
        loadTotals()
    }

    suspend fun getAllByType(type: ReminderType): List<ReminderEntity> {
        return dao.getAllByType(type)
    }

    suspend fun getAllByDateRange(start: LocalDate, end: LocalDate): List<ReminderEntity> {
        return dao.getAllByDateRange(start, end)
    }

    suspend fun getTotalByType(type: ReminderType): Double? {
        return dao.getTotalAmountByType(type)
    }

    suspend fun getAllActive(): List<ReminderEntity> {
        return dao.getAllActive()
    }

    private suspend fun loadTotals() {
        val totals = mutableMapOf<ReminderType, Double>()
        ReminderType.entries.forEach { type ->
            totals[type] = dao.getTotalAmountByType(type) ?: 0.0
        }
        _totalByType.value = totals
    }
}
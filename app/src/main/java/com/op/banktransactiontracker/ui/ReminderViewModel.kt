package com.op.banktransactiontracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.op.banktransactiontracker.data.ReminderDao
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderEntity.ReminderType
import com.op.banktransactiontracker.data.ReminderRepository
import com.op.banktransactiontracker.data.ReminderStatus
import com.op.banktransactiontracker.data.ReminderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

class ReminderViewModel(private val repository: ReminderRepository) : ViewModel() {

    private val _reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val reminders: StateFlow<List<ReminderEntity>> = _reminders

    private val _filteredReminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val filteredReminders: StateFlow<List<ReminderEntity>> = _filteredReminders

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedType = MutableStateFlow<ReminderType?>(null)
    val selectedType: StateFlow<ReminderType?> = _selectedType

    private val _selectedStartDate = MutableStateFlow<LocalDate?>(null)
    val selectedStartDate: StateFlow<LocalDate?> = _selectedStartDate

    private val _selectedEndDate = MutableStateFlow<LocalDate?>(null)
    val selectedEndDate: StateFlow<LocalDate?> = _selectedEndDate

    private val _showDatePicker = MutableStateFlow(false)
    val showDatePicker: StateFlow<Boolean> = _showDatePicker

    private val _currentReminder = MutableStateFlow<ReminderEntity?>(null)
    val currentReminder: StateFlow<ReminderEntity?> = _currentReminder

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    init {
        loadAllReminders()
    }

    private fun loadAllReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllActive()
            _reminders.value = all
            filterReminders()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        filterReminders()
    }

    fun setSelectedType(type: ReminderType?) {
        _selectedType.value = type
        filterReminders()
    }

    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        _selectedStartDate.value = start
        _selectedEndDate.value = end
        filterReminders()
    }

    fun openDatePicker() {
        _showDatePicker.value = true
    }

    fun closeDatePicker() {
        _showDatePicker.value = false
    }

    private fun filterReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val query = _searchQuery.value.lowercase()
            val type = _selectedType.value
            val start = _selectedStartDate.value
            val end = _selectedEndDate.value

            val filtered = _reminders.value.filter { reminder ->
                val matchesSearch = query.isEmpty() ||
                        reminder.title.lowercase().contains(query) ||
                        (reminder.bank?.lowercase()?.contains(query) == true) ||
                        (reminder.description?.lowercase()?.contains(query) == true)

                val matchesType = type == null || reminder.type == type
                val matchesDate = if (start != null && end != null) {
                    reminder.reminderDate.isAfter(start.minusDays(1)) &&
                            reminder.reminderDate.isBefore(end.plusDays(1))
                } else true

                matchesSearch && matchesType && matchesDate
            }

            _filteredReminders.value = filtered
        }
    }

    fun openAddDialog() {
        _currentReminder.value = null
        _isEditMode.value = false
    }

    fun openEditDialog(reminder: ReminderEntity) {
        _currentReminder.value = reminder
        _isEditMode.value = true
    }

    fun setReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            if (_isEditMode.value) {
                repository.update(reminder)
            } else {
                repository.insert(reminder)
            }
            loadAllReminders()
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(reminder)
            loadAllReminders()
        }
    }

    fun clearFilter() {
        _searchQuery.value = ""
        _selectedType.value = null
        _selectedStartDate.value = null
        _selectedEndDate.value = null
        _filteredReminders.value = _reminders.value
    }

    // کالکشن‌های آماده برای Spinner
    val allTypes = ReminderType.entries.toList()
    val allStatuses = ReminderStatus.entries.toList()

    // برای نمایش تاریخ‌ها
    fun formatDate(date: LocalDate?): String {
        return date?.toString() ?: ""
    }
}
package com.op.banktransactiontracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.op.banktransactiontracker.data.AppDatabase
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderRepository
import com.op.banktransactiontracker.data.ReminderStatus
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository

    private val _reminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val reminders: StateFlow<List<ReminderEntity>> = _reminders.asStateFlow()

    private val _filteredReminders = MutableStateFlow<List<ReminderEntity>>(emptyList())
    val filteredReminders: StateFlow<List<ReminderEntity>> = _filteredReminders.asStateFlow()

    private val _totalByType = MutableStateFlow<Map<ReminderType, Double>>(emptyMap())
    val totalByType: StateFlow<Map<ReminderType, Double>> = _totalByType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _selectedType = MutableStateFlow<ReminderType?>(null)
    private val _selectedStartDate = MutableStateFlow<LocalDate?>(null)
    private val _selectedEndDate = MutableStateFlow<LocalDate?>(null)

    private val _currentReminder = MutableStateFlow<ReminderEntity?>(null)
    val currentReminder: StateFlow<ReminderEntity?> = _currentReminder.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    val allTypes = ReminderType.entries.toList()

    init {
        val dao = AppDatabase.getDatabase(application).reminderDao()
        repository = ReminderRepository(dao)
        loadAllReminders()
    }

    fun loadAllReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            val all = repository.getAllActive()
            _reminders.value = all
            repository.loadTotals()
            _totalByType.value = repository.totalByType.value
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

    private fun filterReminders() {
        val query = _searchQuery.value.lowercase().trim()
        val type = _selectedType.value
        val start = _selectedStartDate.value
        val end = _selectedEndDate.value

        val filtered = _reminders.value.filter { reminder ->
            val matchesSearch = query.isEmpty() ||
                    reminder.title.lowercase().contains(query) ||
                    (reminder.bank?.lowercase()?.contains(query) == true) ||
                    (reminder.beneficiary?.lowercase()?.contains(query) == true) ||
                    (reminder.description?.lowercase()?.contains(query) == true)

            val matchesType = type == null || reminder.type == type

            val matchesDate = if (start != null && end != null) {
                !reminder.reminderDate.isBefore(start) && !reminder.reminderDate.isAfter(end)
            } else true

            matchesSearch && matchesType && matchesDate
        }

        _filteredReminders.value = filtered
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
            val context = getApplication<Application>()
            if (_isEditMode.value && reminder.id != 0L) {
                repository.update(reminder)
                ReminderScheduler.cancel(context, reminder.id)
                ReminderScheduler.schedule(context, reminder)
            } else {
                val id = repository.insert(reminder)
                val withId = reminder.copy(id = id)
                ReminderScheduler.schedule(context, withId)
            }
            loadAllReminders()
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            ReminderScheduler.cancel(context, reminder.id)
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
}

class ReminderViewModelFactory(private val application: Application) :
    androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReminderViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReminderViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
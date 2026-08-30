package com.op.banktransactiontracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.op.banktransactiontracker.data.AppDatabase
import com.op.banktransactiontracker.data.ReminderEntity
import com.op.banktransactiontracker.data.ReminderRepository
import com.op.banktransactiontracker.data.ReminderStatus
import com.op.banktransactiontracker.data.ReminderType
import com.op.banktransactiontracker.utils.ReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ReminderRepository

    private val _reminders = MutableLiveData<List<ReminderEntity>>()
    val reminders: LiveData<List<ReminderEntity>> = _reminders

    private val _totals = MutableLiveData<Map<ReminderType, Double>>()
    val totals: LiveData<Map<ReminderType, Double>> = _totals

    private val _selectedType = MutableLiveData<ReminderType?>(null)
    private val _startDate = MutableLiveData<LocalDate?>(null)
    private val _endDate = MutableLiveData<LocalDate?>(null)
    private val _searchQuery = MutableLiveData<String>("")

    val allTypes = ReminderType.entries

    init {
        val dao = AppDatabase.getDatabase(application).reminderDao()
        repository = ReminderRepository(dao)
        loadReminders()
    }

    fun loadReminders() {
        viewModelScope.launch(Dispatchers.IO) {
            applyFilters()
            _totals.postValue(repository.getTotals())
        }
    }

    private suspend fun applyFilters() {
        var list = repository.getAllActive()

        val type = _selectedType.value
        if (type != null) {
            list = list.filter { it.type == type }
        }

        val start = _startDate.value
        val end = _endDate.value
        if (start != null && end != null) {
            list = list.filter {
                !it.reminderDate.isBefore(start) && !it.reminderDate.isAfter(end)
            }
        }

        val query = _searchQuery.value?.trim()?.lowercase().orEmpty()
        if (query.isNotEmpty()) {
            list = list.filter {
                (it.bank?.lowercase()?.contains(query) == true) ||
                        (it.beneficiary?.lowercase()?.contains(query) == true) ||
                        (it.description?.lowercase()?.contains(query) == true) ||
                        (it.title?.lowercase()?.contains(query) == true)
            }
        }

        _reminders.postValue(list.sortedBy { it.reminderDate })
    }

    fun setTypeFilter(type: ReminderType?) {
        _selectedType.value = type
        loadReminders()
    }

    fun setDateRange(start: LocalDate?, end: LocalDate?) {
        _startDate.value = start
        _endDate.value = end
        loadReminders()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        loadReminders()
    }

    fun clearFilters() {
        _selectedType.value = null
        _startDate.value = null
        _endDate.value = null
        _searchQuery.value = ""
        loadReminders()
    }

    fun insertReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(reminder)
            val withId = reminder.copy(id = id)
            ReminderScheduler.schedule(getApplication(), withId)
            loadReminders()
        }
    }

    fun updateReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(reminder)
            ReminderScheduler.schedule(getApplication(), reminder)
            loadReminders()
        }
    }

    fun deleteReminder(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(reminder)
            ReminderScheduler.cancel(getApplication(), reminder.id)
            loadReminders()
        }
    }

    fun markCompleted(reminder: ReminderEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.update(reminder.copy(status = ReminderStatus.COMPLETED))
            ReminderScheduler.cancel(getApplication(), reminder.id)
            loadReminders()
        }
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
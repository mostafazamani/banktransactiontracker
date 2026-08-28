package com.op.banktransactiontracker.ui

import android.app.Application
import androidx.lifecycle.*
//import androidx.lifecycle.MediatorLiveData
import com.op.banktransactiontracker.data.AppDatabase
import com.op.banktransactiontracker.data.PhoneNumberPreferences
import com.op.banktransactiontracker.data.TransactionEntity
import com.op.banktransactiontracker.data.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    private val phonePrefs: PhoneNumberPreferences

    // فیلترها
    private val _selectedPhone = MutableStateFlow<String?>(null)
    val selectedPhone: StateFlow<String?> = _selectedPhone.asStateFlow()

    private val _fromDate = MutableStateFlow<Long?>(null)
    val fromDate: StateFlow<Long?> = _fromDate.asStateFlow()

    private val _toDate = MutableStateFlow<Long?>(null)
    val toDate: StateFlow<Long?> = _toDate.asStateFlow()

    private val _searchQuery = MutableStateFlow<String?>(null)
    val searchQuery: StateFlow<String?> = _searchQuery.asStateFlow()

    // لیست شماره‌های بانکی
    val phoneNumbers: LiveData<Set<String>>

    // لیست تراکنش‌ها (با فیلتر زنده)
    val transactions: LiveData<List<TransactionEntity>>

    init {
        val dao = AppDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(dao)
        phonePrefs = PhoneNumberPreferences(application)

        phoneNumbers = phonePrefs.phoneNumbersFlow.asLiveData()

        // ترکیب فیلترها برای LiveData زنده
        transactions = MediatorLiveData<List<TransactionEntity>>().apply {
            var currentSource: LiveData<List<TransactionEntity>>? = null

            fun updateSource() {
                currentSource?.let { removeSource(it) }
                val newSource = repository.getFilteredTransactions(
                    phoneNumber = _selectedPhone.value,
                    fromDate = _fromDate.value,
                    toDate = _toDate.value,
                    searchQuery = _searchQuery.value?.takeIf { it.isNotBlank() }
                )
                currentSource = newSource
                addSource(newSource) { value = it }
            }

            // گوش دادن به تغییرات فیلترها
            viewModelScope.launch {
                _selectedPhone.collect { updateSource() }
            }
            viewModelScope.launch {
                _fromDate.collect { updateSource() }
            }
            viewModelScope.launch {
                _toDate.collect { updateSource() }
            }
            viewModelScope.launch {
                _searchQuery.collect { updateSource() }
            }

            // بارگذاری اولیه
            updateSource()
        }
    }

    // --- فیلترها ---
    fun setPhoneFilter(phone: String?) {
        _selectedPhone.value = phone
    }

    fun setDateRange(from: Long?, to: Long?) {
        _fromDate.value = from
        _toDate.value = to
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }

    fun clearFilters() {
        _selectedPhone.value = null
        _fromDate.value = null
        _toDate.value = null
        _searchQuery.value = null
    }

    // --- عملیات روی تراکنش‌ها ---
    fun insertTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insert(transaction)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.update(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.delete(transaction)
        }
    }

    // --- مدیریت شماره‌ها ---
    fun addPhoneNumber(number: String) {
        viewModelScope.launch {
            phonePrefs.addPhoneNumber(number)
        }
    }

    fun removePhoneNumber(number: String) {
        viewModelScope.launch {
            phonePrefs.removePhoneNumber(number)
        }
    }
}

// Factory برای ساخت ViewModel
class TransactionViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TransactionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TransactionViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
package com.op.banktransactiontracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bank_phones")

class PhoneNumberPreferences(private val context: Context) {

    private val PHONE_NUMBERS_KEY = stringSetPreferencesKey("phone_numbers")

    val phoneNumbersFlow: Flow<Set<String>> = context.dataStore.data
        .map { preferences ->
            preferences[PHONE_NUMBERS_KEY] ?: emptySet()
        }

    suspend fun addPhoneNumber(number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PHONE_NUMBERS_KEY] ?: emptySet()
            preferences[PHONE_NUMBERS_KEY] = current + number.trim()
        }
    }

    suspend fun removePhoneNumber(number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[PHONE_NUMBERS_KEY] ?: emptySet()
            preferences[PHONE_NUMBERS_KEY] = current - number.trim()
        }
    }

    suspend fun setPhoneNumbers(numbers: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PHONE_NUMBERS_KEY] = numbers
        }
    }
}
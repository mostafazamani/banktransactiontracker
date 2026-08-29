package com.op.banktransactiontracker.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "bank_phones")

/**
 * هر بانک چند شماره دارد.
 * ذخیره به‌صورت: "نام بانک||شماره1,شماره2,شماره3"
 */
data class BankEntry(
    val name: String,
    val numbers: List<String>
)

class PhoneNumberPreferences(private val context: Context) {

    private val BANKS_KEY = stringSetPreferencesKey("banks")

    val banksFlow: Flow<List<BankEntry>> = context.dataStore.data.map { preferences ->
        val raw = preferences[BANKS_KEY] ?: emptySet()
        raw.mapNotNull { parseEntry(it) }
            .sortedBy { it.name }
    }

    val phoneNumbersFlow: Flow<Set<String>> = banksFlow.map { banks ->
        banks.flatMap { it.numbers }.toSet()
    }

    /** افزودن یا جایگزینی بانک با لیست شماره‌ها */
    suspend fun addBank(name: String, numbers: List<String>) {
        val cleanName = name.trim()
        val cleanNumbers = numbers.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if (cleanName.isBlank() || cleanNumbers.isEmpty()) return

        context.dataStore.edit { preferences ->
            val current = preferences[BANKS_KEY] ?: emptySet()
            val withoutSame = current.filterNot {
                parseEntry(it)?.name.equals(cleanName, ignoreCase = true)
            }.toSet()
            // اگر بانک از قبل بود، شماره‌های قبلی + جدید با هم ادغام می‌شوند
            val existing = current.mapNotNull { parseEntry(it) }
                .firstOrNull { it.name.equals(cleanName, ignoreCase = true) }
            val merged = if (existing != null) {
                (existing.numbers + cleanNumbers).distinct()
            } else {
                cleanNumbers
            }
            preferences[BANKS_KEY] = withoutSame + serializeEntry(cleanName, merged)
        }
    }

    suspend fun removeBank(name: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BANKS_KEY] ?: emptySet()
            preferences[BANKS_KEY] = current.filterNot {
                parseEntry(it)?.name.equals(name, ignoreCase = true)
            }.toSet()
        }
    }

    suspend fun addNumbersToBank(bankName: String, newNumbers: List<String>) {
        val cleanNew = newNumbers.map { it.trim() }.filter { it.isNotBlank() }
        if (cleanNew.isEmpty()) return

        context.dataStore.edit { preferences ->
            val current = preferences[BANKS_KEY] ?: emptySet()
            val updated = current.map { raw ->
                val entry = parseEntry(raw) ?: return@map raw
                if (entry.name.equals(bankName, ignoreCase = true)) {
                    val merged = (entry.numbers + cleanNew).distinct()
                    serializeEntry(entry.name, merged)
                } else {
                    raw
                }
            }.toSet()
            preferences[BANKS_KEY] = updated
        }
    }

    suspend fun removeNumberFromBank(bankName: String, number: String) {
        context.dataStore.edit { preferences ->
            val current = preferences[BANKS_KEY] ?: emptySet()
            val updated = current.mapNotNull { raw ->
                val entry = parseEntry(raw) ?: return@mapNotNull raw
                if (entry.name.equals(bankName, ignoreCase = true)) {
                    val remaining = entry.numbers.filterNot {
                        normalize(it) == normalize(number)
                    }
                    if (remaining.isEmpty()) null
                    else serializeEntry(entry.name, remaining)
                } else {
                    raw
                }
            }.toSet()
            preferences[BANKS_KEY] = updated
        }
    }

    /** پیدا کردن نام بانک از روی شماره پیامک */
    suspend fun findBankName(phone: String): String? {
        val normalized = normalize(phone)
        val banks: List<BankEntry> = banksFlow.first()
        return banks.firstOrNull { bank ->
            bank.numbers.any { saved ->
                val n = normalize(saved)
                n == normalized ||
                        normalized.endsWith(n) ||
                        n.endsWith(normalized)
            }
        }?.name
    }

    private fun serializeEntry(name: String, numbers: List<String>): String {
        return "$name||${numbers.joinToString(",")}"
    }

    private fun parseEntry(raw: String): BankEntry? {
        val parts = raw.split("||", limit = 2)
        if (parts.size != 2) return null
        val name = parts[0].trim()
        val numbers = parts[1].split(",").map { it.trim() }.filter { it.isNotBlank() }
        if (name.isBlank() || numbers.isEmpty()) return null
        return BankEntry(name, numbers)
    }

    private fun normalize(phone: String): String {
        var p = phone.trim()
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")

        // حذف پیش‌شماره بین‌المللی ایران
        when {
            p.startsWith("+98") -> p = p.removePrefix("+98")
            p.startsWith("0098") -> p = p.removePrefix("0098")
            p.startsWith("98") && p.length > 10 -> p = p.removePrefix("98")
        }

        // حذف صفر اول (مثلاً 0912 → 912)
        if (p.startsWith("0") && p.length > 1) {
            p = p.removePrefix("0")
        }

        return p
    }
    /** ویرایش یک شماره داخل بانک (حذف قبلی + افزودن جدید) */
    suspend fun editNumberInBank(bankName: String, oldNumber: String, newNumber: String) {
        val cleanNew = newNumber.trim()
        if (cleanNew.isBlank()) return

        context.dataStore.edit { preferences ->
            val current = preferences[BANKS_KEY] ?: emptySet()
            val updated = current.map { raw ->
                val entry = parseEntry(raw) ?: return@map raw
                if (entry.name.equals(bankName, ignoreCase = true)) {
                    val numbers = entry.numbers
                        .filterNot { normalize(it) == normalize(oldNumber) }
                        .toMutableList()
                    if (numbers.none { normalize(it) == normalize(cleanNew) }) {
                        numbers.add(cleanNew)
                    }
                    serializeEntry(entry.name, numbers)
                } else {
                    raw
                }
            }.toSet()
            preferences[BANKS_KEY] = updated
        }
    }
}
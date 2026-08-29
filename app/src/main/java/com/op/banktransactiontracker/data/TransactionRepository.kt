//package com.op.banktransactiontracker.data
//
//import androidx.lifecycle.LiveData
//
//class TransactionRepository(private val dao: TransactionDao) {
//
//    fun getAllTransactions(): LiveData<List<TransactionEntity>> = dao.getAllTransactions()
//
//    fun getFilteredTransactions(
//        phoneNumber: String?,
//        fromDate: Long?,
//        toDate: Long?,
//        searchQuery: String?
//    ): LiveData<List<TransactionEntity>> {
//        return dao.getFilteredTransactions(phoneNumber, fromDate, toDate, searchQuery)
//    }
//
//    suspend fun insert(transaction: TransactionEntity): Long {
//        return dao.insert(transaction)
//    }
//
//    suspend fun update(transaction: TransactionEntity) {
//        dao.update(transaction)
//    }
//
//    suspend fun delete(transaction: TransactionEntity) {
//        dao.delete(transaction)
//    }
//
//    suspend fun deleteById(id: Long) {
//        dao.deleteById(id)
//    }
//}
package com.op.banktransactiontracker.data

import androidx.lifecycle.LiveData

class TransactionRepository(private val dao: TransactionDao) {

    fun getAllTransactions(): LiveData<List<TransactionEntity>> = dao.getAllTransactions()

    fun getFilteredTransactions(
        bankName: String?,
        fromDate: Long?,
        toDate: Long?,
        searchQuery: String?
    ): LiveData<List<TransactionEntity>> {
        return dao.getFilteredTransactions(bankName, fromDate, toDate, searchQuery)
    }

    suspend fun insert(transaction: TransactionEntity): Long {
        return dao.insert(transaction)
    }

    suspend fun update(transaction: TransactionEntity) {
        dao.update(transaction)
    }

    suspend fun delete(transaction: TransactionEntity) {
        dao.delete(transaction)
    }

    suspend fun deleteById(id: Long) {
        dao.deleteById(id)
    }
}
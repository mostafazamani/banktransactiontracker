package com.op.banktransactiontracker.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY dateTime DESC")
    fun getAllTransactions(): LiveData<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions 
        WHERE (:phoneNumber IS NULL OR phoneNumber = :phoneNumber)
        AND (:fromDate IS NULL OR dateTime >= :fromDate)
        AND (:toDate IS NULL OR dateTime <= :toDate)
        AND (:searchQuery IS NULL OR description LIKE '%' || :searchQuery || '%' 
             OR messageBody LIKE '%' || :searchQuery || '%'
             OR title LIKE '%' || :searchQuery || '%'
             OR senderName LIKE '%' || :searchQuery || '%')
        ORDER BY dateTime DESC
    """)
    fun getFilteredTransactions(
        phoneNumber: String?,
        fromDate: Long?,
        toDate: Long?,
        searchQuery: String?
    ): LiveData<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)
}
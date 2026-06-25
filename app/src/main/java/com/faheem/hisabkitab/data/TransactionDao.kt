package com.faheem.hisabkitab.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("""
        SELECT * FROM transactions
        ORDER BY occurredAt DESC, id DESC
    """)
    fun observeTransactions(): Flow<List<TransactionEntity>>

    @Query("""
        SELECT * FROM transactions
        ORDER BY occurredAt ASC, id ASC
    """)
    suspend fun getTransactionsForExport(): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(transaction: TransactionEntity): Long
}

package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 ORDER BY date DESC")
    fun getAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND strftime('%Y-%m', date / 1000, 'unixepoch') = :monthKey ORDER BY date DESC")
    fun getByMonthKey(monthKey: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND date BETWEEN :start AND :end ORDER BY date DESC")
    fun getByDateRange(start: Long, end: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND categoryId = :categoryId ORDER BY date DESC")
    fun getByCategory(categoryId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND (accountId = :accountId OR toAccountId = :accountId) ORDER BY date DESC")
    fun getByAccount(accountId: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND (note LIKE '%' || :query || '%' OR source LIKE '%' || :query || '%') ORDER BY date DESC")
    fun search(query: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND needsReview = 1 ORDER BY date DESC")
    fun getNeedsReview(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE isDeleted = 0 AND kind = :kind ORDER BY date DESC")
    fun getByKind(kind: String): Flow<List<TransactionEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isDeleted = 0 AND kind = 'INCOME' AND strftime('%Y-%m', date / 1000, 'unixepoch') = :monthKey")
    suspend fun getIncomeTotal(monthKey: String): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM transactions WHERE isDeleted = 0 AND kind = 'EXPENSE' AND strftime('%Y-%m', date / 1000, 'unixepoch') = :monthKey")
    suspend fun getExpenseTotal(monthKey: String): Double

    @Query("SELECT categoryId, COALESCE(SUM(amount), 0.0) as total FROM transactions WHERE isDeleted = 0 AND kind = 'EXPENSE' AND categoryId IS NOT NULL AND strftime('%Y-%m', date / 1000, 'unixepoch') = :monthKey GROUP BY categoryId")
    suspend fun getCategoryBreakdown(monthKey: String): List<CategoryBreakdown>
}

data class CategoryBreakdown(
    val categoryId: Long,
    val total: Double
)

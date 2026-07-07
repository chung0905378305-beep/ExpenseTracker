package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface TransactionRepository {
    suspend fun getAll(): List<Transaction>
    suspend fun getById(id: Long): Transaction?
    suspend fun getByMonthKey(monthKey: String): List<Transaction>
    suspend fun getByDateRange(startDate: Long, endDate: Long): List<Transaction>
    suspend fun getByCategory(categoryId: Long): List<Transaction>
    suspend fun getByAccount(accountId: Long): List<Transaction>
    suspend fun search(query: String): List<Transaction>
    suspend fun getNeedsReview(): List<Transaction>
    suspend fun getIncomeTotal(monthKey: String): Double
    suspend fun getExpenseTotal(monthKey: String): Double
    suspend fun getCategoryBreakdown(monthKey: String): Map<Category, Double>
    suspend fun insert(transaction: Transaction): Long
    suspend fun update(transaction: Transaction)
    suspend fun softDelete(id: Long)
    suspend fun delete(id: Long)
}

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : TransactionRepository {

    private val transactionDao: TransactionDao = database.transactionDao()

    override suspend fun getAll(): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getAll().map { it.toModel() }
    }

    override suspend fun getById(id: Long): Transaction? = withContext(Dispatchers.IO) {
        transactionDao.getById(id)?.toModel()
    }

    override suspend fun getByMonthKey(monthKey: String): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getByMonthKey(monthKey).map { it.toModel() }
    }

    override suspend fun getByDateRange(startDate: Long, endDate: Long): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getByDateRange(startDate, endDate).map { it.toModel() }
    }

    override suspend fun getByCategory(categoryId: Long): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getByCategory(categoryId).map { it.toModel() }
    }

    override suspend fun getByAccount(accountId: Long): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getByAccount(accountId).map { it.toModel() }
    }

    override suspend fun search(query: String): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.search(query).map { it.toModel() }
    }

    override suspend fun getNeedsReview(): List<Transaction> = withContext(Dispatchers.IO) {
        transactionDao.getNeedsReview().map { it.toModel() }
    }

    override suspend fun getIncomeTotal(monthKey: String): Double = withContext(Dispatchers.IO) {
        transactionDao.getIncomeTotal(monthKey) ?: 0.0
    }

    override suspend fun getExpenseTotal(monthKey: String): Double = withContext(Dispatchers.IO) {
        transactionDao.getExpenseTotal(monthKey) ?: 0.0
    }

    override suspend fun getCategoryBreakdown(monthKey: String): Map<Category, Double> = withContext(Dispatchers.IO) {
        val breakdown = transactionDao.getCategoryBreakdown(monthKey)
        val categories = database.categoryDao().getAll()
        breakdown.mapNotNull { entry ->
            val category = categories.find { it.id == entry.categoryId }?.toModel()
            category?.let { it to (entry.total ?: 0.0) }
        }.toMap()
    }

    override suspend fun insert(transaction: Transaction): Long = withContext(Dispatchers.IO) {
        transactionDao.insert(transaction.toEntity())
    }

    override suspend fun update(transaction: Transaction) = withContext(Dispatchers.IO) {
        transactionDao.update(transaction.toEntity())
    }

    override suspend fun softDelete(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.softDelete(id)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        transactionDao.deleteById(id)
    }
}

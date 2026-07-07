package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.BudgetDao
import com.expensetracker.app.domain.model.Budget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface BudgetRepository {
    suspend fun getByMonth(monthKey: String): List<Budget>
    suspend fun getAll(): List<Budget>
    suspend fun insert(budget: Budget): Long
    suspend fun update(budget: Budget)
    suspend fun delete(id: Long)
    suspend fun copyFromLastMonth(monthKey: String): List<Budget>
}

@Singleton
class BudgetRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : BudgetRepository {

    private val budgetDao: BudgetDao = database.budgetDao()

    override suspend fun getByMonth(monthKey: String): List<Budget> = withContext(Dispatchers.IO) {
        budgetDao.getByMonth(monthKey).map { it.toModel() }
    }

    override suspend fun getAll(): List<Budget> = withContext(Dispatchers.IO) {
        budgetDao.getAll().map { it.toModel() }
    }

    override suspend fun insert(budget: Budget): Long = withContext(Dispatchers.IO) {
        budgetDao.insert(budget.toEntity())
    }

    override suspend fun update(budget: Budget) = withContext(Dispatchers.IO) {
        budgetDao.update(budget.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        budgetDao.deleteById(id)
    }

    override suspend fun copyFromLastMonth(monthKey: String): List<Budget> = withContext(Dispatchers.IO) {
        val lastMonth = deriveLastMonthKey(monthKey)
        val lastBudgets = budgetDao.getByMonth(lastMonth).map { it.toModel() }
        val newBudgets = lastBudgets.map { budget ->
            Budget(
                id = 0L,
                monthKey = monthKey,
                categoryId = budget.categoryId,
                limitAmount = budget.limitAmount,
                spentAmount = 0.0,
                alertThreshold = budget.alertThreshold
            )
        }
        newBudgets.map { budget ->
            val id = budgetDao.insert(budget.toEntity())
            budget.copy(id = id)
        }
    }

    private fun deriveLastMonthKey(monthKey: String): String {
        val parts = monthKey.split("-")
        if (parts.size != 2) return monthKey
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        if (month == 1) return "${year - 1}-12"
        val newMonth = month - 1
        return "$year-${newMonth.toString().padStart(2, '0')}"
    }
}

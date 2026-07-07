package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.RecurringRuleDao
import com.expensetracker.app.domain.model.RecurringRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface RecurringRuleRepository {
    suspend fun getActive(): List<RecurringRule>
    suspend fun getById(id: Long): RecurringRule?
    suspend fun getAll(): List<RecurringRule>
    suspend fun insert(rule: RecurringRule): Long
    suspend fun update(rule: RecurringRule)
    suspend fun delete(id: Long)
}

@Singleton
class RecurringRuleRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : RecurringRuleRepository {

    private val recurringRuleDao: RecurringRuleDao = database.recurringRuleDao()

    override suspend fun getActive(): List<RecurringRule> = withContext(Dispatchers.IO) {
        recurringRuleDao.getActive().map { it.toModel() }
    }

    override suspend fun getById(id: Long): RecurringRule? = withContext(Dispatchers.IO) {
        recurringRuleDao.getById(id)?.toModel()
    }

    override suspend fun getAll(): List<RecurringRule> = withContext(Dispatchers.IO) {
        recurringRuleDao.getAll().map { it.toModel() }
    }

    override suspend fun insert(rule: RecurringRule): Long = withContext(Dispatchers.IO) {
        recurringRuleDao.insert(rule.toEntity())
    }

    override suspend fun update(rule: RecurringRule) = withContext(Dispatchers.IO) {
        recurringRuleDao.update(rule.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        recurringRuleDao.deleteById(id)
    }
}

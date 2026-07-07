package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.AccountDao
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.AccountRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AccountRepository {
    suspend fun getAll(): List<Account>
    suspend fun getByRole(role: AccountRole): List<Account>
    suspend fun getActive(): List<Account>
    suspend fun insert(account: Account): Long
    suspend fun update(account: Account)
    suspend fun archive(id: Long)
    suspend fun delete(id: Long)
}

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : AccountRepository {

    private val accountDao: AccountDao = database.accountDao()

    override suspend fun getAll(): List<Account> = withContext(Dispatchers.IO) {
        accountDao.getAll().map { it.toModel() }
    }

    override suspend fun getByRole(role: AccountRole): List<Account> = withContext(Dispatchers.IO) {
        accountDao.getByRole(role.name).map { it.toModel() }
    }

    override suspend fun getActive(): List<Account> = withContext(Dispatchers.IO) {
        accountDao.getActive().map { it.toModel() }
    }

    override suspend fun insert(account: Account): Long = withContext(Dispatchers.IO) {
        accountDao.insert(account.toEntity())
    }

    override suspend fun update(account: Account) = withContext(Dispatchers.IO) {
        accountDao.update(account.toEntity())
    }

    override suspend fun archive(id: Long) = withContext(Dispatchers.IO) {
        accountDao.archive(id)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        accountDao.deleteById(id)
    }
}

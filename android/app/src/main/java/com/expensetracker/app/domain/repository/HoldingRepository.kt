package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.HoldingDao
import com.expensetracker.app.domain.model.Holding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface HoldingRepository {
    suspend fun getAll(): List<Holding>
    suspend fun getByAccount(accountId: Long): List<Holding>
    suspend fun getById(id: Long): Holding?
    suspend fun insert(holding: Holding): Long
    suspend fun update(holding: Holding)
    suspend fun delete(id: Long)
    suspend fun updatePrice(id: Long, price: Double)
}

@Singleton
class HoldingRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : HoldingRepository {

    private val holdingDao: HoldingDao = database.holdingDao()

    override suspend fun getAll(): List<Holding> = withContext(Dispatchers.IO) {
        holdingDao.getAll().map { it.toModel() }
    }

    override suspend fun getByAccount(accountId: Long): List<Holding> = withContext(Dispatchers.IO) {
        holdingDao.getByAccount(accountId).map { it.toModel() }
    }

    override suspend fun getById(id: Long): Holding? = withContext(Dispatchers.IO) {
        holdingDao.getById(id)?.toModel()
    }

    override suspend fun insert(holding: Holding): Long = withContext(Dispatchers.IO) {
        holdingDao.insert(holding.toEntity())
    }

    override suspend fun update(holding: Holding) = withContext(Dispatchers.IO) {
        holdingDao.update(holding.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        holdingDao.deleteById(id)
    }

    override suspend fun updatePrice(id: Long, price: Double) = withContext(Dispatchers.IO) {
        holdingDao.updatePrice(id, price)
    }
}

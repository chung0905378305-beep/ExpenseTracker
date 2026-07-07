package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.HoldingSnapshotDao
import com.expensetracker.app.domain.model.HoldingSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface HoldingSnapshotRepository {
    suspend fun getByHolding(holdingId: Long): List<HoldingSnapshot>
    suspend fun getByHoldingAndRange(holdingId: Long, startDate: Long, endDate: Long): List<HoldingSnapshot>
    suspend fun insert(snapshot: HoldingSnapshot): Long
    suspend fun deleteByHolding(holdingId: Long)
}

@Singleton
class HoldingSnapshotRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : HoldingSnapshotRepository {

    private val snapshotDao: HoldingSnapshotDao = database.holdingSnapshotDao()

    override suspend fun getByHolding(holdingId: Long): List<HoldingSnapshot> = withContext(Dispatchers.IO) {
        snapshotDao.getByHolding(holdingId).map { it.toModel() }
    }

    override suspend fun getByHoldingAndRange(holdingId: Long, startDate: Long, endDate: Long): List<HoldingSnapshot> = withContext(Dispatchers.IO) {
        snapshotDao.getByHoldingAndRange(holdingId, startDate, endDate).map { it.toModel() }
    }

    override suspend fun insert(snapshot: HoldingSnapshot): Long = withContext(Dispatchers.IO) {
        snapshotDao.insert(snapshot.toEntity())
    }

    override suspend fun deleteByHolding(holdingId: Long) = withContext(Dispatchers.IO) {
        snapshotDao.deleteByHolding(holdingId)
    }
}

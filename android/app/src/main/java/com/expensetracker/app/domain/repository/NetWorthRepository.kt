package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.NetWorthSnapshotDao
import com.expensetracker.app.domain.model.NetWorthSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface NetWorthRepository {
    suspend fun getAll(): List<NetWorthSnapshot>
    suspend fun getByRange(startDate: Long, endDate: Long): List<NetWorthSnapshot>
    suspend fun getLatest(): NetWorthSnapshot?
    suspend fun insert(snapshot: NetWorthSnapshot): Long
}

@Singleton
class NetWorthRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : NetWorthRepository {

    private val snapshotDao: NetWorthSnapshotDao = database.netWorthSnapshotDao()

    override suspend fun getAll(): List<NetWorthSnapshot> = withContext(Dispatchers.IO) {
        snapshotDao.getAll().map { it.toModel() }
    }

    override suspend fun getByRange(startDate: Long, endDate: Long): List<NetWorthSnapshot> = withContext(Dispatchers.IO) {
        snapshotDao.getByRange(startDate, endDate).map { it.toModel() }
    }

    override suspend fun getLatest(): NetWorthSnapshot? = withContext(Dispatchers.IO) {
        snapshotDao.getLatest()?.toModel()
    }

    override suspend fun insert(snapshot: NetWorthSnapshot): Long = withContext(Dispatchers.IO) {
        snapshotDao.insert(snapshot.toEntity())
    }
}

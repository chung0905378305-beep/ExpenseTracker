package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensetracker.app.data.local.entity.NetWorthSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: NetWorthSnapshotEntity): Long

    @Query("SELECT * FROM net_worth_snapshots ORDER BY date DESC")
    fun getAll(): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots WHERE date BETWEEN :start AND :end ORDER BY date ASC")
    fun getByRange(start: Long, end: Long): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots ORDER BY date DESC LIMIT 1")
    suspend fun getLatest(): NetWorthSnapshotEntity?
}

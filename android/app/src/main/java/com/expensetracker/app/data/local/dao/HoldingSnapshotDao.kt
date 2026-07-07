package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.expensetracker.app.data.local.entity.HoldingSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingSnapshotDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(snapshot: HoldingSnapshotEntity): Long

    @Query("SELECT * FROM holding_snapshots WHERE holdingId = :holdingId ORDER BY date DESC")
    fun getByHolding(holdingId: Long): Flow<List<HoldingSnapshotEntity>>

    @Query("SELECT * FROM holding_snapshots WHERE holdingId = :holdingId AND date BETWEEN :start AND :end ORDER BY date ASC")
    fun getByHoldingAndRange(holdingId: Long, start: Long, end: Long): Flow<List<HoldingSnapshotEntity>>

    @Query("DELETE FROM holding_snapshots WHERE holdingId = :holdingId")
    suspend fun deleteByHolding(holdingId: Long)
}

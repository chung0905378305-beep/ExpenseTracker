package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.local.entity.HoldingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HoldingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(holding: HoldingEntity): Long

    @Update
    suspend fun update(holding: HoldingEntity)

    @Delete
    suspend fun delete(holding: HoldingEntity)

    @Query("SELECT * FROM holdings ORDER BY name ASC")
    fun getAll(): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE accountId = :accountId ORDER BY name ASC")
    fun getByAccount(accountId: Long): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE type = :type ORDER BY name ASC")
    fun getByType(type: String): Flow<List<HoldingEntity>>

    @Query("SELECT * FROM holdings WHERE id = :id")
    suspend fun getById(id: Long): HoldingEntity?
}

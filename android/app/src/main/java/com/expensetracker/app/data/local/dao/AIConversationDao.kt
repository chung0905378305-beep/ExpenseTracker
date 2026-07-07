package com.expensetracker.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.expensetracker.app.data.local.entity.AIConversationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AIConversationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conversation: AIConversationEntity): Long

    @Update
    suspend fun update(conversation: AIConversationEntity)

    @Delete
    suspend fun delete(conversation: AIConversationEntity)

    @Query("SELECT * FROM ai_conversations ORDER BY createdAt DESC")
    fun getAll(): Flow<List<AIConversationEntity>>

    @Query("SELECT * FROM ai_conversations WHERE id = :id")
    suspend fun getById(id: Long): AIConversationEntity?
}

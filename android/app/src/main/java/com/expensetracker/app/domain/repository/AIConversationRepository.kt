package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.AIConversationDao
import com.expensetracker.app.domain.model.AIConversation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AIConversationRepository {
    suspend fun getAll(): List<AIConversation>
    suspend fun getById(id: Long): AIConversation?
    suspend fun insert(conversation: AIConversation): Long
    suspend fun update(conversation: AIConversation)
    suspend fun delete(id: Long)
    suspend fun clearAll()
}

@Singleton
class AIConversationRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : AIConversationRepository {

    private val conversationDao: AIConversationDao = database.aiConversationDao()

    override suspend fun getAll(): List<AIConversation> = withContext(Dispatchers.IO) {
        conversationDao.getAll().map { it.toModel() }
    }

    override suspend fun getById(id: Long): AIConversation? = withContext(Dispatchers.IO) {
        conversationDao.getById(id)?.toModel()
    }

    override suspend fun insert(conversation: AIConversation): Long = withContext(Dispatchers.IO) {
        conversationDao.insert(conversation.toEntity())
    }

    override suspend fun update(conversation: AIConversation) = withContext(Dispatchers.IO) {
        conversationDao.update(conversation.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        conversationDao.deleteById(id)
    }

    override suspend fun clearAll() = withContext(Dispatchers.IO) {
        conversationDao.deleteAll()
    }
}

package com.expensetracker.app.domain.repository

import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.SubscriptionDao
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface SubscriptionRepository {
    suspend fun getActive(): List<Subscription>
    suspend fun getUpcoming(days: Int): List<Subscription>
    suspend fun getAll(): List<Subscription>
    suspend fun insert(subscription: Subscription): Long
    suspend fun update(subscription: Subscription)
    suspend fun cancel(id: Long)
    suspend fun delete(id: Long)
}

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val database: AppDatabase
) : SubscriptionRepository {

    private val subscriptionDao: SubscriptionDao = database.subscriptionDao()

    override suspend fun getActive(): List<Subscription> = withContext(Dispatchers.IO) {
        subscriptionDao.getActive().map { it.toModel() }
    }

    override suspend fun getUpcoming(days: Int): List<Subscription> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val future = now + days * 24L * 60L * 60L * 1000L
        subscriptionDao.getUpcoming(now, future).map { it.toModel() }
    }

    override suspend fun getAll(): List<Subscription> = withContext(Dispatchers.IO) {
        subscriptionDao.getAll().map { it.toModel() }
    }

    override suspend fun insert(subscription: Subscription): Long = withContext(Dispatchers.IO) {
        subscriptionDao.insert(subscription.toEntity())
    }

    override suspend fun update(subscription: Subscription) = withContext(Dispatchers.IO) {
        subscriptionDao.update(subscription.toEntity())
    }

    override suspend fun cancel(id: Long) = withContext(Dispatchers.IO) {
        subscriptionDao.updateStatus(id, SubscriptionStatus.CANCELLED.name)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        subscriptionDao.deleteById(id)
    }
}

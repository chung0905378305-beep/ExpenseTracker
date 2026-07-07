package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.RecurringFrequency
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionMode
import com.expensetracker.app.domain.model.SubscriptionStatus

@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val categoryId: Long?,
    val accountId: Long?,
    val mode: String,
    val frequency: String?,
    val nextBillingDate: Long,
    val lastGenerated: Long?,
    val status: String,
    val remindDaysBefore: Int
)

fun SubscriptionEntity.toModel(): Subscription = Subscription(
    id = id,
    name = name,
    amount = amount,
    categoryId = categoryId,
    accountId = accountId,
    mode = SubscriptionMode.valueOf(mode),
    frequency = frequency?.let { RecurringFrequency.valueOf(it) },
    nextBillingDate = nextBillingDate,
    lastGenerated = lastGenerated,
    status = SubscriptionStatus.valueOf(status),
    remindDaysBefore = remindDaysBefore
)

fun Subscription.toEntity(): SubscriptionEntity = SubscriptionEntity(
    id = id,
    name = name,
    amount = amount,
    categoryId = categoryId,
    accountId = accountId,
    mode = mode.name,
    frequency = frequency?.name,
    nextBillingDate = nextBillingDate,
    lastGenerated = lastGenerated,
    status = status.name,
    remindDaysBefore = remindDaysBefore
)

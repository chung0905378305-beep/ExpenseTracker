package com.expensetracker.app.domain.model

data class Subscription(
    val id: Long = 0,
    val name: String,
    val amount: Double,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val mode: SubscriptionMode,
    val frequency: RecurringFrequency? = null,
    val nextBillingDate: Long,
    val lastGenerated: Long? = null,
    val status: SubscriptionStatus,
    val remindDaysBefore: Int = 0
)

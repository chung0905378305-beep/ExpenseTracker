package com.expensetracker.app.domain.model

data class RecurringRule(
    val id: Long = 0,
    val kind: TransactionKind,
    val amount: Double,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val note: String = "",
    val frequency: RecurringFrequency,
    val nextRunDate: Long,
    val lastGenerated: Long? = null,
    val active: Boolean = true,
    val dedupHash: String = ""
)

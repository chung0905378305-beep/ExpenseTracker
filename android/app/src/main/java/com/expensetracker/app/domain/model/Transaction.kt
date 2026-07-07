package com.expensetracker.app.domain.model

import java.util.Calendar
import java.util.TimeZone

data class Transaction(
    val id: Long = 0,
    val kind: TransactionKind,
    val amount: Double,
    val categoryId: Long? = null,
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val date: Long,
    val note: String = "",
    val tagIds: List<Long> = emptyList(),
    val attachments: List<String> = emptyList(),
    val needsReview: Boolean = false,
    val source: String = "",
    val affectsAsset: Boolean = true,
    val isDeleted: Boolean = false,
    val refundOf: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val monthKey: String
        get() {
            val calendar = Calendar.getInstance(TimeZone.getDefault())
            calendar.timeInMillis = date
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            return String.format("%04d-%02d", year, month)
        }
}

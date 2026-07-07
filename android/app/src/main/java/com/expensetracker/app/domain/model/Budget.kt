package com.expensetracker.app.domain.model

data class Budget(
    val id: Long = 0,
    val month: String,
    val categoryId: Long? = null,
    val limitAmount: Double = 0.0,
    val copiedFromLast: Boolean = false
)

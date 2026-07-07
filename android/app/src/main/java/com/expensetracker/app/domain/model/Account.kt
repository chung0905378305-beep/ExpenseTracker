package com.expensetracker.app.domain.model

data class Account(
    val id: Long = 0,
    val name: String,
    val role: AccountRole,
    val subtype: String = "",
    val initialBalance: Double = 0.0,
    val currentValueSource: String = "",
    val statementDay: Int? = null,
    val dueDay: Int? = null,
    val archived: Boolean = false,
    val excludeFromNetWorth: Boolean = false,
    val isDeleted: Boolean = false
)

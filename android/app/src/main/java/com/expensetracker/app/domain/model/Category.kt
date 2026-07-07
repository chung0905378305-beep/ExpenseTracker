package com.expensetracker.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val icon: String = "",
    val parentId: Long? = null,
    val defaultKind: TransactionKind? = null,
    val keywords: List<String> = emptyList(),
    val sortHint: Int = 0,
    val isDeleted: Boolean = false
)

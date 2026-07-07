package com.expensetracker.app.domain.model

data class Tag(
    val id: Long = 0,
    val name: String,
    val color: String = ""
)

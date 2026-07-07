package com.expensetracker.app.domain.model

data class AIMessage(
    val role: String,
    val content: String,
    val timestamp: Long
)

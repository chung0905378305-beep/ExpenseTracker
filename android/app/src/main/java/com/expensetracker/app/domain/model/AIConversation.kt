package com.expensetracker.app.domain.model

data class AIConversation(
    val id: Long = 0,
    val title: String,
    val messages: List<AIMessage> = emptyList(),
    val createdAt: Long = System.currentTimeMillis()
)

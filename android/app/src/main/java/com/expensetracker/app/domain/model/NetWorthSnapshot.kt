package com.expensetracker.app.domain.model

data class NetWorthSnapshot(
    val id: Long = 0,
    val date: Long,
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double
)

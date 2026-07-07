package com.expensetracker.app.domain.model

data class HoldingSnapshot(
    val id: Long = 0,
    val holdingId: Long,
    val date: Long,
    val quantity: Double,
    val marketValue: Double,
    val costValue: Double,
    val unrealizedPnl: Double
)

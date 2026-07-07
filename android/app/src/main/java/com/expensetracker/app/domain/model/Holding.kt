package com.expensetracker.app.domain.model

data class Holding(
    val id: Long = 0,
    val type: HoldingType,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val costPrice: Double,
    val currentPrice: Double,
    val priceUpdatedAt: Long,
    val accountId: Long,
    val quoteSource: String = "",
    val costBasisRule: CostBasisRule = CostBasisRule.WEIGHTED_AVG,
    val realizedPnl: Double = 0.0
)

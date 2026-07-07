package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.CostBasisRule
import com.expensetracker.app.domain.model.Holding
import com.expensetracker.app.domain.model.HoldingType

@Entity(tableName = "holdings")
data class HoldingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val symbol: String,
    val name: String,
    val quantity: Double,
    val costPrice: Double,
    val currentPrice: Double,
    val priceUpdatedAt: Long,
    val accountId: Long,
    val quoteSource: String,
    val costBasisRule: String,
    val realizedPnl: Double
)

fun HoldingEntity.toModel(): Holding = Holding(
    id = id,
    type = HoldingType.valueOf(type),
    symbol = symbol,
    name = name,
    quantity = quantity,
    costPrice = costPrice,
    currentPrice = currentPrice,
    priceUpdatedAt = priceUpdatedAt,
    accountId = accountId,
    quoteSource = quoteSource,
    costBasisRule = CostBasisRule.valueOf(costBasisRule),
    realizedPnl = realizedPnl
)

fun Holding.toEntity(): HoldingEntity = HoldingEntity(
    id = id,
    type = type.name,
    symbol = symbol,
    name = name,
    quantity = quantity,
    costPrice = costPrice,
    currentPrice = currentPrice,
    priceUpdatedAt = priceUpdatedAt,
    accountId = accountId,
    quoteSource = quoteSource,
    costBasisRule = costBasisRule.name,
    realizedPnl = realizedPnl
)

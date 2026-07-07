package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.HoldingSnapshot

@Entity(tableName = "holding_snapshots")
data class HoldingSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val holdingId: Long,
    val date: Long,
    val quantity: Double,
    val marketValue: Double,
    val costValue: Double,
    val unrealizedPnl: Double
)

fun HoldingSnapshotEntity.toModel(): HoldingSnapshot = HoldingSnapshot(
    id = id,
    holdingId = holdingId,
    date = date,
    quantity = quantity,
    marketValue = marketValue,
    costValue = costValue,
    unrealizedPnl = unrealizedPnl
)

fun HoldingSnapshot.toEntity(): HoldingSnapshotEntity = HoldingSnapshotEntity(
    id = id,
    holdingId = holdingId,
    date = date,
    quantity = quantity,
    marketValue = marketValue,
    costValue = costValue,
    unrealizedPnl = unrealizedPnl
)

package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.NetWorthSnapshot

@Entity(tableName = "net_worth_snapshots")
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: Long,
    val netWorth: Double,
    val totalAssets: Double,
    val totalLiabilities: Double
)

fun NetWorthSnapshotEntity.toModel(): NetWorthSnapshot = NetWorthSnapshot(
    id = id,
    date = date,
    netWorth = netWorth,
    totalAssets = totalAssets,
    totalLiabilities = totalLiabilities
)

fun NetWorthSnapshot.toEntity(): NetWorthSnapshotEntity = NetWorthSnapshotEntity(
    id = id,
    date = date,
    netWorth = netWorth,
    totalAssets = totalAssets,
    totalLiabilities = totalLiabilities
)

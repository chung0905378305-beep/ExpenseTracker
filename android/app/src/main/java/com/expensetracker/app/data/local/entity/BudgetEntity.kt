package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Budget

@Entity(tableName = "budgets")
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val month: String,
    val categoryId: Long?,
    val limitAmount: Double,
    val copiedFromLast: Boolean
)

fun BudgetEntity.toModel(): Budget = Budget(
    id = id,
    month = month,
    categoryId = categoryId,
    limitAmount = limitAmount,
    copiedFromLast = copiedFromLast
)

fun Budget.toEntity(): BudgetEntity = BudgetEntity(
    id = id,
    month = month,
    categoryId = categoryId,
    limitAmount = limitAmount,
    copiedFromLast = copiedFromLast
)

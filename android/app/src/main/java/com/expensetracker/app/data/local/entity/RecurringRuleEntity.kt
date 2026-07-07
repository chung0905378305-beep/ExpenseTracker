package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.RecurringFrequency
import com.expensetracker.app.domain.model.RecurringRule
import com.expensetracker.app.domain.model.TransactionKind

@Entity(tableName = "recurring_rules")
data class RecurringRuleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String,
    val amount: Double,
    val categoryId: Long?,
    val accountId: Long?,
    val note: String,
    val frequency: String,
    val nextRunDate: Long,
    val lastGenerated: Long?,
    val active: Boolean,
    val dedupHash: String
)

fun RecurringRuleEntity.toModel(): RecurringRule = RecurringRule(
    id = id,
    kind = TransactionKind.valueOf(kind),
    amount = amount,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    frequency = RecurringFrequency.valueOf(frequency),
    nextRunDate = nextRunDate,
    lastGenerated = lastGenerated,
    active = active,
    dedupHash = dedupHash
)

fun RecurringRule.toEntity(): RecurringRuleEntity = RecurringRuleEntity(
    id = id,
    kind = kind.name,
    amount = amount,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    frequency = frequency.name,
    nextRunDate = nextRunDate,
    lastGenerated = lastGenerated,
    active = active,
    dedupHash = dedupHash
)

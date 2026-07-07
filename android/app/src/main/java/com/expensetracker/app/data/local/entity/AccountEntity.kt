package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.AccountRole

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val role: String,
    val subtype: String,
    val initialBalance: Double,
    val currentValueSource: String,
    val statementDay: Int?,
    val dueDay: Int?,
    val archived: Boolean,
    val excludeFromNetWorth: Boolean,
    val isDeleted: Boolean
)

fun AccountEntity.toModel(): Account = Account(
    id = id,
    name = name,
    role = AccountRole.valueOf(role),
    subtype = subtype,
    initialBalance = initialBalance,
    currentValueSource = currentValueSource,
    statementDay = statementDay,
    dueDay = dueDay,
    archived = archived,
    excludeFromNetWorth = excludeFromNetWorth,
    isDeleted = isDeleted
)

fun Account.toEntity(): AccountEntity = AccountEntity(
    id = id,
    name = name,
    role = role.name,
    subtype = subtype,
    initialBalance = initialBalance,
    currentValueSource = currentValueSource,
    statementDay = statementDay,
    dueDay = dueDay,
    archived = archived,
    excludeFromNetWorth = excludeFromNetWorth,
    isDeleted = isDeleted
)

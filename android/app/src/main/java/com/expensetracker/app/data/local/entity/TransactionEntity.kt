package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["toAccountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index("categoryId"),
        Index("accountId"),
        Index("toAccountId"),
        Index("date")
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val kind: String,
    val amount: Double,
    val categoryId: Long?,
    val accountId: Long?,
    val toAccountId: Long?,
    val date: Long,
    val note: String,
    val tagIds: String,
    val attachments: String,
    val needsReview: Boolean,
    val source: String,
    val affectsAsset: Boolean,
    val isDeleted: Boolean,
    val refundOf: Long?,
    val createdAt: Long
)

fun TransactionEntity.toModel(): Transaction = Transaction(
    id = id,
    kind = TransactionKind.valueOf(kind),
    amount = amount,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    date = date,
    note = note,
    tagIds = if (tagIds.isBlank()) emptyList() else tagIds.split("||").map { it.toLong() },
    attachments = if (attachments.isBlank()) emptyList() else attachments.split("||"),
    needsReview = needsReview,
    source = source,
    affectsAsset = affectsAsset,
    isDeleted = isDeleted,
    refundOf = refundOf,
    createdAt = createdAt
)

fun Transaction.toEntity(): TransactionEntity = TransactionEntity(
    id = id,
    kind = kind.name,
    amount = amount,
    categoryId = categoryId,
    accountId = accountId,
    toAccountId = toAccountId,
    date = date,
    note = note,
    tagIds = if (tagIds.isEmpty()) "" else tagIds.joinToString("||") { it.toString() },
    attachments = if (attachments.isEmpty()) "" else attachments.joinToString("||"),
    needsReview = needsReview,
    source = source,
    affectsAsset = affectsAsset,
    isDeleted = isDeleted,
    refundOf = refundOf,
    createdAt = createdAt
)

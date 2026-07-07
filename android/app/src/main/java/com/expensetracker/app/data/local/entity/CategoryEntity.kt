package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.TransactionKind

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val icon: String,
    val parentId: Long?,
    val defaultKind: String?,
    val keywords: String,
    val sortHint: Int,
    val isDeleted: Boolean
)

fun CategoryEntity.toModel(): Category = Category(
    id = id,
    name = name,
    icon = icon,
    parentId = parentId,
    defaultKind = defaultKind?.let { TransactionKind.valueOf(it) },
    keywords = if (keywords.isBlank()) emptyList() else keywords.split("||"),
    sortHint = sortHint,
    isDeleted = isDeleted
)

fun Category.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    parentId = parentId,
    defaultKind = defaultKind?.name,
    keywords = if (keywords.isEmpty()) "" else keywords.joinToString("||"),
    sortHint = sortHint,
    isDeleted = isDeleted
)

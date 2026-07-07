package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.Tag

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val color: String
)

fun TagEntity.toModel(): Tag = Tag(
    id = id,
    name = name,
    color = color
)

fun Tag.toEntity(): TagEntity = TagEntity(
    id = id,
    name = name,
    color = color
)

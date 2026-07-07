package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.AIConversation
import com.expensetracker.app.domain.model.AIMessage
import org.json.JSONArray
import org.json.JSONObject

@Entity(tableName = "ai_conversations")
data class AIConversationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val messages: String,
    val createdAt: Long
)

fun AIConversationEntity.toModel(): AIConversation = AIConversation(
    id = id,
    title = title,
    messages = parseMessages(messages),
    createdAt = createdAt
)

fun AIConversation.toEntity(): AIConversationEntity = AIConversationEntity(
    id = id,
    title = title,
    messages = serializeMessages(messages),
    createdAt = createdAt
)

private fun parseMessages(json: String): List<AIMessage> {
    if (json.isBlank()) return emptyList()
    val array = JSONArray(json)
    val result = mutableListOf<AIMessage>()
    for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        result.add(
            AIMessage(
                role = obj.getString("role"),
                content = obj.getString("content"),
                timestamp = obj.getLong("timestamp")
            )
        )
    }
    return result
}

private fun serializeMessages(messages: List<AIMessage>): String {
    val array = JSONArray()
    for (msg in messages) {
        val obj = JSONObject()
        obj.put("role", msg.role)
        obj.put("content", msg.content)
        obj.put("timestamp", msg.timestamp)
        array.put(obj)
    }
    return array.toString()
}

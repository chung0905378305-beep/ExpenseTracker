package com.expensetracker.app.data.local.converter

import androidx.room.TypeConverter
import com.expensetracker.app.domain.model.AccountRole
import com.expensetracker.app.domain.model.AIMessage
import com.expensetracker.app.domain.model.CostBasisRule
import com.expensetracker.app.domain.model.HoldingType
import com.expensetracker.app.domain.model.RecurringFrequency
import com.expensetracker.app.domain.model.SubscriptionMode
import com.expensetracker.app.domain.model.SubscriptionStatus
import com.expensetracker.app.domain.model.TransactionKind
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

class Converters {

    // List<String> <-> String
    @TypeConverter
    fun stringListToString(value: List<String>?): String {
        if (value.isNullOrEmpty()) return ""
        return value.joinToString("||")
    }

    @TypeConverter
    fun stringToStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("||")
    }

    // List<Long> <-> String
    @TypeConverter
    fun longListToString(value: List<Long>?): String {
        if (value.isNullOrEmpty()) return ""
        return value.joinToString("||") { it.toString() }
    }

    @TypeConverter
    fun stringToLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split("||").map { it.toLong() }
    }

    // List<AIMessage> <-> String
    @TypeConverter
    fun messageListToString(value: List<AIMessage>?): String {
        if (value.isNullOrEmpty()) return ""
        val array = JSONArray()
        for (msg in value) {
            val obj = JSONObject()
            obj.put("role", msg.role)
            obj.put("content", msg.content)
            obj.put("timestamp", msg.timestamp)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun stringToMessageList(value: String?): List<AIMessage> {
        if (value.isNullOrBlank()) return emptyList()
        val array = JSONArray(value)
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

    // Long? null-safe (identity, Room handles natively, but explicit for clarity)
    @TypeConverter
    fun longToLong(value: Long?): Long? = value

    @TypeConverter
    fun longFromLong(value: Long?): Long? = value

    // Instant <-> Long
    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let { Instant.ofEpochMilli(it) }

    // TransactionKind <-> String
    @TypeConverter
    fun transactionKindToString(value: TransactionKind?): String? = value?.name

    @TypeConverter
    fun stringToTransactionKind(value: String?): TransactionKind? =
        value?.let { TransactionKind.valueOf(it) }

    // AccountRole <-> String
    @TypeConverter
    fun accountRoleToString(value: AccountRole?): String? = value?.name

    @TypeConverter
    fun stringToAccountRole(value: String?): AccountRole? =
        value?.let { AccountRole.valueOf(it) }

    // RecurringFrequency <-> String
    @TypeConverter
    fun recurringFrequencyToString(value: RecurringFrequency?): String? = value?.name

    @TypeConverter
    fun stringToRecurringFrequency(value: String?): RecurringFrequency? =
        value?.let { RecurringFrequency.valueOf(it) }

    // SubscriptionMode <-> String
    @TypeConverter
    fun subscriptionModeToString(value: SubscriptionMode?): String? = value?.name

    @TypeConverter
    fun stringToSubscriptionMode(value: String?): SubscriptionMode? =
        value?.let { SubscriptionMode.valueOf(it) }

    // SubscriptionStatus <-> String
    @TypeConverter
    fun subscriptionStatusToString(value: SubscriptionStatus?): String? = value?.name

    @TypeConverter
    fun stringToSubscriptionStatus(value: String?): SubscriptionStatus? =
        value?.let { SubscriptionStatus.valueOf(it) }

    // HoldingType <-> String
    @TypeConverter
    fun holdingTypeToString(value: HoldingType?): String? = value?.name

    @TypeConverter
    fun stringToHoldingType(value: String?): HoldingType? =
        value?.let { HoldingType.valueOf(it) }

    // CostBasisRule <-> String
    @TypeConverter
    fun costBasisRuleToString(value: CostBasisRule?): String? = value?.name

    @TypeConverter
    fun stringToCostBasisRule(value: String?): CostBasisRule? =
        value?.let { CostBasisRule.valueOf(it) }
}

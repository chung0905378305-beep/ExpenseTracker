package com.expensetracker.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.expensetracker.app.domain.model.AppSettings

@Entity(tableName = "app_settings")
data class AppSettingsEntity(
    @PrimaryKey
    val id: Long = 1,
    val baseCurrency: String,
    val includeLiabilityInNetWorth: Boolean,
    val linkAssetToCash: Boolean,
    val appLockEnabled: Boolean,
    val appLockMode: String,
    val hideAmount: Boolean,
    val quoteRefreshTimes: String,
    val language: String,
    val aiApiKey: String,
    val aiBaseUrl: String,
    val aiModel: String,
    val mailEnabled: Boolean,
    val notificationLeadDays: Int
)

fun AppSettingsEntity.toModel(): AppSettings = AppSettings(
    id = id,
    baseCurrency = baseCurrency,
    includeLiabilityInNetWorth = includeLiabilityInNetWorth,
    linkAssetToCash = linkAssetToCash,
    appLockEnabled = appLockEnabled,
    appLockMode = appLockMode,
    hideAmount = hideAmount,
    quoteRefreshTimes = if (quoteRefreshTimes.isBlank()) emptyList() else quoteRefreshTimes.split("||"),
    language = language,
    aiApiKey = aiApiKey,
    aiBaseUrl = aiBaseUrl,
    aiModel = aiModel,
    mailEnabled = mailEnabled,
    notificationLeadDays = notificationLeadDays
)

fun AppSettings.toEntity(): AppSettingsEntity = AppSettingsEntity(
    id = 1,
    baseCurrency = baseCurrency,
    includeLiabilityInNetWorth = includeLiabilityInNetWorth,
    linkAssetToCash = linkAssetToCash,
    appLockEnabled = appLockEnabled,
    appLockMode = appLockMode,
    hideAmount = hideAmount,
    quoteRefreshTimes = if (quoteRefreshTimes.isEmpty()) "" else quoteRefreshTimes.joinToString("||"),
    language = language,
    aiApiKey = aiApiKey,
    aiBaseUrl = aiBaseUrl,
    aiModel = aiModel,
    mailEnabled = mailEnabled,
    notificationLeadDays = notificationLeadDays
)

package com.expensetracker.app.domain.model

data class AppSettings(
    val id: Long = 1,
    val baseCurrency: String = "CNY",
    val includeLiabilityInNetWorth: Boolean = true,
    val linkAssetToCash: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockMode: String = "",
    val hideAmount: Boolean = false,
    val quoteRefreshTimes: List<String> = emptyList(),
    val language: String = "zh-CN",
    val aiApiKey: String = "",
    val aiBaseUrl: String = "",
    val aiModel: String = "",
    val mailEnabled: Boolean = false,
    val notificationLeadDays: Int = 3
)

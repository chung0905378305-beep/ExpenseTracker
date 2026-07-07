package com.expensetracker.app.domain.model

enum class SubscriptionMode(val displayName: String) {
    AUTO_RENEW("周期自动"),
    ONE_TIME("一次性"),
    MANUAL_REMINDER("手动提醒")
}

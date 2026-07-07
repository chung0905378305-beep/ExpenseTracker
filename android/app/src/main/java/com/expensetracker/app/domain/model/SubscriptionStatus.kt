package com.expensetracker.app.domain.model

enum class SubscriptionStatus(val displayName: String) {
    ACTIVE("生效中"),
    CANCELLED("已取消"),
    PAUSED("已暂停")
}

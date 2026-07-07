package com.expensetracker.app.domain.model

enum class TransactionKind(val displayName: String) {
    EXPENSE("支出"),
    INCOME("收入"),
    TRANSFER("转账"),
    REFUND("退款")
}

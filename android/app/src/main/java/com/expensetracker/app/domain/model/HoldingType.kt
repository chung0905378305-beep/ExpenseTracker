package com.expensetracker.app.domain.model

enum class HoldingType(val displayName: String) {
    STOCK("股票"),
    CRYPTO("加密货币"),
    FUND("基金"),
    GOLD("黄金"),
    REAL_ESTATE("房产"),
    CASH_FOREX("外汇现金")
}

package com.expensetracker.app.data.remote.api

import com.google.gson.annotations.SerializedName

data class ExchangeRateDto(
    @SerializedName("base")
    val base: String?,
    @SerializedName("date")
    val date: String?,
    @SerializedName("rates")
    val rates: Map<String, Double>?,
    @SerializedName("success")
    val success: Boolean?,
    @SerializedName("error")
    val error: ErrorInfo?
) {
    data class ErrorInfo(
        @SerializedName("code")
        val code: Int?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("info")
        val info: String?
    )

    /**
     * Get the exchange rate from base currency to target currency.
     * Returns 1.0 if target equals base, or null if not available.
     */
    fun getRate(targetCurrency: String): Double? {
        if (base.equals(targetCurrency, ignoreCase = true)) return 1.0
        return rates?.get(targetCurrency.uppercase())
    }
}

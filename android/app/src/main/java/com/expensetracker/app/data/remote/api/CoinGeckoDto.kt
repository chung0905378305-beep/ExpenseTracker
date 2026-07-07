package com.expensetracker.app.data.remote.api

import com.google.gson.annotations.SerializedName

data class CoinGeckoDto(
    @SerializedName("bitcoin")
    val bitcoin: Map<String, Double>?,
    @SerializedName("ethereum")
    val ethereum: Map<String, Double>?
) {
    /**
     * Generic price response from CoinGecko simple/price endpoint.
     * The top-level keys are coin IDs (e.g. "bitcoin", "ethereum"),
     * and each value maps currency symbols to prices (e.g. {"usd": 50000.0}).
     */
    data class SimplePriceResponse(
        @SerializedName("data")
        val data: Map<String, Map<String, Double>>?
    )

    /**
     * Extract the price for a given coin in a target currency.
     */
    fun getPrice(coinId: String, currency: String): Double? {
        val coinData = when (coinId.lowercase()) {
            "bitcoin", "btc" -> bitcoin
            "ethereum", "eth" -> ethereum
            else -> null
        }
        return coinData?.get(currency.lowercase())
    }
}

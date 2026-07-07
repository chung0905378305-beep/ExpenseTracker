package com.expensetracker.app.data.remote.api

import com.google.gson.annotations.SerializedName

data class YahooQuoteDto(
    @SerializedName("regularMarketPrice")
    val regularMarketPrice: Double?,
    @SerializedName("symbol")
    val symbol: String?,
    @SerializedName("shortName")
    val shortName: String?,
    @SerializedName("currency")
    val currency: String?,
    @SerializedName("marketState")
    val marketState: String?,
    @SerializedName("regularMarketChangePercent")
    val regularMarketChangePercent: Double?
) {
    data class Response(
        @SerializedName("quoteResponse")
        val quoteResponse: QuoteResponse?
    )

    data class QuoteResponse(
        @SerializedName("result")
        val result: List<YahooQuoteDto>?,
        @SerializedName("error")
        val error: Any?
    )
}

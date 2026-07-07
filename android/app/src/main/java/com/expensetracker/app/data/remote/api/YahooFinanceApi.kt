package com.expensetracker.app.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class YahooQuoteDto(
    val symbol: String,
    val regularMarketPrice: Double,
    val currency: String
)

@Singleton
class YahooFinanceApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    suspend fun getQuote(symbol: String): YahooQuoteDto? = withContext(Dispatchers.IO) {
        try {
            val yahooSymbol = convertToYahooSymbol(symbol)
            val url = "https://query1.finance.yahoo.com/v8/finance/chart/${yahooSymbol}"
            val request = Request.Builder().url(url).build()

            val response = suspendCancellableCoroutine<String?> { cont ->
                val call = okHttpClient.newCall(request)
                cont.invokeOnCancellation { call.cancel() }
                call.enqueue(object : okhttp3.Callback {
                    override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                        cont.resume(null) {}
                    }
                    override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                        if (response.isSuccessful) {
                            cont.resume(response.body?.string()) {}
                        } else {
                            cont.resume(null) {}
                        }
                    }
                })
            }

            if (response == null) return@withContext null

            val json = JSONObject(response)
            val chart = json.getJSONObject("chart")
            val result = chart.getJSONArray("result").getJSONObject(0)
            val meta = result.getJSONObject("meta")
            val price = meta.getDouble("regularMarketPrice")
            val currency = meta.optString("currency", "USD")

            YahooQuoteDto(
                symbol = symbol,
                regularMarketPrice = price,
                currency = currency
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun convertToYahooSymbol(symbol: String): String {
        val upper = symbol.uppercase()
        if (upper.matches(Regex("^\\d{6}$"))) {
            return "${upper}.SS"
        }
        if (upper.endsWith(".HK") || upper.endsWith(".SS") || upper.endsWith(".SZ")) {
            return upper
        }
        if (upper.length == 5 && upper.all { it.isDigit() } && !upper.startsWith("0")) {
            return "${upper}.HK"
        }
        return upper
    }
}

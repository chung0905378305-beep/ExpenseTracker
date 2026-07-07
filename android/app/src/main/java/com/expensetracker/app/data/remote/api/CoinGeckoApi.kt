package com.expensetracker.app.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoinGeckoApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val symbolToIdMap = mapOf(
        "BTC" to "bitcoin",
        "ETH" to "ethereum",
        "USDT" to "tether",
        "BNB" to "binancecoin",
        "SOL" to "solana",
        "XRP" to "ripple",
        "ADA" to "cardano",
        "DOGE" to "dogecoin",
        "AVAX" to "avalanche-2",
        "DOT" to "polkadot",
        "MATIC" to "matic-network",
        "LINK" to "chainlink",
        "UNI" to "uniswap",
        "LTC" to "litecoin",
        "ATOM" to "cosmos"
    )

    suspend fun getPrice(coinId: String, currency: String): Double? = withContext(Dispatchers.IO) {
        try {
            val resolvedId = symbolToIdMap[coinId.uppercase()] ?: coinId.lowercase()
            val lowerCurrency = currency.lowercase()
            val url = "https://api.coingecko.com/api/v3/simple/price?ids=${resolvedId}&vs_currencies=${lowerCurrency}"
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
            val coinData = json.optJSONObject(resolvedId)
            coinData?.optDouble(lowerCurrency, 0.0)
        } catch (_: Exception) {
            null
        }
    }
}

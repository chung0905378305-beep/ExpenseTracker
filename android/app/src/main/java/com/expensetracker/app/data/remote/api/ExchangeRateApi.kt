package com.expensetracker.app.data.remote.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRateApi @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val cache = ConcurrentHashMap<String, CachedRates>()
    private val CACHE_DURATION_MS = 60L * 60L * 1000L

    private data class CachedRates(
        val rates: Map<String, Double>,
        val timestamp: Long
    )

    suspend fun getRates(base: String): Map<String, Double>? = withContext(Dispatchers.IO) {
        val cached = cache[base]
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_DURATION_MS) {
            return@withContext cached.rates
        }

        try {
            val url = "https://open.er-api.com/v6/latest/${base.uppercase()}"
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

            if (response == null) {
                return@withContext cached?.rates
            }

            val json = JSONObject(response)
            val ratesObject = json.getJSONObject("rates")
            val rates = mutableMapOf<String, Double>()
            val keys = ratesObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                rates[key] = ratesObject.getDouble(key)
            }

            cache[base] = CachedRates(rates, System.currentTimeMillis())
            rates
        } catch (_: Exception) {
            cached?.rates
        }
    }
}

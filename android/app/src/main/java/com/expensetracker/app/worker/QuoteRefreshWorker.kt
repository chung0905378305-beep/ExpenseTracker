package com.expensetracker.app.worker

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.expensetracker.app.domain.model.HoldingType
import com.expensetracker.app.domain.repository.HoldingRepository
import com.expensetracker.app.data.remote.api.YahooFinanceApi
import com.expensetracker.app.data.remote.api.CoinGeckoApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class QuoteRefreshWorker @AssistedInject constructor(
    @Assisted appContext: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val holdingRepo: HoldingRepository,
    private val yahooApi: YahooFinanceApi,
    private val coinGeckoApi: CoinGeckoApi
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val holdings = holdingRepo.getAll()
            for (holding in holdings) {
                val price = when (holding.type) {
                    HoldingType.STOCK, HoldingType.FUND, HoldingType.BOND -> {
                        try {
                            yahooApi.getQuote(holding.symbol)?.regularMarketPrice
                        } catch (_: Exception) {
                            null
                        }
                    }
                    HoldingType.CRYPTO -> {
                        try {
                            coinGeckoApi.getPrice(holding.coinGeckoId ?: "", holding.currency)
                        } catch (_: Exception) {
                            null
                        }
                    }
                    else -> null
                }

                if (price != null && price > 0.0) {
                    holdingRepo.updatePrice(holding.id, price)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

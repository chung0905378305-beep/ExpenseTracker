package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.AccountRole
import com.expensetracker.app.domain.model.HoldingType
import com.expensetracker.app.domain.repository.AccountRepository
import com.expensetracker.app.domain.repository.HoldingRepository
import com.expensetracker.app.data.remote.api.ExchangeRateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CalculateNetWorthUseCase @Inject constructor(
    private val accountRepo: AccountRepository,
    private val holdingRepo: HoldingRepository,
    private val exchangeRateApi: ExchangeRateApi
) {
    suspend fun execute(): Triple<Double, Double, Double> = withContext(Dispatchers.IO) {
        var totalAssets = 0.0
        var totalLiabilities = 0.0

        val accounts = accountRepo.getActive()
        val baseCurrency = "CNY"

        val rates = try {
            exchangeRateApi.getRates(baseCurrency)
        } catch (_: Exception) {
            null
        }

        for (account in accounts) {
            if (account.excludeFromNetWorth) continue

            val rate = if (account.currency != baseCurrency && rates != null) {
                rates[account.currency] ?: 1.0
            } else {
                1.0
            }

            val convertedBalance = account.balance * rate

            when (account.role) {
                AccountRole.ASSET -> totalAssets += convertedBalance
                AccountRole.LIABILITY -> totalLiabilities += convertedBalance
            }
        }

        val holdings = holdingRepo.getAll()
        for (holding in holdings) {
            if (holding.excludeFromNetWorth) continue

            val rate = if (holding.currency != baseCurrency && rates != null) {
                rates[holding.currency] ?: 1.0
            } else {
                1.0
            }

            val marketValue = holding.quantity * holding.currentPrice * rate
            when (holding.type) {
                HoldingType.STOCK, HoldingType.FUND, HoldingType.CRYPTO, HoldingType.BOND -> {
                    totalAssets += marketValue
                }
            }
        }

        val netWorth = totalAssets - totalLiabilities
        Triple(totalAssets, totalLiabilities, netWorth)
    }
}

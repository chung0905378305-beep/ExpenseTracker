package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind
import com.expensetracker.app.domain.model.RecurringRule
import com.expensetracker.app.domain.model.RecurringFrequency
import com.expensetracker.app.domain.model.Subscription
import com.expensetracker.app.domain.model.SubscriptionStatus
import com.expensetracker.app.domain.repository.TransactionRepository
import com.expensetracker.app.domain.repository.RecurringRuleRepository
import com.expensetracker.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class AutoGenerateTransactionsUseCase @Inject constructor(
    private val txRepo: TransactionRepository,
    private val recurringRuleRepo: RecurringRuleRepository,
    private val subscriptionRepo: SubscriptionRepository
) {
    suspend fun execute(): List<Transaction> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val generated = mutableListOf<Transaction>()

        val activeRules = recurringRuleRepo.getActive()
        for (rule in activeRules) {
            if (rule.nextRunDate <= now) {
                val hash = DedupUseCase.computeHash(
                    "auto", rule.merchant ?: "", rule.amount, rule.nextRunDate
                )
                val existing = txRepo.search(hash)
                if (existing.none { it.dedupHash == hash }) {
                    val tx = Transaction(
                        id = 0L,
                        kind = TransactionKind.EXPENSE,
                        amount = rule.amount,
                        categoryId = rule.categoryId,
                        accountId = rule.accountId,
                        date = rule.nextRunDate,
                        merchant = rule.merchant,
                        note = rule.note,
                        needsReview = true,
                        source = "auto",
                        dedupHash = hash,
                        tags = emptyList()
                    )
                    val id = txRepo.insert(tx)
                    generated.add(tx.copy(id = id))
                    val nextDate = advanceDate(rule.nextRunDate, rule.frequency)
                    recurringRuleRepo.update(rule.copy(nextRunDate = nextDate))
                }
            }
        }

        val activeSubscriptions = subscriptionRepo.getActive()
        for (sub in activeSubscriptions) {
            if (sub.status == SubscriptionStatus.ACTIVE && sub.nextBillingDate <= now) {
                val hash = DedupUseCase.computeHash(
                    "auto", sub.name, sub.amount, sub.nextBillingDate
                )
                val existing = txRepo.search(hash)
                if (existing.none { it.dedupHash == hash }) {
                    val tx = Transaction(
                        id = 0L,
                        kind = TransactionKind.EXPENSE,
                        amount = sub.amount,
                        categoryId = sub.categoryId,
                        accountId = sub.accountId,
                        date = sub.nextBillingDate,
                        merchant = sub.name,
                        note = "订阅自动续费: ${sub.name}",
                        needsReview = true,
                        source = "auto",
                        dedupHash = hash,
                        tags = emptyList()
                    )
                    val id = txRepo.insert(tx)
                    generated.add(tx.copy(id = id))
                    val nextBilling = advanceDate(sub.nextBillingDate, sub.frequency)
                    subscriptionRepo.update(sub.copy(nextBillingDate = nextBilling))
                }
            }
        }

        generated
    }

    private fun advanceDate(currentDate: Long, frequency: RecurringFrequency): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = currentDate
        when (frequency) {
            RecurringFrequency.DAILY -> calendar.add(java.util.Calendar.DAY_OF_MONTH, 1)
            RecurringFrequency.WEEKLY -> calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            RecurringFrequency.MONTHLY -> calendar.add(java.util.Calendar.MONTH, 1)
            RecurringFrequency.YEARLY -> calendar.add(java.util.Calendar.YEAR, 1)
        }
        return calendar.timeInMillis
    }
}


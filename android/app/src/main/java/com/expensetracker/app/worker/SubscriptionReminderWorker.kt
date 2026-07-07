package com.expensetracker.app.worker

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.util.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class SubscriptionReminderWorker @AssistedInject constructor(
    @Assisted appContext: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val subscriptionRepo: SubscriptionRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val upcoming = subscriptionRepo.getUpcoming(7)
            for (sub in upcoming) {
                val daysLeft = ((sub.nextBillingDate - System.currentTimeMillis()) / (24L * 60L * 60L * 1000L)).toInt()
                if (daysLeft >= 0) {
                    NotificationHelper.showSubscriptionReminder(
                        applicationContext, sub.name, daysLeft
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

package com.expensetracker.app.worker

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.expensetracker.app.domain.usecase.AutoGenerateTransactionsUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class AutoGenerateWorker @AssistedInject constructor(
    @Assisted appContext: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val autoGenerateUseCase: AutoGenerateTransactionsUseCase
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val generated = autoGenerateUseCase.execute()
            if (generated.isNotEmpty()) {
                val summary = "自动生成了 ${generated.size} 条交易记录，请检查确认"
                com.expensetracker.app.util.NotificationHelper.showAutoEntrySummary(
                    applicationContext, generated.size
                )
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

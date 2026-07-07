package com.expensetracker.app.worker

import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.hilt.work.HiltWorker
import com.expensetracker.app.domain.usecase.CalculateNetWorthUseCase
import com.expensetracker.app.domain.model.NetWorthSnapshot
import com.expensetracker.app.domain.repository.NetWorthRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

@HiltWorker
class DailySnapshotWorker @AssistedInject constructor(
    @Assisted appContext: android.content.Context,
    @Assisted workerParams: WorkerParameters,
    private val calculateNetWorthUseCase: CalculateNetWorthUseCase,
    private val netWorthRepository: NetWorthRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val (totalAssets, totalLiabilities, netWorth) = calculateNetWorthUseCase.execute()
            val today = System.currentTimeMillis()
            val monthKey = SimpleDateFormat("yyyy-MM", Locale.CHINA).format(today)

            val snapshot = NetWorthSnapshot(
                id = 0L,
                date = today,
                monthKey = monthKey,
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                netWorth = netWorth
            )
            netWorthRepository.insert(snapshot)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

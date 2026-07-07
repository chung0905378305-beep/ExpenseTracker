package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.TransactionEntity
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

enum class TimeRange { WEEK, MONTH, YEAR, CUSTOM }

data class MonthlySummaryData(
    val monthKey: String,
    val income: Double,
    val expense: Double,
    val balance: Double,
    val savingRate: Double
)

data class CategoryBreakdownData(
    val categoryId: Long,
    val categoryName: String,
    val amount: Double,
    val percentage: Float
)

data class TrendDataPoint(
    val monthKey: String,
    val income: Double,
    val expense: Double
)

data class YearlyDataPoint(
    val month: Int,
    val income: Double,
    val expense: Double
)

data class StatsUiState(
    val selectedTimeRange: TimeRange = TimeRange.MONTH,
    val customStart: Long? = null,
    val customEnd: Long? = null,
    val monthlySummary: MonthlySummaryData? = null,
    val categoryBreakdown: List<CategoryBreakdownData> = emptyList(),
    val trendData: List<TrendDataPoint> = emptyList(),
    val yearlyData: List<YearlyDataPoint> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats(TimeRange.MONTH)
    }

    fun loadStats(range: TimeRange, start: Long? = null, end: Long? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedTimeRange = range, customStart = start, customEnd = end) }

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis

            // Calculate range boundaries
            val (rangeStart, rangeEnd) = when (range) {
                TimeRange.WEEK -> {
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                    val weekStart = calendar.timeInMillis
                    calendar.add(Calendar.DAY_OF_WEEK, 6)
                    val weekEnd = calendar.timeInMillis
                    Pair(weekStart, weekEnd)
                }
                TimeRange.MONTH -> {
                    calendar.set(Calendar.DAY_OF_MONTH, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val monthStart = calendar.timeInMillis
                    calendar.add(Calendar.MONTH, 1)
                    calendar.add(Calendar.MILLISECOND, -1)
                    val monthEnd = calendar.timeInMillis
                    Pair(monthStart, monthEnd)
                }
                TimeRange.YEAR -> {
                    calendar.set(Calendar.DAY_OF_YEAR, 1)
                    calendar.set(Calendar.HOUR_OF_DAY, 0)
                    calendar.set(Calendar.MINUTE, 0)
                    calendar.set(Calendar.SECOND, 0)
                    val yearStart = calendar.timeInMillis
                    calendar.add(Calendar.YEAR, 1)
                    calendar.add(Calendar.MILLISECOND, -1)
                    val yearEnd = calendar.timeInMillis
                    Pair(yearStart, yearEnd)
                }
                TimeRange.CUSTOM -> Pair(start ?: now, end ?: now)
            }

            val transactions = transactionDao.getByDateRange(rangeStart, rangeEnd).let { flow ->
                var list = emptyList<TransactionEntity>()
                flow.collect { list = it }
                list
            }

            val monthlySummary = computeMonthlySummary(transactions)
            val categoryBreakdown = computeCategoryBreakdown(transactions)
            val trendData = computeTrendData()
            val yearlyData = computeYearlyData()

            _uiState.update {
                it.copy(
                    monthlySummary = monthlySummary,
                    categoryBreakdown = categoryBreakdown,
                    trendData = trendData,
                    yearlyData = yearlyData,
                    isLoading = false
                )
            }
        }
    }

    fun dragCard(from: Int, to: Int) {
        // Reorder cards - handled by the composable directly
    }

    private fun computeMonthlySummary(entities: List<TransactionEntity>): MonthlySummaryData {
        val income = entities.filter { it.kind == "INCOME" }.sumOf { it.amount }
        val expense = entities.filter { it.kind == "EXPENSE" }.sumOf { it.amount }
        val balance = income - expense
        val savingRate = if (income > 0) (balance / income * 100) else 0.0
        val cal = Calendar.getInstance()
        val monthKey = String.format("%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

        return MonthlySummaryData(
            monthKey = monthKey,
            income = income,
            expense = expense,
            balance = balance,
            savingRate = savingRate
        )
    }

    private suspend fun computeCategoryBreakdown(entities: List<TransactionEntity>): List<CategoryBreakdownData> {
        val expenseEntities = entities.filter { it.kind == "EXPENSE" }
        val totalExpense = expenseEntities.sumOf { it.amount }
        if (totalExpense <= 0) return emptyList()

        val categories = categoryDao.getAll().let { flow ->
            var list = emptyList<com.expensetracker.app.data.local.entity.CategoryEntity>()
            flow.collect { list = it }
            list
        }
        val categoryMap = categories.associateBy { it.id }

        val grouped = expenseEntities.groupBy { it.categoryId ?: -1L }
        return grouped.map { (categoryId, list) ->
            val amount = list.sumOf { it.amount }
            CategoryBreakdownData(
                categoryId = categoryId,
                categoryName = categoryMap[categoryId]?.name ?: "未分类",
                amount = amount,
                percentage = (amount / totalExpense).toFloat()
            )
        }.sortedByDescending { it.amount }
    }

    private suspend fun computeTrendData(): List<TrendDataPoint> {
        val calendar = Calendar.getInstance()
        val trendList = mutableListOf<TrendDataPoint>()

        for (i in 5 downTo 0) {
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(Calendar.MONTH, -i)
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val monthStart = calendar.timeInMillis
            calendar.add(Calendar.MONTH, 1)
            calendar.add(Calendar.MILLISECOND, -1)
            val monthEnd = calendar.timeInMillis

            val monthKey = String.format(
                "%04d-%02d",
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH) + 1 // Since we added 1 month, subtract 1
            )
            // Recalculate proper key
            val cal2 = Calendar.getInstance()
            cal2.timeInMillis = monthStart
            val key = String.format("%04d-%02d", cal2.get(Calendar.YEAR), cal2.get(Calendar.MONTH) + 1)

            val income = transactionDao.getIncomeTotal(key)
            val expense = transactionDao.getExpenseTotal(key)

            trendList.add(TrendDataPoint(key, income, expense))
        }

        return trendList
    }

    private suspend fun computeYearlyData(): List<YearlyDataPoint> {
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val yearlyList = mutableListOf<YearlyDataPoint>()

        for (month in 1..12) {
            val monthKey = String.format("%04d-%02d", currentYear, month)
            val income = transactionDao.getIncomeTotal(monthKey)
            val expense = transactionDao.getExpenseTotal(monthKey)
            yearlyList.add(YearlyDataPoint(month, income, expense))
        }

        return yearlyList
    }
}

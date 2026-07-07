package com.expensetracker.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.component.EmptyState
import com.expensetracker.app.ui.component.LoadingSkeletonCard
import com.expensetracker.app.ui.component.formatAmount
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.ExpenseLight
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.IncomeLight
import com.expensetracker.app.ui.theme.TextSecondary
import com.expensetracker.app.ui.viewmodel.StatsViewModel
import com.expensetracker.app.ui.viewmodel.TimeRange

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onCategoryClick: (Long) -> Unit,
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("统计", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Time range selector
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                listOf(
                    TimeRange.WEEK to "本周",
                    TimeRange.MONTH to "本月",
                    TimeRange.YEAR to "本年",
                ).forEachIndexed { index, (range, label) ->
                    SegmentedButton(
                        selected = uiState.selectedTimeRange == range,
                        onClick = { viewModel.loadStats(range) },
                        shape = SegmentedButtonDefaults.itemShape(index, 3)
                    ) {
                        Text(text = label, fontSize = 13.sp)
                    }
                }
            }

            if (uiState.isLoading) {
                Column {
                    repeat(5) { LoadingSkeletonCard() }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)
                ) {
                    // Subscription summary card
                    item(key = "subscription") {
                        SubscriptionSummaryCard(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }

                    // Monthly summary card
                    uiState.monthlySummary?.let { summary ->
                        item(key = "summary") {
                            MonthlySummaryStatsCard(
                                summary = summary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Category breakdown
                    if (uiState.categoryBreakdown.isNotEmpty()) {
                        item(key = "category_header") {
                            CardTitle("分类支出", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                        item(key = "category_chart") {
                            CategoryBreakdownCard(
                                categories = uiState.categoryBreakdown,
                                onCategoryClick = onCategoryClick,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Trend card
                    if (uiState.trendData.isNotEmpty()) {
                        item(key = "trend_header") {
                            CardTitle("收支趋势", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                        item(key = "trend_chart") {
                            TrendCard(
                                data = uiState.trendData,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }

                    // Yearly comparison
                    if (uiState.yearlyData.isNotEmpty()) {
                        item(key = "yearly_header") {
                            CardTitle("年度对比", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                        }
                        item(key = "yearly_chart") {
                            YearlyComparisonCard(
                                data = uiState.yearlyData,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CardTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { /* drag handle */ }, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.SwapVert, contentDescription = "拖动排序", tint = TextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun SubscriptionSummaryCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("即将到期的订阅", style = MaterialTheme.typography.titleSmall)
                Text("暂无即将到期的订阅", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun MonthlySummaryStatsCard(
    summary: com.expensetracker.app.ui.viewmodel.MonthlySummaryData,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("月度概览", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem("收入", formatAmount(summary.income), Income)
                StatItem("支出", formatAmount(summary.expense), Expense)
                StatItem("结余", formatAmount(summary.balance), if (summary.balance >= 0) Income else Expense)
                StatItem("储蓄率", "${String.format("%.1f", summary.savingRate)}%", MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = color, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun CategoryBreakdownCard(
    categories: List<com.expensetracker.app.ui.viewmodel.CategoryBreakdownData>,
    onCategoryClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            categories.take(8).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCategoryClick(item.categoryId) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(72.dp)
                    )
                    // Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(12.dp)
                            .padding(horizontal = 8.dp)
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val barWidth = size.width * item.percentage
                            drawRoundRect(
                                color = Expense,
                                topLeft = Offset.Zero,
                                size = Size(barWidth, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                            )
                        }
                    }
                    Text(
                        text = formatAmount(item.amount),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        modifier = Modifier.width(80.dp),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = "${String.format("%.1f", item.percentage * 100)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.width(44.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

@Composable
fun TrendCard(
    data: List<com.expensetracker.app.ui.viewmodel.TrendDataPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                LegendItem("收入", Income)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("支出", Expense)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            ) {
                val allValues = data.flatMap { listOf(it.income, it.expense) }
                val maxVal = allValues.maxOrNull()?.toFloat() ?: 1f
                val minVal = 0f
                val range = maxVal - minVal
                val stepX = size.width / (data.size - 1).coerceAtLeast(1)

                // Draw income line
                val incomePath = Path()
                val expensePath = Path()

                data.forEachIndexed { index, point ->
                    val x = index * stepX
                    val incomeY = size.height - ((point.income.toFloat() - minVal) / range * size.height)
                    val expenseY = size.height - ((point.expense.toFloat() - minVal) / range * size.height)

                    if (index == 0) {
                        incomePath.moveTo(x, incomeY)
                        expensePath.moveTo(x, expenseY)
                    } else {
                        incomePath.lineTo(x, incomeY)
                        expensePath.lineTo(x, expenseY)
                    }
                }

                drawPath(
                    path = incomePath,
                    color = Income,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )
                drawPath(
                    path = expensePath,
                    color = Expense,
                    style = Stroke(width = 2.5f, cap = StrokeCap.Round)
                )

                // Draw points
                data.forEachIndexed { index, point ->
                    val x = index * stepX
                    val incomeY = size.height - ((point.income.toFloat() - minVal) / range * size.height)
                    val expenseY = size.height - ((point.expense.toFloat() - minVal) / range * size.height)
                    drawCircle(Income, 4f, Offset(x, incomeY))
                    drawCircle(Expense, 4f, Offset(x, expenseY))
                }
            }
            // X-axis labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { point ->
                    Text(
                        text = point.monthKey.takeLast(2) + "月",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(modifier = Modifier.size(10.dp)) {
            drawCircle(color = color, radius = 5f)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun YearlyComparisonCard(
    data: List<com.expensetracker.app.ui.viewmodel.YearlyDataPoint>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row {
                LegendItem("收入", Income)
                Spacer(modifier = Modifier.width(16.dp))
                LegendItem("支出", Expense)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                val allValues = data.flatMap { listOf(it.income, it.expense) }
                val maxVal = allValues.maxOrNull()?.toFloat() ?: 1f
                val barCount = data.size
                val totalBarWidth = size.width / barCount
                val barWidth = totalBarWidth * 0.35f
                val spacing = totalBarWidth * 0.15f

                data.forEachIndexed { index, point ->
                    val groupX = index * totalBarWidth
                    val incomeH = (point.income.toFloat() / maxVal * size.height)
                    val expenseH = (point.expense.toFloat() / maxVal * size.height)

                    // Income bar
                    drawRect(
                        color = Income,
                        topLeft = Offset(groupX + spacing, size.height - incomeH),
                        size = Size(barWidth, incomeH)
                    )
                    // Expense bar
                    drawRect(
                        color = Expense,
                        topLeft = Offset(groupX + spacing + barWidth, size.height - expenseH),
                        size = Size(barWidth, expenseH)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                data.forEach { point ->
                    Text(
                        text = "${point.month}月",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.width(24.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

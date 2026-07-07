package com.expensetracker.app.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.ui.component.LoadingSkeletonCard
import com.expensetracker.app.ui.component.formatAmount
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.ExpenseLight
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.IncomeLight
import com.expensetracker.app.ui.theme.TextSecondary
import com.expensetracker.app.ui.viewmodel.HoldingDetailViewModel
import com.expensetracker.app.ui.viewmodel.SnapshotsTimeRange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HoldingDetailScreen(
    holdingId: Long,
    onBack: () -> Unit,
    viewModel: HoldingDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showSellSheet by remember { mutableStateOf(false) }
    var sellQuantity by remember { mutableStateOf("") }
    var sellPrice by remember { mutableStateOf("") }

    LaunchedEffect(holdingId) {
        viewModel.load(holdingId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.holding?.name ?: "持仓详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(modifier = Modifier.padding(padding)) {
                repeat(4) { LoadingSkeletonCard() }
            }
        } else {
            val holding = uiState.holding
            if (holding == null) {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("持仓不存在", color = TextSecondary)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Header
                    val marketValue = holding.quantity * holding.currentPrice
                    val costValue = holding.quantity * holding.costPrice
                    val unrealizedPnl = marketValue - costValue
                    val pnlPercent = if (costValue > 0) (unrealizedPnl / costValue * 100) else 0.0

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (unrealizedPnl >= 0) IncomeLight else ExpenseLight
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(holding.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text(holding.symbol, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(formatAmount(holding.currentPrice), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text("当前价", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("市值", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    Text(formatAmount(marketValue), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("盈亏", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                                    val pnlColor = if (unrealizedPnl >= 0) Income else Expense
                                    val sign = if (unrealizedPnl >= 0) "+" else ""
                                    Text("${sign}${String.format("%.2f", pnlPercent)}%", style = MaterialTheme.typography.titleSmall, color = pnlColor, fontWeight = FontWeight.SemiBold)
                                    Text("${sign}${formatAmount(unrealizedPnl)}", style = MaterialTheme.typography.bodySmall, color = pnlColor)
                                }
                            }
                        }
                    }

                    // Time range tabs
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        listOf(
                            SnapshotsTimeRange.WEEK to "周",
                            SnapshotsTimeRange.MONTH to "月",
                            SnapshotsTimeRange.QUARTER to "季",
                            SnapshotsTimeRange.YEAR to "年",
                            SnapshotsTimeRange.ALL to "总计"
                        ).forEachIndexed { index, (range, label) ->
                            SegmentedButton(
                                selected = uiState.timeRange == range,
                                onClick = { viewModel.loadSnapshots(range) },
                                shape = SegmentedButtonDefaults.itemShape(index, 5)
                            ) {
                                Text(text = label, fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Area chart
                    if (uiState.snapshots.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .padding(12.dp)
                            ) {
                                val values = uiState.snapshots.map { it.marketValue.toFloat() }
                                val maxVal = values.maxOrNull() ?: 1f
                                val minVal = values.minOrNull() ?: 0f
                                val range = maxVal - minVal
                                if (range <= 0 || values.size < 2) return@Canvas

                                val stepX = size.width / (values.size - 1)

                                // Draw gradient fill
                                val fillPath = Path()
                                fillPath.moveTo(0f, size.height)
                                values.forEachIndexed { index, value ->
                                    val x = index * stepX
                                    val y = size.height - ((value - minVal) / range * size.height * 0.8f + size.height * 0.1f)
                                    if (index == 0) fillPath.lineTo(x, y)
                                    else fillPath.lineTo(x, y)
                                }
                                fillPath.lineTo(size.width, size.height)
                                fillPath.close()

                                drawPath(
                                    path = fillPath,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                                        )
                                    )
                                )

                                // Draw line
                                val linePath = Path()
                                values.forEachIndexed { index, value ->
                                    val x = index * stepX
                                    val y = size.height - ((value - minVal) / range * size.height * 0.8f + size.height * 0.1f)
                                    if (index == 0) linePath.moveTo(x, y) else linePath.lineTo(x, y)
                                }
                                drawPath(
                                    path = linePath,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = Stroke(width = 2.5f)
                                )

                                // Draw dots
                                values.forEachIndexed { index, value ->
                                    val x = index * stepX
                                    val y = size.height - ((value - minVal) / range * size.height * 0.8f + size.height * 0.1f)
                                    drawCircle(MaterialTheme.colorScheme.primary, 4f, Offset(x, y))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Stats row
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatCell("已实现盈亏", formatAmount(holding.realizedPnl), if (holding.realizedPnl >= 0) Income else Expense)
                            StatCell("未实现盈亏", "${if (unrealizedPnl >= 0) "+" else ""}${String.format("%.2f", pnlPercent)}%", if (unrealizedPnl >= 0) Income else Expense)
                            StatCell("持仓数量", String.format("%.4f", holding.quantity), MaterialTheme.colorScheme.onSurface)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sell button
                    Button(
                        onClick = { showSellSheet = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Expense)
                    ) {
                        Text("卖出", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Sell sheet
                if (showSellSheet) {
                    SellSheet(
                        quantity = sellQuantity,
                        onQuantityChange = { sellQuantity = it },
                        price = sellPrice,
                        onPriceChange = { sellPrice = it },
                        currentPrice = holding.currentPrice,
                        maxQuantity = holding.quantity,
                        onSell = {
                            val qty = sellQuantity.toDoubleOrNull()
                            val prc = sellPrice.toDoubleOrNull()
                            if (qty != null && prc != null && qty > 0 && qty <= holding.quantity) {
                                viewModel.sell(qty, prc)
                                showSellSheet = false
                                sellQuantity = ""
                                sellPrice = ""
                            }
                        },
                        onDismiss = {
                            showSellSheet = false
                            sellQuantity = ""
                            sellPrice = ""
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.bodyMedium, color = color, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}

@Composable
fun SellSheet(
    quantity: String,
    onQuantityChange: (String) -> Unit,
    price: String,
    onPriceChange: (String) -> Unit,
    currentPrice: Double,
    maxQuantity: Double,
    onSell: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("卖出", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = quantity,
                onValueChange = onQuantityChange,
                label = { Text("卖出数量 (最多${maxQuantity})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("卖出价格 (当前价 ¥${String.format("%.2f", currentPrice)})") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onSell,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Expense)
                ) {
                    Text("确认卖出")
                }
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text("取消", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

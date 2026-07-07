package com.expensetracker.app.ui.screen

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind
import com.expensetracker.app.ui.component.EmptyState
import com.expensetracker.app.ui.component.LoadingSkeletonRow
import com.expensetracker.app.ui.component.SectionHeader
import com.expensetracker.app.ui.component.TransactionRow
import com.expensetracker.app.ui.component.formatAmount
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.ExpenseLight
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.IncomeLight
import com.expensetracker.app.ui.theme.TextSecondary
import com.expensetracker.app.ui.viewmodel.TransactionsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val categoryIconMap = mapOf(
    "餐饮" to Icons.Default.Fastfood,
    "购物" to Icons.Default.ShoppingCart,
    "交通" to Icons.Default.DirectionsCar,
    "居住" to Icons.Default.Home,
    "医疗" to Icons.Default.LocalHospital,
    "教育" to Icons.Default.School,
    "旅行" to Icons.Default.Flight,
    "娱乐" to Icons.Default.SportsEsports,
    "宠物" to Icons.Default.Pets,
    "礼物" to Icons.Default.CardGiftcard
)

fun getCategoryIcon(categoryName: String): ImageVector {
    return categoryIconMap.entries.firstOrNull { categoryName.contains(it.key) }?.value
        ?: Icons.Default.MoreHoriz
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    onTransactionClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var searchQuery by remember { mutableStateOf("") }

    val pullToRefreshState = rememberPullToRefreshState()
    if (pullToRefreshState.isRefreshing) {
        LaunchedEffect(true) {
            viewModel.loadAll()
            pullToRefreshState.endRefresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "明细",
                        fontWeight = FontWeight.SemiBold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "添加",
                    tint = MaterialTheme.colorScheme.surface
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(pullToRefreshState.nestedScrollConnection)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        viewModel.search(it)
                    },
                    placeholder = { Text("搜索交易记录", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "搜索", tint = TextSecondary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = {
                                searchQuery = ""
                                viewModel.search("")
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "清除")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                if (uiState.isLoading) {
                    Column {
                        repeat(8) { LoadingSkeletonRow() }
                    }
                } else if (uiState.filteredTransactions.isEmpty()) {
                    EmptyState(
                        icon = Icons.Default.AccountBalance,
                        title = "暂无交易记录",
                        subtitle = "点击右下角+号添加第一笔记录",
                        actionLabel = "添加交易",
                        onAction = onAddClick
                    )
                } else {
                    // Monthly summary card
                    val transactions = uiState.filteredTransactions
                    val totalIncome = transactions.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amount }
                    val totalExpense = transactions.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.amount }
                    val balance = totalIncome - totalExpense

                    MonthlySummaryCard(
                        income = totalIncome,
                        expense = totalExpense,
                        balance = balance,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    // Grouped transactions
                    val grouped = transactions.groupBy { it.monthKey }
                    val sortedMonths = grouped.keys.sortedDescending()

                    LazyColumn {
                        sortedMonths.forEach { monthKey ->
                            val monthTransactions = grouped[monthKey] ?: emptyList()
                            val monthIncome = monthTransactions.filter { it.kind == TransactionKind.INCOME }.sumOf { it.amount }
                            val monthExpense = monthTransactions.filter { it.kind == TransactionKind.EXPENSE }.sumOf { it.amount }

                            item(key = "header_$monthKey") {
                                SectionHeader(
                                    title = formatMonthHeader(monthKey),
                                    incomeTotal = formatAmount(monthIncome),
                                    expenseTotal = formatAmount(monthExpense)
                                )
                            }

                            items(
                                items = monthTransactions,
                                key = { "tx_${it.id}" }
                            ) { transaction ->
                                val categoryName = "分类" // Would come from category lookup
                                val accountName = "账户" // Would come from account lookup
                                TransactionRow(
                                    transaction = transaction,
                                    categoryIcon = getCategoryIcon(categoryName),
                                    categoryName = categoryName,
                                    accountName = accountName,
                                    onTransactionClick = { onTransactionClick(transaction.id) }
                                )
                            }
                        }
                    }
                }
            }
            PullToRefreshContainer(
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}

@Composable
fun MonthlySummaryCard(
    income: Double,
    expense: Double,
    balance: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "本月结余",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatAmount(balance),
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 32.sp),
                color = if (balance >= 0) Income else Expense,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MiniAmountBlock(
                    label = "收入",
                    amount = income,
                    bgColor = IncomeLight,
                    textColor = Income,
                    modifier = Modifier.weight(1f)
                )
                MiniAmountBlock(
                    label = "支出",
                    amount = expense,
                    bgColor = ExpenseLight,
                    textColor = Expense,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun MiniAmountBlock(
    label: String,
    amount: Double,
    bgColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bgColor),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = textColor.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatAmount(amount),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}

fun formatMonthHeader(monthKey: String): String {
    val parts = monthKey.split("-")
    if (parts.size == 2) {
        return "${parts[0]}年${parts[1]}月"
    }
    return monthKey
}

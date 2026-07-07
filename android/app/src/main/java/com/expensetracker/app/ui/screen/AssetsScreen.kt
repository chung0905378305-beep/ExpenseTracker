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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.AccountRole
import com.expensetracker.app.domain.model.Holding
import com.expensetracker.app.domain.model.HoldingType
import com.expensetracker.app.ui.component.EmptyState
import com.expensetracker.app.ui.component.LoadingSkeletonCard
import com.expensetracker.app.ui.component.formatAmount
import com.expensetracker.app.ui.theme.Accent
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.TextSecondary
import com.expensetracker.app.ui.theme.TextOnDark
import com.expensetracker.app.ui.viewmodel.AssetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetsScreen(
    onHoldingClick: (Long) -> Unit,
    viewModel: AssetsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("资产", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshQuotes() },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新行情",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* add holding/account */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "添加", tint = MaterialTheme.colorScheme.surface)
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Column(modifier = Modifier.padding(padding)) {
                repeat(5) { LoadingSkeletonCard() }
            }
        } else if (uiState.accounts.isEmpty() && uiState.holdings.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    icon = Icons.Default.AccountBalance,
                    title = "暂无资产数据",
                    subtitle = "添加账户和持仓开始追踪你的净资产"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 80.dp)
            ) {
                // Net worth hero card
                item(key = "networth") {
                    NetWorthHeroCard(
                        netWorth = uiState.netWorth,
                        totalAssets = uiState.totalAssets,
                        totalLiabilities = uiState.totalLiabilities,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // Investment accounts
                val investmentAccounts = uiState.accounts.filter { it.role == AccountRole.ASSET && it.subtype == "investment" }
                if (investmentAccounts.isNotEmpty()) {
                    item(key = "invest_header") {
                        SectionTitle("投资账户", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    items(investmentAccounts, key = { "invest_${it.id}" }) { account ->
                        InvestmentAccountCard(
                            account = account,
                            holdings = uiState.holdings.filter { it.accountId == account.id },
                            onHoldingClick = onHoldingClick,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Cash accounts
                val cashAccounts = uiState.accounts.filter { it.role == AccountRole.ASSET && it.subtype != "investment" }
                if (cashAccounts.isNotEmpty()) {
                    item(key = "cash_header") {
                        SectionTitle("现金账户", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    items(cashAccounts, key = { "cash_${it.id}" }) { account ->
                        CashAccountCard(
                            account = account,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Physical assets (non-investment holdings)
                val physicalHoldings = uiState.holdings.filter {
                    it.type in listOf(HoldingType.GOLD, HoldingType.REAL_ESTATE)
                }
                if (physicalHoldings.isNotEmpty()) {
                    item(key = "physical_header") {
                        SectionTitle("实物资产", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    items(physicalHoldings, key = { "phys_${it.id}" }) { holding ->
                        PhysicalAssetCard(
                            holding = holding,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }

                // Liabilities
                val liabilityAccounts = uiState.accounts.filter { it.role == AccountRole.LIABILITY }
                if (liabilityAccounts.isNotEmpty()) {
                    item(key = "liability_header") {
                        SectionTitle("负债", Modifier.padding(horizontal = 16.dp, vertical = 6.dp))
                    }
                    items(liabilityAccounts, key = { "liab_${it.id}" }) { account ->
                        LiabilityCard(
                            account = account,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NetWorthHeroCard(
    netWorth: Double,
    totalAssets: Double,
    totalLiabilities: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Accent, Color(0xFF6366F1)),
                        startX = 0f,
                        endX = 1000f
                    ),
                    RoundedCornerShape(16.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Text("净资产", style = MaterialTheme.typography.bodySmall, color = TextOnDark.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatAmount(netWorth),
                    style = MaterialTheme.typography.headlineLarge.copy(fontSize = 36.sp),
                    color = TextOnDark,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("总资产", style = MaterialTheme.typography.labelSmall, color = TextOnDark.copy(alpha = 0.7f))
                        Text(formatAmount(totalAssets), style = MaterialTheme.typography.titleSmall, color = TextOnDark)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("总负债", style = MaterialTheme.typography.labelSmall, color = TextOnDark.copy(alpha = 0.7f))
                        Text(formatAmount(totalLiabilities), style = MaterialTheme.typography.titleSmall, color = TextOnDark)
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(bottom = 2.dp)
    )
}

@Composable
fun InvestmentAccountCard(
    account: Account,
    holdings: List<Holding>,
    onHoldingClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMarketValue = holdings.sumOf { it.quantity * it.currentPrice }
    val totalCost = holdings.sumOf { it.quantity * it.costPrice }
    val totalPnl = totalMarketValue - totalCost
    val pnlPercent = if (totalCost > 0) (totalPnl / totalCost * 100) else 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(account.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("总市值", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(formatAmount(totalMarketValue), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("成本", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    Text(formatAmount(totalCost), style = MaterialTheme.typography.bodyMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("盈亏", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    val pnlColor = if (totalPnl >= 0) Income else Expense
                    val pnlText = "${if (totalPnl >= 0) "+" else ""}${String.format("%.2f", pnlPercent)}%"
                    Text(pnlText, style = MaterialTheme.typography.bodyMedium, color = pnlColor, fontWeight = FontWeight.Medium)
                }
            }

            if (holdings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                holdings.forEach { holding ->
                    HoldingSummaryRow(
                        holding = holding,
                        onClick = { onHoldingClick(holding.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun HoldingSummaryRow(
    holding: Holding,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val marketValue = holding.quantity * holding.currentPrice
    val costValue = holding.quantity * holding.costPrice
    val pnl = marketValue - costValue
    val pnlPercent = if (costValue > 0) (pnl / costValue * 100) else 0.0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = when (holding.type) {
            HoldingType.STOCK -> Icons.Default.ShowChart
            HoldingType.FUND -> Icons.Default.AccountBalance
            HoldingType.CRYPTO -> Icons.Default.CurrencyBitcoin
            else -> Icons.Default.AttachMoney
        }
        Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(holding.name, style = MaterialTheme.typography.bodyMedium, fontSize = 14.sp)
            Text(holding.symbol, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(formatAmount(marketValue), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
            val pnlColor = if (pnl >= 0) Income else Expense
            val pnlSign = if (pnl >= 0) "+" else ""
            Text("${pnlSign}${String.format("%.2f", pnlPercent)}%", style = MaterialTheme.typography.labelSmall, color = pnlColor)
        }
    }
}

@Composable
fun CashAccountCard(
    account: Account,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(account.name, style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                text = formatAmount(account.initialBalance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PhysicalAssetCard(
    holding: Holding,
    modifier: Modifier = Modifier
) {
    val value = holding.quantity * holding.currentPrice

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Home, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(holding.name, style = MaterialTheme.typography.bodyLarge)
                    Text(holding.type.displayName, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                }
            }
            Text(
                text = formatAmount(value),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun LiabilityCard(
    account: Account,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(account.name, style = MaterialTheme.typography.bodyLarge)
                    if (account.statementDay != null && account.dueDay != null) {
                        Text(
                            "账单日${account.statementDay}日 / 还款日${account.dueDay}日",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
            Text(
                text = formatAmount(account.initialBalance),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Expense
            )
        }
    }
}

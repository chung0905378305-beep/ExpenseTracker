package com.expensetracker.app.ui.screen

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.TransactionKind
import com.expensetracker.app.ui.component.AmountText
import com.expensetracker.app.ui.component.CategoryGrid
import com.expensetracker.app.ui.component.CategoryItem
import com.expensetracker.app.ui.component.NumberKeyboard
import com.expensetracker.app.ui.component.formatAmount
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.TextSecondary
import com.expensetracker.app.ui.viewmodel.AddTransactionViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionScreen(
    transactionId: Long? = null,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
    viewModel: AddTransactionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    LaunchedEffect(transactionId) {
        if (transactionId != null) {
            viewModel.loadForEdit(transactionId)
        } else {
            viewModel.reset()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Header
        Text(
            text = if (transactionId != null) "编辑交易" else "添加交易",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        // Kind segmented control
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                TransactionKind.EXPENSE to "支出",
                TransactionKind.INCOME to "收入",
                TransactionKind.TRANSFER to "转账",
                TransactionKind.REFUND to "退款"
            ).forEachIndexed { index, (kind, label) ->
                SegmentedButton(
                    selected = uiState.kind == kind,
                    onClick = { viewModel.updateKind(kind) },
                    shape = SegmentedButtonDefaults.itemShape(index, 4),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = when (kind) {
                            TransactionKind.EXPENSE -> Expense
                            TransactionKind.INCOME -> Income
                            else -> MaterialTheme.colorScheme.primary
                        }
                    )
                ) {
                    Text(text = label, fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Amount display
        AmountText(
            amount = uiState.amount,
            kind = uiState.kind,
            large = true,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category grid
        Text("选择分类", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        val categoryItems = uiState.availableCategories.map { category ->
            CategoryItem(
                category = category,
                icon = getCategoryIcon(category.name),
                isSelected = category.id == uiState.selectedCategory?.id
            )
        }
        CategoryGrid(
            categories = categoryItems,
            onCategorySelected = { viewModel.updateCategory(it) },
            columns = 5
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Account picker
        AccountPickerRow(
            selectedAccount = uiState.selectedAccount,
            toAccount = uiState.toAccount,
            accounts = uiState.availableAccounts,
            showToAccount = uiState.kind == TransactionKind.TRANSFER,
            onAccountSelected = { viewModel.updateAccount(it) },
            onToAccountSelected = { viewModel.updateToAccount(it) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Date picker
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(end = 8.dp))
            TextButton(onClick = { showDatePicker = true }) {
                val dateFormat = SimpleDateFormat("yyyy年M月d日", Locale.CHINA)
                Text(
                    text = dateFormat.format(Date(uiState.date)),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Note input
        OutlinedTextField(
            value = uiState.note,
            onValueChange = { viewModel.updateNote(it) },
            placeholder = { Text("添加备注", color = TextSecondary) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Tag chips
        if (uiState.availableTags.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                uiState.availableTags.forEach { tag ->
                    FilterChip(
                        selected = uiState.selectedTags.any { it.id == tag.id },
                        onClick = { viewModel.toggleTag(tag) },
                        label = { Text(tag.name, fontSize = 12.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Save button
        Button(
            onClick = {
                if (transactionId != null) {
                    viewModel.saveEdit(transactionId)
                } else {
                    viewModel.save()
                }
                onSaved()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Expense)
        ) {
            Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = uiState.date)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDate(it) }
                    showDatePicker = false
                }) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("取消")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun AccountPickerRow(
    selectedAccount: com.expensetracker.app.domain.model.Account?,
    toAccount: com.expensetracker.app.domain.model.Account?,
    accounts: List<com.expensetracker.app.domain.model.Account>,
    showToAccount: Boolean,
    onAccountSelected: (com.expensetracker.app.domain.model.Account) -> Unit,
    onToAccountSelected: (com.expensetracker.app.domain.model.Account) -> Unit
) {
    var showAccountMenu by remember { mutableStateOf(false) }
    var showToAccountMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(
            onClick = { showAccountMenu = true },
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.AccountBalance, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = selectedAccount?.name ?: "选择账户",
                color = if (selectedAccount != null) MaterialTheme.colorScheme.onSurface else TextSecondary
            )
        }

        if (showToAccount) {
            Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = TextSecondary)
            TextButton(
                onClick = { showToAccountMenu = true },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = toAccount?.name ?: "目标账户",
                    color = if (toAccount != null) MaterialTheme.colorScheme.onSurface else TextSecondary
                )
            }
        }
    }

    // Simple account selection - in a real app this would be a DropdownMenu
    // For now listing the accounts inline for selection
    if (showAccountMenu) {
        Column {
            accounts.take(5).forEach { account ->
                TextButton(onClick = {
                    onAccountSelected(account)
                    showAccountMenu = false
                }) {
                    Text(account.name)
                }
            }
        }
    }
    if (showToAccountMenu) {
        Column {
            accounts.take(5).forEach { account ->
                TextButton(onClick = {
                    onToAccountSelected(account)
                    showToAccountMenu = false
                }) {
                    Text(account.name)
                }
            }
        }
    }
}

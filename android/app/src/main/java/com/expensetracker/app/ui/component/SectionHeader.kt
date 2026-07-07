package com.expensetracker.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.TextSecondary

@Composable
fun SectionHeader(
    title: String,
    incomeTotal: String? = null,
    expenseTotal: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (incomeTotal != null || expenseTotal != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (incomeTotal != null) {
                    Text(
                        text = "收入 $incomeTotal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Income
                    )
                }
                if (incomeTotal != null && expenseTotal != null) {
                    Spacer(modifier = Modifier.width(12.dp))
                }
                if (expenseTotal != null) {
                    Text(
                        text = "支出 $expenseTotal",
                        style = MaterialTheme.typography.bodySmall,
                        color = Expense
                    )
                }
            }
        }
    }
}

package com.expensetracker.app.ui.component

import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.expensetracker.app.domain.model.TransactionKind
import com.expensetracker.app.ui.theme.AmountLargeStyle
import com.expensetracker.app.ui.theme.AmountStyle
import com.expensetracker.app.ui.theme.Expense
import com.expensetracker.app.ui.theme.Income
import com.expensetracker.app.ui.theme.TextPrimary
import com.expensetracker.app.ui.theme.TextSecondary

@Composable
fun AmountText(
    amount: Double,
    kind: TransactionKind? = null,
    isHidden: Boolean = false,
    modifier: Modifier = Modifier,
    large: Boolean = false,
    showPrefix: Boolean = true,
    textAlign: TextAlign? = null
) {
    val color = when (kind) {
        TransactionKind.INCOME -> Income
        TransactionKind.EXPENSE -> Expense
        TransactionKind.TRANSFER -> TextSecondary
        TransactionKind.REFUND -> Expense
        else -> TextPrimary
    }

    val style = if (large) AmountLargeStyle else AmountStyle

    val text = if (isHidden) {
        "***"
    } else {
        val prefix = when {
            !showPrefix -> ""
            kind == TransactionKind.INCOME -> "+"
            kind == TransactionKind.EXPENSE -> "-"
            kind == TransactionKind.REFUND -> "+"
            else -> ""
        }
        val formatted = formatAmount(amount)
        "$prefix$formatted"
    }

    BasicText(
        text = text,
        style = style.copy(
            color = if (isHidden) TextSecondary else color,
            textAlign = textAlign
        ),
        modifier = modifier
    )
}

fun formatAmount(amount: Double): String {
    val wholePart = amount.toLong()
    val decimalPart = ((amount - wholePart) * 100).toLong()
    val formattedWhole = String.format("%,d", wholePart)
    return "¥$formattedWhole.${String.format("%02d", decimalPart)}"
}

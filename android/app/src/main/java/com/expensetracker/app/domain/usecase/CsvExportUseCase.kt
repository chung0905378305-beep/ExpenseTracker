package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind
import com.expensetracker.app.domain.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class CsvExportUseCase @Inject constructor(
    private val txRepo: TransactionRepository
) {
    suspend fun execute(): String = withContext(Dispatchers.IO) {
        val transactions = txRepo.getAll()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        val builder = StringBuilder()

        builder.appendLine("类型,金额,分类,账户,转入账户,日期,备注,标签,已审核,来源")

        for (tx in transactions) {
            val kindLabel = when (tx.kind) {
                TransactionKind.INCOME -> "收入"
                TransactionKind.EXPENSE -> "支出"
                TransactionKind.TRANSFER -> "转账"
            }
            val amount = tx.amount
            val category = tx.categoryName ?: ""
            val account = tx.accountName ?: ""
            val transferToAccount = tx.transferToAccountName ?: ""
            val date = dateFormat.format(tx.date)
            val note = escapeCsvField(tx.note ?: "")
            val tags = escapeCsvField(tx.tags.joinToString(";"))
            val reviewed = if (tx.needsReview) "否" else "是"
            val source = tx.source ?: "手动"

            builder.appendLine("$kindLabel,$amount,$category,$account,$transferToAccount,$date,$note,$tags,$reviewed,$source")
        }

        builder.toString()
    }

    private fun escapeCsvField(field: String): String {
        if (field.contains(',') || field.contains('"') || field.contains('\n')) {
            return "\"${field.replace("\"", "\"\"")}\""
        }
        return field
    }
}

package com.expensetracker.app.domain.usecase

import com.expensetracker.app.domain.model.Transaction
import com.expensetracker.app.domain.model.TransactionKind
import com.expensetracker.app.domain.repository.TransactionRepository
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class CsvImportUseCase @Inject constructor(
    private val txRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository
) {
    suspend fun execute(csvContent: String, columnMapping: Map<String, Int>): Int = withContext(Dispatchers.IO) {
        val lines = csvContent.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) return@withContext 0

        val categories = categoryRepo.getAllRoot().flatMap { root ->
            listOf(root) + categoryRepo.getChildren(root.id)
        }
        val accounts = accountRepo.getAll()

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
        var importedCount = 0

        for (line in lines.drop(1)) {
            try {
                val fields = parseCsvLine(line)
                if (fields.isEmpty()) continue

                val kindStr = getField(fields, columnMapping["类型"])
                val amountStr = getField(fields, columnMapping["金额"])
                val categoryStr = getField(fields, columnMapping["分类"])
                val accountStr = getField(fields, columnMapping["账户"])
                val transferToAccountStr = getField(fields, columnMapping["转入账户"])
                val dateStr = getField(fields, columnMapping["日期"])
                val noteStr = getField(fields, columnMapping["备注"])
                val tagsStr = getField(fields, columnMapping["标签"])

                val kind = when (kindStr?.lowercase()) {
                    "收入", "income" -> TransactionKind.INCOME
                    "支出", "expense" -> TransactionKind.EXPENSE
                    "转账", "transfer" -> TransactionKind.TRANSFER
                    else -> TransactionKind.EXPENSE
                }

                val amount = amountStr?.toDoubleOrNull() ?: continue

                val categoryId = categories.find { it.name == categoryStr }?.id ?: 0L
                val accountId = accounts.find { it.name == accountStr }?.id ?: 0L
                val transferToAccountId = accounts.find { it.name == transferToAccountStr }?.id ?: 0L

                val date = dateStr?.let { dateFormat.parse(it)?.time } ?: System.currentTimeMillis()

                val tags = tagsStr?.split(";")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()

                val tx = Transaction(
                    id = 0L,
                    kind = kind,
                    amount = amount,
                    categoryId = categoryId,
                    accountId = accountId,
                    transferToAccountId = transferToAccountId,
                    date = date,
                    note = noteStr,
                    needsReview = true,
                    source = "csv_import",
                    dedupHash = DedupUseCase.computeHash(
                        "csv_import", noteStr ?: "", amount, date
                    ),
                    tags = tags
                )

                txRepo.insert(tx)
                importedCount++
            } catch (_: Exception) {
                continue
            }
        }

        importedCount
    }

    private fun getField(fields: List<String>, index: Int?): String? {
        if (index == null || index < 0 || index >= fields.size) return null
        return fields[index].trim()
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var inQuotes = false

        for (char in line) {
            if (char == '"') {
                inQuotes = !inQuotes
            } else if (char == ',' && !inQuotes) {
                result.add(current.toString().trim())
                current = StringBuilder()
            } else {
                current.append(char)
            }
        }
        result.add(current.toString().trim())
        return result
    }
}

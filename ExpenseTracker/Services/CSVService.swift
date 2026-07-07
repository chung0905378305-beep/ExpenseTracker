import Foundation
import SwiftData

/// CSV 导入/导出服务
final class CSVService {
    static let shared = CSVService()

    /// 导出交易为 CSV
    func exportTransactions(_ transactions: [Transaction], categories: [Category], accounts: [Account]) -> String {
        let catMap = Dictionary(uniqueKeysWithValues: categories.map { ($0.id, $0.name) })
        let acctMap = Dictionary(uniqueKeysWithValues: accounts.map { ($0.id, $0.name) })

        var csv = "日期,类型,金额,分类,账户,转出账户,备注,标签,来源\n"
        let df = DateFormatter()
        df.dateFormat = "yyyy-MM-dd HH:mm"

        for t in transactions {
            let catName = t.categoryID.flatMap { catMap[$0] } ?? ""
            let acctName = t.accountID.flatMap { acctMap[$0] } ?? ""
            let toAcct = t.toAccountID.flatMap { acctMap[$0] } ?? ""
            let kindName = t.kind.title
            let note = t.note.replacingOccurrences(of: "\"", with: "\"\"")
            csv += "\(df.string(from: t.date)),\"\(kindName)\",\(t.amount),\"\(catName)\",\"\(acctName)\",\"\(toAcct)\",\"\(note)\",,\(t.source)\n"
        }
        return csv
    }

    /// 导入 CSV（支持列映射）
    func importCSV(content: String, mapping: [String: String], context: ModelContext) -> (imported: Int, errors: Int) {
        let rows = parseCSV(content)
        guard !rows.isEmpty else { return (0, 1) }

        // 默认列映射：靠列顺序，也可用 header
        let headers = rows[0]
        var imported = 0, errors = 0
        let df = DateFormatter()

        for row in rows.dropFirst() {
            guard row.count >= 4 else { errors += 1; continue }
            let dateStr = row[0]
            let typeStr = row.count > 1 ? row[1] : "支出"
            let amountStr = row.count > 2 ? row[2] : "0"
            let catStr = row.count > 3 ? row[3] : "其他"
            let noteStr = row.count > 5 ? row[5] : ""

            df.dateFormat = "yyyy-MM-dd"
            let date = df.date(from: dateStr) ?? Date()
            let amount = Double(amountStr) ?? 0
            guard amount > 0 else { errors += 1; continue }

            let kind: TransactionKind = typeStr.contains("收入") ? .income : .expense

            // 匹配分类
            let cats = DataService.shared.fetchCategories()
            let matched = cats.first { $0.name == catStr }

            let txn = Transaction(kind: kind, amount: amount, categoryID: matched?.id, date: date, note: noteStr, source: "csv_import")
            context.insert(txn)
            imported += 1
        }

        try? context.save()
        return (imported, errors)
    }

    private func parseCSV(_ content: String) -> [[String]] {
        var rows: [[String]] = []
        var currentRow: [String] = []
        var currentField = ""
        var inQuotes = false

        for char in content {
            switch char {
            case "\"":
                inQuotes.toggle()
            case ",":
                if inQuotes {
                    currentField.append(char)
                } else {
                    currentRow.append(currentField.trimmingCharacters(in: .whitespaces))
                    currentField = ""
                }
            case "\n", "\r\n":
                if inQuotes {
                    currentField.append(char)
                } else {
                    currentRow.append(currentField.trimmingCharacters(in: .whitespaces))
                    if !currentRow.isEmpty {
                        rows.append(currentRow)
                    }
                    currentRow = []
                    currentField = ""
                }
            default:
                currentField.append(char)
            }
        }

        currentRow.append(currentField.trimmingCharacters(in: .whitespaces))
        if !currentRow.isEmpty { rows.append(currentRow) }

        return rows
    }
}

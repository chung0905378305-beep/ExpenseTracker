import SwiftUI
import SwiftData

struct TransactionsTab: View {
    @Query(
        filter: #Predicate<Transaction> { $0.isDeleted == false },
        sort: \Transaction.date,
        order: .reverse
    ) private var transactions: [Transaction]

    @Query(sort: \Category.sortHint) private var categories: [Category]
    @Query(sort: \Account.sortHint) private var accounts: [Account]
    @Query(sort: \Tag.name) private var tags: [Tag]

    @State private var searchText = ""
    @State private var selectedMonth: String?
    @State private var showPendingOnly = false
    @State private var selectedTransaction: Transaction?

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // 搜索条
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("搜索账单...", text: $searchText)
                        .textFieldStyle(.plain)
                    if !searchText.isEmpty {
                        Button { searchText = "" } label: {
                            Image(systemName: "xmark.circle.fill").foregroundColor(.secondary)
                        }
                    }
                }
                .padding(10)
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 12))
                .padding(.horizontal, 16)
                .padding(.top, 8)

                // 筛选栏
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 8) {
                        FilterChip("全部", isSelected: selectedMonth == nil && !showPendingOnly) {
                            selectedMonth = nil; showPendingOnly = false
                        }
                        ForEach(monthGroups, id: \.self) { month in
                            FilterChip(month, isSelected: selectedMonth == month) {
                                selectedMonth = month; showPendingOnly = false
                            }
                        }
                        let pendingCount = transactions.filter { $0.needsReview }.count
                        if pendingCount > 0 {
                            FilterChip("待确认(\(pendingCount))", isSelected: showPendingOnly) {
                                showPendingOnly = true; selectedMonth = nil
                            }
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 8)
                }

                // 月度汇总卡
                if let month = selectedMonth {
                    MonthlyHeroCard(monthKey: month)
                        .padding(.horizontal, 16)
                        .padding(.bottom, 4)
                }

                // 账单列表
                List {
                    if showPendingOnly {
                        pendingSection
                    } else if selectedMonth != nil {
                        transactionRows(filteredTransactions)
                    } else {
                        ForEach(monthGroups, id: \.self) { month in
                            Section(header: Text(month).sectionHeader()) {
                                transactionRows(forMonth: month)
                            }
                        }
                    }
                }
                .listStyle(.plain)
                .overlay {
                    if filteredTransactions.isEmpty {
                        EmptyStateView(
                            icon: "tray",
                            title: searchText.isEmpty ? "暂无账单" : "未找到匹配结果",
                            subtitle: searchText.isEmpty ? "点击右下角 + 开始记一笔" : "试试其他关键词"
                        )
                    }
                }
            }
            .navigationTitle("")
            .navigationBarHidden(true)
            .toolbar(.hidden)
        }
    }

    // MARK: - Computed

    private var monthGroups: [String] {
        let months = Set(transactions.map { $0.monthKey })
        return months.sorted(by: >)
    }

    private var filteredTransactions: [Transaction] {
        var result = transactions
        if showPendingOnly { result = result.filter { $0.needsReview } }
        else if let month = selectedMonth { result = result.filter { $0.monthKey == month } }
        if !searchText.isEmpty {
            result = result.filter { $0.note.localizedCaseInsensitiveContains(searchText) }
        }
        return Array(result.prefix(200))
    }

    private func transactionRows(_ txns: [Transaction]) -> some View {
        ForEach(txns) { txn in
            TransactionRow(transaction: txn, categories: categories, accounts: accounts)
                .contentShape(Rectangle())
                .onTapGesture { selectedTransaction = txn }
        }
    }

    private func transactionRows(forMonth month: String) -> some View {
        let monthTxns = transactions.filter { $0.monthKey == month && !$0.needsReview }
        return ForEach(monthTxns) { txn in
            TransactionRow(transaction: txn, categories: categories, accounts: accounts)
                .contentShape(Rectangle())
                .onTapGesture { selectedTransaction = txn }
        }
    }

    private var pendingSection: some View {
        let pending = transactions.filter { $0.needsReview }
        return ForEach(pending) { txn in
            TransactionRow(transaction: txn, categories: categories, accounts: accounts)
                .contentShape(Rectangle())
                .onTapGesture { selectedTransaction = txn }
        }
    }
}

// MARK: - MonthlyHeroCard
struct MonthlyHeroCard: View {
    let monthKey: String
    @Query private var transactions: [Transaction]

    init(monthKey: String) {
        self.monthKey = monthKey
        _transactions = Query(
            filter: #Predicate { $0.isDeleted == false },
            sort: \Transaction.date
        )
    }

    var body: some View {
        let monthTxns = transactions.filter { $0.monthKey == monthKey }
        let expense = monthTxns.filter { $0.kind == .expense }.reduce(0.0) { $0 + $1.amount }
        let income = monthTxns.filter { $0.kind == .income }.reduce(0.0) { $0 + $1.amount }
        let balance = income - expense

        HStack(spacing: 0) {
            VStack(alignment: .leading, spacing: 4) {
                Text("本月结余")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(FormatHelper.currencyWithSign(balance))
                    .font(.title2.weight(.bold))
                    .foregroundColor(balance >= 0 ? .incomeGreen : .expenseRed)
            }
            Spacer()
            HStack(spacing: 20) {
                VStack(alignment: .trailing, spacing: 2) {
                    Text("收入").font(.caption2).foregroundColor(.secondary)
                    Text(FormatHelper.currency(income))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.incomeGreen)
                }
                VStack(alignment: .trailing, spacing: 2) {
                    Text("支出").font(.caption2).foregroundColor(.secondary)
                    Text(FormatHelper.currency(expense))
                        .font(.subheadline.weight(.semibold))
                        .foregroundColor(.expenseRed)
                }
            }
        }
        .padding(16)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - TransactionRow
struct TransactionRow: View {
    let transaction: Transaction
    let categories: [Category]
    let accounts: [Account]

    @EnvironmentObject var appState: AppState

    private var category: Category? {
        categories.first { $0.id == transaction.categoryID }
    }

    private var account: Account? {
        accounts.first { $0.id == transaction.accountID }
    }

    var body: some View {
        HStack(spacing: 12) {
            // 分类图标
            Image(systemName: category?.icon ?? "questionmark.circle")
                .font(.title3)
                .frame(width: 40, height: 40)
                .background(.blue.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 12))

            // 主信息
            VStack(alignment: .leading, spacing: 3) {
                Text(category?.name ?? "未分类")
                    .font(.body)
                HStack(spacing: 6) {
                    if !transaction.note.isEmpty {
                        Text(transaction.note)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    if let acct = account {
                        Text("· \(acct.name)")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }

            Spacer()

            // 金额
            VStack(alignment: .trailing, spacing: 2) {
                let sign = transaction.kind == .income ? "+" : "-"
                Text("\(sign)\(appState.hideAmount ? "****" : FormatHelper.currency(transaction.amount))")
                    .font(.body.weight(.semibold))
                    .foregroundColor(transaction.kind == .income ? .incomeGreen : .expenseRed)
                Text(FormatHelper.day(transaction.date))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            if transaction.needsReview {
                Image(systemName: "exclamationmark.circle.fill")
                    .font(.caption)
                    .foregroundColor(.orange)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - FilterChip
struct FilterChip: View {
    let label: String
    let isSelected: Bool
    let action: () -> Void

    init(_ label: String, isSelected: Bool, action: @escaping () -> Void) {
        self.label = label
        self.isSelected = isSelected
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text(label)
                .font(.caption.weight(isSelected ? .semibold : .regular))
                .padding(.horizontal, 12)
                .padding(.vertical, 6)
                .background(isSelected ? .blue : .gray.opacity(0.1))
                .foregroundColor(isSelected ? .white : .primary)
                .clipShape(Capsule())
        }
    }
}

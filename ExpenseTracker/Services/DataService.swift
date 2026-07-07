import Foundation
import SwiftData

/// 统一数据服务 — 提供所有模型的标准 CRUD 操作
@MainActor
final class DataService {
    static let shared = DataService()

    private var context: ModelContext {
        ExpenseTrackerApp().container.mainContext
    }

    // MARK: - Transactions
    func fetchTransactions(
        monthKey: String? = nil,
        accountID: UUID? = nil,
        categoryID: UUID? = nil,
        needsReview: Bool? = nil,
        searchText: String = "",
        limit: Int = 500
    ) -> [Transaction] {
        var fd = FetchDescriptor<Transaction>(
            predicate: #Predicate { $0.isDeleted == false },
            sortBy: [SortDescriptor(\.date, order: .reverse)]
        )
        fd.fetchLimit = limit
        guard let result = try? context.fetch(fd) else { return [] }
        var filtered = result
        if let mk = monthKey { filtered = filtered.filter { $0.monthKey == mk } }
        if let aid = accountID { filtered = filtered.filter { $0.accountID == aid } }
        if let cid = categoryID { filtered = filtered.filter { $0.categoryID == cid } }
        if let nr = needsReview { filtered = filtered.filter { $0.needsReview == nr } }
        if !searchText.isEmpty {
            filtered = filtered.filter { $0.note.localizedCaseInsensitiveContains(searchText) }
        }
        return filtered
    }

    func monthlySummary(monthKey: String) -> (expense: Double, income: Double) {
        let txns = fetchTransactions(monthKey: monthKey)
        let expense = txns.filter { $0.kind == .expense }.reduce(0) { $0 + $1.amount }
        let income = txns.filter { $0.kind == .income }.reduce(0) { $0 + $1.amount }
        return (expense, income)
    }

    // MARK: - Categories
    func fetchCategories(parentID: UUID? = nil) -> [Category] {
        var fd = FetchDescriptor<Category>(
            predicate: #Predicate { $0.isDeleted == false },
            sortBy: [SortDescriptor(\.sortHint)]
        )
        let all = (try? context.fetch(fd)) ?? []
        return all.filter { $0.parentID == parentID }
            .sorted { $0.useCount > $1.useCount }
    }

    // MARK: - Accounts
    func fetchAccounts(includeArchived: Bool = false) -> [Account] {
        var fd = FetchDescriptor<Account>(
            predicate: #Predicate { $0.isDeleted == false },
            sortBy: [SortDescriptor(\.sortHint)]
        )
        let all = (try? context.fetch(fd)) ?? []
        return includeArchived ? all : all.filter { !$0.archived }
    }

    /// 计算账户当前余额 = 期初 + 交易净额
    func accountBalance(_ account: Account) -> Double {
        let txns = fetchTransactions()
        var balance = account.initialBalance
        for t in txns {
            if t.accountID == account.id, t.affectsAsset {
                switch t.kind {
                case .expense: balance -= t.amount
                case .income: balance += t.amount
                case .transfer: balance -= t.amount
                case .refund: balance += t.amount
                }
            }
            if t.toAccountID == account.id, t.kind == .transfer, t.affectsAsset {
                balance += t.amount
            }
        }
        return balance
    }

    // MARK: - Tags
    func fetchTags() -> [Tag] {
        var fd = FetchDescriptor<Tag>(
            predicate: #Predicate { $0.isDeleted == false },
            sortBy: [SortDescriptor(\.name)]
        )
        return (try? context.fetch(fd)) ?? []
    }

    // MARK: - Budget
    func fetchBudgets(monthKey: String) -> [Budget] {
        var fd = FetchDescriptor<Budget>(sortBy: [SortDescriptor(\.createdAt)])
        return (try? context.fetch(fd))?.filter { $0.monthKey == monthKey } ?? []
    }

    func budgetProgress(_ budget: Budget) -> (spent: Double, percent: Double) {
        let txn = fetchTransactions(monthKey: budget.monthKey)
        let spent: Double
        if let cid = budget.categoryID {
            spent = txn.filter { $0.kind == .expense && $0.categoryID == cid }.reduce(0) { $0 + $1.amount }
        } else {
            spent = txn.filter { $0.kind == .expense }.reduce(0) { $0 + $1.amount }
        }
        return (spent, budget.limitAmount > 0 ? spent / budget.limitAmount : 0)
    }

    // MARK: - Subscriptions
    func fetchSubscriptions() -> [Subscription] {
        var fd = FetchDescriptor<Subscription>(sortBy: [SortDescriptor(\.nextBillingDate)])
        return (try? context.fetch(fd)) ?? []
    }

    // MARK: - Holdings
    func fetchHoldings(accountID: UUID? = nil) -> [Holding] {
        var fd = FetchDescriptor<Holding>(
            predicate: #Predicate { $0.isDeleted == false }
        )
        let all = (try? context.fetch(fd)) ?? []
        if let aid = accountID { return all.filter { $0.accountID == aid } }
        return all
    }

    // MARK: - NetWorth
    func netWorth() -> (assets: Double, liabilities: Double, net: Double) {
        let accounts = fetchAccounts()
        var assets = 0.0, liabilities = 0.0
        for a in accounts {
            if a.role == .asset {
                if a.subtype.isInvestment {
                    assets += fetchHoldings(accountID: a.id).reduce(0) { $0 + $1.marketValue }
                } else {
                    assets += accountBalance(a)
                }
            } else {
                if a.subtype == .creditCard {
                    liabilities += accountBalance(a)
                } else {
                    liabilities += accountBalance(a)
                }
            }
        }
        return (assets, liabilities, assets - liabilities)
    }
}

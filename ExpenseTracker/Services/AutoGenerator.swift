import Foundation
import SwiftData
import BackgroundTasks

/// 自动生成引擎 — 统一处理 循环记账 + 订阅自动扣款 + 每日净资产快照
@MainActor
final class AutoGenerator {
    static let shared = AutoGenerator()

    /// 前台扫描（每次 App 进入前台调用）
    func scan(context: ModelContext) async {
        await generateRecurring(context: context)
        await generateSubscriptions(context: context)
        await recordNetWorthSnapshot(context: context)
    }

    /// 循环记账自动生成
    private func generateRecurring(context: ModelContext) async {
        let fd = FetchDescriptor<RecurringRule>(
            predicate: #Predicate { $0.active == true }
        )
        guard let rules = try? context.fetch(fd) else { return }
        let now = Date()

        for rule in rules {
            guard rule.nextGenerateDate <= now else { continue }

            // 生成交易实例
            let txn = Transaction(
                kind: rule.kind,
                amount: rule.amount,
                categoryID: rule.categoryID,
                accountID: rule.accountID,
                date: now,
                note: rule.note,
                source: "recurring",
                fromRecurring: true,
                dedupHash: rule.dedupHash
            )

            // 去重
            let existingFd = FetchDescriptor<Transaction>(
                predicate: #Predicate { $0.dedupHash == rule.dedupHash && $0.fromRecurring == true }
            )
            if let existing = try? context.fetch(existingFd), !existing.isEmpty { continue }

            context.insert(txn)

            // 更新下次生成日
            rule.lastGeneratedDate = now
            switch rule.frequency {
            case .daily: rule.nextGenerateDate = Calendar.current.date(byAdding: .day, value: 1, to: now)!
            case .weekly: rule.nextGenerateDate = Calendar.current.date(byAdding: .weekOfYear, value: 1, to: now)!
            case .monthly: rule.nextGenerateDate = Calendar.current.date(byAdding: .month, value: 1, to: now)!
            case .yearly: rule.nextGenerateDate = Calendar.current.date(byAdding: .year, value: 1, to: now)!
            }
            rule.dedupHash = "recurring:\(rule.id):\(now.timeIntervalSince1970)"
        }
        try? context.save()
    }

    /// 订阅自动扣款生成
    private func generateSubscriptions(context: ModelContext) async {
        let fd = FetchDescriptor<Subscription>(
            predicate: #Predicate { $0.modeRaw == SubscriptionMode.autoCharge.rawValue && $0.statusRaw == SubscriptionStatus.active.rawValue }
        )
        guard let subs = try? context.fetch(fd) else { return }
        let now = Date()

        for sub in subs {
            guard sub.nextBillingDate <= now else { continue }
            let txn = Transaction(
                kind: .expense,
                amount: sub.amount,
                categoryID: sub.categoryID,
                accountID: sub.accountID,
                date: now,
                note: sub.name,
                source: "subscription",
                dedupHash: "sub:\(sub.id):\(now.timeIntervalSince1970)"
            )
            context.insert(txn)

            sub.lastGeneratedDate = now
            switch sub.period {
            case .weekly: sub.nextBillingDate = Calendar.current.date(byAdding: .weekOfYear, value: 1, to: now)!
            case .monthly: sub.nextBillingDate = Calendar.current.date(byAdding: .month, value: 1, to: now)!
            case .quarterly: sub.nextBillingDate = Calendar.current.date(byAdding: .month, value: 3, to: now)!
            case .yearly: sub.nextBillingDate = Calendar.current.date(byAdding: .year, value: 1, to: now)!
            }
        }
        try? context.save()
    }

    /// 每日净资产快照
    private func recordNetWorthSnapshot(context: ModelContext) async {
        let today = Calendar.current.startOfDay(for: Date())
        let fd = FetchDescriptor<NetWorthSnapshot>(
            predicate: #Predicate { $0.date >= today }
        )
        if let existing = try? context.fetch(fd), !existing.isEmpty { return }

        let nw = DataService.shared.netWorth()
        context.insert(NetWorthSnapshot(date: today, totalAssets: nw.assets, totalLiabilities: nw.liabilities, netWorth: nw.net))
        try? context.save()
    }

    /// 后台每日快照 (BGProcessingTask)
    func handleDailySnapshot(_ task: BGProcessingTask) async {
        // 需要 ModelContext 的共享实例
        task.setTaskCompleted(success: true)
    }
}

import Foundation
import SwiftData

enum SubscriptionMode: String, Codable, CaseIterable {
    case autoCharge = "自动扣款"
    case oneTime = "一次性"
    case manualRemind = "手动提醒"

    var description: String { rawValue }
}

enum SubscriptionPeriod: String, Codable, CaseIterable {
    case weekly = "每周"
    case monthly = "每月"
    case quarterly = "每季"
    case yearly = "每年"
}

enum SubscriptionStatus: String, Codable {
    case active = "生效中"
    case canceled = "已取消"
    case paused = "已暂停"
}

@Model
final class Subscription {
    @Attribute(.unique) var id: UUID
    var name: String
    var amount: Double
    var categoryID: UUID?
    var accountID: UUID?
    var modeRaw: String
    var periodRaw: String
    var statusRaw: String
    var nextBillingDate: Date
    var lastGeneratedDate: Date?
    var note: String
    var createdAt: Date

    var mode: SubscriptionMode {
        get { SubscriptionMode(rawValue: modeRaw) ?? .manualRemind }
        set { modeRaw = newValue.rawValue }
    }

    var period: SubscriptionPeriod {
        get { SubscriptionPeriod(rawValue: periodRaw) ?? .monthly }
        set { periodRaw = newValue.rawValue }
    }

    var status: SubscriptionStatus {
        get { SubscriptionStatus(rawValue: statusRaw) ?? .active }
        set { statusRaw = newValue.rawValue }
    }

    var daysUntilBilling: Int {
        Calendar.current.dateComponents([.day], from: Date(), to: nextBillingDate).day ?? 0
    }

    init(
        id: UUID = UUID(),
        name: String,
        amount: Double = 0,
        categoryID: UUID? = nil,
        accountID: UUID? = nil,
        mode: SubscriptionMode = .manualRemind,
        period: SubscriptionPeriod = .monthly,
        nextBillingDate: Date = Date()
    ) {
        self.id = id
        self.name = name
        self.amount = amount
        self.categoryID = categoryID
        self.accountID = accountID
        self.modeRaw = mode.rawValue
        self.periodRaw = period.rawValue
        self.statusRaw = SubscriptionStatus.active.rawValue
        self.nextBillingDate = nextBillingDate
        self.note = ""
        self.createdAt = Date()
    }
}

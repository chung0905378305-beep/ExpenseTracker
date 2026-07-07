import Foundation
import SwiftData

enum RecurringFrequency: String, Codable, CaseIterable {
    case daily = "每天"
    case weekly = "每周"
    case monthly = "每月"
    case yearly = "每年"
}

@Model
final class RecurringRule {
    @Attribute(.unique) var id: UUID
    var kindRaw: Int
    var amount: Double
    var categoryID: UUID?
    var accountID: UUID?
    var note: String
    var frequencyRaw: String
    var nextGenerateDate: Date
    var lastGeneratedDate: Date?
    var dedupHash: String?
    var active: Bool
    var createdAt: Date

    var kind: TransactionKind {
        get { TransactionKind(rawValue: kindRaw) ?? .expense }
        set { kindRaw = newValue.rawValue }
    }

    var frequency: RecurringFrequency {
        get { RecurringFrequency(rawValue: frequencyRaw) ?? .monthly }
        set { frequencyRaw = newValue.rawValue }
    }

    init(
        id: UUID = UUID(),
        kind: TransactionKind = .expense,
        amount: Double = 0,
        categoryID: UUID? = nil,
        accountID: UUID? = nil,
        note: String = "",
        frequency: RecurringFrequency = .monthly,
        nextGenerateDate: Date = Date()
    ) {
        self.id = id
        self.kindRaw = kind.rawValue
        self.amount = amount
        self.categoryID = categoryID
        self.accountID = accountID
        self.note = note
        self.frequencyRaw = frequency.rawValue
        self.nextGenerateDate = nextGenerateDate
        self.active = true
        self.createdAt = Date()
    }
}

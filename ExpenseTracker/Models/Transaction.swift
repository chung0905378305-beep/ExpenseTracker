import Foundation
import SwiftData

enum TransactionKind: Int, Codable {
    case expense = 0
    case income = 1
    case transfer = 2
    case refund = 3

    var title: String {
        switch self {
        case .expense: "支出"
        case .income: "收入"
        case .transfer: "转账"
        case .refund: "退款"
        }
    }
}

@Model
final class Transaction {
    @Attribute(.unique) var id: UUID
    var kindRaw: Int
    var amount: Double
    var categoryID: UUID?
    var accountID: UUID?
    var toAccountID: UUID?
    var date: Date
    var note: String
    var tagIDs: [UUID]
    var attachmentPaths: [String]
    var createdAt: Date
    var updatedAt: Date
    var needsReview: Bool
    var source: String
    var affectsAsset: Bool
    var fromRecurring: Bool
    var refundOfID: UUID?
    var isDeleted: Bool

    /// 去重 hash（来源+商户+金额+日期）
    var dedupHash: String?
    /// 对账用: 记账后账户快照值
    var balanceSnapshot: Double?

    var kind: TransactionKind {
        get { TransactionKind(rawValue: kindRaw) ?? .expense }
        set { kindRaw = newValue.rawValue }
    }

    var monthKey: String {
        let f = DateFormatter()
        f.dateFormat = "yyyy年M月"
        return f.string(from: date)
    }

    init(
        id: UUID = UUID(),
        kind: TransactionKind = .expense,
        amount: Double = 0,
        categoryID: UUID? = nil,
        accountID: UUID? = nil,
        toAccountID: UUID? = nil,
        date: Date = Date(),
        note: String = "",
        tagIDs: [UUID] = [],
        attachmentPaths: [String] = [],
        needsReview: Bool = false,
        source: String = "manual",
        affectsAsset: Bool = true,
        fromRecurring: Bool = false,
        refundOfID: UUID? = nil,
        dedupHash: String? = nil
    ) {
        self.id = id
        self.kindRaw = kind.rawValue
        self.amount = amount
        self.categoryID = categoryID
        self.accountID = accountID
        self.toAccountID = toAccountID
        self.date = date
        self.note = note
        self.tagIDs = tagIDs
        self.attachmentPaths = attachmentPaths
        self.createdAt = Date()
        self.updatedAt = Date()
        self.needsReview = needsReview
        self.source = source
        self.affectsAsset = affectsAsset
        self.fromRecurring = fromRecurring
        self.refundOfID = refundOfID
        self.isDeleted = false
        self.dedupHash = dedupHash
    }
}

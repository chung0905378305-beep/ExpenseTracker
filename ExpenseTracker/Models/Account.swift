import Foundation
import SwiftData

enum AccountRole: Int, Codable {
    case asset = 0
    case liability = 1
}

enum AccountSubtype: String, Codable {
    // 资产类
    case cash = "现金"
    case bank = "银行卡"
    case ewallet = "电子钱包"
    case investment = "投资账户"
    case receivable = "应收款"
    case realEstate = "房产"
    case gold = "黄金"
    case crypto = "加密资产"
    // 负债类
    case creditCard = "信用卡"
    case loan = "贷款"
    case borrow = "借入"

    var isInvestment: Bool {
        self == .investment || self == .crypto
    }
}

@Model
final class Account {
    @Attribute(.unique) var id: UUID
    var name: String
    var roleRaw: Int
    var subtypeRaw: String
    var initialBalance: Double
    var statementDay: Int?
    var dueDay: Int?
    var archived: Bool
    var isDeleted: Bool
    var sortHint: Int

    var role: AccountRole {
        get { AccountRole(rawValue: roleRaw) ?? .asset }
        set { roleRaw = newValue.rawValue }
    }

    var subtype: AccountSubtype {
        get { AccountSubtype(rawValue: subtypeRaw) ?? .cash }
        set { subtypeRaw = newValue.rawValue }
    }

    init(
        id: UUID = UUID(),
        name: String,
        role: AccountRole = .asset,
        subtype: AccountSubtype = .cash,
        initialBalance: Double = 0,
        statementDay: Int? = nil,
        dueDay: Int? = nil,
        sortHint: Int = 0
    ) {
        self.id = id
        self.name = name
        self.roleRaw = role.rawValue
        self.subtypeRaw = subtype.rawValue
        self.initialBalance = initialBalance
        self.statementDay = statementDay
        self.dueDay = dueDay
        self.archived = false
        self.isDeleted = false
        self.sortHint = sortHint
    }
}

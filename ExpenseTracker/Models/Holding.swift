import Foundation
import SwiftData

enum HoldingType: String, Codable, CaseIterable {
    case stock = "股票"
    case crypto = "加密"
    case fund = "基金"
    case gold = "黄金"
}

enum CostBasisRule: String, Codable, CaseIterable {
    case weightedAvg = "加权平均"
    case fifo = "先进先出"
    case manualLot = "手动批次"
}

@Model
final class Holding {
    @Attribute(.unique) var id: UUID
    var typeRaw: String
    var symbol: String
    var name: String
    var quantity: Double
    var costPrice: Double
    var currentPrice: Double
    var priceTime: Date?
    var accountID: UUID
    var quoteSource: String
    var costBasisRaw: String
    var realizedPnl: Double
    var isDeleted: Bool

    var type: HoldingType {
        get { HoldingType(rawValue: typeRaw) ?? .stock }
        set { typeRaw = newValue.rawValue }
    }

    var costBasis: CostBasisRule {
        get { CostBasisRule(rawValue: costBasisRaw) ?? .weightedAvg }
        set { costBasisRaw = newValue.rawValue }
    }

    var marketValue: Double { quantity * currentPrice }
    var costValue: Double { quantity * costPrice }
    var unrealizedPnl: Double { marketValue - costValue }
    var pnlPercent: Double { costValue > 0 ? (unrealizedPnl / costValue) * 100 : 0 }
    var totalPnl: Double { unrealizedPnl + realizedPnl }

    var formattedMarketValue: String { FormatHelper.currency(marketValue) }
    var formattedPnlPercent: String { String(format: "%+.2f%%", pnlPercent) }
    var isProfitable: Bool { unrealizedPnl >= 0 }

    init(
        id: UUID = UUID(),
        type: HoldingType = .stock,
        symbol: String = "",
        name: String = "",
        quantity: Double = 0,
        costPrice: Double = 0,
        accountID: UUID
    ) {
        self.id = id
        self.typeRaw = type.rawValue
        self.symbol = symbol
        self.name = name
        self.quantity = quantity
        self.costPrice = costPrice
        self.currentPrice = costPrice
        self.accountID = accountID
        self.quoteSource = ""
        self.costBasisRaw = CostBasisRule.weightedAvg.rawValue
        self.realizedPnl = 0
        self.isDeleted = false
    }
}

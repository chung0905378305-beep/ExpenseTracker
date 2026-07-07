import Foundation
import SwiftData

@Model
final class HoldingSnapshot {
    @Attribute(.unique) var id: UUID
    var holdingID: UUID
    var date: Date
    var quantity: Double
    var marketValue: Double
    var costValue: Double
    var unrealizedPnl: Double

    init(
        id: UUID = UUID(),
        holdingID: UUID,
        date: Date = Date(),
        quantity: Double = 0,
        marketValue: Double = 0,
        costValue: Double = 0,
        unrealizedPnl: Double = 0
    ) {
        self.id = id
        self.holdingID = holdingID
        self.date = date
        self.quantity = quantity
        self.marketValue = marketValue
        self.costValue = costValue
        self.unrealizedPnl = unrealizedPnl
    }
}

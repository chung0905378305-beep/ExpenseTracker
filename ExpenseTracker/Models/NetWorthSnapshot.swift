import Foundation
import SwiftData

@Model
final class NetWorthSnapshot {
    @Attribute(.unique) var id: UUID
    var date: Date
    var totalAssets: Double
    var totalLiabilities: Double
    var netWorth: Double

    init(
        id: UUID = UUID(),
        date: Date = Date(),
        totalAssets: Double = 0,
        totalLiabilities: Double = 0,
        netWorth: Double = 0
    ) {
        self.id = id
        self.date = date
        self.totalAssets = totalAssets
        self.totalLiabilities = totalLiabilities
        self.netWorth = netWorth
    }
}

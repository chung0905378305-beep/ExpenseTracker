import Foundation
import SwiftData

@Model
final class Budget {
    @Attribute(.unique) var id: UUID
    var monthKey: String // yyyy-MM
    var categoryID: UUID? // nil = 总预算
    var limitAmount: Double
    var copiedFromLast: Bool
    var createdAt: Date

    init(
        id: UUID = UUID(),
        monthKey: String,
        categoryID: UUID? = nil,
        limitAmount: Double = 0,
        copiedFromLast: Bool = false
    ) {
        self.id = id
        self.monthKey = monthKey
        self.categoryID = categoryID
        self.limitAmount = limitAmount
        self.copiedFromLast = copiedFromLast
        self.createdAt = Date()
    }
}

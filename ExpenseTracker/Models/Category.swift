import Foundation
import SwiftData

@Model
final class Category {
    @Attribute(.unique) var id: UUID
    var name: String
    var icon: String
    var parentID: UUID?
    var defaultKind: Int // 0=expense, 1=income
    var keywords: [String]
    var sortHint: Int
    var isBuiltin: Bool
    var isDeleted: Bool
    var useCount: Int

    var isSubcategory: Bool { parentID != nil }

    init(
        id: UUID = UUID(),
        name: String,
        icon: String = "questionmark.circle",
        parentID: UUID? = nil,
        defaultKind: Int = 0,
        keywords: [String] = [],
        sortHint: Int = 0,
        isBuiltin: Bool = false,
        useCount: Int = 0
    ) {
        self.id = id
        self.name = name
        self.icon = icon
        self.parentID = parentID
        self.defaultKind = defaultKind
        self.keywords = keywords
        self.sortHint = sortHint
        self.isBuiltin = isBuiltin
        self.isDeleted = false
        self.useCount = useCount
    }
}

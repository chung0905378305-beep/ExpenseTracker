import Foundation
import SwiftData

@Model
final class Tag {
    @Attribute(.unique) var id: UUID
    var name: String
    var colorHex: String
    var isDeleted: Bool

    init(
        id: UUID = UUID(),
        name: String,
        colorHex: String = "#007AFF",
        isDeleted: Bool = false
    ) {
        self.id = id
        self.name = name
        self.colorHex = colorHex
        self.isDeleted = false
    }
}

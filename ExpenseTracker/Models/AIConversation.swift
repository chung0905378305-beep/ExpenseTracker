import Foundation
import SwiftData

struct ChatMessage: Codable, Identifiable {
    var id: UUID
    var role: String // "user" | "assistant"
    var content: String
    var timestamp: Date
}

@Model
final class AIConversation {
    @Attribute(.unique) var id: UUID
    var title: String
    var messagesJSON: String // JSON-encoded [ChatMessage]
    var createdAt: Date

    var messages: [ChatMessage] {
        get {
            guard let data = messagesJSON.data(using: .utf8),
                  let msgs = try? JSONDecoder().decode([ChatMessage].self, from: data)
            else { return [] }
            return msgs
        }
        set {
            if let data = try? JSONEncoder().encode(newValue) {
                messagesJSON = String(data: data, encoding: .utf8) ?? "[]"
            }
        }
    }

    init(
        id: UUID = UUID(),
        title: String = "新对话",
        messages: [ChatMessage] = []
    ) {
        self.id = id
        self.title = title
        self.messagesJSON = "[]"
        self.createdAt = Date()
        self.messages = messages
    }
}

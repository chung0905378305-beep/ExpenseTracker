import SwiftUI
import Combine

enum AppLockMode: String, Codable, CaseIterable {
    case off = "关闭"
    case coldStart = "每次启动"
    case background = "切后台"

    var description: String { rawValue }
}

enum AppTheme: String, Codable, CaseIterable {
    case system = "跟随系统"
    case light = "浅色"
    case dark = "暗色"

    var colorScheme: ColorScheme? {
        switch self {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

final class AppState: ObservableObject {
    static let shared = AppState()

    @Published var appLockEnabled = false
    @Published var appLockMode: AppLockMode = .coldStart
    @Published var hideAmount = false
    @Published var baseCurrency = "CNY"
    @Published var theme: AppTheme = .system

    @Published var pendingCount = 0
    @Published var dueSubscriptionCount = 0

    var badgeCount: Int {
        pendingCount + dueSubscriptionCount
    }

    private init() {}
}

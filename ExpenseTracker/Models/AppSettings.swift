import Foundation
import SwiftData

@Model
final class AppSettings {
    @Attribute(.unique) var id: UUID

    var baseCurrency: String
    var includeLiabilityInNetWorth: Bool
    var linkAssetToCash: Bool

    var appLockEnabled: Bool
    var appLockModeRaw: String
    var hideAmount: Bool

    var quoteRefreshTimes: [String] // ["11:30", "15:00"]
    var language: String // "zh" | "en"
    var notifyDaysBefore: Int

    var aiAPIKey: String  // stored in Keychain in production
    var aiBaseURL: String
    var aiModel: String

    var themeRaw: String
    var costBasisRaw: String
    var guidCompleted: Bool

    var appLockMode: AppLockMode {
        get { AppLockMode(rawValue: appLockModeRaw) ?? .coldStart }
        set { appLockModeRaw = newValue.rawValue }
    }

    var theme: AppTheme {
        get { AppTheme(rawValue: themeRaw) ?? .system }
        set { themeRaw = newValue.rawValue }
    }

    static var `default`: AppSettings {
        AppSettings()
    }

    init(
        id: UUID = UUID(),
        baseCurrency: String = "CNY",
        includeLiabilityInNetWorth: Bool = true,
        linkAssetToCash: Bool = false,
        appLockEnabled: Bool = false,
        appLockMode: AppLockMode = .coldStart,
        hideAmount: Bool = false,
        quoteRefreshTimes: [String] = ["11:30", "15:00"],
        language: String = "zh",
        notifyDaysBefore: Int = 1,
        aiAPIKey: String = "",
        aiBaseURL: String = "https://api.openai.com",
        aiModel: String = "gpt-4o",
        theme: AppTheme = .system,
        costBasis: CostBasisRule = .weightedAvg,
        guidCompleted: Bool = false
    ) {
        self.id = id
        self.baseCurrency = baseCurrency
        self.includeLiabilityInNetWorth = includeLiabilityInNetWorth
        self.linkAssetToCash = linkAssetToCash
        self.appLockEnabled = appLockEnabled
        self.appLockModeRaw = appLockMode.rawValue
        self.hideAmount = hideAmount
        self.quoteRefreshTimes = quoteRefreshTimes
        self.language = language
        self.notifyDaysBefore = notifyDaysBefore
        self.aiAPIKey = aiAPIKey
        self.aiBaseURL = aiBaseURL
        self.aiModel = aiModel
        self.themeRaw = theme.rawValue
        self.costBasisRaw = costBasis.rawValue
        self.guidCompleted = guidCompleted
    }
}

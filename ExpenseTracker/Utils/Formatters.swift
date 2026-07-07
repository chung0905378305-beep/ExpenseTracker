import Foundation

/// 全局格式化工具
enum FormatHelper {
    private static let currencyFmt: NumberFormatter = {
        let f = NumberFormatter()
        f.numberStyle = .currency
        f.currencySymbol = "¥"
        f.minimumFractionDigits = 0
        f.maximumFractionDigits = 2
        return f
    }()

    static func currency(_ value: Double) -> String {
        currencyFmt.string(from: NSNumber(value: value)) ?? "¥0"
    }

    static func currencyWithSign(_ value: Double) -> String {
        let sign = value >= 0 ? "+" : ""
        return sign + currency(value)
    }

    static func percent(_ value: Double) -> String {
        String(format: "%+.2f%%", value)
    }

    private static let dayFmt: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "M月d日"
        return f
    }()

    static func day(_ date: Date) -> String {
        dayFmt.string(from: date)
    }

    private static let monthFmt: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy年M月"
        return f
    }()

    static func month(_ date: Date) -> String {
        monthFmt.string(from: date)
    }

    private static let monthKeyFmt: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM"
        return f
    }()

    static func monthKey(_ date: Date) -> String {
        monthKeyFmt.string(from: date)
    }

    static var currentMonthKey: String { monthKey(Date()) }
    static var lastMonthKey: String {
        let d = Calendar.current.date(byAdding: .month, value: -1, to: Date())!
        return monthKey(d)
    }

    static func dateFromKey(_ key: String) -> Date {
        monthKeyFmt.date(from: key) ?? Date()
    }
}

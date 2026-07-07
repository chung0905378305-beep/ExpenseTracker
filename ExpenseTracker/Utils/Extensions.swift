import Foundation
import SwiftUI

// MARK: - Date Extensions
extension Date {
    var startOfDay: Date {
        Calendar.current.startOfDay(for: self)
    }

    var startOfMonth: Date {
        Calendar.current.date(from: Calendar.current.dateComponents([.year, .month], from: self))!
    }

    var endOfMonth: Date {
        Calendar.current.date(byAdding: DateComponents(month: 1, day: -1), to: startOfMonth)!
    }

    var startOfWeek: Date {
        Calendar.current.date(from: Calendar.current.dateComponents([.yearForWeekOfYear, .weekOfYear], from: self))!
    }

    var startOfYear: Date {
        Calendar.current.date(from: Calendar.current.dateComponents([.year], from: self))!
    }

    func advanced(days: Int) -> Date {
        Calendar.current.date(byAdding: .day, value: days, to: self)!
    }

    func advanced(months: Int) -> Date {
        Calendar.current.date(byAdding: .month, value: months, to: self)!
    }
}

// MARK: - Color Extensions
extension Color {
    static let expenseRed = Color(red: 0.92, green: 0.23, blue: 0.23)
    static let incomeGreen = Color(red: 0.18, green: 0.74, blue: 0.44)
    static let profitGreen = Color(red: 0.18, green: 0.74, blue: 0.44)
    static let accentBlue = Color(red: 0.0, green: 0.48, blue: 1.0)

    static let expenseLight = Color(red: 1.0, green: 0.92, blue: 0.92)
    static let incomeLight = Color(red: 0.90, green: 1.0, blue: 0.94)
    static let profitLight = Color(red: 0.90, green: 1.0, blue: 0.94)

    var uiColor: UIColor {
        UIColor(self)
    }
}

// MARK: - Double Extensions
extension Double {
    var currencyStr: String { FormatHelper.currency(self) }
    var signedCurrency: String { FormatHelper.currencyWithSign(self) }
    var percentStr: String { FormatHelper.percent(self) }
}

// MARK: - View Extensions
extension View {
    func cardStyle(radius: CGFloat = 16, shadow: Bool = true) -> some View {
        self
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: radius))
            .shadow(color: .black.opacity(shadow ? 0.04 : 0), radius: 6, y: 1)
    }

    func sectionHeader() -> some View {
        self
            .font(.subheadline.weight(.semibold))
            .foregroundColor(.secondary)
            .textCase(nil)
    }
}

// MARK: - Color from hex
extension Color {
    init(hex: String) {
        let hex = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
        var int: UInt64 = 0
        Scanner(string: hex).scanHexInt64(&int)
        let r = Double((int >> 16) & 0xFF) / 255
        let g = Double((int >> 8) & 0xFF) / 255
        let b = Double(int & 0xFF) / 255
        self.init(red: r, green: g, blue: b)
    }
}

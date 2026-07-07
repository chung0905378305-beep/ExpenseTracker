import Foundation
import UserNotifications

/// 通知服务 — 预算超支 + 订阅到期提醒 + 还款提醒
final class NotificationService {
    static let shared = NotificationService()

    private init() {}

    func requestAuthorization() async -> Bool {
        do {
            return try await UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound])
        } catch {
            return false
        }
    }

    /// 预算超支通知
    func scheduleBudgetOverspend(categoryName: String, spent: Double, limit: Double, monthKey: String) async {
        let content = UNMutableNotificationContent()
        content.title = "预算超支提醒"
        content.body = "「\(categoryName)」已支出 \(FormatHelper.currency(spent))，超出预算 \(FormatHelper.currency(spent - limit))"
        content.sound = .default

        let request = UNNotificationRequest(identifier: "budget:\(monthKey):\(categoryName)", content: content, trigger: nil)
        try? await UNUserNotificationCenter.current().add(request)
    }

    /// 订阅即将续费
    func scheduleSubscriptionReminder(subName: String, amount: Double, billingDate: Date, daysBefore: Int = 1) async {
        let content = UNMutableNotificationContent()
        content.title = "订阅续费提醒"
        content.body = "「\(subName)」将在 \(daysBefore) 天后续费 ¥\(amount)"
        content.sound = .default

        let triggerDate = Calendar.current.date(byAdding: .day, value: -daysBefore, to: billingDate)!
        let components = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute], from: triggerDate)
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)

        let request = UNNotificationRequest(identifier: "sub:\(subName):\(billingDate.timeIntervalSince1970)", content: content, trigger: trigger)
        try? await UNUserNotificationCenter.current().add(request)
    }

    /// 信用卡还款提醒
    func scheduleCreditCardReminder(accountName: String, dueDay: Int) async {
        let content = UNMutableNotificationContent()
        content.title = "信用卡还款提醒"
        content.body = "「\(accountName)」还款日将至，请及时还款"
        content.sound = .default

        var components = DateComponents()
        components.day = dueDay
        components.hour = 9
        let trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: true)

        let request = UNNotificationRequest(identifier: "card:\(accountName)", content: content, trigger: trigger)
        try? await UNUserNotificationCenter.current().add(request)
    }

    /// 清除通知
    func removeNotifications(withPrefix prefix: String) {
        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            let ids = requests.filter { $0.identifier.hasPrefix(prefix) }.map { $0.identifier }
            UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ids)
        }
    }
}

import Foundation

/// App 常量定义
enum AppConstants {
    static let appName = "记账本"
    static let defaultCurrency = "CNY"
    static let defaultLanguage = "zh"
    static let maxAttachmentsPerTransaction = 5
    static let quoteRefreshTimes = ["11:30", "15:00"]
    static let notifyDaysBeforeDefault = 1
    static let aiReportCacheDays = 7
    static let snapshotIntervalHours = 24.0

    // CloudKit
    static let cloudKitContainerID = "iCloud.com.expensetracker.app"

    // Backend API (会员系统)
    static let backendBaseURL = "https://api.expensetracker.app"
    static let appAPIKey = "ak-change-me-in-production"

    // External APIs (endpoints configured in Settings)
    static let yahooFinanceBase = "https://query1.finance.yahoo.com"
    static let coinGeckoBase = "https://api.coingecko.com/api/v3"
    static let exchangeRateBase = "https://open.er-api.com/v6/latest"

    // Budget notification threshold (percentage)
    static let budgetWarningThreshold = 0.8
    static let budgetCriticalThreshold = 1.0
}

/// 预设分类种子数据
enum SeedCategories {
    static let builtin: [(name: String, icon: String, defaultKind: Int, children: [(String, String)])] = [
        ("餐饮", "fork.knife", 0, [("外卖", "takeoutbag.and.cup.and.straw"), ("聚餐", "person.3.fill"), ("咖啡", "cup.and.saucer.fill"), ("零食", "birthday.cake")]),
        ("交通", "car.fill", 0, [("加油", "fuelpump.fill"), ("停车", "parkingsign"), ("公交", "bus.fill"), ("打车", "figure.walk")]),
        ("购物", "bag.fill", 0, [("日用", "basket.fill"), ("服饰", "tshirt.fill"), ("数码", "desktopcomputer"), ("家居", "house.fill")]),
        ("娱乐", "gamecontroller.fill", 0, [("电影", "film.fill"), ("游戏", "gamecontroller"), ("旅行", "airplane"), ("运动", "figure.run")]),
        ("居住", "house.fill", 0, [("房租", "building.2.fill"), ("水电", "bolt.fill"), ("物业", "wrench.and.screwdriver.fill"), ("网费", "wifi")]),
        ("医疗", "heart.text.square.fill", 0, [("门诊", "stethoscope"), ("药品", "pills.fill"), ("体检", "doc.text.magnifyingglass")]),
        ("教育", "book.fill", 0, [("课程", "books.vertical.fill"), ("书籍", "text.book.closed.fill"), ("培训", "person.fill.turn.down")]),
        ("通讯", "iphone.gen3", 0, [("话费", "phone.fill"), ("快递", "shippingbox.fill")]),
        ("收入", "banknote.fill", 1, [("工资", "dollarsign.circle.fill"), ("兼职", "briefcase.fill"), ("理财", "chart.line.uptrend.xyaxis"), ("红包", "gift.fill")]),
        ("其他", "ellipsis.circle.fill", 0, []),
    ]
}

/// 预设账户种子
enum SeedAccounts {
    static let builtin: [(name: String, role: Int, subtype: String, initialBalance: Double)] = [
        ("现金", 0, "cash", 0),
        ("银行卡", 0, "bank", 0),
        ("微信钱包", 0, "ewallet", 0),
        ("支付宝", 0, "ewallet", 0),
        ("投资账户", 0, "investment", 0),
    ]
}

/// 预设标签
enum SeedTags {
    static let builtin: [(name: String, colorHex: String)] = [
        ("出差", "#FF9500"),
        ("宝宝", "#FF2D55"),
        ("家庭", "#34C759"),
        ("宠物", "#AF52DE"),
        ("礼物", "#FF3B30"),
    ]
}

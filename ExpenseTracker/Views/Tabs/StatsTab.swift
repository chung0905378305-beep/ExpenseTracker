import SwiftUI
import SwiftData

struct StatsTab: View {
    enum TimeRange: String, CaseIterable {
        case week = "周"
        case month = "本月"
        case year = "本年"
        case custom = "自定义"
    }

    @State private var selectedRange: TimeRange = .week
    @State private var customStart = Calendar.current.date(byAdding: .month, value: -1, to: Date())!
    @State private var customEnd = Date()
    @State private var showCustomPicker = false

    @Query(
        filter: #Predicate<Transaction> { $0.isDeleted == false },
        sort: \Transaction.date
    ) private var transactions: [Transaction]

    @EnvironmentObject var appState: AppState

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 12) {
                    // 时间分段
                    Picker("时间范围", selection: $selectedRange) {
                        ForEach(TimeRange.allCases, id: \.self) { range in
                            Text(range.rawValue).tag(range)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)
                    .onChange(of: selectedRange) { _, new in
                        if new == .custom { showCustomPicker = true }
                    }

                    // 自定义日期
                    if selectedRange == .custom {
                        HStack {
                            DatePicker("从", selection: $customStart, displayedComponents: .date)
                                .labelsHidden()
                            Text("至")
                            DatePicker("到", selection: $customEnd, displayedComponents: .date)
                                .labelsHidden()
                        }
                        .padding(.horizontal, 16)
                    }

                    // 订阅卡（可拖拽卡片流第一张）
                    SubscriptionSummaryCard()
                        .padding(.horizontal, 16)

                    // 月度汇总
                    MonthlySummaryCard(transactions: periodTransactions)
                        .padding(.horizontal, 16)

                    // 分类占比
                    CategoryPieCard(transactions: periodTransactions)
                        .padding(.horizontal, 16)

                    // 趋势图
                    TrendCard(transactions: transactions, selectedRange: selectedRange)
                        .padding(.horizontal, 16)

                    // 年度对比
                    YearlyComparisonCard(transactions: transactions)
                        .padding(.horizontal, 16)
                }
                .padding(.vertical, 12)
            }
            .navigationTitle("")
            .navigationBarHidden(true)
        }
    }

    private var periodTransactions: [Transaction] {
        let (start, end) = dateRange
        return transactions.filter { $0.date >= start && $0.date <= end }
    }

    private var dateRange: (Date, Date) {
        switch selectedRange {
        case .week:
            return (Date().startOfWeek, Date())
        case .month:
            return (Date().startOfMonth, Date())
        case .year:
            return (Date().startOfYear, Date())
        case .custom:
            return (customStart.startOfDay, customEnd.startOfDay.advanced(days: 1))
        }
    }
}

// MARK: - SubscriptionSummaryCard
struct SubscriptionSummaryCard: View {
    @Query(sort: \Subscription.nextBillingDate) private var subscriptions: [Subscription]

    var body: some View {
        let active = subscriptions.filter { $0.status == .active }
        guard !active.isEmpty else { return AnyView(EmptyView()) }

        return AnyView(
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "arrow.triangle.2.circlepath")
                        .font(.caption)
                    Text("我的订阅")
                        .font(.subheadline.weight(.semibold))
                    Spacer()
                    NavigationLink(destination: SubscriptionListView()) {
                        Text("查看全部")
                            .font(.caption)
                    }
                }

                ForEach(active.prefix(3)) { sub in
                    HStack {
                        Text(sub.name).font(.callout)
                        Spacer()
                        Text(FormatHelper.currency(sub.amount))
                            .font(.callout.weight(.medium))
                            .foregroundColor(.secondary)
                        Text(sub.period.rawValue)
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .padding(.horizontal, 6)
                            .padding(.vertical, 2)
                            .background(.gray.opacity(0.1))
                            .clipShape(Capsule())
                    }
                }
            }
            .padding(14)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        )
    }
}

// MARK: - MonthlySummaryCard
struct MonthlySummaryCard: View {
    let transactions: [Transaction]
    @EnvironmentObject var appState: AppState

    var body: some View {
        let income = transactions.filter { $0.kind == .income }.reduce(0.0) { $0 + $1.amount }
        let expense = transactions.filter { $0.kind == .expense }.reduce(0.0) { $0 + $1.amount }
        let balance = income - expense
        let savingRate = income > 0 ? (balance / income * 100) : 0

        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "chart.bar.doc.horizontal")
                Text("月度汇总").font(.subheadline.weight(.semibold))
            }

            HStack(spacing: 0) {
                VStack(spacing: 4) {
                    Text("收入").font(.caption).foregroundColor(.secondary)
                    Text(appState.hideAmount ? "****" : FormatHelper.currency(income))
                        .font(.title3.weight(.bold))
                        .foregroundColor(.incomeGreen)
                }
                Spacer()
                VStack(spacing: 4) {
                    Text("支出").font(.caption).foregroundColor(.secondary)
                    Text(appState.hideAmount ? "****" : FormatHelper.currency(expense))
                        .font(.title3.weight(.bold))
                        .foregroundColor(.expenseRed)
                }
                Spacer()
                VStack(spacing: 4) {
                    Text("结余").font(.caption).foregroundColor(.secondary)
                    Text(appState.hideAmount ? "****" : FormatHelper.currencyWithSign(balance))
                        .font(.title3.weight(.bold))
                        .foregroundColor(balance >= 0 ? .incomeGreen : .expenseRed)
                }
            }

            if income > 0 {
                Text("储蓄率 \(String(format: "%.1f", savingRate))%")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(14)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - CategoryPieCard
struct CategoryPieCard: View {
    let transactions: [Transaction]
    @Query(sort: \Category.sortHint) private var categories: [Category]

    var body: some View {
        let expenseTxns = transactions.filter { $0.kind == .expense }
        let total = expenseTxns.reduce(0.0) { $0 + $1.amount }
        let catMap = Dictionary(uniqueKeysWithValues: categories.map { ($0.id, $0) })

        var grouped: [(name: String, icon: String, amount: Double, percent: Double)] = []
        for t in expenseTxns {
            let cat = t.categoryID.flatMap { catMap[$0] }
            let name = cat?.name ?? "其他"
            if let idx = grouped.firstIndex(where: { $0.name == name }) {
                grouped[idx].amount += t.amount
            } else {
                grouped.append((name, cat?.icon ?? "questionmark", t.amount, 0))
            }
        }
        for i in grouped.indices { grouped[i].percent = total > 0 ? grouped[i].amount / total * 100 : 0 }
        grouped.sort { $0.amount > $1.amount }

        return AnyView(
            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Image(systemName: "chart.pie")
                    Text("分类占比").font(.subheadline.weight(.semibold))
                }

                ForEach(grouped.prefix(8), id: \.name) { item in
                    HStack(spacing: 8) {
                        Image(systemName: item.icon)
                            .font(.caption)
                            .frame(width: 24)
                        Text(item.name)
                            .font(.callout)
                        Spacer()
                        Text(FormatHelper.currency(item.amount))
                            .font(.callout.weight(.medium))
                        Text(String(format: "%.1f%%", item.percent))
                            .font(.caption)
                            .foregroundColor(.secondary)
                            .frame(width: 48, alignment: .trailing)
                    }
                }
            }
            .padding(14)
            .background(.regularMaterial)
            .clipShape(RoundedRectangle(cornerRadius: 16))
        )
    }
}

// MARK: - TrendCard
struct TrendCard: View {
    let transactions: [Transaction]
    let selectedRange: StatsTab.TimeRange
    @EnvironmentObject var appState: AppState

    var body: some View {
        let months = last6Months()
        let data = months.map { month in
            let txns = transactions.filter { $0.monthKey == month }
            let income = txns.filter { $0.kind == .income }.reduce(0.0) { $0 + $1.amount }
            let expense = txns.filter { $0.kind == .expense }.reduce(0.0) { $0 + $1.amount }
            return (month, income, expense)
        }

        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "chart.line.uptrend.xyaxis")
                Text("收支趋势").font(.subheadline.weight(.semibold))
            }

            // 简易折线图（Swift Charts 可用时替换）
            GeometryReader { geo in
                let maxVal = max(data.map { $0.1 }.max() ?? 1, data.map { $0.2 }.max() ?? 1)
                let w = geo.size.width / CGFloat(max(data.count - 1, 1))
                let h = geo.size.height

                ZStack(alignment: .topLeading) {
                    // 渐变填充
                    Path { path in
                        for (i, d) in data.enumerated() {
                            let x = CGFloat(i) * w
                            let y = h - CGFloat(d.2 / maxVal) * h * 0.9
                            if i == 0 { path.move(to: CGPoint(x: x, y: y)) }
                            else { path.addLine(to: CGPoint(x: x, y: y)) }
                        }
                    }
                    .stroke(.red.opacity(0.6), style: StrokeStyle(lineWidth: 2, lineCap: .round))

                    Path { path in
                        for (i, d) in data.enumerated() {
                            let x = CGFloat(i) * w
                            let y = h - CGFloat(d.1 / maxVal) * h * 0.9
                            if i == 0 { path.move(to: CGPoint(x: x, y: y)) }
                            else { path.addLine(to: CGPoint(x: x, y: y)) }
                        }
                    }
                    .stroke(.green.opacity(0.6), style: StrokeStyle(lineWidth: 2, lineCap: .round))
                }
            }
            .frame(height: 120)

            // 图例
            HStack(spacing: 16) {
                HStack(spacing: 4) {
                    Circle().fill(.green).frame(width: 8, height: 8)
                    Text("收入").font(.caption2)
                }
                HStack(spacing: 4) {
                    Circle().fill(.red).frame(width: 8, height: 8)
                    Text("支出").font(.caption2)
                }
            }
        }
        .padding(14)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func last6Months() -> [String] {
        let cal = Calendar.current
        var result: [String] = []
        for i in (0..<6).reversed() {
            if let d = cal.date(byAdding: .month, value: -i, to: Date()) {
                result.append(FormatHelper.monthKey(d))
            }
        }
        return result
    }
}

// MARK: - YearlyComparisonCard
struct YearlyComparisonCard: View {
    let transactions: [Transaction]
    @EnvironmentObject var appState: AppState

    var body: some View {
        let cal = Calendar.current
        let year = cal.component(.year, from: Date())
        let months = (1...12).map { m in
            let date = cal.date(from: DateComponents(year: year, month: m, day: 1))!
            return FormatHelper.monthKey(date)
        }

        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "chart.bar.fill")
                Text("\(String(year))年度汇总").font(.subheadline.weight(.semibold))
            }

            ForEach(months, id: \.self) { month in
                let txns = transactions.filter { $0.monthKey == month }
                let expense = txns.filter { $0.kind == .expense }.reduce(0.0) { $0 + $1.amount }
                let income = txns.filter { $0.kind == .income }.reduce(0.0) { $0 + $1.amount }
                let maxVal = transactions.reduce(0.0) { max($0, $1.amount) }

                VStack(alignment: .leading, spacing: 4) {
                    HStack {
                        Text(String(month.suffix(2)) + "月")
                            .font(.caption)
                            .frame(width: 28, alignment: .leading)
                        GeometryReader { geo in
                            ZStack(alignment: .leading) {
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(.gray.opacity(0.1))
                                    .frame(height: 6)
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(.blue.opacity(0.6))
                                    .frame(width: maxVal > 0 ? CGFloat(expense / maxVal) * geo.size.width * 0.8 : 0, height: 6)
                            }
                        }
                        .frame(height: 6)
                        Text(appState.hideAmount ? "***" : FormatHelper.currency(expense))
                            .font(.caption)
                            .frame(width: 60, alignment: .trailing)
                    }
                }
            }
        }
        .padding(14)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

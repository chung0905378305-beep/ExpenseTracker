import SwiftUI
import SwiftData

struct HoldingDetailView: View {
    let holding: Holding

    enum Period: String, CaseIterable {
        case week = "周", month = "月", quarter = "季", year = "年", all = "总计"
    }

    @State private var selectedPeriod: Period = .month
    @Query private var snapshots: [HoldingSnapshot]
    @EnvironmentObject var appState: AppState

    init(holding: Holding) {
        self.holding = holding
        let hid = holding.id
        _snapshots = Query(
            filter: #Predicate { $0.holdingID == hid },
            sort: \HoldingSnapshot.date
        )
    }

    var body: some View {
        ScrollView {
            VStack(spacing: 16) {
                // 头部信息
                VStack(spacing: 6) {
                    Text(holding.name.isEmpty ? holding.symbol : holding.name)
                        .font(.title2.weight(.bold))
                    Text(holding.symbol)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 8)

                // 关键数据
                HStack(spacing: 20) {
                    dataItem("现价", FormatHelper.currency(holding.currentPrice))
                    dataItem("数量", String(format: "%.4f", holding.quantity))
                    dataItem("市值", appState.hideAmount ? "****" : FormatHelper.currency(holding.marketValue))
                }

                HStack(spacing: 20) {
                    dataItem("成本", FormatHelper.currency(holding.costValue))
                    dataItem("未实现盈亏", appState.hideAmount ? "****" : FormatHelper.currencyWithSign(holding.unrealizedPnl),
                             color: holding.isProfitable ? .profitGreen : .expenseRed)
                }

                if holding.realizedPnl != 0 {
                    dataItem("已实现盈亏", FormatHelper.currencyWithSign(holding.realizedPnl),
                             color: holding.realizedPnl >= 0 ? .profitGreen : .expenseRed)
                }

                // 时间分段
                Picker("周期", selection: $selectedPeriod) {
                    ForEach(Period.allCases, id: \.self) { p in
                        Text(p.rawValue).tag(p)
                    }
                }
                .pickerStyle(.segmented)
                .padding(.horizontal, 16)

                // 走势图
                trendChart
                    .frame(height: 200)
                    .padding(.horizontal, 16)
                    .background(.regularMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 16))

                // 占比信息
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("占所属账户")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(String(format: "%.1f%%", accountPercent))
                            .font(.title3.weight(.bold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(12)
                    .background(.regularMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 12))

                    VStack(alignment: .leading, spacing: 4) {
                        Text("占总资产")
                            .font(.caption)
                            .foregroundColor(.secondary)
                        Text(String(format: "%.1f%%", totalPercent))
                            .font(.title3.weight(.bold))
                    }
                    .frame(maxWidth: .infinity)
                    .padding(12)
                    .background(.regularMaterial)
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
                .padding(.horizontal, 16)

                // 期间汇总表
                VStack(alignment: .leading, spacing: 10) {
                    Text("期间汇总").font(.subheadline.weight(.semibold))
                    summaryRow("期初市值", holding.costValue)
                    summaryRow("期末市值", holding.marketValue)
                    summaryRow("期间变动", holding.marketValue - holding.costValue)
                    summaryRow("未实现盈亏", holding.unrealizedPnl, color: holding.isProfitable ? .profitGreen : .expenseRed)
                    if holding.realizedPnl != 0 {
                        summaryRow("已实现盈亏", holding.realizedPnl, color: holding.realizedPnl >= 0 ? .profitGreen : .expenseRed)
                    }
                }
                .padding(14)
                .background(.regularMaterial)
                .clipShape(RoundedRectangle(cornerRadius: 16))
                .padding(.horizontal, 16)
            }
            .padding(.bottom, 32)
        }
        .navigationTitle("")
        .navigationBarTitleDisplayMode(.inline)
    }

    private func dataItem(_ title: String, _ value: String, color: Color? = nil) -> some View {
        VStack(spacing: 2) {
            Text(title).font(.caption2).foregroundColor(.secondary)
            Text(value).font(.callout.weight(.semibold))
                .foregroundColor(color ?? .primary)
        }
    }

    private func summaryRow(_ title: String, _ value: Double, color: Color? = nil) -> some View {
        HStack {
            Text(title).font(.callout).foregroundColor(.secondary)
            Spacer()
            Text(appState.hideAmount ? "****" : FormatHelper.currencyWithSign(value))
                .font(.callout.weight(.medium))
                .foregroundColor(color ?? .primary)
        }
    }

    private var trendChart: some View {
        let periodSnapshots = filteredSnapshots
        return GeometryReader { geo in
            if periodSnapshots.isEmpty {
                VStack {
                    Spacer()
                    Text("暂无走势数据")
                        .font(.caption)
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity)
                    Text("每日行情刷新后自动记录")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            } else {
                let maxVal = periodSnapshots.map { $0.marketValue }.max() ?? 1
                let minVal = periodSnapshots.map { $0.marketValue }.min() ?? 0
                let range = max(maxVal - minVal, 1)
                let w = geo.size.width / CGFloat(max(periodSnapshots.count - 1, 1))
                let h = geo.size.height * 0.85

                Path { path in
                    for (i, s) in periodSnapshots.enumerated() {
                        let x = CGFloat(i) * w
                        let y = h - CGFloat((s.marketValue - minVal) / range) * h
                        if i == 0 { path.move(to: CGPoint(x: x, y: y)) }
                        else { path.addLine(to: CGPoint(x: x, y: y)) }
                    }
                }
                .stroke(.blue, style: StrokeStyle(lineWidth: 2, lineCap: .round, lineJoin: .round))

                // 成本线
                Path { path in
                    for (i, s) in periodSnapshots.enumerated() {
                        let x = CGFloat(i) * w
                        let y = h - CGFloat((s.costValue - minVal) / range) * h
                        if i == 0 { path.move(to: CGPoint(x: x, y: y)) }
                        else { path.addLine(to: CGPoint(x: x, y: y)) }
                    }
                }
                .stroke(.gray, style: StrokeStyle(lineWidth: 1, dash: [4, 3]))
            }
        }
    }

    private var filteredSnapshots: [HoldingSnapshot] {
        let cutoff: Date
        switch selectedPeriod {
        case .week: cutoff = Calendar.current.date(byAdding: .weekOfYear, value: -12, to: Date())!
        case .month: cutoff = Calendar.current.date(byAdding: .month, value: -12, to: Date())!
        case .quarter: cutoff = Calendar.current.date(byAdding: .month, value: -24, to: Date())!
        case .year: cutoff = Calendar.current.date(byAdding: .year, value: -5, to: Date())!
        case .all: cutoff = .distantPast
        }
        return snapshots.filter { $0.date >= cutoff }
    }

    private var accountPercent: Double {
        let allInAccount = DataService.shared.fetchHoldings().filter { $0.accountID == holding.accountID }
        let total = allInAccount.reduce(0.0) { $0 + $1.marketValue }
        return total > 0 ? holding.marketValue / total * 100 : 0
    }

    private var totalPercent: Double {
        let nw = DataService.shared.netWorth()
        return nw.assets > 0 ? holding.marketValue / nw.assets * 100 : 0
    }
}

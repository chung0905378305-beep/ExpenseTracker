import SwiftUI
import SwiftData

struct BudgetListView: View {
    @Query(sort: \Category.sortHint) private var categories: [Category]
    @State private var monthKey = FormatHelper.currentMonthKey
    @State private var showAdd = false

    var body: some View {
        List {
            Section("月份选择") {
                Picker("月份", selection: $monthKey) {
                    ForEach(recentMonths(), id: \.self) { m in
                        Text(m).tag(m)
                    }
                }
            }

            Section("分类预算") {
                ForEach(categories.filter { !$0.isSubcategory && !$0.isDeleted }) { cat in
                    BudgetRowView(category: cat, monthKey: monthKey)
                }
            }
        }
        .navigationTitle("预算管理")
        .toolbar {
            Button("自动生成") {
                autoGenerate()
            }
        }
    }

    private func recentMonths() -> [String] {
        let cal = Calendar.current
        var months: [String] = []
        for i in (-1..<3) {
            if let d = cal.date(byAdding: .month, value: i, to: Date()) {
                months.append(FormatHelper.monthKey(d))
            }
        }
        return months
    }

    private func autoGenerate() {
        // TODO: 基于历史平均值自动建议预算
    }
}

struct BudgetRowView: View {
    let category: Category
    let monthKey: String
    @Environment(\.modelContext) private var context
    @State private var limit = ""
    @State private var existingBudget: Budget?

    @EnvironmentObject var appState: AppState

    var body: some View {
        let progress = DataService.shared.budgetProgress(existingBudget ?? Budget(monthKey: monthKey, limitAmount: 0))
        let overspent = progress.percent >= 1.0
        let warning = progress.percent >= 0.8 && progress.percent < 1.0

        HStack(spacing: 12) {
            Image(systemName: category.icon)
                .font(.callout)
                .frame(width: 32)

            VStack(alignment: .leading, spacing: 4) {
                Text(category.name).font(.callout)
                GeometryReader { geo in
                    ZStack(alignment: .leading) {
                        RoundedRectangle(cornerRadius: 3)
                            .fill(.gray.opacity(0.1))
                            .frame(height: 5)
                        RoundedRectangle(cornerRadius: 3)
                            .fill(overspent ? .red : warning ? .orange : .blue)
                            .frame(width: min(CGFloat(progress.percent) * geo.size.width, geo.size.width), height: 5)
                    }
                }
                .frame(height: 5)

                HStack {
                    Text("\(FormatHelper.currency(progress.spent)) / \(FormatHelper.currency(existingBudget?.limitAmount ?? 0))")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    if warning || overspent {
                        Text(overspent ? "超支" : "预警")
                            .font(.caption2)
                            .foregroundColor(overspent ? .red : .orange)
                    }
                }
            }
        }
        .onAppear {
            let budgets = DataService.shared.fetchBudgets(monthKey: monthKey)
            existingBudget = budgets.first { $0.categoryID == category.id }
            if let b = existingBudget {
                limit = String(format: "%.0f", b.limitAmount)
            }
        }
    }
}

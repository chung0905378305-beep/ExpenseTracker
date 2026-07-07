import SwiftUI
import SwiftData

struct SubscriptionListView: View {
    @Query(sort: \Subscription.nextBillingDate) private var subscriptions: [Subscription]
    @State private var showAdd = false
    @State private var filter: String? = nil

    @EnvironmentObject var appState: AppState

    var body: some View {
        List {
            // 即将续费区
            Section("即将续费") {
                let upcoming = subscriptions.filter {
                    $0.status == .active && $0.daysUntilBilling <= 7
                }
                if upcoming.isEmpty {
                    Text("未来 7 天无续费").font(.caption).foregroundColor(.secondary)
                } else {
                    ForEach(upcoming) { sub in
                        SubscriptionRow(sub: sub)
                    }
                }
            }

            // 筛选
            Section {
                Picker("筛选", selection: $filter) {
                    Text("全部").tag(nil as String?)
                    Text("自动扣款").tag(SubscriptionMode.autoCharge.rawValue)
                    Text("一次性").tag(SubscriptionMode.oneTime.rawValue)
                    Text("手动提醒").tag(SubscriptionMode.manualRemind.rawValue)
                }
                .pickerStyle(.segmented)
            }
            .listRowBackground(Color.clear)

            // 全部列表
            Section {
                ForEach(filteredSubscriptions) { sub in
                    SubscriptionRow(sub: sub)
                }
                .onDelete { _ in }
            }
        }
        .navigationTitle("订阅管理")
        .toolbar {
            Button { showAdd = true } label: {
                Image(systemName: "plus")
            }
        }
        .sheet(isPresented: $showAdd) {
            AddSubscriptionSheet()
        }
    }

    private var filteredSubscriptions: [Subscription] {
        guard let f = filter else { return subscriptions }
        return subscriptions.filter { $0.modeRaw == f }
    }
}

struct SubscriptionRow: View {
    let sub: Subscription
    @EnvironmentObject var appState: AppState

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "arrow.triangle.2.circlepath.circle.fill")
                .font(.title2)
                .foregroundColor(.blue)

            VStack(alignment: .leading, spacing: 3) {
                Text(sub.name).font(.callout)
                HStack(spacing: 6) {
                    Text(sub.mode.rawValue)
                        .font(.caption2)
                        .padding(.horizontal, 5)
                        .padding(.vertical, 1)
                        .background(.blue.opacity(0.1))
                        .clipShape(Capsule())
                    Text(sub.period.rawValue)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                if sub.daysUntilBilling > 0 {
                    Text("\(sub.daysUntilBilling) 天后续费")
                        .font(.caption2)
                        .foregroundColor(.orange)
                }
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(appState.hideAmount ? "****" : FormatHelper.currency(sub.amount))
                    .font(.callout.weight(.medium))
                Text(FormatHelper.day(sub.nextBillingDate))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
    }
}

struct AddSubscriptionSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context

    @State private var name = ""
    @State private var amount = ""
    @State private var mode: SubscriptionMode = .manualRemind
    @State private var period: SubscriptionPeriod = .monthly
    @State private var nextDate = Date()

    var body: some View {
        NavigationStack {
            Form {
                Section("基本信息") {
                    TextField("名称", text: $name)
                    TextField("金额", text: $amount).keyboardType(.decimalPad)
                }

                Section("扣款方式") {
                    Picker("模式", selection: $mode) {
                        ForEach(SubscriptionMode.allCases, id: \.self) { m in
                            Text(m.rawValue).tag(m)
                        }
                    }
                    Picker("频率", selection: $period) {
                        ForEach(SubscriptionPeriod.allCases, id: \.self) { p in
                            Text(p.rawValue).tag(p)
                        }
                    }
                }

                Section("下次续费") {
                    DatePicker("日期", selection: $nextDate, displayedComponents: .date)
                }
            }
            .navigationTitle("添加订阅")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("取消") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") { save() }.disabled(name.isEmpty || amount.isEmpty)
                }
            }
        }
    }

    private func save() {
        guard let amt = Double(amount) else { return }
        let sub = Subscription(name: name, amount: amt, mode: mode, period: period, nextBillingDate: nextDate)
        context.insert(sub)
        try? context.save()
        dismiss()
    }
}

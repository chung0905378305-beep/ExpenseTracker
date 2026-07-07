import SwiftUI
import SwiftData

struct AddTransactionSheet: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context

    @Query(sort: \Category.sortHint) private var allCategories: [Category]
    @Query(sort: \Account.sortHint) private var accounts: [Account]
    @Query(sort: \Tag.name) private var tags: [Tag]

    @State private var kind: TransactionKind = .expense
    @State private var amountStr = ""
    @State private var selectedCategory: Category?
    @State private var selectedAccount: Account?
    @State private var date = Date()
    @State private var note = ""
    @State private var selectedTags: [Tag] = []

    // 编辑模式
    var editingTransaction: Transaction?

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // 类型分段
                    Picker("类型", selection: $kind) {
                        ForEach(TransactionKind.allCases.filter { $0 != .transfer && $0 != .refund }, id: \.self) { k in
                            Text(k.title).tag(k)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal, 16)

                    // 金额 Hero
                    VStack(spacing: 4) {
                        HStack(alignment: .lastTextBaseline, spacing: 4) {
                            Text("¥")
                                .font(.title)
                                .foregroundColor(kind == .income ? .incomeGreen : .expenseRed)
                            TextField("0", text: $amountStr)
                                .font(.system(size: 48, weight: .bold, design: .monospaced))
                                .foregroundColor(kind == .income ? .incomeGreen : .expenseRed)
                                .multilineTextAlignment(.center)
                                .keyboardType(.decimalPad)
                        }
                    }
                    .padding(.vertical, 12)

                    // 分类选择
                    VStack(alignment: .leading, spacing: 8) {
                        Text("分类").font(.caption).foregroundColor(.secondary)
                            .padding(.horizontal, 16)

                        CategoryPickerView(
                            categories: visibleCategories,
                            selected: $selectedCategory,
                            kind: kind
                        )
                        .padding(.horizontal, 16)
                    }

                    // 账户选择
                    VStack(alignment: .leading, spacing: 8) {
                        Text("账户").font(.caption).foregroundColor(.secondary)
                        Picker("账户", selection: $selectedAccount) {
                            Text("未选择").tag(nil as Account?)
                            ForEach(accounts.filter { !$0.archived }) { account in
                                Text(account.name).tag(account as Account?)
                            }
                        }
                        .pickerStyle(.menu)
                    }
                    .padding(.horizontal, 16)

                    // 日期
                    VStack(alignment: .leading, spacing: 8) {
                        Text("日期").font(.caption).foregroundColor(.secondary)
                        DatePicker("", selection: $date, displayedComponents: .date)
                            .labelsHidden()
                    }
                    .padding(.horizontal, 16)

                    // 备注
                    VStack(alignment: .leading, spacing: 8) {
                        Text("备注").font(.caption).foregroundColor(.secondary)
                        TextField("添加备注...", text: $note)
                            .textFieldStyle(.roundedBorder)
                    }
                    .padding(.horizontal, 16)

                    // 标签
                    if !tags.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text("标签").font(.caption).foregroundColor(.secondary)
                            ScrollView(.horizontal) {
                                HStack {
                                    ForEach(tags) { tag in
                                        Button {
                                            if selectedTags.contains(where: { $0.id == tag.id }) {
                                                selectedTags.removeAll { $0.id == tag.id }
                                            } else {
                                                selectedTags.append(tag)
                                            }
                                        } label: {
                                            Text(tag.name)
                                                .font(.caption)
                                                .padding(.horizontal, 10)
                                                .padding(.vertical, 5)
                                                .background(
                                                    selectedTags.contains(where: { $0.id == tag.id })
                                                    ? Color(hex: tag.colorHex)
                                                    : Color.gray.opacity(0.1)
                                                )
                                                .foregroundColor(
                                                    selectedTags.contains(where: { $0.id == tag.id })
                                                    ? .white : .primary
                                                )
                                                .clipShape(Capsule())
                                        }
                                    }
                                }
                            }
                        }
                        .padding(.horizontal, 16)
                    }
                }
                .padding(.vertical, 12)
            }
            .navigationTitle(editingTransaction != nil ? "编辑账单" : "记一笔")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") { save() }
                        .fontWeight(.semibold)
                        .disabled(amountStr.isEmpty)
                }
            }
            .onAppear {
                if let txn = editingTransaction {
                    kind = txn.kind
                    amountStr = String(format: "%.2f", txn.amount)
                    selectedCategory = allCategories.first { $0.id == txn.categoryID }
                    selectedAccount = accounts.first { $0.id == txn.accountID }
                    date = txn.date
                    note = txn.note
                } else {
                    selectedAccount = accounts.first
                }
            }
        }
    }

    private var visibleCategories: [Category] {
        allCategories.filter { $0.parentID != nil }
    }

    private func save() {
        guard let amount = Double(amountStr), amount > 0 else { return }

        if let txn = editingTransaction {
            txn.kind = kind
            txn.amount = amount
            txn.categoryID = selectedCategory?.id
            txn.accountID = selectedAccount?.id
            txn.date = date
            txn.note = note
            txn.tagIDs = selectedTags.map { $0.id }
        } else {
            let txn = Transaction(
                kind: kind,
                amount: amount,
                categoryID: selectedCategory?.id,
                accountID: selectedAccount?.id,
                date: date,
                note: note,
                tagIDs: selectedTags.map { $0.id }
            )
            context.insert(txn)

            // 更新分类使用计数
            if let cat = selectedCategory {
                cat.useCount += 1
            }
        }

        try? context.save()
        dismiss()
    }
}

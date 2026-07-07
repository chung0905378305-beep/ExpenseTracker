import SwiftUI
import SwiftData

struct AddHoldingSheet: View {
    let accountID: UUID
    @Environment(\.dismiss) private var dismiss
    @Environment(\.modelContext) private var context

    @State private var type: HoldingType = .stock
    @State private var symbol = ""
    @State private var name = ""
    @State private var quantity = ""
    @State private var costPrice = ""
    @State private var searchResults: [(symbol: String, name: String)] = []
    @State private var isSearching = false

    var body: some View {
        NavigationStack {
            Form {
                Section("类型") {
                    Picker("资产类型", selection: $type) {
                        ForEach(HoldingType.allCases, id: \.self) { t in
                            Text(t.rawValue).tag(t)
                        }
                    }
                    .pickerStyle(.segmented)
                }

                Section("代码") {
                    TextField("输入代码...", text: $symbol)
                        .autocapitalization(.none)
                        .onSubmit { Task { await search() } }

                    Button("搜索") { Task { await search() } }
                        .disabled(symbol.isEmpty || isSearching)

                    if isSearching {
                        ProgressView()
                    }

                    ForEach(searchResults, id: \.symbol) { result in
                        Button {
                            symbol = result.symbol
                            name = result.name
                            searchResults = []
                        } label: {
                            HStack {
                                Text(result.symbol).fontWeight(.medium)
                                Text(result.name)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                    }
                }

                Section("名称") {
                    TextField("资产名称", text: $name)
                }

                Section("持仓") {
                    TextField("数量", text: $quantity)
                        .keyboardType(.decimalPad)
                    TextField("成本价", text: $costPrice)
                        .keyboardType(.decimalPad)
                }
            }
            .navigationTitle("添加持仓")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("取消") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("保存") { save() }
                        .disabled(symbol.isEmpty || quantity.isEmpty || costPrice.isEmpty)
                }
            }
        }
    }

    private func search() async {
        isSearching = true
        defer { isSearching = false }
        searchResults = await MarketService.shared.searchYahoo(query: symbol)
    }

    private func save() {
        guard let qty = Double(quantity),
              let price = Double(costPrice) else { return }

        let holding = Holding(
            type: type,
            symbol: symbol.uppercased(),
            name: name.isEmpty ? symbol : name,
            quantity: qty,
            costPrice: price,
            accountID: accountID
        )
        context.insert(holding)
        try? context.save()
        dismiss()
    }
}

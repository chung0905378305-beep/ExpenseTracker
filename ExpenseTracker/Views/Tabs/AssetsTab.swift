import SwiftUI
import SwiftData

struct AssetsTab: View {
    @Query(sort: \Account.sortHint) private var accounts: [Account]
    @Query(filter: #Predicate<Holding> { $0.isDeleted == false }) private var holdings: [Holding]

    @State private var isRefreshing = false
    @EnvironmentObject var appState: AppState

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 16) {
                    // L0: 净资产 Hero
                    NetWorthHeroCard(
                        accounts: accounts,
                        holdings: holdings,
                        isRefreshing: $isRefreshing
                    )
                    .padding(.horizontal, 16)

                    // 投资账户分区
                    let investmentAccounts = accounts.filter { $0.subtype.isInvestment && !$0.archived }
                    if !investmentAccounts.isEmpty {
                        sectionHeader("投资账户")
                        ForEach(investmentAccounts) { account in
                            InvestmentAccountCard(account: account, holdings: holdings)
                                .padding(.horizontal, 16)
                        }
                    }

                    // 现金账户
                    let cashAccounts = accounts.filter { $0.role == .asset && !$0.subtype.isInvestment && !$0.archived && $0.subtype != .realEstate && $0.subtype != .gold }
                    if !cashAccounts.isEmpty {
                        sectionHeader("现金账户")
                        ForEach(cashAccounts) { account in
                            CashAccountRow(account: account)
                                .padding(.horizontal, 16)
                        }
                    }

                    // 实物资产
                    let physicalAccounts = accounts.filter { ($0.subtype == .realEstate || $0.subtype == .gold) && !$0.archived }
                    if !physicalAccounts.isEmpty {
                        sectionHeader("实物资产")
                        ForEach(physicalAccounts) { account in
                            CashAccountRow(account: account)
                                .padding(.horizontal, 16)
                        }
                    }

                    // 负债
                    let liabilityAccounts = accounts.filter { $0.role == .liability && !$0.archived }
                    if !liabilityAccounts.isEmpty {
                        sectionHeader("负债")
                        ForEach(liabilityAccounts) { account in
                            LiabilityAccountRow(account: account)
                                .padding(.horizontal, 16)
                        }
                    }
                }
                .padding(.vertical, 12)
            }
            .navigationTitle("")
            .navigationBarHidden(true)
            .refreshable {
                await refreshQuotes()
            }
        }
    }

    @ViewBuilder
    private func sectionHeader(_ title: String) -> some View {
        HStack {
            Text(title)
                .sectionHeader()
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, 4)
    }

    private func refreshQuotes() async {
        isRefreshing = true
        defer { isRefreshing = false }

        for h in holdings {
            if h.type == .crypto {
                if let result = await MarketService.shared.fetchCryptoPrice(coinID: h.symbol.lowercased()) {
                    h.currentPrice = result.price
                    h.priceTime = Date()
                }
            } else {
                if let result = await MarketService.shared.fetchYahooQuote(symbol: h.symbol) {
                    h.currentPrice = result.price
                    h.priceTime = Date()
                }
            }
        }
    }
}

// MARK: - L0 NetWorthHeroCard
struct NetWorthHeroCard: View {
    let accounts: [Account]
    let holdings: [Holding]
    @Binding var isRefreshing: Bool
    @EnvironmentObject var appState: AppState

    var body: some View {
        let totalAssets = computeTotalAssets()
        let totalLiabilities = computeTotalLiabilities()
        let net = totalAssets - totalLiabilities

        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("净资产")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.8))
                Spacer()
                Button {
                    Task { await refreshQuotes() }
                } label: {
                    Image(systemName: "arrow.clockwise")
                        .font(.caption)
                        .foregroundColor(.white.opacity(0.8))
                }
            }

            Text(appState.hideAmount ? "****" : FormatHelper.currency(net))
                .font(.largeTitle.weight(.bold))
                .foregroundColor(.white)

            HStack(spacing: 16) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("总资产").font(.caption2).foregroundColor(.white.opacity(0.7))
                    Text(appState.hideAmount ? "***" : FormatHelper.currency(totalAssets))
                        .font(.callout.weight(.semibold))
                        .foregroundColor(.white)
                }
                Spacer()
                VStack(alignment: .trailing, spacing: 2) {
                    Text("总负债").font(.caption2).foregroundColor(.white.opacity(0.7))
                    Text(appState.hideAmount ? "***" : FormatHelper.currency(totalLiabilities))
                        .font(.callout.weight(.semibold))
                        .foregroundColor(.white)
                }
            }
        }
        .padding(18)
        .background(
            LinearGradient(colors: [.blue, .blue.opacity(0.7)], startPoint: .topLeading, endPoint: .bottomTrailing)
        )
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }

    private func computeTotalAssets() -> Double {
        var total = 0.0
        for acct in accounts where acct.role == .asset {
            if acct.subtype.isInvestment {
                total += holdings.filter { $0.accountID == acct.id }.reduce(0) { $0 + $1.marketValue }
            } else {
                total += DataService.shared.accountBalance(acct)
            }
        }
        return total
    }

    private func computeTotalLiabilities() -> Double {
        accounts.filter { $0.role == .liability }.reduce(0) {
            $0 + DataService.shared.accountBalance($1)
        }
    }

    private func refreshQuotes() async {
        isRefreshing = true
        defer { isRefreshing = false }

        for h in holdings {
            if h.type == .crypto {
                if let result = await MarketService.shared.fetchCryptoPrice(coinID: h.symbol.lowercased()) {
                    h.currentPrice = result.price
                    h.priceTime = Date()
                }
            } else {
                if let result = await MarketService.shared.fetchYahooQuote(symbol: h.symbol) {
                    h.currentPrice = result.price
                    h.priceTime = Date()
                }
            }
        }
    }
}

// MARK: - L1 InvestmentAccountCard
struct InvestmentAccountCard: View {
    let account: Account
    let holdings: [Holding]
    let accountHoldings: [Holding]

    init(account: Account, holdings: [Holding]) {
        self.account = account
        self.holdings = holdings
        self.accountHoldings = holdings.filter { $0.accountID == account.id }
    }

    var body: some View {
        let totalMV = accountHoldings.reduce(0.0) { $0 + $1.marketValue }
        let totalCV = accountHoldings.reduce(0.0) { $0 + $1.costValue }
        let pnl = totalMV - totalCV
        let pnlPct = totalCV > 0 ? (pnl / totalCV) * 100 : 0

        VStack(alignment: .leading, spacing: 10) {
            // 账户头
            HStack {
                Image(systemName: "briefcase.fill")
                    .foregroundColor(.blue)
                Text(account.name)
                    .font(.subheadline.weight(.semibold))
                Spacer()
                Text(FormatHelper.currency(totalMV))
                    .font(.subheadline.weight(.semibold))
            }

            // 总计盈亏
            HStack(spacing: 8) {
                Text("成本 \(FormatHelper.currency(totalCV))")
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(String(format: "%+.2f%%", pnlPct))
                    .font(.caption.weight(.medium))
                    .foregroundColor(pnl >= 0 ? .profitGreen : .expenseRed)
                    .padding(.horizontal, 6)
                    .padding(.vertical, 2)
                    .background(pnl >= 0 ? Color.green.opacity(0.1) : Color.red.opacity(0.1))
                    .clipShape(Capsule())
                Spacer()
                NavigationLink("添加持仓") {
                    AddHoldingSheet(accountID: account.id)
                }
                .font(.caption)
            }

            if !accountHoldings.isEmpty {
                Divider()
                // L2: 持仓列表
                ForEach(accountHoldings) { holding in
                    NavigationLink(destination: HoldingDetailView(holding: holding)) {
                        HoldingRowView(holding: holding)
                    }
                    .buttonStyle(.plain)
                }
            } else {
                Text("暂无持仓")
                    .font(.caption)
                    .foregroundColor(.secondary)
                    .padding(.vertical, 8)
            }
        }
        .padding(14)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - L2 HoldingRow
struct HoldingRowView: View {
    let holding: Holding

    var body: some View {
        HStack(spacing: 10) {
            Image(systemName: holding.type == .crypto ? "bitcoinsign.circle" : "chart.line.uptrend.xyaxis")
                .font(.title3)
                .foregroundColor(holding.type == .crypto ? .orange : .blue)

            VStack(alignment: .leading, spacing: 2) {
                Text(holding.name.isEmpty ? holding.symbol : holding.name)
                    .font(.callout)
                Text(holding.symbol)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }

            Spacer()

            VStack(alignment: .trailing, spacing: 2) {
                Text(FormatHelper.currency(holding.marketValue))
                    .font(.callout.weight(.medium))
                Text(String(format: "%+.2f%%", holding.pnlPercent))
                    .font(.caption2.weight(.medium))
                    .foregroundColor(holding.isProfitable ? .profitGreen : .expenseRed)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - CashAccountRow
struct CashAccountRow: View {
    let account: Account
    @EnvironmentObject var appState: AppState

    var body: some View {
        HStack {
            Image(systemName: account.subtype == .realEstate ? "house.fill" :
                     account.subtype == .gold ? "circle.hexagongrid.fill" :
                     account.subtype == .ewallet ? "iphone.gen3" : "creditcard.fill")
                .foregroundColor(.blue)
            VStack(alignment: .leading, spacing: 2) {
                Text(account.name).font(.callout)
                Text(account.subtype.rawValue).font(.caption2).foregroundColor(.secondary)
            }
            Spacer()
            Text(appState.hideAmount ? "****" : FormatHelper.currency(DataService.shared.accountBalance(account)))
                .font(.callout.weight(.medium))
        }
        .padding(12)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - LiabilityAccountRow
struct LiabilityAccountRow: View {
    let account: Account
    @EnvironmentObject var appState: AppState

    var body: some View {
        let balance = DataService.shared.accountBalance(account)
        VStack(spacing: 8) {
            HStack {
                Image(systemName: "creditcard.fill").foregroundColor(.orange)
                VStack(alignment: .leading, spacing: 2) {
                    Text(account.name).font(.callout)
                    if account.subtype == .creditCard {
                        Text("账单日 \(account.statementDay ?? 0)日 · 还款日 \(account.dueDay ?? 0)日")
                            .font(.caption2).foregroundColor(.secondary)
                    } else {
                        Text(account.subtype.rawValue).font(.caption2).foregroundColor(.secondary)
                    }
                }
                Spacer()
                Text(appState.hideAmount ? "****" : FormatHelper.currency(balance))
                    .font(.callout.weight(.medium))
                    .foregroundColor(.expenseRed)
            }
        }
        .padding(12)
        .background(.regularMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

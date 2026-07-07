import Foundation
import BackgroundTasks

/// 行情服务 — 多源行情拉取
@MainActor
final class MarketService {
    static let shared = MarketService()

    private let session: URLSession = {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 15
        return URLSession(configuration: config)
    }()

    /// Yahoo Finance — 股票/ETF（A股/港股/美股）
    func fetchYahooQuote(symbol: String) async -> (price: Double, change: Double)? {
        let formattedSymbol = symbol.replacingOccurrences(of: ".", with: "-")
        guard let url = URL(string: "\(AppConstants.yahooFinanceBase)/v8/finance/chart/\(formattedSymbol)?interval=1d&range=1d") else { return nil }

        do {
            let (data, _) = try await session.data(from: url)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let chart = json["chart"] as? [String: Any],
                  let result = (chart["result"] as? [[String: Any]])?.first,
                  let meta = result["meta"] as? [String: Any],
                  let price = meta["regularMarketPrice"] as? Double else { return nil }
            let prevClose = (meta["previousClose"] as? Double) ?? price
            return (price, price - prevClose)
        } catch {
            return nil
        }
    }

    /// CoinGecko — 加密货币
    func fetchCryptoPrice(coinID: String) async -> (price: Double, change24h: Double)? {
        guard let url = URL(string: "\(AppConstants.coinGeckoBase)/simple/price?ids=\(coinID)&vs_currencies=usd&include_24hr_change=true") else { return nil }

        do {
            let (data, _) = try await session.data(from: url)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let coin = json[coinID] as? [String: Any],
                  let price = coin["usd"] as? Double else { return nil }
            let change = (coin["usd_24h_change"] as? Double) ?? 0
            return (price, change)
        } catch {
            return nil
        }
    }

    /// 搜索股票代码
    func searchYahoo(query: String) async -> [(symbol: String, name: String)] {
        guard let url = URL(string: "\(AppConstants.yahooFinanceBase)/v1/finance/search?q=\(query.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? query)") else { return [] }

        do {
            let (data, _) = try await session.data(from: url)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let quotes = json["quotes"] as? [[String: Any]] else { return [] }
            return quotes.compactMap {
                guard let sym = $0["symbol"] as? String,
                      let name = $0["shortname"] as? String ?? $0["longname"] as? String else { return nil }
                return (sym, name)
            }
        } catch {
            return []
        }
    }

    /// BGAppRefreshTask 入口
    func handleBackgroundRefresh(_ task: BGAppRefreshTask) async {
        task.setTaskCompleted(success: true)
    }
}

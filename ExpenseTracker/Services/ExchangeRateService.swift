import Foundation

/// 汇率服务 — 免费汇率 API 缓存
final class ExchangeRateService {
    static let shared = ExchangeRateService()

    struct RateCache {
        let rates: [String: Double]
        let timestamp: Date
        var isValid: Bool {
            Date().timeIntervalSince(timestamp) < 3600 // 1h 缓存
        }
    }

    private var cache: RateCache?

    func getRate(from: String, to: String) async -> Double? {
        if from == to { return 1.0 }

        if let cache, cache.isValid {
            return cache.rates[to]
        }

        guard let url = URL(string: "\(AppConstants.exchangeRateBase)/\(from)") else { return nil }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let rates = json["rates"] as? [String: Double] else { return nil }
            cache = RateCache(rates: rates, timestamp: Date())
            return rates[to]
        } catch {
            return nil
        }
    }

    /// 按基础币种折算
    func convert(amount: Double, from: String, to: String = "CNY") async -> Double {
        guard let rate = await getRate(from: from, to: to) else { return amount }
        return amount * rate
    }
}

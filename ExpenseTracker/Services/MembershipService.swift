import Foundation
import SwiftData

/// 会员系统对接服务 — 对接后台会员 API
final class MembershipService {
    static let shared = MembershipService()

    struct MemberInfo {
        let active: Bool
        let planName: String
        let isTrial: Bool
        let expireAt: Date?
        let autoRenew: Bool
        let features: Set<String>
    }

    private let session = URLSession.shared
    private let baseURL = AppConstants.backendBaseURL
    private let apiKey = AppConstants.appAPIKey

    /// 校验会员状态
    func verifyMembership(appUserID: String) async -> MemberInfo? {
        guard let url = URL(string: "\(baseURL)/api/v1/verify/membership?appUserId=\(appUserID)") else { return nil }
        var request = URLRequest(url: url)
        request.setValue(apiKey, forHTTPHeaderField: "X-App-Key")
        request.timeoutInterval = 10

        do {
            let (data, _) = try await session.data(for: request)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                  let d = json["data"] as? [String: Any],
                  let active = d["active"] as? Bool else { return nil }

            let plan = d["plan"] as? [String: Any]
            let dateStr = d["expireAt"] as? String
            let expireAt: Date? = {
                let f = ISO8601DateFormatter()
                return dateStr.flatMap { f.date(from: $0) }
            }()

            return MemberInfo(
                active: active,
                planName: plan?["name"] as? String ?? "免费版",
                isTrial: d["isTrial"] as? Bool ?? false,
                expireAt: expireAt,
                autoRenew: d["autoRenew"] as? Bool ?? false,
                features: Set(d["features"] as? [String] ?? [])
            )
        } catch {
            return nil
        }
    }

    /// 兑换激活码
    func redeemCode(appUserID: String, code: String) async -> (success: Bool, message: String) {
        guard let url = URL(string: "\(baseURL)/api/v1/activate") else {
            return (false, "URL 无效")
        }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-App-Key")
        request.timeoutInterval = 10

        let body = ["appUserId": appUserID, "code": code]
        request.httpBody = try? JSONSerialization.data(withJSONObject: body)

        do {
            let (data, _) = try await session.data(for: request)
            guard let json = try JSONSerialization.jsonObject(with: data) as? [String: Any] else {
                return (false, "响应格式错误")
            }
            if let success = json["success"] as? Bool, success {
                return (true, "兑换成功！会员有效期至 \(json["expireAt"] as? String ?? "?")")
            }
            return (false, json["message"] as? String ?? "兑换失败")
        } catch {
            return (false, "网络错误: \(error.localizedDescription)")
        }
    }
}

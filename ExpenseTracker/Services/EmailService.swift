import Foundation

/// 邮箱账单服务 — IMAP/OAuth 连接 + AI 抽取
/// 注意: IMAP 访问需要 App Store 审核说明，真机联调时配置
final class EmailService {
    static let shared = EmailService()

    struct EmailBill {
        let sender: String
        let subject: String
        let body: String
        let date: Date
        let extractedMerchant: String?
        let extractedAmount: Double?
        let extractedCategory: String?
    }

    /// OAuth 配置状态
    private var configuredProviders: Set<String> = []

    /// 配置邮箱 OAuth（Gmail/iCloud/Outlook）
    func configure(provider: String) {
        configuredProviders.insert(provider)
    }

    /// 检查是否已配置
    func isConfigured(_ provider: String) -> Bool {
        configuredProviders.contains(provider)
    }

    /// 拉取账单邮件（骨架，真机实现需要 MailCore2 或直接 IMAP）
    func fetchBills() async -> [EmailBill] {
        // 骨架实现：iOS 原生 IMAP 需要 MailKit 或第三方库
        // 当前返回空，真机构建时替换
        return []
    }

    /// AI 抽取结构化账单信息
    func extractBillInfo(from email: EmailBill, apiKey: String, baseURL: String, model: String) async -> [String: Any]? {
        let text = "发件人: \(email.sender)\n主题: \(email.subject)\n内容: \(email.body)"
        return try? await AIService.shared.extractBillInfo(apiKey: apiKey, baseURL: baseURL, model: model, text: text)
    }

    /// OCR 识别图片中的账单信息（使用 Vision 框架）
    func performOCR(imageData: Data) async -> String? {
        // 骨架：iOS Vision 框架的 VNRecognizeTextRequest
        // 真机实现：
        // let handler = VNImageRequestHandler(data: imageData)
        // let request = VNRecognizeTextRequest()
        // try? handler.perform([request])
        // return request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n")
        return nil
    }
}

import Foundation

/// AI 分析服务 — 对接 OpenAI 兼容接口
final class AIService {
    static let shared = AIService()

    private let session = URLSession.shared

    struct AIResponse: Codable {
        let choices: [Choice]
        struct Choice: Codable {
            let message: Message
            struct Message: Codable { let content: String }
        }
    }

    /// 生成月度财务洞察报告
    func generateMonthlyReport(
        apiKey: String,
        baseURL: String,
        model: String,
        monthlyData: String
    ) async throws -> String {
        let prompt = """
        你是一位专业的个人财务顾问。以下是用户本月的财务数据：

        \(monthlyData)

        请提供：
        1. 本月财务健康评分（百分制）
        2. 主要支出类别分析
        3. 与上月的对比变化
        4. 3 条具体的省钱建议
        5. 下月预算建议

        回复使用中文，简洁有条理。
        """

        return try await chat(apiKey: apiKey, baseURL: baseURL, model: model, message: prompt)
    }

    /// 对话式问答
    func chat(apiKey: String, baseURL: String, model: String, message: String) async throws -> String {
        let url = URL(string: "\(baseURL)/v1/chat/completions")!
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")

        let body: [String: Any] = [
            "model": model,
            "messages": [
                ["role": "system", "content": "你是一位专业的个人财务顾问，帮助用户分析财务状况并提供建议。"],
                ["role": "user", "content": message]
            ],
            "temperature": 0.7
        ]
        request.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (data, _) = try await session.data(for: request)
        let response = try JSONDecoder().decode(AIResponse.self, from: data)
        return response.choices.first?.message.content ?? "无法获取 AI 回复"
    }

    /// 从账单文本中提取结构化信息
    func extractBillInfo(apiKey: String, baseURL: String, model: String, text: String) async throws -> [String: Any]? {
        let prompt = """
        从以下账单文本中提取结构化信息，返回 JSON：
        { "merchant": "商户名", "amount": 金额数字, "date": "日期YYYY-MM-DD", "category": "推测分类" }

        如果无法识别，返回 { "error": "无法识别" }。

        文本内容：
        \(text)
        """
        let result = try await chat(apiKey: apiKey, baseURL: baseURL, model: model, message: prompt)
        guard let data = result.data(using: .utf8),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else { return nil }
        return json
    }
}

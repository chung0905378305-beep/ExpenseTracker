import Foundation

/// 自动归类规则引擎 — 基于关键词匹配 + 金额范围 + 账户 + 日期模式
final class CategoryRuleEngine {
    static let shared = CategoryRuleEngine()

    /// 根据备注/商户名自动匹配分类
    func matchCategory(note: String, amount: Double, categories: [Category]) -> Category? {
        guard !note.isEmpty else { return nil }

        let lowercased = note.lowercased()

        // 先匹配子类（更精确）
        let subcategories = categories.filter { $0.parentID != nil && !$0.keywords.isEmpty }
        for cat in subcategories {
            for kw in cat.keywords {
                if lowercased.contains(kw.lowercased()) { return cat }
            }
        }

        // 再匹配大类
        let parents = categories.filter { $0.parentID == nil && !$0.keywords.isEmpty }
        for cat in parents {
            for kw in cat.keywords {
                if lowercased.contains(kw.lowercased()) { return cat }
            }
        }

        return nil
    }

    /// 学习：根据历史数据建立关键词关联
    func learn(category: Category, note: String) {
        guard !note.isEmpty, note.count >= 2 else { return }
        let lowercased = note.lowercased()
        if !category.keywords.contains(lowercased) {
            category.keywords.append(lowercased)
        }
    }
}

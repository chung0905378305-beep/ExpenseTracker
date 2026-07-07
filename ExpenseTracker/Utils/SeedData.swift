import Foundation
import SwiftData

/// 首次启动幂等种子数据
enum SeedData {
    static func seedIfNeeded(context: ModelContext) async {
        let settingsFd = FetchDescriptor<AppSettings>()
        if (try? context.fetch(settingsFd).first) != nil { return }

        // AppSettings
        context.insert(AppSettings.default)

        // Categories (两级: 大类 + 子类)
        for (name, icon, defaultKind, children) in SeedCategories.builtin {
            let parent = Category(name: name, icon: icon, defaultKind: defaultKind, keywords: [name], sortHint: 0, isBuiltin: true)
            context.insert(parent)
            for (i, (cName, cIcon)) in children.enumerated() {
                let child = Category(name: cName, icon: cIcon, parentID: parent.id, defaultKind: defaultKind, keywords: [cName], sortHint: i, isBuiltin: true)
                context.insert(child)
            }
        }

        // Accounts
        for (name, role, subtype, balance) in SeedAccounts.builtin {
            let acct = Account(name: name, role: AccountRole(rawValue: role)!, subtype: AccountSubtype(rawValue: subtype)!)
            acct.initialBalance = balance
            context.insert(acct)
        }

        // Tags
        for (name, color) in SeedTags.builtin {
            context.insert(Tag(name: name, colorHex: color))
        }

        try? context.save()
    }
}

package com.expensetracker.app.util

import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.AppSettingsEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.TagEntity
import com.expensetracker.app.domain.model.AccountRole
import com.expensetracker.app.domain.model.TransactionKind

object SeedData {

    private data class CategorySeed(
        val name: String,
        val icon: String,
        val defaultKind: TransactionKind?,
        val children: List<String>
    )

    private val categorySeeds: List<CategorySeed> = listOf(
        CategorySeed("餐饮", "🍽️", TransactionKind.EXPENSE, listOf("外卖", "聚餐", "咖啡", "零食")),
        CategorySeed("交通", "🚗", TransactionKind.EXPENSE, listOf("公交", "打车", "加油", "停车")),
        CategorySeed("购物", "🛒", TransactionKind.EXPENSE, listOf("日用品", "服饰", "数码", "家居")),
        CategorySeed("住房", "🏠", TransactionKind.EXPENSE, listOf("租金", "水电", "物业", "维修")),
        CategorySeed("娱乐", "🎮", TransactionKind.EXPENSE, listOf("电影", "游戏", "旅游", "运动")),
        CategorySeed("医疗", "💊", TransactionKind.EXPENSE, listOf("问诊", "药品", "体检")),
        CategorySeed("教育", "📚", TransactionKind.EXPENSE, listOf("书籍", "课程", "培训")),
        CategorySeed("通讯", "📱", TransactionKind.EXPENSE, listOf("话费", "网费")),
        CategorySeed("收入", "💰", TransactionKind.INCOME, listOf("工资", "奖金", "理财", "兼职")),
        CategorySeed("转账", "🔄", TransactionKind.TRANSFER, listOf("转入", "转出"))
    )

    private val tagSeeds: List<Pair<String, String>> = listOf(
        "出差" to "#FF6B6B",
        "宝宝" to "#4ECDC4",
        "家庭" to "#45B7D1",
        "报销" to "#96CEB4",
        "学习" to "#FFEAA7",
        "礼物" to "#DDA0DD"
    )

    private val accountSeeds: List<Triple<String, AccountRole, String>> = listOf(
        Triple("现金", AccountRole.ASSET, "cash"),
        Triple("银行卡", AccountRole.ASSET, "bank_card"),
        Triple("支付宝", AccountRole.ASSET, "alipay"),
        Triple("信用卡", AccountRole.LIABILITY, "credit_card")
    )

    suspend fun seedIfNeeded(database: AppDatabase) {
        seedCategories(database)
        seedTags(database)
        seedAccounts(database)
        seedSettings(database)
    }

    private suspend fun seedCategories(database: AppDatabase) {
        val categoryDao = database.categoryDao()
        val existing = categoryDao.getById(1L)
        if (existing != null) return

        var sortHint = 0
        for (seed in categorySeeds) {
            val parentId = categoryDao.insert(
                CategoryEntity(
                    name = seed.name,
                    icon = seed.icon,
                    parentId = null,
                    defaultKind = seed.defaultKind?.name,
                    keywords = "",
                    sortHint = sortHint++,
                    isDeleted = false
                )
            )
            var childSort = 0
            for (childName in seed.children) {
                categoryDao.insert(
                    CategoryEntity(
                        name = childName,
                        icon = "",
                        parentId = parentId,
                        defaultKind = seed.defaultKind?.name,
                        keywords = "",
                        sortHint = childSort++,
                        isDeleted = false
                    )
                )
            }
        }
    }

    private suspend fun seedTags(database: AppDatabase) {
        val tagDao = database.tagDao()
        val existing = tagDao.getById(1L)
        if (existing != null) return

        for ((name, color) in tagSeeds) {
            tagDao.insert(
                TagEntity(
                    name = name,
                    color = color
                )
            )
        }
    }

    private suspend fun seedAccounts(database: AppDatabase) {
        val accountDao = database.accountDao()
        val existing = accountDao.getById(1L)
        if (existing != null) return

        for ((name, role, subtype) in accountSeeds) {
            accountDao.insert(
                AccountEntity(
                    name = name,
                    role = role.name,
                    subtype = subtype,
                    initialBalance = 0.0,
                    currentValueSource = "",
                    statementDay = if (role == AccountRole.LIABILITY) 1 else null,
                    dueDay = if (role == AccountRole.LIABILITY) 25 else null,
                    archived = false,
                    excludeFromNetWorth = false,
                    isDeleted = false
                )
            )
        }
    }

    private suspend fun seedSettings(database: AppDatabase) {
        val settingsDao = database.appSettingsDao()
        val existing = settingsDao.getSettings()
        if (existing != null) return

        settingsDao.insert(
            AppSettingsEntity(
                id = 1,
                baseCurrency = "CNY",
                includeLiabilityInNetWorth = true,
                linkAssetToCash = false,
                appLockEnabled = false,
                appLockMode = "",
                hideAmount = false,
                quoteRefreshTimes = "",
                language = "zh-CN",
                aiApiKey = "",
                aiBaseUrl = "",
                aiModel = "",
                mailEnabled = false,
                notificationLeadDays = 3
            )
        )
    }
}

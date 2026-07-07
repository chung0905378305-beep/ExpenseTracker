package com.expensetracker.app.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.expensetracker.app.data.local.converter.Converters
import com.expensetracker.app.data.local.dao.AccountDao
import com.expensetracker.app.data.local.dao.AIConversationDao
import com.expensetracker.app.data.local.dao.AppSettingsDao
import com.expensetracker.app.data.local.dao.BudgetDao
import com.expensetracker.app.data.local.dao.CategoryDao
import com.expensetracker.app.data.local.dao.HoldingDao
import com.expensetracker.app.data.local.dao.HoldingSnapshotDao
import com.expensetracker.app.data.local.dao.NetWorthSnapshotDao
import com.expensetracker.app.data.local.dao.RecurringRuleDao
import com.expensetracker.app.data.local.dao.SubscriptionDao
import com.expensetracker.app.data.local.dao.TagDao
import com.expensetracker.app.data.local.dao.TransactionDao
import com.expensetracker.app.data.local.entity.AccountEntity
import com.expensetracker.app.data.local.entity.AIConversationEntity
import com.expensetracker.app.data.local.entity.AppSettingsEntity
import com.expensetracker.app.data.local.entity.BudgetEntity
import com.expensetracker.app.data.local.entity.CategoryEntity
import com.expensetracker.app.data.local.entity.HoldingEntity
import com.expensetracker.app.data.local.entity.HoldingSnapshotEntity
import com.expensetracker.app.data.local.entity.NetWorthSnapshotEntity
import com.expensetracker.app.data.local.entity.RecurringRuleEntity
import com.expensetracker.app.data.local.entity.SubscriptionEntity
import com.expensetracker.app.data.local.entity.TagEntity
import com.expensetracker.app.data.local.entity.TransactionEntity

@Database(
    entities = [
        TransactionEntity::class,
        CategoryEntity::class,
        TagEntity::class,
        AccountEntity::class,
        BudgetEntity::class,
        RecurringRuleEntity::class,
        SubscriptionEntity::class,
        HoldingEntity::class,
        HoldingSnapshotEntity::class,
        NetWorthSnapshotEntity::class,
        AIConversationEntity::class,
        AppSettingsEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun holdingDao(): HoldingDao
    abstract fun holdingSnapshotDao(): HoldingSnapshotDao
    abstract fun netWorthSnapshotDao(): NetWorthSnapshotDao
    abstract fun aiConversationDao(): AIConversationDao
    abstract fun appSettingsDao(): AppSettingsDao

    companion object {
        const val DATABASE_NAME = "expense_tracker.db"
    }
}

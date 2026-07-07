package com.expensetracker.app.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.expensetracker.app.data.local.AppDatabase
import com.expensetracker.app.data.local.TransactionDao
import com.expensetracker.app.data.local.CategoryDao
import com.expensetracker.app.data.local.TagDao
import com.expensetracker.app.data.local.AccountDao
import com.expensetracker.app.data.local.BudgetDao
import com.expensetracker.app.data.local.RecurringRuleDao
import com.expensetracker.app.data.local.SubscriptionDao
import com.expensetracker.app.data.local.HoldingDao
import com.expensetracker.app.data.local.HoldingSnapshotDao
import com.expensetracker.app.data.local.NetWorthSnapshotDao
import com.expensetracker.app.data.local.AIConversationDao
import com.expensetracker.app.data.local.AppSettingsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

    @Provides
    @Singleton
    fun provideAppDatabase(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "expense_tracker.db"
        ).build()
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    fun provideTransactionDao(database: AppDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: AppDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideAccountDao(database: AppDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideBudgetDao(database: AppDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideRecurringRuleDao(database: AppDatabase): RecurringRuleDao = database.recurringRuleDao()

    @Provides
    fun provideSubscriptionDao(database: AppDatabase): SubscriptionDao = database.subscriptionDao()

    @Provides
    fun provideHoldingDao(database: AppDatabase): HoldingDao = database.holdingDao()

    @Provides
    fun provideHoldingSnapshotDao(database: AppDatabase): HoldingSnapshotDao = database.holdingSnapshotDao()

    @Provides
    fun provideNetWorthSnapshotDao(database: AppDatabase): NetWorthSnapshotDao = database.netWorthSnapshotDao()

    @Provides
    fun provideAIConversationDao(database: AppDatabase): AIConversationDao = database.aiConversationDao()

    @Provides
    fun provideAppSettingsDao(database: AppDatabase): AppSettingsDao = database.appSettingsDao()

    @Provides
    @Singleton
    fun provideDataStore(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): DataStore<Preferences> {
        return context.dataStore
    }
}

package com.expensetracker.app.di

import com.expensetracker.app.domain.repository.TransactionRepository
import com.expensetracker.app.domain.repository.TransactionRepositoryImpl
import com.expensetracker.app.domain.repository.CategoryRepository
import com.expensetracker.app.domain.repository.CategoryRepositoryImpl
import com.expensetracker.app.domain.repository.AccountRepository
import com.expensetracker.app.domain.repository.AccountRepositoryImpl
import com.expensetracker.app.domain.repository.BudgetRepository
import com.expensetracker.app.domain.repository.BudgetRepositoryImpl
import com.expensetracker.app.domain.repository.SubscriptionRepository
import com.expensetracker.app.domain.repository.SubscriptionRepositoryImpl
import com.expensetracker.app.domain.repository.HoldingRepository
import com.expensetracker.app.domain.repository.HoldingRepositoryImpl
import com.expensetracker.app.domain.repository.HoldingSnapshotRepository
import com.expensetracker.app.domain.repository.HoldingSnapshotRepositoryImpl
import com.expensetracker.app.domain.repository.NetWorthRepository
import com.expensetracker.app.domain.repository.NetWorthRepositoryImpl
import com.expensetracker.app.domain.repository.AIConversationRepository
import com.expensetracker.app.domain.repository.AIConversationRepositoryImpl
import com.expensetracker.app.domain.repository.SettingsRepository
import com.expensetracker.app.domain.repository.SettingsRepositoryImpl
import com.expensetracker.app.domain.repository.RecurringRuleRepository
import com.expensetracker.app.domain.repository.RecurringRuleRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindBudgetRepository(impl: BudgetRepositoryImpl): BudgetRepository

    @Binds
    @Singleton
    abstract fun bindSubscriptionRepository(impl: SubscriptionRepositoryImpl): SubscriptionRepository

    @Binds
    @Singleton
    abstract fun bindHoldingRepository(impl: HoldingRepositoryImpl): HoldingRepository

    @Binds
    @Singleton
    abstract fun bindHoldingSnapshotRepository(impl: HoldingSnapshotRepositoryImpl): HoldingSnapshotRepository

    @Binds
    @Singleton
    abstract fun bindNetWorthRepository(impl: NetWorthRepositoryImpl): NetWorthRepository

    @Binds
    @Singleton
    abstract fun bindAIConversationRepository(impl: AIConversationRepositoryImpl): AIConversationRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindRecurringRuleRepository(impl: RecurringRuleRepositoryImpl): RecurringRuleRepository
}

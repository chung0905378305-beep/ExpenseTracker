package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.toModel
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.AccountRole
import com.expensetracker.app.domain.model.Holding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AssetsUiState(
    val accounts: List<Account> = emptyList(),
    val holdings: List<Holding> = emptyList(),
    val netWorth: Double = 0.0,
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class AssetsViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val accountDao = database.accountDao()
    private val holdingDao = database.holdingDao()

    private val _uiState = MutableStateFlow(AssetsUiState())
    val uiState: StateFlow<AssetsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            accountDao.getAll().collect { entities ->
                _uiState.update { it.copy(accounts = entities.map { e -> e.toModel() }) }
            }

            holdingDao.getAll().collect { entities ->
                _uiState.update { it.copy(holdings = entities.map { e -> e.toModel() }) }
            }

            calculateNetWorth()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun refreshQuotes() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            loadAll()
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun calculateNetWorth() {
        viewModelScope.launch {
            val state = _uiState.value
            val holdingsValue = state.holdings.sumOf { it.quantity * it.currentPrice }
            val totalAssets = state.accounts
                .filter { it.role == AccountRole.ASSET && !it.excludeFromNetWorth }
                .sumOf { it.initialBalance } + holdingsValue
            val totalLiabilities = state.accounts
                .filter { it.role == AccountRole.LIABILITY && !it.excludeFromNetWorth }
                .sumOf { it.initialBalance }
            val netWorth = totalAssets - totalLiabilities

            _uiState.update {
                it.copy(netWorth = netWorth, totalAssets = totalAssets, totalLiabilities = totalLiabilities)
            }
        }
    }
}

package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.toModel
import com.expensetracker.app.domain.model.Transaction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val selectedMonth: String? = null
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val transactionDao = database.transactionDao()

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            transactionDao.getAll().collect { entities ->
                val domainList = entities.map { it.toModel() }
                _uiState.update { state ->
                    state.copy(
                        transactions = domainList,
                        filteredTransactions = applyFilter(domainList, state.searchQuery, state.selectedMonth),
                        isLoading = false
                    )
                }
            }
        }
    }

    fun search(query: String) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    searchQuery = query,
                    filteredTransactions = applyFilter(state.transactions, query, state.selectedMonth)
                )
            }
        }
    }

    fun filterByMonth(monthKey: String?) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    selectedMonth = monthKey,
                    filteredTransactions = applyFilter(state.transactions, state.searchQuery, monthKey)
                )
            }
        }
    }

    fun softDelete(id: Long) {
        viewModelScope.launch {
            val entity = transactionDao.getById(id) ?: return@launch
            transactionDao.update(entity.copy(isDeleted = true))
        }
    }

    fun markReviewed(id: Long) {
        viewModelScope.launch {
            val entity = transactionDao.getById(id) ?: return@launch
            transactionDao.update(entity.copy(needsReview = false))
        }
    }

    private fun applyFilter(
        transactions: List<Transaction>,
        query: String,
        monthKey: String?
    ): List<Transaction> {
        var result = transactions
        if (!query.isNullOrBlank()) {
            result = result.filter {
                it.note.contains(query, ignoreCase = true) ||
                it.source.contains(query, ignoreCase = true)
            }
        }
        if (!monthKey.isNullOrBlank()) {
            result = result.filter { it.monthKey == monthKey }
        }
        return result
    }
}

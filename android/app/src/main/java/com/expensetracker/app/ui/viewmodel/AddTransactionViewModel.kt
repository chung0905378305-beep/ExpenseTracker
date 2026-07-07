package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.toEntity
import com.expensetracker.app.data.local.entity.toModel
import com.expensetracker.app.domain.model.Account
import com.expensetracker.app.domain.model.Category
import com.expensetracker.app.domain.model.Tag
import com.expensetracker.app.domain.model.TransactionKind
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddTransactionUiState(
    val amount: Double = 0.0,
    val kind: TransactionKind = TransactionKind.EXPENSE,
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val toAccount: Account? = null,
    val date: Long = System.currentTimeMillis(),
    val note: String = "",
    val selectedTags: List<Tag> = emptyList(),
    val typeAheadCategories: List<Category> = emptyList(),
    val availableCategories: List<Category> = emptyList(),
    val availableAccounts: List<Account> = emptyList(),
    val availableTags: List<Tag> = emptyList()
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val transactionDao = database.transactionDao()
    private val categoryDao = database.categoryDao()
    private val accountDao = database.accountDao()
    private val tagDao = database.tagDao()

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
        loadAccounts()
        loadTags()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryDao.getAll().collect { entities ->
                _uiState.update { it.copy(
                    availableCategories = entities.map { e -> e.toModel() },
                    typeAheadCategories = entities.map { e -> e.toModel() }
                )}
            }
        }
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            accountDao.getAll().collect { entities ->
                _uiState.update { it.copy(availableAccounts = entities.map { e -> e.toModel() }) }
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            tagDao.getAll().collect { entities ->
                _uiState.update { it.copy(availableTags = entities.map { e -> e.toModel() }) }
            }
        }
    }

    fun updateAmount(amount: Double) { _uiState.update { it.copy(amount = amount) } }
    fun updateKind(kind: TransactionKind) { _uiState.update { it.copy(kind = kind) } }
    fun updateCategory(category: Category) { _uiState.update { it.copy(selectedCategory = category) } }
    fun updateAccount(account: Account) { _uiState.update { it.copy(selectedAccount = account) } }
    fun updateToAccount(account: Account) { _uiState.update { it.copy(toAccount = account) } }
    fun updateDate(date: Long) { _uiState.update { it.copy(date = date) } }
    fun updateNote(note: String) { _uiState.update { it.copy(note = note) } }

    fun toggleTag(tag: Tag) {
        _uiState.update { state ->
            val tags = state.selectedTags.toMutableList()
            if (tags.any { it.id == tag.id }) tags.removeAll { it.id == tag.id }
            else tags.add(tag)
            state.copy(selectedTags = tags)
        }
    }

    fun typeAheadCategory(query: String) {
        val categories = _uiState.value.availableCategories
        val filtered = if (query.isBlank()) categories
        else categories.filter { it.name.contains(query, ignoreCase = true) || it.keywords.any { kw -> kw.contains(query, ignoreCase = true) } }
        _uiState.update { it.copy(typeAheadCategories = filtered) }
    }

    fun reset() {
        _uiState.update {
            AddTransactionUiState(
                availableCategories = it.availableCategories,
                availableAccounts = it.availableAccounts,
                availableTags = it.availableTags
            )
        }
    }

    fun save() {
        viewModelScope.launch {
            val state = _uiState.value
            val model = com.expensetracker.app.domain.model.Transaction(
                kind = state.kind,
                amount = state.amount,
                categoryId = state.selectedCategory?.id,
                accountId = state.selectedAccount?.id,
                toAccountId = state.toAccount?.id,
                date = state.date,
                note = state.note,
                tagIds = state.selectedTags.map { it.id }
            )
            transactionDao.insert(model.toEntity())
        }
    }

    fun saveEdit(id: Long) {
        viewModelScope.launch {
            val state = _uiState.value
            val existing = transactionDao.getById(id) ?: return@launch
            val updated = existing.copy(
                kind = state.kind.name,
                amount = state.amount,
                categoryId = state.selectedCategory?.id,
                accountId = state.selectedAccount?.id,
                toAccountId = state.toAccount?.id,
                date = state.date,
                note = state.note,
                tagIds = state.selectedTags.joinToString("||") { it.id.toString() }
            )
            transactionDao.update(updated)
        }
    }

    fun loadForEdit(id: Long) {
        viewModelScope.launch {
            val model = transactionDao.getById(id)?.toModel() ?: return@launch
            _uiState.update { state ->
                state.copy(
                    amount = model.amount,
                    kind = model.kind,
                    selectedCategory = state.availableCategories.find { it.id == model.categoryId },
                    selectedAccount = state.availableAccounts.find { it.id == model.accountId },
                    toAccount = state.availableAccounts.find { it.id == model.toAccountId },
                    date = model.date,
                    note = model.note,
                    selectedTags = model.tagIds.mapNotNull { tagId -> state.availableTags.find { it.id == tagId } }
                )
            }
        }
    }
}

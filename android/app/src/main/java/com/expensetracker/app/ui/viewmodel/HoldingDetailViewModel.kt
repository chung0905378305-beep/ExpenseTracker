package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.toEntity
import com.expensetracker.app.data.local.entity.toModel
import com.expensetracker.app.domain.model.Holding
import com.expensetracker.app.domain.model.HoldingSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class SnapshotsTimeRange { WEEK, MONTH, QUARTER, YEAR, ALL }

data class HoldingDetailUiState(
    val holding: Holding? = null,
    val snapshots: List<HoldingSnapshot> = emptyList(),
    val timeRange: SnapshotsTimeRange = SnapshotsTimeRange.MONTH,
    val isLoading: Boolean = true
)

@HiltViewModel
class HoldingDetailViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val holdingDao = database.holdingDao()
    private val holdingSnapshotDao = database.holdingSnapshotDao()

    private val _uiState = MutableStateFlow(HoldingDetailUiState())
    val uiState: StateFlow<HoldingDetailUiState> = _uiState.asStateFlow()

    fun load(holdingId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val entity = holdingDao.getById(holdingId)
            if (entity != null) {
                _uiState.update { it.copy(holding = entity.toModel(), isLoading = false) }
                loadSnapshots(SnapshotsTimeRange.MONTH)
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun loadSnapshots(range: SnapshotsTimeRange) {
        viewModelScope.launch {
            _uiState.update { it.copy(timeRange = range) }
            val holdingId = _uiState.value.holding?.id ?: return@launch

            val calendar = Calendar.getInstance()
            val now = calendar.timeInMillis
            val start = when (range) {
                SnapshotsTimeRange.WEEK -> {
                    calendar.add(Calendar.DAY_OF_YEAR, -7)
                    calendar.timeInMillis
                }
                SnapshotsTimeRange.MONTH -> {
                    calendar.add(Calendar.MONTH, -1)
                    calendar.timeInMillis
                }
                SnapshotsTimeRange.QUARTER -> {
                    calendar.add(Calendar.MONTH, -3)
                    calendar.timeInMillis
                }
                SnapshotsTimeRange.YEAR -> {
                    calendar.add(Calendar.YEAR, -1)
                    calendar.timeInMillis
                }
                SnapshotsTimeRange.ALL -> 0L
            }

            holdingSnapshotDao.getByHoldingAndRange(holdingId, start, now).let { flow ->
                flow.collect { entities ->
                    val snapshots = entities.map {
                        HoldingSnapshot(
                            id = it.id, holdingId = it.holdingId, date = it.date,
                            quantity = it.quantity, marketValue = it.marketValue,
                            costValue = it.costValue, unrealizedPnl = it.unrealizedPnl
                        )
                    }
                    _uiState.update { it.copy(snapshots = snapshots) }
                }
            }
        }
    }

    fun sell(quantity: Double, price: Double) {
        viewModelScope.launch {
            val holding = _uiState.value.holding ?: return@launch
            val realizedPnl = (price - holding.costPrice) * quantity
            val updated = holding.copy(
                quantity = holding.quantity - quantity,
                currentPrice = price,
                priceUpdatedAt = System.currentTimeMillis(),
                realizedPnl = holding.realizedPnl + realizedPnl
            )
            holdingDao.update(updated.toEntity())
            load(holding.id)
        }
    }
}

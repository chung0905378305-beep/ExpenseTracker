package com.expensetracker.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.expensetracker.app.data.local.database.AppDatabase
import com.expensetracker.app.data.local.entity.toModel
import com.expensetracker.app.data.local.entity.toEntity
import com.expensetracker.app.domain.model.AppSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val settings: AppSettings = AppSettings(),
    val memberInfo: MemberInfo? = null,
    val appLockState: Boolean = false,
    val isLoading: Boolean = true
)

data class MemberInfo(
    val isMember: Boolean = false,
    val nickname: String = "",
    val avatar: String = "",
    val expiryDate: String = ""
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val database: AppDatabase
) : ViewModel() {

    private val appSettingsDao = database.appSettingsDao()

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    fun loadSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val entity = appSettingsDao.getSettings()
            val settings = entity?.toModel() ?: AppSettings()
            _uiState.update {
                it.copy(settings = settings, appLockState = settings.appLockEnabled, isLoading = false)
            }
        }
    }

    fun updateSetting(update: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            val current = _uiState.value.settings
            val updated = update(current)
            _uiState.update { it.copy(settings = updated) }
            appSettingsDao.insert(updated.toEntity())
        }
    }

    fun verifyMembership() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    memberInfo = MemberInfo(
                        isMember = false,
                        nickname = "未登录",
                        avatar = "",
                        expiryDate = ""
                    )
                )
            }
        }
    }

    fun activateCode(code: String) {
        viewModelScope.launch {
            // Placeholder for code activation
        }
    }
}

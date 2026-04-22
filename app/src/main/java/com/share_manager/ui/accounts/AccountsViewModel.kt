package com.share_manager.ui.accounts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.share_manager.data.model.Account
import com.share_manager.data.repository.MeroShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountsUiState(
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val repository: MeroShareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState: StateFlow<AccountsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.accounts.collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun saveAccount(account: Account) = viewModelScope.launch {
        runCatching { repository.saveAccount(account) }
            .onSuccess { _uiState.update { it.copy(successMessage = "Account saved!") } }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Save failed") } }
    }

    fun updateAccount(account: Account) = viewModelScope.launch {
        runCatching { repository.updateAccount(account) }
            .onSuccess { _uiState.update { it.copy(successMessage = "Account updated!") } }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Update failed") } }
    }

    fun deleteAccount(account: Account) = viewModelScope.launch {
        runCatching { repository.deleteAccount(account) }
            .onSuccess { _uiState.update { it.copy(successMessage = "Account deleted") } }
            .onFailure { e -> _uiState.update { it.copy(errorMessage = e.message ?: "Delete failed") } }
    }

    fun clearMessage() = _uiState.update { it.copy(successMessage = null, errorMessage = null) }
}

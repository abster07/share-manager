package com.share_manager.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.share_manager.data.model.Account
import com.share_manager.data.model.ReportItem
import com.share_manager.data.repository.MeroShareRepository
import com.share_manager.data.repository.MeroShareUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ReportsUiState(
    val accounts: List<Account> = emptyList(),
    val reportsByAccount: Map<String, List<ReportItem>> = emptyMap(),
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val errorMessage: String? = null
)

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: MeroShareRepository,
    private val userRepository: MeroShareUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.accounts.collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    /**
     * Mirrors Python: for account in accounts → user.generate_reports()
     */
    fun loadReports() {
        val accounts = _uiState.value.accounts
        if (accounts.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, reportsByAccount = emptyMap(), progress = 0f) }

            val results = mutableMapOf<String, List<ReportItem>>()

            accounts.forEachIndexed { index, account ->
                userRepository.generateReports(account)
                    .onSuccess { items -> results[account.name] = items }
                    .onFailure { e ->
                        results[account.name] = emptyList()
                        _uiState.update { it.copy(errorMessage = "${account.name}: ${e.message}") }
                    }

                _uiState.update { state ->
                    state.copy(
                        reportsByAccount = results.toMap(),
                        progress = (index + 1).toFloat() / accounts.size
                    )
                }

                if (index < accounts.size - 1) delay(500)
            }

            _uiState.update { it.copy(isLoading = false, progress = 1f) }
        }
    }
}

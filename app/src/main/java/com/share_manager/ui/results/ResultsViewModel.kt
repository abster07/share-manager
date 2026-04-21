package com.share_manager.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.share_manager.data.model.*
import com.share_manager.data.repository.MeroShareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ResultsUiState(
    val companies: List<CompanyShare> = emptyList(),
    val selectedCompany: CompanyShare? = null,
    val accountResults: List<AccountResult> = emptyList(),
    val isLoadingCompanies: Boolean = false,
    val isChecking: Boolean = false,
    val progress: Float = 0f,
    val errorMessage: String? = null,
    val accounts: List<Account> = emptyList(),
    val debugLog: String = "",         // live-updated debug log
    val showDebugLog: Boolean = false  // toggle visibility
)

@HiltViewModel
class ResultsViewModel @Inject constructor(
    private val repository: MeroShareRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ResultsUiState())
    val uiState: StateFlow<ResultsUiState> = _uiState.asStateFlow()

    private var checkJob: Job? = null

    init {
        loadCompanies()
        viewModelScope.launch {
            repository.accounts.collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun loadCompanies() = viewModelScope.launch {
        _uiState.update { it.copy(isLoadingCompanies = true, errorMessage = null) }
        repository.getCompanyShares()
            .onSuccess { companies ->
                _uiState.update {
                    it.copy(
                        companies = companies,
                        isLoadingCompanies = false,
                        debugLog = repository.debugLog
                    )
                }
            }
            .onFailure { e ->
                _uiState.update {
                    it.copy(
                        isLoadingCompanies = false,
                        errorMessage = e.message,
                        debugLog = repository.debugLog
                    )
                }
            }
    }

    fun selectCompany(company: CompanyShare) {
        _uiState.update { it.copy(selectedCompany = company, accountResults = emptyList()) }
    }

    fun checkBulkResults() {
        val company = _uiState.value.selectedCompany ?: return
        val accounts = _uiState.value.accounts
        if (accounts.isEmpty()) return

        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            val initial = accounts.map { AccountResult(it, ResultStatus.Loading) }
            _uiState.update {
                it.copy(isChecking = true, accountResults = initial, progress = 0f)
            }

            accounts.forEachIndexed { index, account ->
                val isFE = company.name.contains("Foreign Employment", ignoreCase = true)
                val status = if (account.isForeignEmployment && !isFE) {
                    ResultStatus.NotAllotted("Skipped — Not a Foreign Employment IPO")
                } else if (!account.isForeignEmployment && isFE) {
                    ResultStatus.NotAllotted("Skipped — Foreign Employment IPO only")
                } else {
                    repository.checkResult(company.id, account.boid)
                }

                _uiState.update { state ->
                    val updated = state.accountResults.toMutableList()
                    updated[index] = AccountResult(account, status)
                    state.copy(
                        accountResults = updated,
                        progress = (index + 1).toFloat() / accounts.size,
                        debugLog = repository.debugLog
                    )
                }

                if (index < accounts.size - 1) delay(500)
            }

            _uiState.update { it.copy(isChecking = false, progress = 1f, debugLog = repository.debugLog) }
        }
    }

    fun cancelCheck() {
        checkJob?.cancel()
        _uiState.update { state ->
            val updated = state.accountResults.map {
                if (it.status is ResultStatus.Loading)
                    it.copy(status = ResultStatus.Error("Cancelled"))
                else it
            }
            state.copy(isChecking = false, accountResults = updated)
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun toggleDebugLog() = _uiState.update { it.copy(showDebugLog = !it.showDebugLog) }

    fun clearDebugLog() {
        repository.clearLog()
        _uiState.update { it.copy(debugLog = "") }
    }
}

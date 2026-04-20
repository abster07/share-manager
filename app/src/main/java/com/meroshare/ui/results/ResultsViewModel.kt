package com.meroshare.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meroshare.data.model.*
import com.meroshare.data.repository.MeroShareRepository
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
    val progress: Float = 0f,        // 0..1
    val errorMessage: String? = null,
    val accounts: List<Account> = emptyList()
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
                _uiState.update { it.copy(companies = companies, isLoadingCompanies = false) }
            }
            .onFailure { e ->
                _uiState.update { it.copy(isLoadingCompanies = false, errorMessage = e.message) }
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
            // Initialise all as loading
            val initial = accounts.map { AccountResult(it, ResultStatus.Loading) }
            _uiState.update {
                it.copy(
                    isChecking = true,
                    accountResults = initial,
                    progress = 0f
                )
            }

            accounts.forEachIndexed { index, account ->
                // Skip foreign-employment accounts if company is not FE
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
                        progress = (index + 1).toFloat() / accounts.size
                    )
                }

                // Small delay to avoid rate limiting
                if (index < accounts.size - 1) delay(500)
            }

            _uiState.update { it.copy(isChecking = false, progress = 1f) }
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
}

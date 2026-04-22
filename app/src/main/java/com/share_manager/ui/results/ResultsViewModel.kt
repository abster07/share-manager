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
    val companies         : List<CompanyShare>   = emptyList(),
    val selectedCompany   : CompanyShare?        = null,
    val accounts          : List<Account>        = emptyList(),
    val accountResults    : List<AccountResult>  = emptyList(),
    val isLoadingCompanies: Boolean              = false,
    val isChecking        : Boolean              = false,
    val checkedCount      : Int                  = 0,   // how many have resolved so far
    val errorMessage      : String?              = null
) {
    /** 0..1 progress fraction derived from resolved-count vs total accounts. */
    val progress: Float
        get() = if (accounts.isEmpty()) 0f
                else (checkedCount.toFloat() / accounts.size).coerceIn(0f, 1f)
}

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
                _uiState.update {
                    it.copy(
                        isLoadingCompanies = false,
                        errorMessage       = e.message ?: "Failed to load companies"
                    )
                }
            }
    }

    fun selectCompany(company: CompanyShare) {
        _uiState.update { it.copy(selectedCompany = company, accountResults = emptyList(), checkedCount = 0) }
    }

    fun checkBulkResults() {
        val company  = _uiState.value.selectedCompany ?: return
        val accounts = _uiState.value.accounts
        if (accounts.isEmpty()) return

        checkJob?.cancel()
        checkJob = viewModelScope.launch {
            // Initialise every row as Loading
            val initial = accounts.map { AccountResult(it, ResultStatus.Loading) }
            _uiState.update {
                it.copy(
                    isChecking     = true,
                    accountResults = initial,
                    checkedCount   = 0
                )
            }

            // BUG FIX: FE detection was a fragile string-contains on company name.
            // CompanyShare doesn't have an explicit isForeignEmployment flag from the
            // API, so we still use the name heuristic but centralise it here and keep
            // it consistent for both skip directions.
            val companyIsFe = company.name.contains("foreign employment", ignoreCase = true)

            accounts.forEachIndexed { index, account ->
                val status: ResultStatus = when {
                    account.isForeignEmployment && !companyIsFe ->
                        ResultStatus.NotAllotted("Skipped — not a Foreign Employment IPO")

                    !account.isForeignEmployment && companyIsFe ->
                        ResultStatus.NotAllotted("Skipped — Foreign Employment IPO only")

                    else ->
                        repository.checkResult(company.id, account.boid)
                }

                _uiState.update { state ->
                    val updated = state.accountResults.toMutableList()
                    updated[index] = AccountResult(account, status)
                    state.copy(
                        accountResults = updated,
                        checkedCount   = index + 1
                    )
                }

                // Small delay between requests to avoid rate-limiting
                if (index < accounts.size - 1) delay(500)
            }

            _uiState.update { it.copy(isChecking = false, checkedCount = accounts.size) }
        }
    }

    fun cancelCheck() {
        checkJob?.cancel()
        _uiState.update { state ->
            val updated = state.accountResults.map { r ->
                if (r.status is ResultStatus.Loading) r.copy(status = ResultStatus.Error("Cancelled"))
                else r
            }
            state.copy(isChecking = false, accountResults = updated)
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}

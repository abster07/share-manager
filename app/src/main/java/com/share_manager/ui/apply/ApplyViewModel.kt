package com.share_manager.ui.apply

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.share_manager.data.model.*
import com.share_manager.data.repository.MeroShareRepository
import com.share_manager.data.repository.MeroShareUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class ApplyUiState(
    val accounts: List<Account> = emptyList(),
    val applyResults: List<AccountApplyResult> = emptyList(),
    val isApplying: Boolean = false,
    val progress: Float = 0f,
    val companyShareId: Int? = null,
    val numberOfShares: Int = 10,
    val errorMessage: String? = null
)

@HiltViewModel
class ApplyViewModel @Inject constructor(
    private val repository: MeroShareRepository,
    private val userRepository: MeroShareUserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ApplyUiState())
    val uiState: StateFlow<ApplyUiState> = _uiState.asStateFlow()

    private var applyJob: Job? = null

    init {
        viewModelScope.launch {
            repository.accounts.collect { accounts ->
                _uiState.update { it.copy(accounts = accounts) }
            }
        }
    }

    fun setCompanyShareId(id: Int) = _uiState.update { it.copy(companyShareId = id) }
    fun setNumberOfShares(n: Int) = _uiState.update { it.copy(numberOfShares = n) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    /**
     * Bulk apply — mirrors the Python __main__ apply block for all accounts.
     */
    fun applyAll() {
        val companyShareId = _uiState.value.companyShareId ?: run {
            _uiState.update { it.copy(errorMessage = "Set a Company Share ID first") }
            return
        }
        val accounts = _uiState.value.accounts
        if (accounts.isEmpty()) return

        applyJob?.cancel()
        applyJob = viewModelScope.launch {
            val initial = accounts.map { AccountApplyResult(it, ApplyStatus.Loading) }
            _uiState.update { it.copy(isApplying = true, applyResults = initial, progress = 0f) }

            accounts.forEachIndexed { index, account ->
                val status = userRepository.applyForAccount(
                    account = account,
                    companyShareId = companyShareId,
                    numberOfShares = _uiState.value.numberOfShares
                )

                _uiState.update { state ->
                    val updated = state.applyResults.toMutableList()
                    updated[index] = AccountApplyResult(account, status)
                    state.copy(
                        applyResults = updated,
                        progress = (index + 1).toFloat() / accounts.size
                    )
                }

                // Avoid hammering the server — mirrors Python's implicit sequential execution
                if (index < accounts.size - 1) delay(800)
            }

            _uiState.update { it.copy(isApplying = false, progress = 1f) }
        }
    }

    fun cancelApply() {
        applyJob?.cancel()
        _uiState.update { state ->
            val updated = state.applyResults.map {
                if (it.status is ApplyStatus.Loading)
                    it.copy(status = ApplyStatus.Failed("Cancelled"))
                else it
            }
            state.copy(isApplying = false, applyResults = updated)
        }
    }
}

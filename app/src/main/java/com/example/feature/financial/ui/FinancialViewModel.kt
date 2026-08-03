package com.example.feature.financial.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.financial.domain.model.FinancialDashboard
import com.example.feature.financial.domain.repository.FinancialRepository
import com.example.feature.financial.data.repository.FinancialRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FinancialUiState {
    object Loading : FinancialUiState
    data class Success(val data: FinancialDashboard) : FinancialUiState
    data class Error(val message: String) : FinancialUiState
}

class FinancialViewModel(
    private val repository: FinancialRepository = FinancialRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<FinancialUiState>(FinancialUiState.Loading)
    val uiState: StateFlow<FinancialUiState> = _uiState.asStateFlow()

    init {
        loadFinancialData()
    }

    private fun loadFinancialData() {
        viewModelScope.launch {
            _uiState.value = FinancialUiState.Loading
            repository.getFinancialDashboard().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = FinancialUiState.Success(data)
                    },
                    onFailure = { error ->
                        _uiState.value = FinancialUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

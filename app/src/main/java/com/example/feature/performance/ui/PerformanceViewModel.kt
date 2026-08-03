package com.example.feature.performance.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.performance.domain.model.PerformanceDashboard
import com.example.feature.performance.domain.repository.PerformanceRepository
import com.example.feature.performance.data.repository.PerformanceRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PerformanceUiState {
    object Loading : PerformanceUiState
    data class Success(val data: PerformanceDashboard) : PerformanceUiState
    data class Error(val message: String) : PerformanceUiState
}

class PerformanceViewModel(
    private val repository: PerformanceRepository = PerformanceRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<PerformanceUiState>(PerformanceUiState.Loading)
    val uiState: StateFlow<PerformanceUiState> = _uiState.asStateFlow()

    init {
        loadPerformanceData()
    }

    private fun loadPerformanceData() {
        viewModelScope.launch {
            _uiState.value = PerformanceUiState.Loading
            repository.getPerformanceDashboard().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = PerformanceUiState.Success(data)
                    },
                    onFailure = { error ->
                        _uiState.value = PerformanceUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

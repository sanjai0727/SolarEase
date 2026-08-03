package com.example.feature.dashboard.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.dashboard.domain.model.ActivityItem
import com.example.feature.dashboard.domain.model.CustomerDashboard
import com.example.feature.dashboard.domain.model.NotificationItem
import com.example.feature.dashboard.domain.repository.DashboardRepository
import com.example.feature.dashboard.data.repository.DashboardRepositoryImpl
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    object Loading : DashboardUiState
    data class Success(val dashboardData: CustomerDashboard) : DashboardUiState
    data class Empty(val message: String) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

class DashboardViewModel(
    private val dashboardRepository: DashboardRepository = DashboardRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<DashboardUiState>(DashboardUiState.Loading)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            _uiState.value = DashboardUiState.Loading
            dashboardRepository.getDashboardData().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = DashboardUiState.Success(data)
                    },
                    onFailure = { error ->
                        _uiState.value = DashboardUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

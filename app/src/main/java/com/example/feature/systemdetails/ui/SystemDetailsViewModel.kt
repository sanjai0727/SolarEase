package com.example.feature.systemdetails.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.systemdetails.domain.model.SolarSystemDetails
import com.example.feature.systemdetails.domain.repository.SystemDetailsRepository
import com.example.feature.systemdetails.data.repository.SystemDetailsRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SystemDetailsUiState {
    object Loading : SystemDetailsUiState
    data class Success(val details: SolarSystemDetails) : SystemDetailsUiState
    data class Error(val message: String) : SystemDetailsUiState
}

class SystemDetailsViewModel(
    private val repository: SystemDetailsRepository = SystemDetailsRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<SystemDetailsUiState>(SystemDetailsUiState.Loading)
    val uiState: StateFlow<SystemDetailsUiState> = _uiState.asStateFlow()

    init {
        loadSystemDetails()
    }

    private fun loadSystemDetails() {
        viewModelScope.launch {
            _uiState.value = SystemDetailsUiState.Loading
            repository.getSystemDetails().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = SystemDetailsUiState.Success(data)
                    },
                    onFailure = { error ->
                        _uiState.value = SystemDetailsUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

package com.example.feature.environmental.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.environmental.domain.model.EnvironmentalDashboard
import com.example.feature.environmental.domain.repository.EnvironmentalRepository
import com.example.feature.environmental.data.repository.EnvironmentalRepositoryImpl
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface EnvironmentalUiState {
    object Loading : EnvironmentalUiState
    data class Success(val data: EnvironmentalDashboard) : EnvironmentalUiState
    data class Error(val message: String) : EnvironmentalUiState
}

class EnvironmentalViewModel(
    private val repository: EnvironmentalRepository = EnvironmentalRepositoryImpl()
) : ViewModel() {
    private val _uiState = MutableStateFlow<EnvironmentalUiState>(EnvironmentalUiState.Loading)
    val uiState: StateFlow<EnvironmentalUiState> = _uiState.asStateFlow()

    init {
        loadEnvironmentalData()
    }

    private fun loadEnvironmentalData() {
        viewModelScope.launch {
            _uiState.value = EnvironmentalUiState.Loading
            repository.getEnvironmentalDashboard().collect { result ->
                result.fold(
                    onSuccess = { data ->
                        _uiState.value = EnvironmentalUiState.Success(data)
                    },
                    onFailure = { error ->
                        _uiState.value = EnvironmentalUiState.Error(error.message ?: "Unknown error")
                    }
                )
            }
        }
    }
}

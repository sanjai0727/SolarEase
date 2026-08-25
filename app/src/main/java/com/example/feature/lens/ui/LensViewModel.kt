package com.example.feature.lens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.utils.DetectedAppliance
import com.example.feature.lens.utils.PowerEstimation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LensUiState {
    object Scanning : LensUiState()
    data class ConfirmingAppliance(
        val applianceName: String,
        val applianceType: String,
        val powerRatingWatts: Float,
        val dailyUsageHours: Float = 0f,
        val quantity: Int = 1
    ) : LensUiState()
    data class Saving(val appliance: ApplianceCreateRequest) : LensUiState()
    data class Result(val appliance: Appliance) : LensUiState()
    data class Error(val message: String) : LensUiState()
}

class LensViewModel(private val repository: LensRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LensUiState>(LensUiState.Scanning)
    val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

    private val _currentDetection = MutableStateFlow<DetectedAppliance?>(null)
    val currentDetection: StateFlow<DetectedAppliance?> = _currentDetection.asStateFlow()

    fun updateDetection(detection: DetectedAppliance?) {
        if (_uiState.value is LensUiState.Scanning) {
            _currentDetection.value = detection
        }
    }

    fun selectAppliance(detection: DetectedAppliance) {
        _uiState.value = LensUiState.ConfirmingAppliance(
            applianceName = "My ${detection.label}",
            applianceType = detection.label,
            powerRatingWatts = detection.powerWatts
        )
    }

    fun updateApplianceForm(
        name: String,
        type: String,
        power: Float,
        hours: Float,
        quantity: Int
    ) {
        val currentState = _uiState.value
        if (currentState is LensUiState.ConfirmingAppliance) {
            _uiState.value = currentState.copy(
                applianceName = name,
                applianceType = type,
                powerRatingWatts = power,
                dailyUsageHours = hours,
                quantity = quantity
            )
        }
    }

    fun saveAppliance() {
        val currentState = _uiState.value
        if (currentState is LensUiState.ConfirmingAppliance) {
            val dailyConsumption = (currentState.powerRatingWatts / 1000f) * currentState.dailyUsageHours * currentState.quantity
            val monthlyConsumption = dailyConsumption * 30f
            // Assume simple tariff for estimation, e.g., ₹5 per unit (configurable later)
            val estimatedCost = monthlyConsumption * 5.0f

            val request = ApplianceCreateRequest(
                applianceName = currentState.applianceName,
                applianceType = currentState.applianceType,
                powerRatingWatts = currentState.powerRatingWatts,
                dailyUsageHours = currentState.dailyUsageHours,
                quantity = currentState.quantity,
                monthlyConsumptionKwh = monthlyConsumption,
                estimatedMonthlyCost = estimatedCost
            )

            _uiState.value = LensUiState.Saving(request)

            viewModelScope.launch {
                val result = repository.createAppliance(request)
                result.onSuccess { appliance ->
                    _uiState.value = LensUiState.Result(appliance)
                }.onFailure { e ->
                    _uiState.value = LensUiState.Error(e.message ?: "Failed to save appliance")
                }
            }
        }
    }

    fun restartScanning() {
        _currentDetection.value = null
        _uiState.value = LensUiState.Scanning
    }
}

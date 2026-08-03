package com.example.feature.assessment.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import com.example.feature.assessment.util.EstimateCalculator
import com.example.feature.assessment.util.Validation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SiteDetailsUiState(
    val monthlyConsumption: String = "",
    val consumptionError: String? = null,
    val availableArea: String = "",
    val areaError: String? = null,
    val mountType: String = "Rooftop",
    val isPhotoCaptured: Boolean = false
) {
    val isFormValid: Boolean
        get() = monthlyConsumption.isNotBlank() &&
                availableArea.isNotBlank() &&
                consumptionError == null &&
                areaError == null &&
                mountType.isNotBlank() &&
                isPhotoCaptured
}

data class RecommendationResult(
    val monthlyConsumption: Double,
    val availableArea: Double,
    val mountType: String,
    val systemType: String,
    val customerType: String,
    val recommendedSystemSizeKw: Double,
    val requiredAreaSqM: Double,
    val isFeasible: Boolean
)

class SiteDetailsViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SiteDetailsUiState(
            monthlyConsumption = savedStateHandle.get<String>("monthlyConsumption") ?: "",
            availableArea = savedStateHandle.get<String>("availableArea") ?: "",
            mountType = savedStateHandle.get<String>("mountType") ?: "Rooftop",
            isPhotoCaptured = savedStateHandle.get<Boolean>("isPhotoCaptured") ?: false
        )
    )

    val uiState: StateFlow<SiteDetailsUiState> = _uiState.asStateFlow()

    fun updateMonthlyConsumption(input: String) {
        val filtered = input.filter { it.isDigit() }.take(5)
        var error: String? = null
        if (filtered.isEmpty()) {
            error = "Please enter your monthly electricity consumption."
        } else if (!Validation.isValidConsumption(filtered)) {
            error = "Consumption must be greater than 0."
        }
        
        savedStateHandle["monthlyConsumption"] = filtered
        _uiState.update {
            it.copy(
                monthlyConsumption = filtered,
                consumptionError = error
            )
        }
    }

    fun updateAvailableArea(input: String) {
        val filtered = input.filter { it.isDigit() || it == '.' }
        
        // Prevent multiple decimals
        val parts = filtered.split('.')
        val sanitized = if (parts.size > 2) {
            parts[0] + "." + parts.drop(1).joinToString("")
        } else {
            filtered
        }
        
        var error: String? = null
        if (sanitized.isEmpty()) {
            error = "Please enter the available installation area."
        } else if (!Validation.isValidArea(sanitized)) {
            val value = sanitized.toDoubleOrNull() ?: 0.0
            if (value <= 0) {
                error = "Area must be greater than 0."
            } else {
                error = "Area exceeds maximum limit."
            }
        }
        
        savedStateHandle["availableArea"] = sanitized
        _uiState.update {
            it.copy(
                availableArea = sanitized,
                areaError = error
            )
        }
    }

    fun updateMountType(type: String) {
        savedStateHandle["mountType"] = type
        _uiState.update {
            it.copy(mountType = type)
        }
    }

    fun updatePhotoCaptured(captured: Boolean) {
        savedStateHandle["isPhotoCaptured"] = captured
        _uiState.update {
            it.copy(isPhotoCaptured = captured)
        }
    }

    fun calculateEstimate(customerType: String, systemType: String): RecommendationResult? {
        val currentState = _uiState.value
        if (!currentState.isFormValid) return null

        val consumption = currentState.monthlyConsumption.toDoubleOrNull() ?: return null
        val area = currentState.availableArea.toDoubleOrNull() ?: return null
        
        val recommendedSize = EstimateCalculator.calculateRecommendedSystemSize(consumption)
        val requiredArea = EstimateCalculator.calculateRequiredArea(recommendedSize)
        
        return RecommendationResult(
            monthlyConsumption = consumption,
            availableArea = area,
            mountType = currentState.mountType,
            systemType = systemType,
            customerType = customerType,
            recommendedSystemSizeKw = recommendedSize,
            requiredAreaSqM = requiredArea,
            isFeasible = area >= requiredArea
        )
    }
}

package com.example.feature.assessment.ui

import androidx.lifecycle.ViewModel
import com.example.feature.assessment.util.EstimateCalculator
import com.example.feature.assessment.util.SolarEstimateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EstimateUiState(
    val result: SolarEstimateResult? = null
)

class EstimateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(EstimateUiState())
    val uiState: StateFlow<EstimateUiState> = _uiState.asStateFlow()

    fun calculateEstimate(
        monthlyConsumptionKwh: Double,
        availableAreaSqM: Double,
        mountType: String,
        systemType: String,
        panelType: String,
        customerType: String
    ) {
        val estimate = EstimateCalculator.calculateCostEstimate(
            monthlyConsumptionKwh = monthlyConsumptionKwh,
            availableAreaSqM = availableAreaSqM,
            mountType = mountType,
            systemType = systemType,
            panelType = panelType,
            customerType = customerType
        )
        
        _uiState.update { it.copy(result = estimate) }
    }
}

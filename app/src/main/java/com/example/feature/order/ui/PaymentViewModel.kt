package com.example.feature.order.ui

import androidx.lifecycle.ViewModel
import com.example.feature.assessment.util.SolarEstimateResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PaymentUiState(
    val estimateResult: SolarEstimateResult? = null,
    val paymentAmount: Double = 0.0,
    val selectedPaymentMethod: String? = "UPI",
    val error: String? = null
)

class PaymentViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(PaymentUiState())
    val uiState: StateFlow<PaymentUiState> = _uiState.asStateFlow()

    fun setEstimateResult(result: SolarEstimateResult?) {
        if (result == null) {
            _uiState.update { it.copy(error = "Estimate data is missing or invalid.") }
            return
        }
        _uiState.update {
            it.copy(
                estimateResult = result,
                paymentAmount = result.netPayable,
                error = null
            )
        }
    }

    fun selectPaymentMethod(method: String) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }
}

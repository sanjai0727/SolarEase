package com.example.feature.order.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class OrderConfirmationUiState(
    val orderReference: String = "",
    val paymentReferenceId: String = "",
    val recommendedCapacityKW: Double = 0.0,
    val systemType: String = "",
    val mountType: String = "",
    val paymentMethod: String = "",
    val paymentStatus: String = "Success",
    val orderStatus: String = "Confirmed",
    val grossTotal: Double = 0.0,
    val subsidyAmount: Double = 0.0,
    val netPayable: Double = 0.0,
    val orderDate: String = "",
    val surveyDate: String = "",
    val username: String = com.example.core.session.UserSession.currentUser?.username ?: "Valued Customer",
    val mobileNumber: String = com.example.core.session.UserSession.currentUser?.mobileNumber ?: "N/A",
    val installationAddress: String = "123 Solar Way, Green City",
    val panelType: String = ""
)

class OrderConfirmationViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(OrderConfirmationUiState())
    val uiState: StateFlow<OrderConfirmationUiState> = _uiState.asStateFlow()

    fun setOrderDetails(
        recommendedCapacityKW: Double,
        systemType: String,
        mountType: String,
        panelType: String,
        paymentMethod: String,
        grossTotal: Double,
        subsidyAmount: Double,
        netPayable: Double
    ) {
        val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(Date())
        val paymentRef = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
        val orderReference = "SOLAR-$timestamp"
        val orderDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
        
        // Add 1 day for survey
        val calendar = java.util.Calendar.getInstance()
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val surveyDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)
        
        _uiState.update {
            it.copy(
                orderReference = orderReference,
                paymentReferenceId = "PAY-$paymentRef",
                recommendedCapacityKW = recommendedCapacityKW,
                systemType = systemType,
                mountType = mountType,
                panelType = panelType,
                paymentMethod = paymentMethod,
                grossTotal = grossTotal,
                subsidyAmount = subsidyAmount,
                netPayable = netPayable,
                orderDate = orderDate,
                surveyDate = surveyDate
            )
        }
    }
}

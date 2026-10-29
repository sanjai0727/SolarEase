package com.example.feature.assessment.ui

import androidx.lifecycle.ViewModel
import com.example.core.session.UserSession
import com.example.feature.assessment.util.EstimateCalculator
import com.example.feature.ebservice.util.formatConsumerNumber
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

// ─────────────────────────────────────────────────────────────────────────────
// SHARED STATE & VIEWMODEL FOR THE 12-STEP SOLAR VENDOR JOURNEY
// ─────────────────────────────────────────────────────────────────────────────

data class SolarVendorState(
    // Step 1: Consumer Number
    val rawConsumerNumber: String = "09440233135",
    val formattedConsumerNumber: String = "09-440-233-135",
    val isConsumerValid: Boolean = true,

    // Step 2: Property Type
    val propertyType: String = "Home", // Home, Business

    // Step 3: Solar System Type
    val systemType: String = "On-Grid", // On-Grid, Off-Grid, Hybrid

    // Step 4: Monthly Consumption (kWh)
    val monthlyConsumptionKwh: Double = 350.0,

    // Step 5: Mount Type / Location
    val mountType: String = "Rooftop", // Rooftop, Ground

    // Step 6: Available Area (sq. m)
    val availableAreaSqM: Double = 30.0,
    val isSquareFeetMode: Boolean = false,

    // Step 7: Sizing Recommendation (Calculated)
    val recommendedCapacityKw: Double = 2.9,
    val requiredAreaSqM: Double = 30.0,
    val isFeasible: Boolean = true,

    // Step 8: Panel Type
    val panelType: String = "DCR Panels", // DCR Panels, Non-DCR Panels

    // Step 9: Nearby Vendors
    val selectedVendorId: String = "abc_solar",
    val selectedVendorName: String = "ABC Solar",

    // Step 11: Request Details
    val customerName: String = "Sanjai",
    val customerPhone: String = "+91 98401 23456",
    val requestNotes: String = "",

    // Step 12: Confirmation
    val referenceId: String = "REQ-SOL-2026-8841",
    val submissionTimestamp: String = "Today, 14:40 IST"
) {
    val docketId: String get() = referenceId
}

class SolarVendorViewModel : ViewModel() {
    private val _state = MutableStateFlow(SolarVendorState())
    val state: StateFlow<SolarVendorState> = _state.asStateFlow()

    init {
        val user = UserSession.currentUser
        val savedEb = user?.ebServiceNumber ?: UserSession.getSavedEbServiceNumber()
        val initialEb = if (!savedEb.isNullOrBlank()) savedEb.filter { it.isDigit() } else "09440233135"
        val name = if (!user?.name.isNullOrBlank()) user?.name!! else "Sanjai"
        val phone = if (!user?.mobileNumber.isNullOrBlank()) user?.mobileNumber!! else "+91 98401 23456"

        _state.update {
            it.copy(
                rawConsumerNumber = initialEb,
                formattedConsumerNumber = formatConsumerNumber(initialEb),
                isConsumerValid = initialEb.length in 10..12,
                customerName = name,
                customerPhone = phone
            )
        }
        recalculateSizing()
    }

    fun updateConsumerNumber(number: String) {
        val clean = number.filter { it.isDigit() }.take(12)
        _state.update {
            it.copy(
                rawConsumerNumber = clean,
                formattedConsumerNumber = formatConsumerNumber(clean),
                isConsumerValid = clean.length in 10..12
            )
        }
    }

    fun clearConsumerNumber() {
        _state.update {
            it.copy(
                rawConsumerNumber = "",
                formattedConsumerNumber = "",
                isConsumerValid = false
            )
        }
    }

    fun selectPropertyType(type: String) {
        _state.update { it.copy(propertyType = type) }
    }

    fun selectSystemType(type: String) {
        _state.update { it.copy(systemType = type) }
    }

    fun updateMonthlyConsumption(kwh: Double) {
        _state.update { it.copy(monthlyConsumptionKwh = kwh) }
        recalculateSizing()
    }

    fun selectMountType(mount: String) {
        _state.update { it.copy(mountType = mount) }
    }

    fun updateAvailableArea(areaSqM: Double) {
        _state.update { it.copy(availableAreaSqM = areaSqM) }
        recalculateSizing()
    }

    fun toggleAreaUnit(isSqFt: Boolean) {
        _state.update { it.copy(isSquareFeetMode = isSqFt) }
    }

    fun selectPanelType(panel: String) {
        _state.update { it.copy(panelType = panel) }
    }

    fun selectVendor(id: String, name: String) {
        _state.update {
            it.copy(
                selectedVendorId = id,
                selectedVendorName = name
            )
        }
    }

    fun updateRequestNotes(notes: String) {
        _state.update { it.copy(requestNotes = notes) }
    }

    fun submitEstimateRequest(): String {
        val uniqueNumber = (1000..9999).random()
        val generatedId = "REQ-SOL-2026-$uniqueNumber"
        val now = java.text.SimpleDateFormat("dd MMM, HH:mm 'IST'", java.util.Locale.getDefault()).format(java.util.Date())
        _state.update {
            it.copy(
                referenceId = generatedId,
                submissionTimestamp = now
            )
        }
        return generatedId
    }

    private fun recalculateSizing() {
        val current = _state.value
        val capacity = EstimateCalculator.calculateRecommendedSystemSize(current.monthlyConsumptionKwh)
        val reqArea = EstimateCalculator.calculateRequiredArea(capacity)
        _state.update {
            it.copy(
                recommendedCapacityKw = capacity,
                requiredAreaSqM = reqArea,
                isFeasible = current.availableAreaSqM >= reqArea
            )
        }
    }
}


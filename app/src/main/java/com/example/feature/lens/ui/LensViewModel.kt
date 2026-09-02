package com.example.feature.lens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.utils.ApplianceMaster
import com.example.feature.lens.utils.DetectedAppliance
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
    data class Review(val appliances: List<EditableAppliance>) : LensUiState()
    data class Analytics(val appliances: List<EditableAppliance>, val scanDurationMillis: Long) : LensUiState()
}

data class EditableAppliance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val powerWatts: Float,
    val usageHoursPerDay: Float,
    val monthlyUsageDays: Int,
    val quantity: Int
)

data class ScannedAppliance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val powerWatts: Float,
    val powerEstimateStr: String,
    val lastTrackingId: Int?,
    val lastSeenTimestamp: Long
)

class LensViewModel(private val repository: LensRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<LensUiState>(LensUiState.Scanning)
    val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

    private val _currentFrameDetections = MutableStateFlow<List<DetectedAppliance>>(emptyList())
    val currentFrameDetections: StateFlow<List<DetectedAppliance>> = _currentFrameDetections.asStateFlow()

    private val _scanSession = MutableStateFlow<List<ScannedAppliance>>(emptyList())
    val scanSession: StateFlow<List<ScannedAppliance>> = _scanSession.asStateFlow()

    private var scanStartTime = System.currentTimeMillis()

    fun updateDetections(detections: List<DetectedAppliance>) {
        if (_uiState.value !is LensUiState.Scanning) return

        _currentFrameDetections.value = detections

        val currentTime = System.currentTimeMillis()
        val currentSession = _scanSession.value.toMutableList()
        var sessionUpdated = false

        for (detection in detections) {
            var matchedIndex = -1
            // Try to match by tracking ID
            if (detection.trackingId != null) {
                matchedIndex = currentSession.indexOfFirst { it.lastTrackingId == detection.trackingId }
            }
            
            // Fallback: match by label and recent time (e.g. within 3 seconds) to prevent duplicate counting if tracker lost it briefly
            if (matchedIndex == -1) {
                matchedIndex = currentSession.indexOfFirst { 
                    it.label == detection.label && (currentTime - it.lastSeenTimestamp) < 3000
                }
            }

            if (matchedIndex != -1) {
                val existing = currentSession[matchedIndex]
                currentSession[matchedIndex] = existing.copy(
                    lastTrackingId = detection.trackingId,
                    lastSeenTimestamp = currentTime
                )
                sessionUpdated = true
            } else {
                currentSession.add(
                    ScannedAppliance(
                        label = detection.label,
                        powerWatts = detection.powerWatts,
                        powerEstimateStr = detection.powerEstimateStr,
                        lastTrackingId = detection.trackingId,
                        lastSeenTimestamp = currentTime
                    )
                )
                sessionUpdated = true
            }
        }

        if (sessionUpdated) {
            _scanSession.value = currentSession
        }
    }

    fun stopScanning() {
        if (_uiState.value is LensUiState.Scanning) {
            val grouped = _scanSession.value.groupBy { it.label }
            val editableAppliances = grouped.map { (label, list) ->
                val defaults = ApplianceMaster.getDefaults(label)
                EditableAppliance(
                    id = java.util.UUID.randomUUID().toString(),
                    label = defaults?.name ?: label,
                    powerWatts = defaults?.defaultWattage ?: list.first().powerWatts,
                    usageHoursPerDay = defaults?.typicalUsageHoursPerDay ?: 4f,
                    monthlyUsageDays = defaults?.monthlyUsageDays ?: 30,
                    quantity = list.size
                )
            }
            _uiState.value = LensUiState.Review(editableAppliances)
        }
    }

    fun updateReviewAppliance(
        id: String, label: String, powerWatts: Float,
        usageHoursPerDay: Float, monthlyUsageDays: Int, quantity: Int
    ) {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val updated = currentState.appliances.map { 
                if (it.id == id) it.copy(
                    label = label, powerWatts = powerWatts,
                    usageHoursPerDay = usageHoursPerDay,
                    monthlyUsageDays = monthlyUsageDays,
                    quantity = quantity
                ) else it 
            }
            _uiState.value = currentState.copy(appliances = updated)
        }
    }

    fun removeReviewAppliance(id: String) {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val updated = currentState.appliances.filter { it.id != id }
            _uiState.value = currentState.copy(appliances = updated)
        }
    }

    fun addReviewAppliance() {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val defaults = ApplianceMaster.getDefaults("Other / Custom Appliance")
            val newAppliance = EditableAppliance(
                id = java.util.UUID.randomUUID().toString(),
                label = "Other / Custom Appliance",
                powerWatts = defaults?.defaultWattage ?: 0f,
                usageHoursPerDay = defaults?.typicalUsageHoursPerDay ?: 0f,
                monthlyUsageDays = defaults?.monthlyUsageDays ?: 30,
                quantity = 1
            )
            val updated = currentState.appliances + newAppliance
            _uiState.value = currentState.copy(appliances = updated)
        }
    }

    fun confirmReview() {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val duration = System.currentTimeMillis() - scanStartTime
            _uiState.value = LensUiState.Analytics(currentState.appliances, duration)
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
            // Assume simple tariff for estimation (configurable later)
            val estimatedCost = monthlyConsumption * com.example.feature.lens.utils.LensConstants.DEFAULT_TARIFF_RATE

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
        _currentFrameDetections.value = emptyList()
        _scanSession.value = emptyList()
        scanStartTime = System.currentTimeMillis()
        _uiState.value = LensUiState.Scanning
    }
}

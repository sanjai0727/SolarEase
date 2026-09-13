package com.example.feature.lens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.detector.DetectedCandidate
import com.example.feature.lens.utils.ApplianceMaster
import com.example.feature.lens.utils.DetectedAppliance
import com.example.feature.lens.verification.ApplianceVerificationService
import com.example.feature.lens.verification.BackendNemotronVerificationService
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class SaveStatus {
    SAVING,
    SUCCESS,
    PARTIAL_FAILURE,
    FAILED
}

sealed class LensUiState {
    object Idle : LensUiState()
    object Scanning : LensUiState()
    object Finalizing : LensUiState()
    data class Processing(val message: String = "Analyzing appliances...") : LensUiState()
    data class ConfirmingAppliance(
        val applianceName: String,
        val applianceType: String,
        val powerRatingWatts: Float,
        val dailyUsageHours: Float = 0f,
        val quantity: Int = 1
    ) : LensUiState()
    data class Saving(val appliance: ApplianceCreateRequest) : LensUiState()
    data class SavingReview(
        val appliances: List<EditableAppliance>,
        val scanDurationMillis: Long,
        val message: String = "Saving appliances to profile..."
    ) : LensUiState()
    data class Result(val appliance: Appliance) : LensUiState()
    data class Error(val message: String) : LensUiState()
    data class Review(val appliances: List<EditableAppliance>) : LensUiState()
    data class Analytics(
        val appliances: List<EditableAppliance>,
        val scanDurationMillis: Long,
        val saveStatus: SaveStatus = SaveStatus.SUCCESS,
        val savedApplianceIds: Set<String> = emptySet(),
        val failedApplianceIds: Set<String> = emptySet(),
        val errorMessage: String? = null
    ) : LensUiState()
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
    val boundingBox: android.graphics.RectF? = null,
    val lastTrackingId: Int? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

enum class TrackerStatus {
    UNVERIFIED,
    VERIFYING,
    CONFIRMED,
    REJECTED
}

data class CandidateTracker(
    val trackingId: Int,
    var boundingBoxRelative: android.graphics.RectF,
    var frameCount: Int = 1,
    var lastSeenMs: Long = System.currentTimeMillis(),
    var verificationStatus: TrackerStatus = TrackerStatus.UNVERIFIED,
    var confirmedLabel: String? = null,
    var powerWatts: Float = 0f,
    var powerEstimateStr: String = ""
)

class LensViewModel(
    private val repository: LensRepository,
    private val verificationService: ApplianceVerificationService = BackendNemotronVerificationService(repository)
) : ViewModel() {

    private val _uiState = MutableStateFlow<LensUiState>(LensUiState.Idle)
    val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

    private val _currentFrameDetections = MutableStateFlow<List<DetectedAppliance>>(emptyList())
    val currentFrameDetections: StateFlow<List<DetectedAppliance>> = _currentFrameDetections.asStateFlow()

    private val _scanSession = MutableStateFlow<List<ScannedAppliance>>(emptyList())
    val scanSession: StateFlow<List<ScannedAppliance>> = _scanSession.asStateFlow()

    private val _savedAppliances = MutableStateFlow<List<Appliance>>(emptyList())
    val savedAppliances: StateFlow<List<Appliance>> = _savedAppliances.asStateFlow()

    private var scanStartTime = System.currentTimeMillis()
    private var activeAnalysisJob: Job? = null
    private var activeScanJob: Job? = null
    private val isAnalysisSlotReserved = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isFinalizingScan = java.util.concurrent.atomic.AtomicBoolean(false)
    private var overlayTtlJob: Job? = null
    private var finalizationJob: Job? = null
    private var activeSaveJob: Job? = null
    private val _persistedApplianceIds = mutableSetOf<String>()

    // Hybrid scanning state
    private val activeTrackers = ConcurrentHashMap<Int, CandidateTracker>()
    private val isVerificationActive = java.util.concurrent.atomic.AtomicBoolean(false)
    private var activeVerificationJob: Job? = null
    private var nextSyntheticTrackingId = -1

    fun isScanFrameAnalyzing(): Boolean = isAnalysisSlotReserved.get()

    fun reserveCaptureSlot(): Boolean {
        if (_uiState.value !is LensUiState.Scanning) return false
        return isAnalysisSlotReserved.compareAndSet(false, true)
    }

    fun releaseCaptureSlot() {
        isAnalysisSlotReserved.set(false)
    }

    private fun scheduleOverlayTtl(ttlMillis: Long = 1300L) {
        overlayTtlJob?.cancel()
        overlayTtlJob = viewModelScope.launch {
            kotlinx.coroutines.delay(ttlMillis)
            if (_uiState.value is LensUiState.Scanning) {
                _currentFrameDetections.value = emptyList()
            }
        }
    }

    fun analyzeScanFrame(imageBytes: ByteArray) {
        if (_uiState.value !is LensUiState.Scanning) {
            releaseCaptureSlot()
            return
        }

        activeScanJob = viewModelScope.launch {
            try {
                val result = repository.analyzeImage(imageBytes, "CAMERA")
                if (!isActive) return@launch

                result.onSuccess { detectionResult ->
                    val frameDetections = mutableListOf<DetectedAppliance>()
                    val currentTime = System.currentTimeMillis()
                    val currentSession = _scanSession.value.toMutableList()

                    for (det in detectionResult.detections) {
                        val label = det.label
                        val defaults = ApplianceMaster.getDefaults(label)
                        val powerWatts = defaults?.defaultWattage ?: 100f
                        val powerEstimateStr = "Est. ~${powerWatts.toInt()} W"

                        val rectF = android.graphics.RectF(
                            det.boundingBox.left,
                            det.boundingBox.top,
                            det.boundingBox.right,
                            det.boundingBox.bottom
                        )

                        val detected = DetectedAppliance(
                            trackingId = null,
                            boundingBox = android.graphics.Rect(0, 0, 0, 0),
                            boundingBoxRelative = rectF,
                            label = label,
                            powerEstimateStr = powerEstimateStr,
                            powerWatts = powerWatts
                        )
                        frameDetections.add(detected)

                        // Deduplicate into currentSession
                        val matchedIndex = currentSession.indexOfFirst { existing ->
                            isSameAppliance(existing, detected)
                        }

                        if (matchedIndex != -1) {
                            val existing = currentSession[matchedIndex]
                            currentSession[matchedIndex] = existing.copy(
                                boundingBox = rectF,
                                lastSeenTimestamp = currentTime
                            )
                        } else {
                            currentSession.add(
                                ScannedAppliance(
                                    label = label,
                                    powerWatts = powerWatts,
                                    powerEstimateStr = powerEstimateStr,
                                    boundingBox = rectF,
                                    lastTrackingId = null,
                                    lastSeenTimestamp = currentTime
                                )
                            )
                        }
                    }

                    // Always record detections to the accumulated session
                    _scanSession.value = currentSession

                    // Only update currentFrameDetections if still actively scanning
                    if (_uiState.value is LensUiState.Scanning) {
                        _currentFrameDetections.value = frameDetections
                        scheduleOverlayTtl()
                    } else {
                        _currentFrameDetections.value = emptyList()
                    }
                }.onFailure { e ->
                    // Clear current frame on failure rather than retaining stale box
                    if (_uiState.value is LensUiState.Scanning) {
                        _currentFrameDetections.value = emptyList()
                    }
                    if (e is CancellationException || !isActive) return@launch
                    android.util.Log.e("LensViewModel", "Scan frame analysis failed: ${e.message}", e)
                }
            } finally {
                releaseCaptureSlot()
            }
        }
    }

    private fun isSameAppliance(existing: ScannedAppliance, newDet: DetectedAppliance): Boolean {
        if (!existing.label.equals(newDet.label, ignoreCase = true)) {
            return false
        }
        val box1 = existing.boundingBox ?: return false
        val box2 = newDet.boundingBoxRelative

        // Calculate IoU
        val interLeft = maxOf(box1.left, box2.left)
        val interTop = maxOf(box1.top, box2.top)
        val interRight = minOf(box1.right, box2.right)
        val interBottom = minOf(box1.bottom, box2.bottom)

        val interArea = if (interRight > interLeft && interBottom > interTop) {
            (interRight - interLeft) * (interBottom - interTop)
        } else 0f

        val area1 = (box1.right - box1.left) * (box1.bottom - box1.top)
        val area2 = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = area1 + area2 - interArea

        val iou = if (unionArea > 0f) interArea / unionArea else 0f
        if (iou > 0.20f) return true

        // Spatial proximity (center distance squared)
        val cx1 = (box1.left + box1.right) / 2f
        val cy1 = (box1.top + box1.bottom) / 2f
        val cx2 = (box2.left + box2.right) / 2f
        val cy2 = (box2.top + box2.bottom) / 2f
        val distSq = (cx1 - cx2) * (cx1 - cx2) + (cy1 - cy2) * (cy1 - cy2)

        return distSq < 0.12f
    }

    fun requestFinalizeScan() {
        if (!isFinalizingScan.compareAndSet(false, true)) {
            return
        }

        _uiState.value = LensUiState.Finalizing
        _currentFrameDetections.value = emptyList()
        overlayTtlJob?.cancel()
        overlayTtlJob = null

        finalizationJob = viewModelScope.launch {
            try {
                val job = activeVerificationJob ?: activeScanJob
                if (job != null && job.isActive) {
                    kotlinx.coroutines.withTimeoutOrNull(2000L) {
                        job.join()
                    } ?: run {
                        android.util.Log.w("LensViewModel", "Active scan/verification job timed out during finalization; cancelling")
                        job.cancel()
                        isVerificationActive.set(false)
                        releaseCaptureSlot()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("LensViewModel", "Error during scan finalization", e)
            } finally {
                _currentFrameDetections.value = emptyList()
                isVerificationActive.set(false)
                releaseCaptureSlot()
                stopScanning()
            }
        }
    }

    suspend fun finalizeScan() {
        requestFinalizeScan()
        finalizationJob?.join()
    }

    fun onLocalCandidatesDetected(
        candidates: List<DetectedCandidate>,
        cropProvider: (android.graphics.RectF) -> ByteArray?
    ) {
        if (_uiState.value !is LensUiState.Scanning) return

        val now = System.currentTimeMillis()
        val frameDetections = mutableListOf<DetectedAppliance>()

        // 1. Associate candidates with persistent trackers
        for (candidate in candidates) {
            val trackingId = candidate.trackingId ?: run {
                val spatialMatch = activeTrackers.values.find {
                    computeIoU(it.boundingBoxRelative, candidate.boundingBoxRelative) > 0.30f
                }
                spatialMatch?.trackingId ?: run {
                    val newId = nextSyntheticTrackingId--
                    newId
                }
            }

            val tracker = activeTrackers.getOrPut(trackingId) {
                CandidateTracker(
                    trackingId = trackingId,
                    boundingBoxRelative = candidate.boundingBoxRelative,
                    frameCount = 0,
                    lastSeenMs = now
                )
            }

            tracker.boundingBoxRelative = candidate.boundingBoxRelative
            tracker.lastSeenMs = now
            tracker.frameCount++

            // 2. Map tracker status to immediate visual UI representation
            when (tracker.verificationStatus) {
                TrackerStatus.CONFIRMED -> {
                    val label = tracker.confirmedLabel ?: "Appliance"
                    frameDetections.add(
                        DetectedAppliance(
                            trackingId = trackingId,
                            boundingBox = candidate.boundingBox,
                            boundingBoxRelative = candidate.boundingBoxRelative,
                            label = "✓ $label",
                            powerEstimateStr = tracker.powerEstimateStr,
                            powerWatts = tracker.powerWatts,
                            isVerified = true,
                            isVerifying = false
                        )
                    )
                }
                TrackerStatus.VERIFYING -> {
                    frameDetections.add(
                        DetectedAppliance(
                            trackingId = trackingId,
                            boundingBox = candidate.boundingBox,
                            boundingBoxRelative = candidate.boundingBoxRelative,
                            label = "Checking...",
                            powerEstimateStr = "",
                            powerWatts = 0f,
                            isVerified = false,
                            isVerifying = true
                        )
                    )
                }
                TrackerStatus.UNVERIFIED -> {
                    frameDetections.add(
                        DetectedAppliance(
                            trackingId = trackingId,
                            boundingBox = candidate.boundingBox,
                            boundingBoxRelative = candidate.boundingBoxRelative,
                            label = "Appliance detected",
                            powerEstimateStr = "",
                            powerWatts = 0f,
                            isVerified = false,
                            isVerifying = false
                        )
                    )
                }
                TrackerStatus.REJECTED -> {
                    // Omitted from overlay to avoid visual clutter
                }
            }
        }

        // Clean up stale unconfirmed trackers (not seen for > 2500ms)
        val iterator = activeTrackers.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            val trk = entry.value
            if (trk.verificationStatus != TrackerStatus.CONFIRMED && now - trk.lastSeenMs > 2500L) {
                iterator.remove()
            }
        }

        // Update live screen overlay immediately!
        if (_uiState.value is LensUiState.Scanning) {
            _currentFrameDetections.value = frameDetections
            scheduleOverlayTtl()
        }

        // 3. Candidate verification gating: only 1 in flight at a time
        if (_uiState.value is LensUiState.Scanning && isVerificationActive.compareAndSet(false, true)) {
            val candidateToVerify = activeTrackers.values.find {
                it.verificationStatus == TrackerStatus.UNVERIFIED && it.frameCount >= 3
            }

            if (candidateToVerify != null) {
                candidateToVerify.verificationStatus = TrackerStatus.VERIFYING
                val cropBytes = cropProvider(candidateToVerify.boundingBoxRelative)
                if (cropBytes != null && cropBytes.isNotEmpty()) {
                    activeVerificationJob = viewModelScope.launch {
                        try {
                            val verResult = verificationService.verifyCandidate(cropBytes)
                            verResult.onSuccess { result ->
                                if (result.isVerified && !result.label.isNullOrBlank()) {
                                    val canonicalLabel = result.label
                                    val defaults = ApplianceMaster.getDefaults(canonicalLabel)
                                    val powerWatts = defaults?.defaultWattage ?: 100f
                                    val powerEstimateStr = "Est. ~${powerWatts.toInt()} W"

                                    candidateToVerify.verificationStatus = TrackerStatus.CONFIRMED
                                    candidateToVerify.confirmedLabel = canonicalLabel
                                    candidateToVerify.powerWatts = powerWatts
                                    candidateToVerify.powerEstimateStr = powerEstimateStr

                                    recordConfirmedAppliance(
                                        label = canonicalLabel,
                                        powerWatts = powerWatts,
                                        powerEstimateStr = powerEstimateStr,
                                        boundingBox = candidateToVerify.boundingBoxRelative,
                                        trackingId = candidateToVerify.trackingId
                                    )
                                } else {
                                    candidateToVerify.verificationStatus = TrackerStatus.REJECTED
                                }
                            }.onFailure { e ->
                                if (candidateToVerify.verificationStatus == TrackerStatus.VERIFYING) {
                                    candidateToVerify.verificationStatus = TrackerStatus.UNVERIFIED
                                }
                                android.util.Log.w("LensViewModel", "Candidate verification non-fatal: ${e.message}")
                            }
                        } finally {
                            isVerificationActive.set(false)
                        }
                    }
                } else {
                    candidateToVerify.verificationStatus = TrackerStatus.UNVERIFIED
                    isVerificationActive.set(false)
                }
            } else {
                isVerificationActive.set(false)
            }
        }
    }

    private fun recordConfirmedAppliance(
        label: String,
        powerWatts: Float,
        powerEstimateStr: String,
        boundingBox: android.graphics.RectF,
        trackingId: Int
    ) {
        val currentTime = System.currentTimeMillis()
        val currentSession = _scanSession.value.toMutableList()

        var matchedIndex = currentSession.indexOfFirst { it.lastTrackingId == trackingId }

        if (matchedIndex == -1) {
            matchedIndex = currentSession.indexOfFirst { existing ->
                if (!existing.label.equals(label, ignoreCase = true)) return@indexOfFirst false
                val box1 = existing.boundingBox ?: return@indexOfFirst false
                val iou = computeIoU(box1, boundingBox)
                if (iou > 0.20f) return@indexOfFirst true

                val cx1 = (box1.left + box1.right) / 2f
                val cy1 = (box1.top + box1.bottom) / 2f
                val cx2 = (boundingBox.left + boundingBox.right) / 2f
                val cy2 = (boundingBox.top + boundingBox.bottom) / 2f
                val distSq = (cx1 - cx2) * (cx1 - cx2) + (cy1 - cy2) * (cy1 - cy2)
                distSq < 0.12f
            }
        }

        if (matchedIndex != -1) {
            val existing = currentSession[matchedIndex]
            currentSession[matchedIndex] = existing.copy(
                boundingBox = boundingBox,
                lastTrackingId = trackingId,
                lastSeenTimestamp = currentTime
            )
        } else {
            currentSession.add(
                ScannedAppliance(
                    label = label,
                    powerWatts = powerWatts,
                    powerEstimateStr = powerEstimateStr,
                    boundingBox = boundingBox,
                    lastTrackingId = trackingId,
                    lastSeenTimestamp = currentTime
                )
            )
        }

        _scanSession.value = currentSession
    }

    private fun computeIoU(box1: android.graphics.RectF, box2: android.graphics.RectF): Float {
        val interLeft = maxOf(box1.left, box2.left)
        val interTop = maxOf(box1.top, box2.top)
        val interRight = minOf(box1.right, box2.right)
        val interBottom = minOf(box1.bottom, box2.bottom)

        val interArea = if (interRight > interLeft && interBottom > interTop) {
            (interRight - interLeft) * (interBottom - interTop)
        } else 0f

        val area1 = (box1.right - box1.left) * (box1.bottom - box1.top)
        val area2 = (box2.right - box2.left) * (box2.bottom - box2.top)
        val unionArea = area1 + area2 - interArea

        return if (unionArea > 0f) interArea / unionArea else 0f
    }

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
        val currentState = _uiState.value
        if (currentState is LensUiState.Scanning || currentState is LensUiState.Processing || currentState is LensUiState.Finalizing) {
            val grouped = _scanSession.value.groupBy { it.label }
            val editableAppliances = grouped.map { (label, list) ->
                val defaults = ApplianceMaster.getDefaults(label)
                EditableAppliance(
                    id = java.util.UUID.randomUUID().toString(),
                    label = defaults?.name ?: label,
                    powerWatts = defaults?.defaultWattage ?: (list.firstOrNull()?.powerWatts ?: 100f),
                    usageHoursPerDay = defaults?.typicalUsageHoursPerDay ?: 4f,
                    monthlyUsageDays = defaults?.monthlyUsageDays ?: 30,
                    quantity = list.size
                )
            }
            _currentFrameDetections.value = emptyList()
            _uiState.value = LensUiState.Review(editableAppliances)
        }
    }

    fun cancelAnalysis() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        if (_scanSession.value.isNotEmpty()) {
            stopScanning()
        } else {
            _uiState.value = LensUiState.Idle
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
            val appliancesToSave = currentState.appliances

            if (appliancesToSave.isEmpty()) {
                _uiState.value = LensUiState.Analytics(
                    appliances = emptyList(),
                    scanDurationMillis = duration,
                    saveStatus = SaveStatus.SUCCESS
                )
                return
            }

            _uiState.value = LensUiState.SavingReview(appliancesToSave, duration)

            activeSaveJob?.cancel()
            activeSaveJob = viewModelScope.launch {
                val savedIds = mutableSetOf<String>()
                val failedIds = mutableSetOf<String>()
                var lastErrorMessage: String? = null

                try {
                    for (appliance in appliancesToSave) {
                        if (!isActive) return@launch

                        if (_persistedApplianceIds.contains(appliance.id)) {
                            savedIds.add(appliance.id)
                            continue
                        }

                        val power = if (appliance.powerWatts > 0f) appliance.powerWatts else 1f
                        val monthlyKwh = ApplianceMaster.calculateMonthlyKwh(
                            powerWatts = power,
                            dailyUsageHours = appliance.usageHoursPerDay,
                            monthlyUsageDays = appliance.monthlyUsageDays,
                            quantity = appliance.quantity
                        )
                        val estimatedCost = ApplianceMaster.calculateMonthlyCost(monthlyKwh)

                        val request = ApplianceCreateRequest(
                            applianceName = appliance.label,
                            applianceType = appliance.label,
                            powerRatingWatts = power,
                            dailyUsageHours = appliance.usageHoursPerDay,
                            quantity = appliance.quantity.coerceAtLeast(1),
                            monthlyConsumptionKwh = monthlyKwh,
                            estimatedMonthlyCost = estimatedCost
                        )

                        val result = repository.createAppliance(request)
                        result.onSuccess { saved ->
                            _persistedApplianceIds.add(appliance.id)
                            savedIds.add(appliance.id)
                            android.util.Log.d("LensViewModel", "Appliance persisted to DB: ${saved.id} (${saved.applianceName})")
                        }.onFailure { err ->
                            failedIds.add(appliance.id)
                            lastErrorMessage = err.message ?: "Failed to save ${appliance.label}"
                            android.util.Log.e("LensViewModel", "Failed to persist appliance ${appliance.label}: $lastErrorMessage", err)
                        }
                    }

                    if (!isActive) return@launch

                    val finalStatus = when {
                        failedIds.isEmpty() -> SaveStatus.SUCCESS
                        savedIds.isEmpty() -> SaveStatus.FAILED
                        else -> SaveStatus.PARTIAL_FAILURE
                    }

                    _uiState.value = LensUiState.Analytics(
                        appliances = appliancesToSave,
                        scanDurationMillis = duration,
                        saveStatus = finalStatus,
                        savedApplianceIds = savedIds,
                        failedApplianceIds = failedIds,
                        errorMessage = if (finalStatus == SaveStatus.SUCCESS) null else lastErrorMessage
                    )
                } catch (e: CancellationException) {
                    android.util.Log.d("LensViewModel", "Appliance save cancelled")
                    throw e
                } catch (e: Exception) {
                    if (!isActive) return@launch
                    val unpersisted = appliancesToSave.map { it.id }.filterNot { it in _persistedApplianceIds }.toSet()
                    val finalStatus = if (_persistedApplianceIds.isEmpty()) SaveStatus.FAILED else SaveStatus.PARTIAL_FAILURE
                    _uiState.value = LensUiState.Analytics(
                        appliances = appliancesToSave,
                        scanDurationMillis = duration,
                        saveStatus = finalStatus,
                        savedApplianceIds = _persistedApplianceIds.toSet(),
                        failedApplianceIds = unpersisted,
                        errorMessage = e.message ?: "An unexpected error occurred while saving appliances"
                    )
                }
            }
        }
    }

    fun retrySaveFailed() {
        val currentState = _uiState.value
        if (currentState is LensUiState.Analytics) {
            val appliancesToRetry = currentState.appliances.filter { it.id in currentState.failedApplianceIds }
            if (appliancesToRetry.isEmpty()) {
                _uiState.value = currentState.copy(
                    saveStatus = SaveStatus.SUCCESS,
                    failedApplianceIds = emptySet(),
                    errorMessage = null
                )
                return
            }

            _uiState.value = currentState.copy(saveStatus = SaveStatus.SAVING)

            activeSaveJob?.cancel()
            activeSaveJob = viewModelScope.launch {
                val currentSaved = currentState.savedApplianceIds.toMutableSet()
                val currentFailed = mutableSetOf<String>()
                var lastErrorMessage: String? = null

                try {
                    for (appliance in appliancesToRetry) {
                        if (!isActive) return@launch

                        if (_persistedApplianceIds.contains(appliance.id)) {
                            currentSaved.add(appliance.id)
                            continue
                        }

                        val power = if (appliance.powerWatts > 0f) appliance.powerWatts else 1f
                        val monthlyKwh = ApplianceMaster.calculateMonthlyKwh(
                            powerWatts = power,
                            dailyUsageHours = appliance.usageHoursPerDay,
                            monthlyUsageDays = appliance.monthlyUsageDays,
                            quantity = appliance.quantity
                        )
                        val estimatedCost = ApplianceMaster.calculateMonthlyCost(monthlyKwh)

                        val request = ApplianceCreateRequest(
                            applianceName = appliance.label,
                            applianceType = appliance.label,
                            powerRatingWatts = power,
                            dailyUsageHours = appliance.usageHoursPerDay,
                            quantity = appliance.quantity.coerceAtLeast(1),
                            monthlyConsumptionKwh = monthlyKwh,
                            estimatedMonthlyCost = estimatedCost
                        )

                        val result = repository.createAppliance(request)
                        result.onSuccess { saved ->
                            _persistedApplianceIds.add(appliance.id)
                            currentSaved.add(appliance.id)
                            android.util.Log.d("LensViewModel", "Appliance persisted to DB on retry: ${saved.id} (${saved.applianceName})")
                        }.onFailure { err ->
                            currentFailed.add(appliance.id)
                            lastErrorMessage = err.message ?: "Failed to save ${appliance.label}"
                            android.util.Log.e("LensViewModel", "Retry failed for appliance ${appliance.label}: $lastErrorMessage", err)
                        }
                    }

                    if (!isActive) return@launch

                    val finalStatus = when {
                        currentFailed.isEmpty() -> SaveStatus.SUCCESS
                        currentSaved.isEmpty() -> SaveStatus.FAILED
                        else -> SaveStatus.PARTIAL_FAILURE
                    }

                    _uiState.value = currentState.copy(
                        saveStatus = finalStatus,
                        savedApplianceIds = currentSaved,
                        failedApplianceIds = currentFailed,
                        errorMessage = if (finalStatus == SaveStatus.SUCCESS) null else lastErrorMessage
                    )
                } catch (e: CancellationException) {
                    android.util.Log.d("LensViewModel", "Appliance retry save cancelled")
                    throw e
                } catch (e: Exception) {
                    if (!isActive) return@launch
                    val unpersisted = currentState.appliances.map { it.id }.filterNot { it in _persistedApplianceIds }.toSet()
                    val finalStatus = if (_persistedApplianceIds.isEmpty()) SaveStatus.FAILED else SaveStatus.PARTIAL_FAILURE
                    _uiState.value = currentState.copy(
                        saveStatus = finalStatus,
                        savedApplianceIds = _persistedApplianceIds.toSet(),
                        failedApplianceIds = unpersisted,
                        errorMessage = e.message ?: "An unexpected error occurred while saving appliances"
                    )
                }
            }
        }
    }

    fun returnToReview() {
        val currentState = _uiState.value
        if (currentState is LensUiState.Analytics) {
            _uiState.value = LensUiState.Review(currentState.appliances)
        }
    }

    fun loadSavedAppliances() {
        viewModelScope.launch {
            repository.getAppliances().onSuccess { list ->
                _savedAppliances.value = list
            }.onFailure { err ->
                android.util.Log.w("LensViewModel", "Failed to load saved appliances: ${err.message}")
            }
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
            val monthlyConsumption = ApplianceMaster.calculateMonthlyKwh(
                powerWatts = currentState.powerRatingWatts,
                dailyUsageHours = currentState.dailyUsageHours,
                monthlyUsageDays = 30,
                quantity = currentState.quantity
            )
            val estimatedCost = ApplianceMaster.calculateMonthlyCost(monthlyConsumption)

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

    fun processImagesAsync(images: List<ByteArray>, source: String) {
        val previousState = _uiState.value
        _uiState.value = LensUiState.Processing("Analyzing ${images.size} image(s)...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch {
            var hasNewDetections = false
            for (imageBytes in images) {
                if (!isActive) return@launch
                val result = repository.analyzeImage(imageBytes, source)
                if (!isActive) return@launch
                result.onSuccess { detectionResult ->
                    val currentTime = System.currentTimeMillis()
                    val currentSession = _scanSession.value.toMutableList()

                    val frameDetections = mutableListOf<DetectedAppliance>()

                    for (det in detectionResult.detections) {
                        val label = det.label
                        val defaults = ApplianceMaster.getDefaults(label)
                        val powerWatts = defaults?.defaultWattage ?: 100f
                        val powerEstimateStr = "Est. ~${powerWatts.toInt()} W"

                        // Map Gemini bounding box to UI relative box
                        val rectF = android.graphics.RectF(
                            det.boundingBox.left,
                            det.boundingBox.top,
                            det.boundingBox.right,
                            det.boundingBox.bottom
                        )

                        frameDetections.add(
                            DetectedAppliance(
                                trackingId = null,
                                boundingBox = android.graphics.Rect(0, 0, 0, 0), // Not used for UI drawing
                                boundingBoxRelative = rectF,
                                label = label,
                                powerEstimateStr = powerEstimateStr,
                                powerWatts = powerWatts
                            )
                        )

                        // Check if it already exists broadly (could be same physical item)
                        val existingIndex = currentSession.indexOfFirst { it.label == label }
                        if (existingIndex == -1) {
                            currentSession.add(
                                ScannedAppliance(
                                    label = label,
                                    powerWatts = powerWatts,
                                    powerEstimateStr = powerEstimateStr,
                                    lastTrackingId = null,
                                    lastSeenTimestamp = currentTime
                                )
                            )
                            hasNewDetections = true
                        }
                    }
                    _scanSession.value = currentSession
                    // Update the UI overlay with the latest detections
                    _currentFrameDetections.value = frameDetections
                }.onFailure { e ->
                    if (e is CancellationException || !isActive) {
                        return@launch
                    }
                    android.util.Log.e("LensViewModel", "Failed to analyze frame", e)
                    withContext(Dispatchers.Main) {
                        val message = if (e is java.io.IOException && e.message?.contains("Firebase authentication required") == true) {
                            "Authentication required. Please sign in again to continue scanning."
                        } else {
                            e.message ?: "Analysis failed. Please check your connection and try again."
                        }
                        _uiState.value = LensUiState.Error(message)
                    }
                    return@launch
                }
            }

            // If we successfully processed all frames and are still in Processing state
            if (_uiState.value is LensUiState.Processing) {
                stopScanning()
            }
        }
    }

    fun processUrisAsync(context: android.content.Context, uris: List<android.net.Uri>) {
        _uiState.value = LensUiState.Processing("Preparing images...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val byteArrayList = mutableListOf<ByteArray>()

            for (uri in uris) {
                if (!isActive) return@launch
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()

                    if (bytes != null) {
                        if (!isActive) return@launch
                        // Resize image if it's too large to save bandwidth & API limits
                        val options = android.graphics.BitmapFactory.Options()
                        options.inJustDecodeBounds = true
                        android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                        val maxDim = 1536
                        var inSampleSize = 1
                        if (options.outHeight > maxDim || options.outWidth > maxDim) {
                            val halfHeight: Int = options.outHeight / 2
                            val halfWidth: Int = options.outWidth / 2
                            while (halfHeight / inSampleSize >= maxDim || halfWidth / inSampleSize >= maxDim) {
                                inSampleSize *= 2
                            }
                        }

                        options.inJustDecodeBounds = false
                        options.inSampleSize = inSampleSize
                        val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                        if (bitmap != null) {
                            val outputStream = java.io.ByteArrayOutputStream()
                            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
                            byteArrayList.add(outputStream.toByteArray())
                            bitmap.recycle()
                        } else {
                            android.util.Log.e("LensViewModel", "Failed to decode bitmap from URI: $uri (unsupported or corrupt format)")
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException || !isActive) return@launch
                    android.util.Log.e("LensViewModel", "Failed to process URI: $uri", e)
                }
            }

            if (!isActive) return@launch

            if (byteArrayList.isNotEmpty()) {
                // Switch back to Main for the State changes inside processImagesAsync
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        processImagesAsync(byteArrayList, "PHOTO")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    if (_uiState.value is LensUiState.Processing) {
                        _uiState.value = LensUiState.Error("Could not process the selected image(s). Please choose valid image files.")
                    }
                }
            }
        }
    }

    @Deprecated("Legacy video frame extraction replaced by still-frame Camera Scan.")
    fun processVideoAsync(videoFile: java.io.File) {
        if (_uiState.value !is LensUiState.Scanning && _uiState.value !is LensUiState.Review) {
            _uiState.value = LensUiState.Processing("Extracting video frames...")
        }

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val frames = com.example.feature.lens.utils.VideoFrameExtractor.extractFrames(videoFile, 3)
            if (!isActive) return@launch
            if (frames.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        processImagesAsync(frames, "VIDEO")
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = LensUiState.Error("Failed to extract frames from video")
                }
            }
        }
    }

    fun resetToIdle() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        activeScanJob?.cancel()
        activeScanJob = null
        activeVerificationJob?.cancel()
        activeVerificationJob = null
        isVerificationActive.set(false)
        activeTrackers.clear()
        overlayTtlJob?.cancel()
        overlayTtlJob = null
        finalizationJob?.cancel()
        finalizationJob = null
        isFinalizingScan.set(false)
        isAnalysisSlotReserved.set(false)
        activeSaveJob?.cancel()
        activeSaveJob = null
        _persistedApplianceIds.clear()
        _currentFrameDetections.value = emptyList()
        _scanSession.value = emptyList()
        _uiState.value = LensUiState.Idle
    }

    fun startScanning() {
        resetToIdle()
        scanStartTime = System.currentTimeMillis()
        _uiState.value = LensUiState.Scanning
    }

    fun restartScanning() {
        startScanning()
    }

    override fun onCleared() {
        super.onCleared()
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        activeScanJob?.cancel()
        activeScanJob = null
        activeVerificationJob?.cancel()
        activeVerificationJob = null
        isVerificationActive.set(false)
        activeTrackers.clear()
        overlayTtlJob?.cancel()
        overlayTtlJob = null
        finalizationJob?.cancel()
        finalizationJob = null
        isFinalizingScan.set(false)
        isAnalysisSlotReserved.set(false)
        activeSaveJob?.cancel()
        activeSaveJob = null
        _persistedApplianceIds.clear()
    }
}

package com.example.feature.lens.ui

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.detector.DetectedCandidate
import com.example.feature.lens.utils.ApplianceMaster
import com.example.feature.lens.utils.DetectedAppliance

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
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

@kotlinx.serialization.Serializable
enum class RoomStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

@kotlinx.serialization.Serializable
data class Room(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val order: Int = 0,
    val status: RoomStatus = RoomStatus.PENDING,
    val appliances: List<EditableAppliance> = emptyList()
) {
    val connectedLoadWatts: Float
        get() = appliances.sumOf { (it.powerWatts * it.quantity).toDouble() }.toFloat()

    val connectedLoadKw: Float
        get() = connectedLoadWatts / 1000f

    val monthlyKwh: Float
        get() = appliances.sumOf { it.monthlyKwh.toDouble() }.toFloat()

    val monthlyCost: Float
        get() = appliances.sumOf { it.monthlyCost.toDouble() }.toFloat()
}

enum class CameraMode {
    PHOTO, VIDEO, LIVE_SCAN
}

sealed class LensUiState {
    object Introduction : LensUiState()
    
    object RoomSetup : LensUiState()
    
    data class RoomOverview(val room: Room) : LensUiState()
    
    data class CameraActive(val room: Room, val mode: CameraMode = CameraMode.PHOTO) : LensUiState()
    
    data class Processing(val room: Room, val message: String = "Analyzing...") : LensUiState()
    
    data class Review(
        val room: Room,
        val newAppliances: List<EditableAppliance>,
        val isEditMode: Boolean = false
    ) : LensUiState()

    data class RoomCompleted(val room: Room, val nextRoom: Room?) : LensUiState()
    
    data class Error(val message: String, val previousState: LensUiState? = null) : LensUiState()
    
    data class WholeHouseAnalytics(
        val rooms: List<Room>,
        val saveStatus: SaveStatus = SaveStatus.SUCCESS,
        val errorMessage: String? = null
    ) : LensUiState()
}

@kotlinx.serialization.Serializable
data class EditableAppliance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val powerWatts: Float,
    val usageHoursPerDay: Float,
    val monthlyUsageDays: Int,
    val quantity: Int,
    val serverId: String? = null
) {
    val monthlyKwh: Float
        get() = (powerWatts * usageHoursPerDay * monthlyUsageDays * quantity.coerceAtLeast(1)) / 1000f

    val monthlyCost: Float
        get() = ApplianceMaster.calculateMonthlyCost(monthlyKwh)
}

data class ScannedAppliance(
    val id: String = java.util.UUID.randomUUID().toString(),
    val label: String,
    val powerWatts: Float,
    val powerEstimateStr: String,
    val boundingBox: android.graphics.RectF? = null,
    val lastTrackingId: Int? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis(),
    val quantity: Int = 1,
    val evidence: String? = null
)

class LensViewModel(
    private val repository: LensRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ViewModel() {

    private val uploadAnalyzer = com.example.feature.lens.utils.LensUploadAnalyzer(repository)

    private val _uiState = MutableStateFlow<LensUiState>(LensUiState.Introduction)
    val uiState: StateFlow<LensUiState> = _uiState.asStateFlow()

    private val _rooms = MutableStateFlow<List<Room>>(emptyList())
    val rooms: StateFlow<List<Room>> = _rooms.asStateFlow()
    
    private val _activeRoomId = MutableStateFlow<String?>(null)
    val activeRoomId: StateFlow<String?> = _activeRoomId.asStateFlow()

    private val _currentFrameDetections = MutableStateFlow<List<DetectedAppliance>>(emptyList())
    val currentFrameDetections: StateFlow<List<DetectedAppliance>> = _currentFrameDetections.asStateFlow()

    private val _scanSession = MutableStateFlow<List<ScannedAppliance>>(emptyList())
    val scanSession: StateFlow<List<ScannedAppliance>> = _scanSession.asStateFlow()

    companion object {
        const val MAX_ROOM_PHOTOS = 4
    }

    private val _selectedRoomPhotos = MutableStateFlow<List<Uri>>(emptyList())
    val selectedRoomPhotos: StateFlow<List<Uri>> = _selectedRoomPhotos.asStateFlow()

    /**
     * Attempts to add a captured photo URI.
     * Returns true if added successfully, false if already at MAX_ROOM_PHOTOS or duplicate.
     */
    fun addSelectedPhoto(uri: Uri): Boolean {
        val current = _selectedRoomPhotos.value
        if (current.size >= MAX_ROOM_PHOTOS) return false
        if (current.contains(uri)) return false
        _selectedRoomPhotos.value = current + uri
        return true
    }

    /**
     * Removes a selected photo by URI.
     */
    fun removeSelectedPhoto(uri: Uri) {
        _selectedRoomPhotos.value = _selectedRoomPhotos.value.filter { it != uri }
    }

    /**
     * Clears selected photos when moving to another room, finishing audit, or cancelling.
     */
    fun clearSelectedPhotos() {
        _selectedRoomPhotos.value = emptyList()
    }

    private var activeAnalysisJob: Job? = null
    private var overlayTtlJob: Job? = null

    init {
        // Load persistently saved rooms or default household rooms
        val savedRooms = com.example.feature.lens.data.LensRoomStorage.loadRooms()
        _rooms.value = if (savedRooms.isNotEmpty()) savedRooms else com.example.feature.lens.data.LensRoomStorage.defaultRooms()
    }

    fun addRoom(name: String) {
        val trimmed = name.trim().ifEmpty { "Custom Room" }
        val newOrder = _rooms.value.size
        val newRoom = Room(name = trimmed, order = newOrder)
        _rooms.value = _rooms.value + newRoom
        com.example.feature.lens.data.LensRoomStorage.saveRooms(_rooms.value)
    }

    fun renameRoom(roomId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        _rooms.value = _rooms.value.map { room ->
            if (room.id == roomId) room.copy(name = trimmed) else room
        }
        com.example.feature.lens.data.LensRoomStorage.saveRooms(_rooms.value)
        val currentState = _uiState.value
        if (currentState is LensUiState.RoomOverview && currentState.room.id == roomId) {
            val updated = _rooms.value.find { it.id == roomId }
            if (updated != null) {
                _uiState.value = LensUiState.RoomOverview(updated)
            }
        }
        return true
    }

    fun removeRoom(roomId: String) {
        val updated = _rooms.value.filter { it.id != roomId }.sortedBy { it.order }
        _rooms.value = updated.mapIndexed { index, room -> room.copy(order = index) }
        com.example.feature.lens.data.LensRoomStorage.saveRooms(_rooms.value)
    }

    fun reorderRooms(fromIndex: Int, toIndex: Int) {
        val current = _rooms.value.sortedBy { it.order }.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _rooms.value = current.mapIndexed { index, room -> room.copy(order = index) }
            com.example.feature.lens.data.LensRoomStorage.saveRooms(_rooms.value)
        }
    }

    fun startRoomSetup() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        _activeRoomId.value = null
        _currentFrameDetections.value = emptyList()
        _scanSession.value = emptyList()
        val resetRooms = _rooms.value.map { it.copy(status = RoomStatus.PENDING, appliances = emptyList()) }
        _rooms.value = resetRooms
        com.example.feature.lens.data.LensRoomStorage.saveRooms(resetRooms)
        _uiState.value = LensUiState.RoomSetup
    }

    fun proceedToScanning() {
        var currentRooms = _rooms.value.sortedBy { it.order }
        if (currentRooms.isEmpty()) {
            val defaults = com.example.feature.lens.data.LensRoomStorage.defaultRooms()
            _rooms.value = defaults
            com.example.feature.lens.data.LensRoomStorage.saveRooms(defaults)
            currentRooms = defaults.sortedBy { it.order }
        }

        // Always start fresh from room 1: reset statuses to PENDING and clear previous appliances
        val resetRooms = currentRooms.map { it.copy(status = RoomStatus.PENDING, appliances = emptyList()) }
        _rooms.value = resetRooms
        com.example.feature.lens.data.LensRoomStorage.saveRooms(resetRooms)
        currentRooms = resetRooms

        val targetRoom = currentRooms.first()

        _activeRoomId.value = targetRoom.id
        updateRoomStatus(targetRoom.id, RoomStatus.IN_PROGRESS)
        val updated = _rooms.value.find { it.id == targetRoom.id } ?: targetRoom
        _uiState.value = LensUiState.RoomOverview(updated)
    }

    fun startNewAudit() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        _activeRoomId.value = null
        clearSelectedPhotos()
        val resetRooms = _rooms.value.map { it.copy(status = RoomStatus.PENDING, appliances = emptyList()) }
        _rooms.value = resetRooms
        com.example.feature.lens.data.LensRoomStorage.saveRooms(resetRooms)
        _currentFrameDetections.value = emptyList()
        _scanSession.value = emptyList()
        _uiState.value = LensUiState.RoomSetup
    }

    private fun updateRoomStatus(roomId: String, status: RoomStatus) {
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(status = status) else it
        }
        com.example.feature.lens.data.LensRoomStorage.saveRooms(_rooms.value)
    }

    fun skipCurrentRoom() {
        val currentState = _uiState.value
        if (currentState is LensUiState.RoomOverview) {
            clearSelectedPhotos()
            updateRoomStatus(currentState.room.id, RoomStatus.SKIPPED)
            moveToNextRoom()
        }
    }

    private fun moveToNextRoom() {
        clearSelectedPhotos()
        val currentActiveId = _activeRoomId.value
        val sortedRooms = _rooms.value.sortedBy { it.order }
        
        val currentIndex = sortedRooms.indexOfFirst { it.id == currentActiveId }
        
        if (currentIndex != -1 && currentIndex + 1 < sortedRooms.size) {
            val nextRoom = sortedRooms[currentIndex + 1]
            _activeRoomId.value = nextRoom.id
            if (nextRoom.status == RoomStatus.PENDING) {
                updateRoomStatus(nextRoom.id, RoomStatus.IN_PROGRESS)
            }
            val updatedNextRoom = _rooms.value.find { it.id == nextRoom.id } ?: nextRoom
            _uiState.value = LensUiState.RoomOverview(updatedNextRoom)
        } else {
            finishWholeHouse()
        }
    }

    fun navigateToPreviousWorkspace() {
        clearSelectedPhotos()
        val currentActiveId = _activeRoomId.value
        if (currentActiveId == null) {
            _uiState.value = LensUiState.RoomSetup
            return
        }
        
        val sortedRooms = _rooms.value.sortedBy { it.order }
        val currentIndex = sortedRooms.indexOfFirst { it.id == currentActiveId }
        
        if (currentIndex > 0) {
            val prevRoom = sortedRooms[currentIndex - 1]
            _activeRoomId.value = prevRoom.id
            val updatedPrevRoom = _rooms.value.find { it.id == prevRoom.id } ?: prevRoom
            _uiState.value = LensUiState.RoomOverview(updatedPrevRoom)
        } else {
            _activeRoomId.value = null
            _uiState.value = LensUiState.RoomSetup
        }
    }

    fun startCamera(mode: CameraMode) {
        val currentState = _uiState.value
        if (currentState is LensUiState.RoomOverview) {
            _scanSession.value = emptyList()
            _currentFrameDetections.value = emptyList()
            _uiState.value = LensUiState.CameraActive(currentState.room, mode)
        }
    }

    fun onLocalCandidatesDetected(candidates: List<DetectedCandidate>) {
        val currentState = _uiState.value
        if (currentState !is LensUiState.CameraActive || currentState.mode != CameraMode.LIVE_SCAN) return

        val now = System.currentTimeMillis()
        val frameDetections = mutableListOf<DetectedAppliance>()
        val currentSession = _scanSession.value.toMutableList()

        for (candidate in candidates) {
            val label = candidate.label
            val isUnclassified = label.equals("Unclassified Candidate", ignoreCase = true) ||
                    label.equals("Unclassified Object", ignoreCase = true) ||
                    label.equals("Appliance", ignoreCase = true)
            val defaults = if (!isUnclassified) ApplianceMaster.getDefaults(label) else null
            val isRecognized = defaults != null && defaults.name != "Other / Custom Appliance"
            val powerWatts = if (isRecognized) defaults!!.defaultWattage else 0f
            val displayLabel = if (isRecognized) defaults!!.name else "Unclassified Object"
            val powerEstimateStr = if (isRecognized && powerWatts > 0f) "Est. ~${powerWatts.toInt()} W" else "Unverified Candidate"

            frameDetections.add(
                DetectedAppliance(
                    trackingId = candidate.trackingId,
                    boundingBox = candidate.boundingBox,
                    boundingBoxRelative = candidate.boundingBoxRelative,
                    label = displayLabel,
                    powerEstimateStr = powerEstimateStr,
                    powerWatts = powerWatts,
                    isVerified = isRecognized,
                    isVerifying = false
                )
            )

            val matchedIndex = currentSession.indexOfFirst {
                it.lastTrackingId == candidate.trackingId || (it.label == displayLabel && now - it.lastSeenTimestamp < 3000L)
            }
            if (matchedIndex != -1) {
                currentSession[matchedIndex] = currentSession[matchedIndex].copy(
                    boundingBox = candidate.boundingBoxRelative,
                    lastTrackingId = candidate.trackingId,
                    lastSeenTimestamp = now
                )
            } else {
                currentSession.add(
                    ScannedAppliance(
                        label = displayLabel,
                        powerWatts = powerWatts,
                        powerEstimateStr = powerEstimateStr,
                        boundingBox = candidate.boundingBoxRelative,
                        lastTrackingId = candidate.trackingId,
                        lastSeenTimestamp = now
                    )
                )
            }
        }

        _scanSession.value = currentSession
        _currentFrameDetections.value = frameDetections

        overlayTtlJob?.cancel()
        overlayTtlJob = viewModelScope.launch {
            kotlinx.coroutines.delay(1300L)
            if (_uiState.value is LensUiState.CameraActive) {
                _currentFrameDetections.value = emptyList()
            }
        }
    }

    fun finishScan() {
        val currentState = _uiState.value
        val room = when (currentState) {
            is LensUiState.CameraActive -> currentState.room
            is LensUiState.Processing -> currentState.room
            else -> return
        }

        val grouped = _scanSession.value.groupBy { it.label }
        val editableAppliances = grouped.map { (label, list) ->
            val isUnclassified = label.equals("Unclassified Candidate", ignoreCase = true) ||
                    label.equals("Unclassified Object", ignoreCase = true) ||
                    label.equals("Unclassified Appliance", ignoreCase = true) ||
                    label.equals("Appliance", ignoreCase = true)
            val defaults = if (!isUnclassified) ApplianceMaster.getDefaults(label) else null
            val isRecognized = defaults != null && defaults.name != "Other / Custom Appliance"
            val totalQty = list.sumOf { it.quantity }.coerceAtLeast(1)
            // Preserve unverified/unknown wattage (0f) if not positively verified, requiring user calibration
            val powerWatts = list.firstOrNull()?.powerWatts?.takeIf { it > 0f } ?: 0f
            val finalLabel = if (isRecognized) defaults!!.name else "Unclassified Appliance"
            EditableAppliance(
                id = java.util.UUID.randomUUID().toString(),
                label = finalLabel,
                powerWatts = powerWatts,
                usageHoursPerDay = defaults?.typicalUsageHoursPerDay ?: 4f,
                monthlyUsageDays = defaults?.monthlyUsageDays ?: 30,
                quantity = totalQty
            )
        }
        
        val latestRoom = _rooms.value.find { it.id == room.id } ?: room
        
        // Clear scan session and frame overlays so stale detections do not leak
        _scanSession.value = emptyList()
        _currentFrameDetections.value = emptyList()

        // Keep current scan detections separate from previously saved room inventory.
        // newAppliances contains ONLY the newly detected items from this scan session.
        _uiState.value = LensUiState.Review(latestRoom, editableAppliances, isEditMode = false)
    }

    fun cancelAnalysis() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        _scanSession.value = emptyList()
        _currentFrameDetections.value = emptyList()
        clearSelectedPhotos()
        
        val currentState = _uiState.value
        if (currentState is LensUiState.Processing) {
            val latestRoom = _rooms.value.find { it.id == currentState.room.id } ?: currentState.room
            _uiState.value = LensUiState.RoomOverview(latestRoom)
        }
    }

    fun updateReviewAppliance(
        id: String, label: String, powerWatts: Float,
        usageHoursPerDay: Float, monthlyUsageDays: Int, quantity: Int
    ) {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val updated = currentState.newAppliances.map {
                if (it.id == id) it.copy(
                    label = label, powerWatts = powerWatts,
                    usageHoursPerDay = usageHoursPerDay,
                    monthlyUsageDays = monthlyUsageDays,
                    quantity = quantity
                ) else it
            }
            _uiState.value = currentState.copy(newAppliances = updated)
        }
    }

    fun removeReviewAppliance(id: String) {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val updated = currentState.newAppliances.filter { it.id != id }
            _uiState.value = currentState.copy(newAppliances = updated)
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
            val updated = currentState.newAppliances + newAppliance
            _uiState.value = currentState.copy(newAppliances = updated)
        }
    }

    fun confirmReview(): Boolean {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            // Block confirmation if any appliance has unverified/zero wattage
            if (currentState.newAppliances.any { it.powerWatts <= 0f }) {
                return false
            }
            val room = currentState.room
            val finalAppliances = if (currentState.isEditMode) {
                // Legitimate room editing: preserve user's reviewed modifications directly
                currentState.newAppliances
            } else {
                // New scan / upload: merge newly confirmed appliances into room inventory without duplicate labels
                val existing = room.appliances.toMutableList()
                for (newApp in currentState.newAppliances) {
                    val existingIndex = existing.indexOfFirst { it.label.equals(newApp.label, ignoreCase = true) }
                    if (existingIndex != -1) {
                        val old = existing[existingIndex]
                        // Quantity merging must not increase counts merely because an appliance with the same name already exists
                        existing[existingIndex] = old.copy(
                            quantity = maxOf(old.quantity, newApp.quantity),
                            powerWatts = if (old.powerWatts > 0f) old.powerWatts else newApp.powerWatts
                        )
                    } else {
                        existing.add(newApp)
                    }
                }
                existing
            }

            val updatedRoom = room.copy(
                appliances = finalAppliances,
                status = RoomStatus.COMPLETED
            )
            _rooms.value = _rooms.value.map { r ->
                if (r.id == room.id) updatedRoom else r
            }
            com.example.feature.lens.data.LensRoomStorage.saveRooms(_rooms.value)

            val sortedRooms = _rooms.value.sortedBy { it.order }
            val currentIndex = sortedRooms.indexOfFirst { it.id == room.id }
            val nextRoom = if (currentIndex != -1 && currentIndex + 1 < sortedRooms.size) {
                sortedRooms[currentIndex + 1]
            } else null

            _uiState.value = LensUiState.RoomCompleted(updatedRoom, nextRoom)
            return true
        }
        return false
    }

    fun advanceFromRoomCompleted() {
        val currentState = _uiState.value
        if (currentState is LensUiState.RoomCompleted) {
            val nextRoom = currentState.nextRoom
            if (nextRoom != null) {
                _activeRoomId.value = nextRoom.id
                if (nextRoom.status == RoomStatus.PENDING) {
                    updateRoomStatus(nextRoom.id, RoomStatus.IN_PROGRESS)
                }
                val updatedNextRoom = _rooms.value.find { it.id == nextRoom.id } ?: nextRoom
                _uiState.value = LensUiState.RoomOverview(updatedNextRoom)
            } else {
                finishWholeHouse()
            }
        }
    }

    fun returnToEditRoom(roomId: String) {
        val targetRoom = _rooms.value.find { it.id == roomId }
        if (targetRoom != null) {
            _activeRoomId.value = targetRoom.id
            _scanSession.value = emptyList()
            _currentFrameDetections.value = emptyList()
            _uiState.value = LensUiState.Review(targetRoom, targetRoom.appliances, isEditMode = true)
        }
    }

    fun finishWholeHouse() {
        val allRooms = _rooms.value
        _uiState.value = LensUiState.WholeHouseAnalytics(rooms = allRooms, saveStatus = SaveStatus.SAVING)

        // Asynchronously persist audited appliances to backend
        viewModelScope.launch(ioDispatcher) {
            try {
                var hadFailure = false
                val updatedRooms = allRooms.map { room ->
                    val updatedAppliances = room.appliances.map { appliance ->
                        if (appliance.serverId == null) {
                            val request = ApplianceCreateRequest(
                                applianceName = "${room.name} - ${appliance.label}",
                                applianceType = appliance.label,
                                powerRatingWatts = appliance.powerWatts,
                                dailyUsageHours = appliance.usageHoursPerDay,
                                quantity = appliance.quantity,
                                monthlyConsumptionKwh = appliance.monthlyKwh,
                                estimatedMonthlyCost = appliance.monthlyCost
                            )
                            val res = repository.createAppliance(request)
                            if (res.isSuccess) {
                                appliance.copy(serverId = res.getOrNull()?.id)
                            } else {
                                hadFailure = true
                                appliance
                            }
                        } else {
                            appliance
                        }
                    }
                    room.copy(appliances = updatedAppliances)
                }

                _rooms.value = updatedRooms
                com.example.feature.lens.data.LensRoomStorage.saveRooms(updatedRooms)

                withContext(Dispatchers.Main) {
                    if (_uiState.value is LensUiState.WholeHouseAnalytics) {
                        _uiState.value = LensUiState.WholeHouseAnalytics(
                            rooms = updatedRooms,
                            saveStatus = if (hadFailure) SaveStatus.PARTIAL_FAILURE else SaveStatus.SUCCESS
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (_uiState.value is LensUiState.WholeHouseAnalytics) {
                        _uiState.value = LensUiState.WholeHouseAnalytics(
                            rooms = allRooms,
                            saveStatus = SaveStatus.FAILED,
                            errorMessage = e.message
                        )
                    }
                }
            }
        }
    }

    fun processImagesAsync(images: List<ByteArray>, source: String, contextRoom: Room) {
        _uiState.value = LensUiState.Processing(contextRoom, "Analyzing ${images.size} image(s) with Gemini 3.8 Flash...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch {
            val currentSession = mutableListOf<ScannedAppliance>()
            for (imageBytes in images) {
                if (!isActive) return@launch
                val result = uploadAnalyzer.analyzeImage(imageBytes, source)
                if (!isActive) return@launch
                result.onSuccess { detectionResult ->
                    val currentTime = System.currentTimeMillis()
                    for (det in detectionResult.detections) {
                        val label = det.applianceType ?: det.label
                        val defaults = ApplianceMaster.getDefaults(label)
                        val powerWatts = det.powerRatingWatts ?: 0f
                        val powerEstimateStr = if (powerWatts > 0f) {
                            "${powerWatts.toInt()} W"
                        } else {
                            "Unverified Load (Requires Input)"
                        }

                        val rectF = android.graphics.RectF(
                            det.boundingBox.left,
                            det.boundingBox.top,
                            det.boundingBox.right,
                            det.boundingBox.bottom
                        )

                        val qty = det.quantity ?: 1
                        val existingIndex = currentSession.indexOfFirst { it.label.equals(label, ignoreCase = true) }
                        if (existingIndex == -1) {
                            currentSession.add(
                                ScannedAppliance(
                                    label = defaults?.name ?: label,
                                    powerWatts = powerWatts,
                                    powerEstimateStr = powerEstimateStr,
                                    boundingBox = rectF,
                                    lastTrackingId = null,
                                    lastSeenTimestamp = currentTime,
                                    quantity = qty,
                                    evidence = det.evidence
                                )
                            )
                        } else {
                            val existing = currentSession[existingIndex]
                            currentSession[existingIndex] = existing.copy(
                                quantity = existing.quantity + qty
                            )
                        }
                    }
                }.onFailure { e ->
                    if (e is CancellationException || !isActive) return@launch
                    withContext(Dispatchers.Main) {
                        _uiState.value = LensUiState.Error(e.message ?: "Analysis failed.", LensUiState.RoomOverview(contextRoom))
                    }
                    return@launch
                }
            }

            _scanSession.value = currentSession
            if (_uiState.value is LensUiState.Processing) {
                finishScan()
            }
        }
    }

    fun processVideoUploadAsync(context: android.content.Context, uri: android.net.Uri, contextRoom: Room) {
        _uiState.value = LensUiState.Processing(contextRoom, "Analyzing video with Gemini 3.8 Flash...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val videoBytes = inputStream?.readBytes()
                inputStream?.close()

                if (videoBytes == null || videoBytes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        _uiState.value = LensUiState.Error("Could not read the selected video file.", LensUiState.RoomOverview(contextRoom))
                    }
                    return@launch
                }

                val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                val result = uploadAnalyzer.analyzeVideo(videoBytes, mimeType, "UPLOAD_VIDEO")

                if (!isActive) return@launch

                result.onSuccess { detectionResult ->
                    val currentSession = mutableListOf<ScannedAppliance>()
                    val currentTime = System.currentTimeMillis()
                    for (det in detectionResult.detections) {
                        val label = det.applianceType ?: det.label
                        val defaults = ApplianceMaster.getDefaults(label)
                        val powerWatts = det.powerRatingWatts ?: 0f
                        val powerEstimateStr = if (powerWatts > 0f) {
                            "${powerWatts.toInt()} W"
                        } else {
                            "Unverified Load (Requires Input)"
                        }

                        val rectF = android.graphics.RectF(
                            det.boundingBox.left,
                            det.boundingBox.top,
                            det.boundingBox.right,
                            det.boundingBox.bottom
                        )

                        val qty = det.quantity ?: 1
                        val existingIndex = currentSession.indexOfFirst { it.label.equals(label, ignoreCase = true) }
                        if (existingIndex == -1) {
                            currentSession.add(
                                ScannedAppliance(
                                    label = defaults?.name ?: label,
                                    powerWatts = powerWatts,
                                    powerEstimateStr = powerEstimateStr,
                                    boundingBox = rectF,
                                    lastTrackingId = null,
                                    lastSeenTimestamp = currentTime,
                                    quantity = qty,
                                    evidence = det.evidence
                                )
                            )
                        } else {
                            val existing = currentSession[existingIndex]
                            currentSession[existingIndex] = existing.copy(
                                quantity = existing.quantity + qty
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _scanSession.value = currentSession
                        if (_uiState.value is LensUiState.Processing) {
                            finishScan()
                        }
                    }
                }.onFailure { e ->
                    if (e is CancellationException || !isActive) return@launch
                    withContext(Dispatchers.Main) {
                        _uiState.value = LensUiState.Error(e.message ?: "Video analysis failed.", LensUiState.RoomOverview(contextRoom))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || !isActive) return@launch
                withContext(Dispatchers.Main) {
                    _uiState.value = LensUiState.Error(e.message ?: "Could not process the selected video.", LensUiState.RoomOverview(contextRoom))
                }
            }
        }
    }
    
    fun processUrisAsync(context: android.content.Context, uris: List<android.net.Uri>, contextRoom: Room) {
        val firstUri = uris.firstOrNull() ?: return
        val type = context.contentResolver.getType(firstUri) ?: ""
        val isVideo = type.startsWith("video/") || firstUri.toString().endsWith(".mp4", ignoreCase = true) || firstUri.toString().endsWith(".mov", ignoreCase = true)

        if (isVideo) {
            processVideoUploadAsync(context, firstUri, contextRoom)
            return
        }

        clearSelectedPhotos()
        _uiState.value = LensUiState.Processing(contextRoom, "Preparing images...")

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
                        }
                    }
                } catch (e: Exception) {
                    if (e is CancellationException || !isActive) return@launch
                }
            }

            if (!isActive) return@launch

            if (byteArrayList.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        processImagesAsync(byteArrayList, "UPLOAD_IMAGE", contextRoom)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = LensUiState.Error("Could not process the selected image(s).", LensUiState.RoomOverview(contextRoom))
                }
            }
        }
    }

    /**
     * Dedicated Scan Flow handler for Instant Photo captures.
     * Operates purely on-device via local vision. Does NOT call Gemini.
     */
    fun processPhotoScan(
        imageBytes: ByteArray,
        scanDetector: com.example.feature.lens.detector.LensScanDetector,
        contextRoom: Room
    ) {
        android.util.Log.i("LENS_SCAN", "[LENS_SCAN] provider=on_device_vision mode=instant_photo size=${imageBytes.size}")
        _uiState.value = LensUiState.Processing(contextRoom, "Processing photo scan with on-device vision...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
            if (bitmap != null) {
                scanDetector.processInstantPhoto(bitmap)
                bitmap.recycle()
            }
            withContext(Dispatchers.Main) {
                if (_uiState.value is LensUiState.Processing) {
                    finishScan()
                }
            }
        }
    }

    fun processVideoAsync(videoFile: java.io.File, contextRoom: Room) {
        _uiState.value = LensUiState.Processing(contextRoom, "Analyzing video with Gemini 3.8 Flash...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val videoBytes = videoFile.readBytes()
                val result = uploadAnalyzer.analyzeVideo(videoBytes, "video/mp4", "UPLOAD_VIDEO")
                result.onSuccess { detectionResult ->
                    val currentSession = mutableListOf<ScannedAppliance>()
                    val currentTime = System.currentTimeMillis()
                    for (det in detectionResult.detections) {
                        val label = det.applianceType ?: det.label
                        val defaults = ApplianceMaster.getDefaults(label)
                        val powerWatts = det.powerRatingWatts ?: 0f
                        val powerEstimateStr = if (powerWatts > 0f) {
                            "${powerWatts.toInt()} W"
                        } else {
                            "Unverified Load (Requires Input)"
                        }

                        val rectF = android.graphics.RectF(
                            det.boundingBox.left,
                            det.boundingBox.top,
                            det.boundingBox.right,
                            det.boundingBox.bottom
                        )

                        val qty = det.quantity ?: 1
                        val existingIndex = currentSession.indexOfFirst { it.label.equals(label, ignoreCase = true) }
                        if (existingIndex == -1) {
                            currentSession.add(
                                ScannedAppliance(
                                    label = defaults?.name ?: label,
                                    powerWatts = powerWatts,
                                    powerEstimateStr = powerEstimateStr,
                                    boundingBox = rectF,
                                    lastTrackingId = null,
                                    lastSeenTimestamp = currentTime,
                                    quantity = qty,
                                    evidence = det.evidence
                                )
                            )
                        } else {
                            val existing = currentSession[existingIndex]
                            currentSession[existingIndex] = existing.copy(
                                quantity = existing.quantity + qty
                            )
                        }
                    }

                    withContext(Dispatchers.Main) {
                        _scanSession.value = currentSession
                        if (_uiState.value is LensUiState.Processing) {
                            finishScan()
                        }
                    }
                }.onFailure { e ->
                    if (e is CancellationException || !isActive) return@launch
                    withContext(Dispatchers.Main) {
                        _uiState.value = LensUiState.Error(e.message ?: "Video analysis failed.", LensUiState.RoomOverview(contextRoom))
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException || !isActive) return@launch
                withContext(Dispatchers.Main) {
                    _uiState.value = LensUiState.Error(e.message ?: "Could not process video.", LensUiState.RoomOverview(contextRoom))
                }
            }
        }
    }

    fun dismissError() {
        val currentState = _uiState.value
        if (currentState is LensUiState.Error) {
            _uiState.value = currentState.previousState ?: LensUiState.Introduction
        }
    }

    fun popUiState() {
        val currentState = _uiState.value
        when (currentState) {
            is LensUiState.WholeHouseAnalytics -> {
                val sortedRooms = _rooms.value.sortedBy { it.order }
                val lastRoom = sortedRooms.lastOrNull { it.status == RoomStatus.COMPLETED }
                if (lastRoom != null) {
                    _activeRoomId.value = lastRoom.id
                    _uiState.value = LensUiState.RoomOverview(lastRoom)
                } else {
                    _uiState.value = LensUiState.RoomSetup
                }
            }
            is LensUiState.RoomCompleted -> {
                _uiState.value = LensUiState.RoomOverview(currentState.room)
            }
            is LensUiState.RoomOverview -> navigateToPreviousWorkspace()
            is LensUiState.Review -> {
                _scanSession.value = emptyList()
                _currentFrameDetections.value = emptyList()
                val latestRoom = _rooms.value.find { it.id == currentState.room.id } ?: currentState.room
                _uiState.value = LensUiState.RoomOverview(latestRoom)
            }
            is LensUiState.CameraActive -> {
                _scanSession.value = emptyList()
                _currentFrameDetections.value = emptyList()
                val latestRoom = _rooms.value.find { it.id == currentState.room.id } ?: currentState.room
                _uiState.value = LensUiState.RoomOverview(latestRoom)
            }
            is LensUiState.RoomSetup -> _uiState.value = LensUiState.Introduction
            else -> { /* Let NavGraph handle it */ }
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeAnalysisJob?.cancel()
        overlayTtlJob?.cancel()
    }
}

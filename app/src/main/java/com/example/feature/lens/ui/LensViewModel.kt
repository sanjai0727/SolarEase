package com.example.feature.lens.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.detector.DetectedCandidate
import com.example.feature.lens.utils.ApplianceMaster
import com.example.feature.lens.utils.DetectedAppliance

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

enum class RoomStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    SKIPPED
}

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
    
    data class Review(val room: Room, val newAppliances: List<EditableAppliance>) : LensUiState()

    data class RoomCompleted(val room: Room, val nextRoom: Room?) : LensUiState()
    
    data class Error(val message: String, val previousState: LensUiState? = null) : LensUiState()
    
    data class WholeHouseAnalytics(
        val rooms: List<Room>,
        val saveStatus: SaveStatus = SaveStatus.SUCCESS,
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
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

class LensViewModel(
    private val repository: LensRepository
) : ViewModel() {

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

    private var activeAnalysisJob: Job? = null
    private var overlayTtlJob: Job? = null

    init {
        // Initialize with default rooms
        _rooms.value = listOf(
            Room(name = "Bedroom", order = 0),
            Room(name = "Hall", order = 1),
            Room(name = "Bathroom", order = 2),
            Room(name = "Kitchen", order = 3)
        )
    }

    fun addRoom(name: String) {
        val newOrder = _rooms.value.size
        val newRoom = Room(name = name, order = newOrder)
        _rooms.value = _rooms.value + newRoom
    }

    fun removeRoom(roomId: String) {
        val updated = _rooms.value.filter { it.id != roomId }.sortedBy { it.order }
        _rooms.value = updated.mapIndexed { index, room -> room.copy(order = index) }
    }

    fun reorderRooms(fromIndex: Int, toIndex: Int) {
        val current = _rooms.value.sortedBy { it.order }.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _rooms.value = current.mapIndexed { index, room -> room.copy(order = index) }
        }
    }

    fun startRoomSetup() {
        _uiState.value = LensUiState.RoomSetup
    }

    fun proceedToScanning() {
        val sortedRooms = _rooms.value.sortedBy { it.order }
        val firstRoom = sortedRooms.firstOrNull()
        if (firstRoom != null) {
            _activeRoomId.value = firstRoom.id
            if (firstRoom.status == RoomStatus.PENDING) {
                updateRoomStatus(firstRoom.id, RoomStatus.IN_PROGRESS)
            }
            val updated = _rooms.value.find { it.id == firstRoom.id } ?: firstRoom
            _uiState.value = LensUiState.RoomOverview(updated)
        } else {
            finishWholeHouse()
        }
    }

    private fun updateRoomStatus(roomId: String, status: RoomStatus) {
        _rooms.value = _rooms.value.map {
            if (it.id == roomId) it.copy(status = status) else it
        }
    }

    fun skipCurrentRoom() {
        val currentState = _uiState.value
        if (currentState is LensUiState.RoomOverview) {
            updateRoomStatus(currentState.room.id, RoomStatus.SKIPPED)
            moveToNextRoom()
        }
    }

    private fun moveToNextRoom() {
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
            val defaults = ApplianceMaster.getDefaults(label)
            val powerWatts = defaults?.defaultWattage ?: 100f
            val powerEstimateStr = "Est. ~${powerWatts.toInt()} W"

            frameDetections.add(
                DetectedAppliance(
                    trackingId = candidate.trackingId,
                    boundingBox = candidate.boundingBox,
                    boundingBoxRelative = candidate.boundingBoxRelative,
                    label = label,
                    powerEstimateStr = powerEstimateStr,
                    powerWatts = powerWatts,
                    isVerified = true,
                    isVerifying = false
                )
            )

            val matchedIndex = currentSession.indexOfFirst { it.lastTrackingId == candidate.trackingId || (it.label == label && now - it.lastSeenTimestamp < 3000L) }
            if (matchedIndex != -1) {
                currentSession[matchedIndex] = currentSession[matchedIndex].copy(
                    boundingBox = candidate.boundingBoxRelative,
                    lastTrackingId = candidate.trackingId,
                    lastSeenTimestamp = now
                )
            } else {
                currentSession.add(
                    ScannedAppliance(
                        label = label,
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
        _uiState.value = LensUiState.Review(room, editableAppliances)
    }

    fun cancelAnalysis() {
        activeAnalysisJob?.cancel()
        activeAnalysisJob = null
        
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

    fun confirmReview() {
        val currentState = _uiState.value
        if (currentState is LensUiState.Review) {
            val room = currentState.room
            val updatedRoom = room.copy(
                appliances = (room.appliances + currentState.newAppliances).toList(),
                status = RoomStatus.COMPLETED
            )
            _rooms.value = _rooms.value.map { r ->
                if (r.id == room.id) updatedRoom else r
            }

            val sortedRooms = _rooms.value.sortedBy { it.order }
            val currentIndex = sortedRooms.indexOfFirst { it.id == room.id }
            val nextRoom = if (currentIndex != -1 && currentIndex + 1 < sortedRooms.size) {
                sortedRooms[currentIndex + 1]
            } else null

            _uiState.value = LensUiState.RoomCompleted(updatedRoom, nextRoom)
        }
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
            _uiState.value = LensUiState.Review(targetRoom, emptyList())
        }
    }

    fun finishWholeHouse() {
        val allRooms = _rooms.value
        _uiState.value = LensUiState.WholeHouseAnalytics(rooms = allRooms, saveStatus = SaveStatus.SAVING)

        // Asynchronously persist audited appliances to backend
        viewModelScope.launch(Dispatchers.IO) {
            try {
                var hadFailure = false
                for (room in allRooms) {
                    for (appliance in room.appliances) {
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
                        if (res.isFailure) {
                            hadFailure = true
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    if (_uiState.value is LensUiState.WholeHouseAnalytics) {
                        _uiState.value = LensUiState.WholeHouseAnalytics(
                            rooms = allRooms,
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
        _uiState.value = LensUiState.Processing(contextRoom, "Analyzing ${images.size} image(s)...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch {
            val currentSession = mutableListOf<ScannedAppliance>()
            for (imageBytes in images) {
                if (!isActive) return@launch
                val result = repository.analyzeImage(imageBytes, source)
                if (!isActive) return@launch
                result.onSuccess { detectionResult ->
                    val currentTime = System.currentTimeMillis()
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

                        val existingIndex = currentSession.indexOfFirst { it.label == label }
                        if (existingIndex == -1) {
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
    
    fun processUrisAsync(context: android.content.Context, uris: List<android.net.Uri>, contextRoom: Room) {
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
                        processImagesAsync(byteArrayList, "PHOTO", contextRoom)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = LensUiState.Error("Could not process the selected image(s).", LensUiState.RoomOverview(contextRoom))
                }
            }
        }
    }

    fun processVideoAsync(videoFile: java.io.File, contextRoom: Room) {
        _uiState.value = LensUiState.Processing(contextRoom, "Extracting video frames...")

        activeAnalysisJob?.cancel()
        activeAnalysisJob = viewModelScope.launch(Dispatchers.IO) {
            val frames = com.example.feature.lens.utils.VideoFrameExtractor.extractFrames(videoFile, 3)
            if (!isActive) return@launch
            if (frames.isNotEmpty()) {
                withContext(Dispatchers.Main) {
                    if (isActive) {
                        processImagesAsync(frames, "VIDEO", contextRoom)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    _uiState.value = LensUiState.Error("Failed to extract frames from video", LensUiState.RoomOverview(contextRoom))
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
            is LensUiState.RoomCompleted -> {
                _uiState.value = LensUiState.RoomOverview(currentState.room)
            }
            is LensUiState.RoomOverview -> navigateToPreviousWorkspace()
            is LensUiState.Review -> {
                val latestRoom = _rooms.value.find { it.id == currentState.room.id } ?: currentState.room
                _uiState.value = LensUiState.RoomOverview(latestRoom)
            }
            is LensUiState.CameraActive -> {
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

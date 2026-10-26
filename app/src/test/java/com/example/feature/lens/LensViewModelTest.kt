package com.example.feature.lens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.DetectionResult
import com.example.feature.lens.data.LensApi
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.data.LensRoomStorage
import com.example.feature.lens.ui.CameraMode
import com.example.feature.lens.ui.EditableAppliance
import com.example.feature.lens.ui.LensViewModel
import com.example.feature.lens.ui.Room
import com.example.feature.lens.ui.RoomStatus
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response
import com.example.feature.lens.ui.LensUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

class FakeLensApi : LensApi {
    var createdCount = 0
    val createdRequests = mutableListOf<ApplianceCreateRequest>()

    override suspend fun getAppliances(): Response<List<Appliance>> = Response.success(emptyList())
    override suspend fun getAppliance(id: String): Response<Appliance> =
        Response.success(
            Appliance(
                id = id,
                applianceName = "Test",
                applianceType = "Test",
                powerRatingWatts = 100f,
                dailyUsageHours = 4f,
                quantity = 1,
                monthlyConsumptionKwh = 12f,
                estimatedMonthlyCost = 84f
            )
        )
    override suspend fun createAppliance(request: ApplianceCreateRequest): Response<Appliance> {
        createdCount++
        createdRequests.add(request)
        return Response.success(
            Appliance(
                id = "server_app_$createdCount",
                applianceName = request.applianceName,
                applianceType = request.applianceType,
                powerRatingWatts = request.powerRatingWatts,
                dailyUsageHours = request.dailyUsageHours,
                quantity = request.quantity,
                monthlyConsumptionKwh = request.monthlyConsumptionKwh,
                estimatedMonthlyCost = request.estimatedMonthlyCost
            )
        )
    }
    override suspend fun updateAppliance(id: String, request: ApplianceCreateRequest): Response<Appliance> =
        Response.success(
            Appliance(
                id = id,
                applianceName = request.applianceName,
                applianceType = request.applianceType,
                powerRatingWatts = request.powerRatingWatts,
                dailyUsageHours = request.dailyUsageHours,
                quantity = request.quantity,
                monthlyConsumptionKwh = request.monthlyConsumptionKwh,
                estimatedMonthlyCost = request.estimatedMonthlyCost
            )
        )
    var analyzeResult: DetectionResult = DetectionResult(emptyList(), emptyList())
    override suspend fun deleteAppliance(id: String): Response<Unit> = Response.success(Unit)
    override suspend fun analyzeImage(image: MultipartBody.Part, source: RequestBody): Response<DetectionResult> =
        Response.success(analyzeResult)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LensViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var viewModel: LensViewModel
    private val fakeApi = FakeLensApi()
    private val repository = LensRepository(fakeApi, testDispatcher)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        LensRoomStorage.init(context)
        LensRoomStorage.clear()
        viewModel = LensViewModel(repository, testDispatcher)
    }

    @After
    fun tearDown() {
        LensRoomStorage.clear()
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state starts with default rooms loaded from storage`() {
        val rooms = viewModel.rooms.value
        assertEquals(4, rooms.size)
        assertEquals("Bedroom", rooms[0].name)
    }

    @Test
    fun `renameRoom successfully updates name with trimmed string and preserves id`() {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id

        val success = viewModel.renameRoom(originalId, "  Master Bedroom  ")
        assertTrue(success)

        val updatedRoom = viewModel.rooms.value.first { it.id == originalId }
        assertEquals("Master Bedroom", updatedRoom.name)
        assertEquals(originalId, updatedRoom.id)
    }

    @Test
    fun `renameRoom rejects empty or blank names without altering state`() {
        val firstRoom = viewModel.rooms.value.first()
        val originalName = firstRoom.name
        val originalId = firstRoom.id

        val blankSuccess = viewModel.renameRoom(originalId, "   ")
        assertFalse(blankSuccess)

        val emptySuccess = viewModel.renameRoom(originalId, "")
        assertFalse(emptySuccess)

        val roomAfterFailedRename = viewModel.rooms.value.first { it.id == originalId }
        assertEquals(originalName, roomAfterFailedRename.name)
    }

    @Test
    fun `renameRoom preserves appliances attached to the room`() {
        // Setup room with existing appliances
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id

        val appliance = EditableAppliance(
            id = "app_1",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        // Store room with appliances in storage
        val roomsWithAppliance = viewModel.rooms.value.map {
            if (it.id == originalId) it.copy(appliances = listOf(appliance), status = RoomStatus.COMPLETED) else it
        }
        LensRoomStorage.saveRooms(roomsWithAppliance)
        // Reload in new ViewModel
        val freshVm = LensViewModel(repository)
        assertEquals(1, freshVm.rooms.value.first { it.id == originalId }.appliances.size)

        // Rename the room
        val success = freshVm.renameRoom(originalId, "Guest Bedroom")
        assertTrue(success)

        val renamedRoom = freshVm.rooms.value.first { it.id == originalId }
        assertEquals("Guest Bedroom", renamedRoom.name)
        assertEquals(1, renamedRoom.appliances.size)
        assertEquals("Ceiling Fan", renamedRoom.appliances.first().label)
        assertEquals(75f, renamedRoom.appliances.first().powerWatts)
    }

    @Test
    fun `addRoom appends new room with pending status and persists`() {
        val initialCount = viewModel.rooms.value.size
        viewModel.addRoom("Dining Area")

        val updatedRooms = viewModel.rooms.value
        assertEquals(initialCount + 1, updatedRooms.size)
        val dining = updatedRooms.find { it.name == "Dining Area" }
        assertNotNull(dining)
        assertEquals(RoomStatus.PENDING, dining?.status)
    }

    @Test
    fun `removeRoom removes room and adjusts indices`() {
        val initialRooms = viewModel.rooms.value
        val toRemove = initialRooms[1]

        viewModel.removeRoom(toRemove.id)

        val afterRemoval = viewModel.rooms.value
        assertEquals(initialRooms.size - 1, afterRemoval.size)
        assertTrue(afterRemoval.none { it.id == toRemove.id })
    }

    @Test
    fun `returnToEditRoom populates review with existing appliances and confirmReview replaces without duplication`() {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id
        val existingAppliance = EditableAppliance(
            id = "app_initial",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val roomWithAppliance = firstRoom.copy(
            appliances = listOf(existingAppliance),
            status = RoomStatus.COMPLETED
        )
        LensRoomStorage.saveRooms(listOf(roomWithAppliance) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository)

        // Return to edit room
        testVm.returnToEditRoom(originalId)
        val state = testVm.uiState.value
        assertTrue("State should be Review", state is LensUiState.Review)
        val reviewState = state as LensUiState.Review
        assertEquals("Should contain 1 existing appliance", 1, reviewState.newAppliances.size)
        assertEquals("Ceiling Fan", reviewState.newAppliances.first().label)

        // Update appliance in review
        testVm.updateReviewAppliance("app_initial", "Ceiling Fan", 80f, 10f, 30, 2)
        val confirmSuccess = testVm.confirmReview()
        assertTrue("Confirmation should succeed", confirmSuccess)

        val updatedRoom = testVm.rooms.value.first { it.id == originalId }
        assertEquals("Room should still have exactly 1 appliance, no duplicates", 1, updatedRoom.appliances.size)
        assertEquals(80f, updatedRoom.appliances.first().powerWatts)
        assertEquals(2, updatedRoom.appliances.first().quantity)
    }

    @Test
    fun `startNewAudit resets rooms to pending, clears appliances, and sets state to RoomSetup`() {
        val roomWithApp = viewModel.rooms.value.first().copy(
            appliances = listOf(
                EditableAppliance(
                    id = "a1", label = "Light", powerWatts = 15f,
                    usageHoursPerDay = 5f, monthlyUsageDays = 30, quantity = 2
                )
            ),
            status = RoomStatus.COMPLETED
        )
        LensRoomStorage.saveRooms(listOf(roomWithApp) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository)
        assertEquals(1, testVm.rooms.value.first().appliances.size)

        testVm.startNewAudit()

        assertTrue("State should be RoomSetup", testVm.uiState.value is LensUiState.RoomSetup)
        assertTrue("All rooms must have PENDING status", testVm.rooms.value.all { it.status == RoomStatus.PENDING })
        assertTrue("All rooms must have empty appliances", testVm.rooms.value.all { it.appliances.isEmpty() })

        val storageRooms = LensRoomStorage.loadRooms()
        assertTrue(storageRooms.all { it.appliances.isEmpty() })
    }

    @Test
    fun `popUiState from WholeHouseAnalytics transitions back to RoomOverview or Setup`() {
        val completedRoom = viewModel.rooms.value.first().copy(status = RoomStatus.COMPLETED)
        viewModel.finishWholeHouse()
        assertTrue(viewModel.uiState.value is LensUiState.WholeHouseAnalytics)

        viewModel.popUiState()
        val newState = viewModel.uiState.value
        assertTrue("State should not be WholeHouseAnalytics after pop", newState !is LensUiState.WholeHouseAnalytics)
    }

    @Test
    fun `proceedToScanning always starts fresh from room 1 even if previous rooms were completed or in-progress`() {
        val rooms = viewModel.rooms.value
        val updatedRooms = listOf(
            rooms[0].copy(status = RoomStatus.COMPLETED),
            rooms[1].copy(status = RoomStatus.IN_PROGRESS),
            rooms[2].copy(status = RoomStatus.PENDING),
            rooms[3].copy(status = RoomStatus.PENDING)
        )
        LensRoomStorage.saveRooms(updatedRooms)
        val testVm = LensViewModel(repository)

        testVm.proceedToScanning()

        val state = testVm.uiState.value
        assertTrue("State should be RoomOverview", state is LensUiState.RoomOverview)
        val overview = state as LensUiState.RoomOverview
        assertEquals("Should always start fresh at room 0 (first room)", rooms[0].id, overview.room.id)
        assertEquals("First room status must be updated to IN_PROGRESS", RoomStatus.IN_PROGRESS, testVm.rooms.value[0].status)
    }

    @Test
    fun `fresh audit with all rooms PENDING opens the first room`() {
        val rooms = viewModel.rooms.value.map { it.copy(status = RoomStatus.PENDING) }
        LensRoomStorage.saveRooms(rooms)
        val testVm = LensViewModel(repository)

        testVm.proceedToScanning()

        val state = testVm.uiState.value
        assertTrue("State should be RoomOverview", state is LensUiState.RoomOverview)
        val overview = state as LensUiState.RoomOverview
        assertEquals("First room must be opened", rooms.first().id, overview.room.id)
        assertEquals("First room must be IN_PROGRESS", RoomStatus.IN_PROGRESS, testVm.rooms.value.first().status)
    }

    @Test
    fun `starting room-by-room audit does not navigate directly to WholeHouseAnalytics when all rooms were completed`() {
        val allCompleted = viewModel.rooms.value.map { it.copy(status = RoomStatus.COMPLETED) }
        LensRoomStorage.saveRooms(allCompleted)
        val testVm = LensViewModel(repository)

        testVm.proceedToScanning()

        assertFalse("Must not navigate directly to WholeHouseAnalytics", testVm.uiState.value is LensUiState.WholeHouseAnalytics)
        assertTrue("State should be RoomOverview", testVm.uiState.value is LensUiState.RoomOverview)
        val overview = testVm.uiState.value as LensUiState.RoomOverview
        assertEquals("Should open the first room", allCompleted.first().id, overview.room.id)
        assertEquals("First room should be IN_PROGRESS", RoomStatus.IN_PROGRESS, testVm.rooms.value.first().status)
    }

    @Test
    fun `a genuinely completed audit still opens results`() {
        val rooms = viewModel.rooms.value
        val lastIndex = rooms.size - 1
        val updatedRooms = rooms.mapIndexed { index, room ->
            if (index < lastIndex) {
                room.copy(status = RoomStatus.COMPLETED)
            } else {
                room.copy(
                    status = RoomStatus.IN_PROGRESS,
                    appliances = listOf(
                        EditableAppliance(id = "last_app", label = "Fan", powerWatts = 75f, usageHoursPerDay = 4f, monthlyUsageDays = 30, quantity = 1)
                    )
                )
            }
        }
        LensRoomStorage.saveRooms(updatedRooms)
        val testVm = LensViewModel(repository)

        val lastRoom = updatedRooms[lastIndex]
        testVm.returnToEditRoom(lastRoom.id)

        val confirmed = testVm.confirmReview()
        assertTrue("Review confirmation must succeed", confirmed)
        assertTrue("State must be RoomCompleted", testVm.uiState.value is LensUiState.RoomCompleted)
        val completedState = testVm.uiState.value as LensUiState.RoomCompleted
        assertNull("Next room must be null for the final room", completedState.nextRoom)

        testVm.advanceFromRoomCompleted()

        assertTrue("Final completed audit must transition to WholeHouseAnalytics", testVm.uiState.value is LensUiState.WholeHouseAnalytics)
    }

    @Test
    fun `proceedToScanning resets previous room statuses to pending and starts from room 1`() {
        val rooms = viewModel.rooms.value
        val updatedRooms = listOf(
            rooms[0].copy(status = RoomStatus.COMPLETED),
            rooms[1].copy(status = RoomStatus.PENDING),
            rooms[2].copy(status = RoomStatus.PENDING),
            rooms[3].copy(status = RoomStatus.PENDING)
        )
        LensRoomStorage.saveRooms(updatedRooms)
        val testVm = LensViewModel(repository)

        testVm.proceedToScanning()

        val state = testVm.uiState.value
        assertTrue("State should be RoomOverview", state is LensUiState.RoomOverview)
        val overview = state as LensUiState.RoomOverview
        assertEquals("Should start fresh at room 0", rooms[0].id, overview.room.id)
        assertEquals("First room status must be updated to IN_PROGRESS", RoomStatus.IN_PROGRESS, testVm.rooms.value[0].status)
    }

    @Test
    fun `scan another resets the audit and starts at the first room`() {
        val completedWithAppliances = viewModel.rooms.value.map { room ->
            room.copy(
                status = RoomStatus.COMPLETED,
                appliances = listOf(
                    EditableAppliance(id = "app_${room.id}", label = "Fan", powerWatts = 75f, usageHoursPerDay = 5f, monthlyUsageDays = 30, quantity = 1)
                )
            )
        }
        LensRoomStorage.saveRooms(completedWithAppliances)
        val testVm = LensViewModel(repository)

        testVm.startNewAudit()

        assertEquals("UI state must reset to RoomSetup", LensUiState.RoomSetup, testVm.uiState.value)
        assertTrue("All rooms must have status PENDING", testVm.rooms.value.all { it.status == RoomStatus.PENDING })
        assertTrue("All rooms must have appliances cleared", testVm.rooms.value.all { it.appliances.isEmpty() })

        testVm.proceedToScanning()

        val state = testVm.uiState.value
        assertTrue("State should be RoomOverview", state is LensUiState.RoomOverview)
        val overview = state as LensUiState.RoomOverview
        assertEquals("First room must be opened", completedWithAppliances.first().id, overview.room.id)
        assertTrue("First room must have empty appliances", overview.room.appliances.isEmpty())
    }

    @Test
    fun `back navigation does not cause a results-screen loop`() {
        val completedRooms = viewModel.rooms.value.map { it.copy(status = RoomStatus.COMPLETED) }
        LensRoomStorage.saveRooms(completedRooms)
        val testVm = LensViewModel(repository)

        testVm.finishWholeHouse()
        assertTrue(testVm.uiState.value is LensUiState.WholeHouseAnalytics)

        testVm.popUiState()
        assertTrue("State should be RoomOverview of last room", testVm.uiState.value is LensUiState.RoomOverview)
        val overview = testVm.uiState.value as LensUiState.RoomOverview
        assertEquals("Should be last completed room", completedRooms.last().id, overview.room.id)

        while (testVm.uiState.value is LensUiState.RoomOverview) {
            testVm.popUiState()
        }
        assertEquals("Should reach RoomSetup", LensUiState.RoomSetup, testVm.uiState.value)

        testVm.proceedToScanning()
        assertFalse("Must not loop back to WholeHouseAnalytics", testVm.uiState.value is LensUiState.WholeHouseAnalytics)
        assertTrue("Must open RoomOverview instead of loop", testVm.uiState.value is LensUiState.RoomOverview)
    }

    @Test
    fun `confirmReview rejects appliances with zero or negative wattage`() {
        val firstRoom = viewModel.rooms.value.first()
        viewModel.returnToEditRoom(firstRoom.id)
        viewModel.addReviewAppliance() // Defaults to 0 W

        val reviewState = viewModel.uiState.value as LensUiState.Review
        assertTrue("Contains 0W custom appliance", reviewState.newAppliances.any { it.powerWatts <= 0f })

        val result = viewModel.confirmReview()
        assertFalse("Confirmation must be rejected for 0W appliance", result)
        assertTrue("State should remain Review", viewModel.uiState.value is LensUiState.Review)
    }

    @Test
    fun `finishWholeHouse only creates unpersisted appliances and attaches serverId`() = runTest(testDispatcher) {
        val appAlreadySaved = EditableAppliance(
            id = "a_saved", label = "AC", powerWatts = 1500f,
            usageHoursPerDay = 4f, monthlyUsageDays = 30, quantity = 1,
            serverId = "server_ac_1"
        )
        val appUnsaved = EditableAppliance(
            id = "a_unsaved", label = "Fan", powerWatts = 75f,
            usageHoursPerDay = 8f, monthlyUsageDays = 30, quantity = 1,
            serverId = null
        )
        val room = viewModel.rooms.value.first().copy(
            appliances = listOf(appAlreadySaved, appUnsaved),
            status = RoomStatus.COMPLETED
        )
        LensRoomStorage.saveRooms(listOf(room) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository, testDispatcher)

        val beforeCount = fakeApi.createdCount
        testVm.finishWholeHouse()
        testScheduler.advanceUntilIdle()

        assertEquals("Only unpersisted appliance should be POSTed", beforeCount + 1, fakeApi.createdCount)
        val appliancesAfter = testVm.rooms.value.first().appliances
        assertTrue("All appliances should now have serverId", appliancesAfter.all { it.serverId != null })

        // Calling finishWholeHouse again should not POST again
        testVm.finishWholeHouse()
        testScheduler.advanceUntilIdle()
        assertEquals("No new appliances should be POSTed on subsequent finish", beforeCount + 1, fakeApi.createdCount)
    }

    @Test
    fun `LENS-BUG-003 onLocalCandidatesDetected marks generic objects as unverified with 0W and never 100W`() {
        val room = viewModel.rooms.value.first()
        viewModel.proceedToScanning() // room overview
        viewModel.startCamera(CameraMode.LIVE_SCAN)

        val unclassifiedCandidate = com.example.feature.lens.detector.DetectedCandidate(
            trackingId = 42,
            boundingBox = android.graphics.Rect(10, 10, 100, 100),
            boundingBoxRelative = android.graphics.RectF(0.1f, 0.1f, 0.5f, 0.5f),
            label = "Unclassified Candidate",
            confidence = null
        )

        viewModel.onLocalCandidatesDetected(listOf(unclassifiedCandidate))

        val detections = viewModel.currentFrameDetections.value
        assertEquals("Should have 1 detection in current frame", 1, detections.size)
        val det = detections.first()
        assertEquals("Unclassified Object", det.label)
        assertEquals("Must NOT assign arbitrary 100W load to generic objects", 0f, det.powerWatts, 0.001f)
        assertFalse("Must NOT mark generic object as verified appliance", det.isVerified)
        assertEquals("Unverified Candidate", det.powerEstimateStr)
    }

    @Test
    fun `LENS-BUG-003 finishScan maps unclassified candidates to Unclassified Appliance with 0W requiring user confirmation`() {
        val room = viewModel.rooms.value.first()
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)

        val unclassifiedCandidate = com.example.feature.lens.detector.DetectedCandidate(
            trackingId = 101,
            boundingBox = android.graphics.Rect(20, 20, 80, 80),
            boundingBoxRelative = android.graphics.RectF(0.2f, 0.2f, 0.6f, 0.6f),
            label = "Unclassified Candidate",
            confidence = null
        )

        viewModel.onLocalCandidatesDetected(listOf(unclassifiedCandidate))
        viewModel.finishScan()

        val state = viewModel.uiState.value
        assertTrue("State should transition to Review", state is LensUiState.Review)
        val review = state as LensUiState.Review
        val appliance = review.newAppliances.find { it.label == "Unclassified Appliance" }
        assertNotNull("Should contain Unclassified Appliance", appliance)
        assertEquals("Wattage must be 0W until user assigns it", 0f, appliance!!.powerWatts, 0.001f)

        val confirmed = viewModel.confirmReview()
        assertFalse("Review must block confirmation until user specifies wattage > 0W", confirmed)
    }

    @Test
    fun `LENS-BUG-003 recognized appliance candidate retains catalog defaults and verified status`() {
        val room = viewModel.rooms.value.first()
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)

        val fanCandidate = com.example.feature.lens.detector.DetectedCandidate(
            trackingId = 202,
            boundingBox = android.graphics.Rect(30, 30, 90, 90),
            boundingBoxRelative = android.graphics.RectF(0.3f, 0.3f, 0.7f, 0.7f),
            label = "Ceiling Fan",
            confidence = 0.9f
        )

        viewModel.onLocalCandidatesDetected(listOf(fanCandidate))

        val detections = viewModel.currentFrameDetections.value
        assertEquals(1, detections.size)
        val det = detections.first()
        assertEquals("Ceiling Fan", det.label)
        assertEquals(75f, det.powerWatts, 0.001f)
        assertTrue("Recognized appliance should be marked verified", det.isVerified)
        assertEquals("Est. ~75 W", det.powerEstimateStr)
    }

    @Test
    fun `photo upload with refrigerator isolates detections from existing room inventory and does not fabricate unrelated items`() = runTest(testDispatcher) {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id
        // Room already has an existing ceiling fan from earlier audit
        val existingFan = EditableAppliance(
            id = "prev_fan_1",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val roomWithFan = firstRoom.copy(
            appliances = listOf(existingFan),
            status = RoomStatus.IN_PROGRESS
        )
        LensRoomStorage.saveRooms(listOf(roomWithFan) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository, testDispatcher)

        // Mock backend returning ONLY a refrigerator (with null wattage)
        fakeApi.analyzeResult = DetectionResult(
            detections = listOf(
                com.example.feature.lens.data.Detection(
                    id = "det_fridge_1",
                    label = "Refrigerator",
                    confidence = 0.95f,
                    source = "UPLOAD",
                    boundingBox = com.example.feature.lens.data.BoundingBox(0f, 0f, 1f, 1f),
                    powerRatingWatts = null,
                    quantity = 1,
                    evidence = "Silver tall refrigerator unit clearly visible.",
                    applianceType = "Refrigerator",
                    applianceName = "Single Door Refrigerator"
                )
            )
        )

        testVm.processImagesAsync(listOf(byteArrayOf(1, 2, 3)), "UPLOAD_IMAGE", roomWithFan)
        testScheduler.advanceUntilIdle()

        val state = testVm.uiState.value
        assertTrue("State should transition to Review", state is LensUiState.Review)
        val review = state as LensUiState.Review

        // Crucial: newAppliances contains ONLY the newly detected refrigerator, NOT the previous ceiling fan or fabricated items
        assertEquals("Only newly detected items should appear in newAppliances", 1, review.newAppliances.size)
        val detected = review.newAppliances.first()
        assertEquals("Refrigerator (Single Door)", detected.label)
        assertFalse("Previous room fan must NOT leak into newAppliances as a newly detected item",
            review.newAppliances.any { it.label == "Ceiling Fan" })
        assertFalse("Unrelated AC must NOT be added",
            review.newAppliances.any { it.label.contains("Air Conditioner", ignoreCase = true) })
        assertFalse("Unrelated laptop must NOT be added",
            review.newAppliances.any { it.label.contains("Laptop", ignoreCase = true) })
        assertFalse("Unrelated tube light must NOT be added",
            review.newAppliances.any { it.label.contains("Tube Light", ignoreCase = true) })

        // Null wattage must be preserved as 0W unverified
        assertEquals("Null wattage from AI must remain 0W unverified", 0f, detected.powerWatts, 0.001f)

        // Confirmation must be blocked until wattage > 0W
        val confirmBlocked = testVm.confirmReview()
        assertFalse("Cannot confirm review with 0W unverified wattage", confirmBlocked)

        // User enters verified wattage in review (e.g. 150W)
        testVm.updateReviewAppliance(detected.id, detected.label, 150f, 24f, 30, 1)
        val confirmSuccess = testVm.confirmReview()
        assertTrue("Confirmation succeeds once wattage is calibrated", confirmSuccess)

        // Upon confirmation, room inventory merges existing fan and new refrigerator cleanly without duplicate entries
        val updatedRoom = testVm.rooms.value.first { it.id == originalId }
        assertEquals("Room inventory should now have exactly 2 appliances (existing fan + confirmed fridge)", 2, updatedRoom.appliances.size)
        assertTrue("Existing fan is preserved", updatedRoom.appliances.any { it.label == "Ceiling Fan" })
        assertTrue("New fridge is added", updatedRoom.appliances.any { it.label == "Refrigerator (Single Door)" })
    }

    @Test
    fun `scan session is cleared after finishScan and cancelAnalysis preventing cross-scan leakage`() = runTest(testDispatcher) {
        val room = viewModel.rooms.value.first()
        fakeApi.analyzeResult = DetectionResult(
            detections = listOf(
                com.example.feature.lens.data.Detection(
                    id = "det_1",
                    label = "Television",
                    confidence = 0.95f,
                    source = "UPLOAD",
                    boundingBox = com.example.feature.lens.data.BoundingBox(0f, 0f, 1f, 1f),
                    powerRatingWatts = 100f,
                    quantity = 1,
                    evidence = "Wall mounted LED television visible.",
                    applianceType = "Television",
                    applianceName = "Television (LED 43\")"
                )
            )
        )

        viewModel.processImagesAsync(listOf(byteArrayOf(1, 2)), "UPLOAD_IMAGE", room)
        testScheduler.advanceUntilIdle()

        // Verify state is Review and scanSession has been cleared
        assertTrue(viewModel.uiState.value is LensUiState.Review)
        assertTrue("scanSession must be cleared after finishScan", viewModel.scanSession.value.isEmpty())

        // Test cancelAnalysis also cleans up scanSession
        viewModel.processImagesAsync(listOf(byteArrayOf(1, 2)), "UPLOAD_IMAGE", room)
        viewModel.cancelAnalysis()
        assertTrue("scanSession must be empty after cancelAnalysis", viewModel.scanSession.value.isEmpty())
    }

    @Test
    fun `controlled test - existing Room 1 with multiple appliances, new scan returns only laptop, confirm review, reopen room, verify persistence and no duplicates`() = runTest(testDispatcher) {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id

        // Controlled setup: Room 1 contains several existing appliances
        val existingFan = EditableAppliance(
            id = "app_fan_1",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val existingFridge = EditableAppliance(
            id = "app_fridge_1",
            label = "Refrigerator (Single Door)",
            powerWatts = 150f,
            usageHoursPerDay = 24f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val existingTubeLight = EditableAppliance(
            id = "app_light_1",
            label = "LED Tube Light",
            powerWatts = 20f,
            usageHoursPerDay = 6f,
            monthlyUsageDays = 30,
            quantity = 2
        )
        val roomWithAppliances = firstRoom.copy(
            appliances = listOf(existingFan, existingFridge, existingTubeLight),
            status = RoomStatus.IN_PROGRESS
        )
        LensRoomStorage.saveRooms(listOf(roomWithAppliances) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository, testDispatcher)

        // Point 1 & 2: Backend response containing ONLY one laptop detection
        fakeApi.analyzeResult = DetectionResult(
            detections = listOf(
                com.example.feature.lens.data.Detection(
                    id = "det_laptop_1",
                    label = "Laptop",
                    confidence = 0.98f,
                    source = "UPLOAD",
                    boundingBox = com.example.feature.lens.data.BoundingBox(0.1f, 0.1f, 0.8f, 0.8f),
                    powerRatingWatts = 65f,
                    quantity = 1,
                    evidence = "Open laptop on computer desk clearly visible.",
                    applianceType = "Laptop",
                    applianceName = "Laptop"
                )
            )
        )

        testVm.processImagesAsync(listOf(byteArrayOf(10, 20, 30)), "UPLOAD_IMAGE", roomWithAppliances)
        testScheduler.advanceUntilIdle()

        // Point 3 & 4: Review screen state
        val state = testVm.uiState.value
        assertTrue("State should transition to Review", state is LensUiState.Review)
        val review = state as LensUiState.Review
        assertFalse("Scan review must have isEditMode = false", review.isEditMode)

        // Review screen must present ONLY the newly detected laptop, not the room's existing inventory
        assertEquals("Review screen must contain ONLY the newly detected laptop", 1, review.newAppliances.size)
        val newLaptop = review.newAppliances.first()
        assertEquals("Laptop", newLaptop.label)
        assertEquals(65f, newLaptop.powerWatts, 0.001f)
        assertEquals(1, newLaptop.quantity)

        // Point 5 & 6 & 7: User confirms review
        val confirmed = testVm.confirmReview()
        assertTrue("Review confirmation should succeed", confirmed)

        // Room 1 Completed screen input
        val completedState = testVm.uiState.value
        assertTrue("State must transition to RoomCompleted", completedState is LensUiState.RoomCompleted)
        val completedRoom = (completedState as LensUiState.RoomCompleted).room

        // Assert completed-room inventory preserves existing items and adds only confirmed new laptop
        assertEquals("Completed room must have exactly 4 appliances (3 existing + 1 new laptop)", 4, completedRoom.appliances.size)
        assertTrue("Existing Ceiling Fan preserved", completedRoom.appliances.any { it.label == "Ceiling Fan" && it.quantity == 1 })
        assertTrue("Existing Refrigerator preserved", completedRoom.appliances.any { it.label == "Refrigerator (Single Door)" && it.quantity == 1 })
        assertTrue("Existing LED Tube Light preserved", completedRoom.appliances.any { it.label == "LED Tube Light" && it.quantity == 2 })
        assertTrue("Confirmed new Laptop added", completedRoom.appliances.any { it.label == "Laptop" && it.quantity == 1 })

        // Assert each appliance is represented exactly once (no duplicates)
        val labels = completedRoom.appliances.map { it.label }
        assertEquals("Every appliance must be represented exactly once", labels.toSet().size, labels.size)

        // Reopening Room 1: verify persisted storage preserves the exact authoritative inventory
        val persistedRooms = LensRoomStorage.loadRooms()
        val persistedRoom1 = persistedRooms.first { it.id == originalId }
        assertEquals("Persisted room inventory matches completed room size", 4, persistedRoom1.appliances.size)
        assertEquals("Persisted room labels match completed room labels", labels, persistedRoom1.appliances.map { it.label })
    }

    @Test
    fun `quantity merging does not increase counts merely because an appliance with the same name already exists`() = runTest(testDispatcher) {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id
        val existingLaptop = EditableAppliance(
            id = "laptop_1",
            label = "Laptop",
            powerWatts = 65f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val existingFan = EditableAppliance(
            id = "fan_1",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 10f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val roomWithLaptop = firstRoom.copy(
            appliances = listOf(existingLaptop, existingFan),
            status = RoomStatus.IN_PROGRESS
        )
        LensRoomStorage.saveRooms(listOf(roomWithLaptop) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository, testDispatcher)

        // Rescanning/uploading photo of the same room that also detects 1 Laptop
        fakeApi.analyzeResult = DetectionResult(
            detections = listOf(
                com.example.feature.lens.data.Detection(
                    id = "det_laptop_duplicate",
                    label = "Laptop",
                    confidence = 0.96f,
                    source = "UPLOAD",
                    boundingBox = com.example.feature.lens.data.BoundingBox(0f, 0f, 1f, 1f),
                    powerRatingWatts = 65f,
                    quantity = 1,
                    evidence = "Work laptop on table.",
                    applianceType = "Laptop",
                    applianceName = "Laptop"
                )
            )
        )

        testVm.processImagesAsync(listOf(byteArrayOf(1, 2, 3)), "UPLOAD_IMAGE", roomWithLaptop)
        testScheduler.advanceUntilIdle()

        testVm.confirmReview()

        val updatedRoom = testVm.rooms.value.first { it.id == originalId }
        assertEquals("Room inventory should have exactly 2 appliances without duplicate cards", 2, updatedRoom.appliances.size)
        val mergedLaptop = updatedRoom.appliances.first { it.label == "Laptop" }
        assertEquals("Quantity merging must NOT increase count to 2 when appliance with same name is detected again", 1, mergedLaptop.quantity)
    }

    @Test
    fun `editing a completed room preserves intentional edits and deletions`() = runTest(testDispatcher) {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id

        val existingFan = EditableAppliance(
            id = "edit_fan_1",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 8f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val existingLaptop = EditableAppliance(
            id = "edit_laptop_1",
            label = "Laptop",
            powerWatts = 65f,
            usageHoursPerDay = 6f,
            monthlyUsageDays = 30,
            quantity = 1
        )
        val existingLight = EditableAppliance(
            id = "edit_light_1",
            label = "LED Tube Light",
            powerWatts = 20f,
            usageHoursPerDay = 5f,
            monthlyUsageDays = 30,
            quantity = 2
        )
        val completedRoom = firstRoom.copy(
            appliances = listOf(existingFan, existingLaptop, existingLight),
            status = RoomStatus.COMPLETED
        )
        LensRoomStorage.saveRooms(listOf(completedRoom) + viewModel.rooms.value.drop(1))
        val testVm = LensViewModel(repository, testDispatcher)

        // User intentionally clicks "Modify Room appliances"
        testVm.returnToEditRoom(originalId)

        val editState = testVm.uiState.value
        assertTrue("State must transition to Review in edit mode", editState is LensUiState.Review)
        val review = editState as LensUiState.Review
        assertTrue("isEditMode must be true", review.isEditMode)
        assertEquals(3, review.newAppliances.size)

        // User intentionally deletes the LED Tube Light
        testVm.removeReviewAppliance("edit_light_1")

        // User edits the Laptop power and quantity
        testVm.updateReviewAppliance("edit_laptop_1", "Laptop", 90f, 8f, 30, 2)

        // User confirms changes
        val confirmed = testVm.confirmReview()
        assertTrue("Confirm review succeeds", confirmed)

        // Assert completed room inventory contains exactly the edited appliances
        val updatedRoom = testVm.rooms.value.first { it.id == originalId }
        assertEquals("Room inventory should now have 2 appliances after intentional deletion", 2, updatedRoom.appliances.size)
        assertFalse("Deleted tube light is no longer present", updatedRoom.appliances.any { it.label == "LED Tube Light" })

        val updatedLaptop = updatedRoom.appliances.first { it.id == "edit_laptop_1" }
        assertEquals(90f, updatedLaptop.powerWatts, 0.001f)
        assertEquals(2, updatedLaptop.quantity)

        // Reopening room from storage verifies persistence preserves the edits
        val persisted = LensRoomStorage.loadRooms().first { it.id == originalId }
        assertEquals(2, persisted.appliances.size)
        assertFalse(persisted.appliances.any { it.label == "LED Tube Light" })
    }

    @Test
    fun `fresh audit does not inherit appliances from an earlier completed audit`() = runTest(testDispatcher) {
        val firstRoom = viewModel.rooms.value.first()
        val originalId = firstRoom.id

        // Earlier audit was completely finished, with rooms having appliances
        val earlierAuditedRooms = viewModel.rooms.value.map { r ->
            r.copy(
                status = RoomStatus.COMPLETED,
                appliances = listOf(
                    EditableAppliance(
                        id = "old_${r.name}",
                        label = "Old Fan in ${r.name}",
                        powerWatts = 75f,
                        usageHoursPerDay = 4f,
                        monthlyUsageDays = 30,
                        quantity = 1
                    )
                )
            )
        }
        LensRoomStorage.saveRooms(earlierAuditedRooms)
        val testVm = LensViewModel(repository, testDispatcher)

        // User proceeds to scanning a fresh audit
        testVm.proceedToScanning()

        // Verify all rooms were reset to PENDING and their old appliances cleared
        val freshRooms = testVm.rooms.value
        assertTrue("All rooms in fresh audit must have empty appliances", freshRooms.all { it.appliances.isEmpty() })
        assertTrue("Target room must be in progress or pending", freshRooms.any { it.status == RoomStatus.IN_PROGRESS })

        // Now user uploads a photo of a laptop into Room 1
        fakeApi.analyzeResult = DetectionResult(
            detections = listOf(
                com.example.feature.lens.data.Detection(
                    id = "det_fresh_laptop",
                    label = "Laptop",
                    confidence = 0.99f,
                    source = "UPLOAD",
                    boundingBox = com.example.feature.lens.data.BoundingBox(0f, 0f, 1f, 1f),
                    powerRatingWatts = 65f,
                    quantity = 1,
                    evidence = "Silver laptop on desk.",
                    applianceType = "Laptop",
                    applianceName = "Laptop"
                )
            )
        )

        testVm.processImagesAsync(listOf(byteArrayOf(1, 2)), "UPLOAD_IMAGE", freshRooms.first())
        testScheduler.advanceUntilIdle()

        testVm.confirmReview()

        val completedRoom1 = testVm.rooms.value.first { it.id == originalId }
        assertEquals("Completed Room 1 must contain ONLY the newly detected laptop (1 item)", 1, completedRoom1.appliances.size)
        assertEquals("Laptop", completedRoom1.appliances.first().label)
        assertFalse("Completed Room 1 must NOT inherit old appliances from earlier audit",
            completedRoom1.appliances.any { it.label.contains("Old Fan") })
    }

    @Test
    fun `proceedToScanning always starts fresh from room 1 resetting previous unfinished audit`() = runTest(testDispatcher) {
        val rooms = viewModel.rooms.value
        val room1 = rooms[0].copy(
            status = RoomStatus.COMPLETED,
            appliances = listOf(
                EditableAppliance(
                    id = "saved_room1_fan",
                    label = "Ceiling Fan",
                    powerWatts = 75f,
                    usageHoursPerDay = 8f,
                    monthlyUsageDays = 30,
                    quantity = 1
                )
            )
        )
        val room2 = rooms[1].copy(
            status = RoomStatus.IN_PROGRESS,
            appliances = emptyList()
        )
        val room3 = rooms[2].copy(
            status = RoomStatus.PENDING,
            appliances = emptyList()
        )
        val room4 = rooms[3].copy(
            status = RoomStatus.PENDING,
            appliances = emptyList()
        )
        LensRoomStorage.saveRooms(listOf(room1, room2, room3, room4))
        val testVm = LensViewModel(repository, testDispatcher)

        testVm.proceedToScanning()

        val activeRooms = testVm.rooms.value
        val resRoom1 = activeRooms.first { it.id == room1.id }
        assertEquals("Room 1 must be reset to IN_PROGRESS", RoomStatus.IN_PROGRESS, resRoom1.status)
        assertTrue("Room 1 appliances must be cleared for a fresh audit", resRoom1.appliances.isEmpty())

        assertEquals("Active room should always be Room 1", room1.id, testVm.activeRoomId.value)
        val state = testVm.uiState.value
        assertTrue("State should transition to RoomOverview for Room 1", state is LensUiState.RoomOverview)
        assertEquals(room1.id, (state as LensUiState.RoomOverview).room.id)
    }
}

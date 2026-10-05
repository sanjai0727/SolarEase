package com.example.feature.lens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.feature.lens.data.Appliance
import com.example.feature.lens.data.ApplianceCreateRequest
import com.example.feature.lens.data.DetectionResult
import com.example.feature.lens.data.LensApi
import com.example.feature.lens.data.LensRepository
import com.example.feature.lens.data.LensRoomStorage
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
import org.junit.Assert.assertTrue
import android.graphics.Rect
import android.graphics.RectF
import com.example.feature.lens.detector.DetectedCandidate
import com.example.feature.lens.ui.CameraMode
import com.example.feature.lens.ui.LensUiState
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

class FakeLensApi : LensApi {
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
    override suspend fun createAppliance(request: ApplianceCreateRequest): Response<Appliance> =
        Response.success(
            Appliance(
                id = "new",
                applianceName = request.applianceName,
                applianceType = request.applianceType,
                powerRatingWatts = request.powerRatingWatts,
                dailyUsageHours = request.dailyUsageHours,
                quantity = request.quantity,
                monthlyConsumptionKwh = request.monthlyConsumptionKwh,
                estimatedMonthlyCost = request.estimatedMonthlyCost
            )
        )
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
    override suspend fun deleteAppliance(id: String): Response<Unit> = Response.success(Unit)
    override suspend fun analyzeImage(image: MultipartBody.Part, source: RequestBody): Response<DetectionResult> =
        Response.success(DetectionResult(emptyList(), emptyList()))
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LensViewModelTest {

    private lateinit var viewModel: LensViewModel
    private val fakeApi = FakeLensApi()
    private val repository = LensRepository(fakeApi)

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LensRoomStorage.init(context)
        LensRoomStorage.clear()
        viewModel = LensViewModel(repository)
    }

    @After
    fun tearDown() {
        LensRoomStorage.clear()
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
    fun `certified room contains ONLY confirmed detected appliance in one-laptop Bedroom scenario`() {
        val bedroom = viewModel.rooms.value.first()
        assertEquals("Bedroom", bedroom.name)

        // 1. Start audit for Bedroom
        viewModel.proceedToScanning()
        val overviewState = viewModel.uiState.value as LensUiState.RoomOverview
        assertEquals("Bedroom", overviewState.room.name)

        // 2. Open camera scan
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        assertTrue(viewModel.uiState.value is LensUiState.CameraActive)

        // 3. Detect 1x Laptop
        val candidate = DetectedCandidate(
            trackingId = 101,
            boundingBox = Rect(0, 0, 100, 100),
            boundingBoxRelative = RectF(0f, 0f, 0.5f, 0.5f),
            label = "Laptop",
            confidence = 0.98f
        )
        viewModel.onLocalCandidatesDetected(listOf(candidate))

        // 4. Finish scan to enter review
        viewModel.finishScan()
        val reviewState = viewModel.uiState.value as LensUiState.Review
        assertEquals("Bedroom", reviewState.room.name)
        assertEquals(1, reviewState.newAppliances.size)
        val laptopReview = reviewState.newAppliances.first()
        assertEquals("Laptop", laptopReview.label)
        assertEquals(60f, laptopReview.powerWatts)
        assertEquals(6.0f, laptopReview.usageHoursPerDay)
        assertEquals(1, laptopReview.quantity)

        // 5. Confirm Bedroom & Continue
        viewModel.confirmReview()

        // 6. Verify Room Certified state
        val completedState = viewModel.uiState.value as LensUiState.RoomCompleted
        val certifiedRoom = completedState.room
        assertEquals("Bedroom", certifiedRoom.name)
        assertEquals(RoomStatus.COMPLETED, certifiedRoom.status)

        // Inventory must contain ONLY 1x Laptop
        assertEquals(1, certifiedRoom.appliances.size)
        val confirmedAppliance = certifiedRoom.appliances.first()
        assertEquals("Laptop", confirmedAppliance.label)
        assertEquals(60f, confirmedAppliance.powerWatts)
        assertEquals(6.0f, confirmedAppliance.usageHoursPerDay)
        assertEquals(1, confirmedAppliance.quantity)

        // Verify calculations match only the 1x Laptop (60W rated, 30W active, 6h/d, 30d -> 5.4 kWh, ₹37.80)
        assertEquals(60f, certifiedRoom.connectedLoadWatts, 0.001f)
        assertEquals(0.06f, certifiedRoom.connectedLoadKw, 0.001f)
        assertEquals(5.4f, certifiedRoom.monthlyKwh, 0.001f)
        assertEquals(37.8f, certifiedRoom.monthlyCost, 0.001f)

        // Verify NO default/mock appliances were inserted
        val applianceLabels = certifiedRoom.appliances.map { it.label }
        assertFalse(applianceLabels.contains("Table Fan"))
        assertFalse(applianceLabels.contains("Ceiling Fan"))
        assertFalse(applianceLabels.contains("Television"))
        assertFalse(applianceLabels.contains("LED Tube Light"))
        assertFalse(applianceLabels.contains("Refrigerator"))
        assertEquals(1, applianceLabels.count { it == "Laptop" })
    }

    @Test
    fun `user edits appliance in review before confirming reflects exact edited values`() {
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(
                DetectedCandidate(
                    trackingId = 1,
                    boundingBox = Rect(0, 0, 50, 50),
                    boundingBoxRelative = RectF(0f, 0f, 0.5f, 0.5f),
                    label = "Laptop",
                    confidence = 0.95f
                )
            )
        )
        viewModel.finishScan()

        val reviewState = viewModel.uiState.value as LensUiState.Review
        val laptopId = reviewState.newAppliances.first().id

        // User edits quantity to 2 and power to 80W
        viewModel.updateReviewAppliance(
            id = laptopId,
            label = "Laptop",
            powerWatts = 80f,
            usageHoursPerDay = 5f,
            monthlyUsageDays = 30,
            quantity = 2
        )

        viewModel.confirmReview()

        val completed = viewModel.uiState.value as LensUiState.RoomCompleted
        assertEquals(1, completed.room.appliances.size)
        val app = completed.room.appliances.first()
        assertEquals(80f, app.powerWatts)
        assertEquals(2, app.quantity)
        assertEquals(5f, app.usageHoursPerDay)

        // 80W rated * 2 = 160W = 0.16 kW connected load
        assertEquals(0.16f, completed.room.connectedLoadKw, 0.001f)
        // 40W active * 5h * 30d * 2 / 1000 = 12.0 kWh
        assertEquals(12.0f, completed.room.monthlyKwh, 0.001f)
        // 12.0 kWh * 7.0 = 84.0
        assertEquals(84.0f, completed.room.monthlyCost, 0.001f)
    }

    @Test
    fun `user removes appliance during review does not appear in certified room`() {
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(
                DetectedCandidate(1, Rect(0, 0, 10, 10), RectF(0f, 0f, 0.2f, 0.2f), "Laptop", 0.9f),
                DetectedCandidate(2, Rect(10, 10, 20, 20), RectF(0.2f, 0.2f, 0.4f, 0.4f), "Table Fan", 0.9f)
            )
        )
        viewModel.finishScan()

        val reviewState = viewModel.uiState.value as LensUiState.Review
        assertEquals(2, reviewState.newAppliances.size)

        val tableFan = reviewState.newAppliances.first { it.label == "Table Fan" }
        // User removes Table Fan
        viewModel.removeReviewAppliance(tableFan.id)

        val updatedReview = viewModel.uiState.value as LensUiState.Review
        assertEquals(1, updatedReview.newAppliances.size)
        assertEquals("Laptop", updatedReview.newAppliances.first().label)

        viewModel.confirmReview()

        val completed = viewModel.uiState.value as LensUiState.RoomCompleted
        assertEquals(1, completed.room.appliances.size)
        assertEquals("Laptop", completed.room.appliances.first().label)
        assertFalse(completed.room.appliances.any { it.label == "Table Fan" })
    }

    @Test
    fun `confirming room with zero appliances produces empty inventory and zero calculations`() {
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(
                DetectedCandidate(1, Rect(0, 0, 10, 10), RectF(0f, 0f, 0.2f, 0.2f), "Laptop", 0.9f)
            )
        )
        viewModel.finishScan()

        val reviewState = viewModel.uiState.value as LensUiState.Review
        // User removes the detected appliance
        viewModel.removeReviewAppliance(reviewState.newAppliances.first().id)

        // Confirm empty room
        viewModel.confirmReview()

        val completed = viewModel.uiState.value as LensUiState.RoomCompleted
        assertTrue(completed.room.appliances.isEmpty())
        assertEquals(0f, completed.room.connectedLoadWatts, 0.001f)
        assertEquals(0f, completed.room.connectedLoadKw, 0.001f)
        assertEquals(0f, completed.room.monthlyKwh, 0.001f)
        assertEquals(0f, completed.room.monthlyCost, 0.001f)
    }

    @Test
    fun `advance to next room does not inherit previous room appliances or detections`() {
        // Complete Bedroom with 1x Laptop
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(DetectedCandidate(1, Rect(0, 0, 10, 10), RectF(0f, 0f, 0.2f, 0.2f), "Laptop", 0.9f))
        )
        viewModel.finishScan()
        viewModel.confirmReview()

        // Advance to Hall
        viewModel.advanceFromRoomCompleted()
        val hallState = viewModel.uiState.value as LensUiState.RoomOverview
        assertEquals("Hall", hallState.room.name)
        assertTrue(hallState.room.appliances.isEmpty())

        // Scan in Hall with Table Fan
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(DetectedCandidate(2, Rect(0, 0, 10, 10), RectF(0f, 0f, 0.2f, 0.2f), "Table Fan", 0.9f))
        )
        viewModel.finishScan()
        viewModel.confirmReview()

        val hallCompleted = viewModel.uiState.value as LensUiState.RoomCompleted
        assertEquals("Hall", hallCompleted.room.name)
        assertEquals(1, hallCompleted.room.appliances.size)
        assertEquals("Table Fan", hallCompleted.room.appliances.first().label)
        assertFalse(hallCompleted.room.appliances.any { it.label == "Laptop" })

        // Check Bedroom still only has Laptop
        val bedroom = viewModel.rooms.value.first { it.name == "Bedroom" }
        assertEquals(1, bedroom.appliances.size)
        assertEquals("Laptop", bedroom.appliances.first().label)
        assertFalse(bedroom.appliances.any { it.label == "Table Fan" })
    }

    @Test
    fun `returnToEditRoom populates confirmed appliances and does not duplicate on re-confirmation`() {
        // Confirm Bedroom with 1x Laptop
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(DetectedCandidate(1, Rect(0, 0, 10, 10), RectF(0f, 0f, 0.2f, 0.2f), "Laptop", 0.9f))
        )
        viewModel.finishScan()
        viewModel.confirmReview()

        val completed = viewModel.uiState.value as LensUiState.RoomCompleted
        val bedroomId = completed.room.id

        // User taps "Edit Appliances"
        viewModel.returnToEditRoom(bedroomId)
        val editState = viewModel.uiState.value as LensUiState.Review
        assertEquals(1, editState.newAppliances.size)
        assertEquals("Laptop", editState.newAppliances.first().label)

        // User re-confirms without adding anything
        viewModel.confirmReview()

        val reconfirmed = viewModel.uiState.value as LensUiState.RoomCompleted
        assertEquals(1, reconfirmed.room.appliances.size)
        assertEquals("Laptop", reconfirmed.room.appliances.first().label)
    }

    @Test
    fun `confirmed room appliances persist across ViewModel reload`() {
        viewModel.proceedToScanning()
        viewModel.startCamera(CameraMode.LIVE_SCAN)
        viewModel.onLocalCandidatesDetected(
            listOf(DetectedCandidate(1, Rect(0, 0, 10, 10), RectF(0f, 0f, 0.2f, 0.2f), "Laptop", 0.9f))
        )
        viewModel.finishScan()
        viewModel.confirmReview()

        // Create new ViewModel instance to simulate app relaunch
        val reloadedVm = LensViewModel(repository)
        val savedBedroom = reloadedVm.rooms.value.first { it.name == "Bedroom" }
        assertEquals(RoomStatus.COMPLETED, savedBedroom.status)
        assertEquals(1, savedBedroom.appliances.size)
        assertEquals("Laptop", savedBedroom.appliances.first().label)
        assertEquals(0.06f, savedBedroom.connectedLoadKw, 0.001f)
        assertEquals(5.4f, savedBedroom.monthlyKwh, 0.001f)
        assertEquals(37.8f, savedBedroom.monthlyCost, 0.001f)
    }
}

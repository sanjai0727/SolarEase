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
}

package com.example.feature.lens

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.feature.lens.data.LensRoomStorage
import com.example.feature.lens.ui.EditableAppliance
import com.example.feature.lens.ui.Room
import com.example.feature.lens.ui.RoomStatus
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LensRoomStorageTest {

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        LensRoomStorage.init(context)
        LensRoomStorage.clear()
    }

    @After
    fun tearDown() {
        LensRoomStorage.clear()
    }

    @Test
    fun `loadRooms returns default rooms on empty storage`() {
        val rooms = LensRoomStorage.loadRooms()
        assertTrue("Expected default rooms to be loaded", rooms.isNotEmpty())
        assertEquals("Bedroom", rooms[0].name)
        assertEquals(4, rooms.size)
    }

    @Test
    fun `saveRooms and loadRooms persists renamed room`() {
        val initialRooms = LensRoomStorage.loadRooms()
        val originalRoom = initialRooms[0]
        val originalId = originalRoom.id

        // Rename the first room
        val updatedRooms = initialRooms.map { room ->
            if (room.id == originalId) room.copy(name = "Master Bedroom") else room
        }
        LensRoomStorage.saveRooms(updatedRooms)

        // Reload from storage
        val reloadedRooms = LensRoomStorage.loadRooms()
        val reloadedRoom = reloadedRooms.find { it.id == originalId }

        assertNotNull("Renamed room should exist with original ID", reloadedRoom)
        assertEquals("Master Bedroom", reloadedRoom?.name)
        assertEquals(originalId, reloadedRoom?.id)
    }

    @Test
    fun `renamed room preserves associated appliances across serialization`() {
        val initialRooms = LensRoomStorage.loadRooms()
        val targetRoom = initialRooms[0]

        // Associate appliances with the room
        val testAppliance = EditableAppliance(
            id = "app_101",
            label = "Ceiling Fan",
            powerWatts = 75f,
            usageHoursPerDay = 10f,
            monthlyUsageDays = 30,
            quantity = 2
        )
        val roomWithAppliances = targetRoom.copy(
            appliances = listOf(testAppliance),
            status = RoomStatus.COMPLETED
        )

        val updatedRooms = initialRooms.map {
            if (it.id == targetRoom.id) roomWithAppliances else it
        }
        LensRoomStorage.saveRooms(updatedRooms)

        // Now rename that room to "Kids Room"
        val renamedRooms = LensRoomStorage.loadRooms().map {
            if (it.id == targetRoom.id) it.copy(name = "Kids Room") else it
        }
        LensRoomStorage.saveRooms(renamedRooms)

        // Reload and verify appliances are intact
        val reloaded = LensRoomStorage.loadRooms().find { it.id == targetRoom.id }
        assertNotNull(reloaded)
        assertEquals("Kids Room", reloaded?.name)
        assertEquals(1, reloaded?.appliances?.size)
        assertEquals("Ceiling Fan", reloaded?.appliances?.first()?.label)
        assertEquals(75f, reloaded?.appliances?.first()?.powerWatts)
        assertEquals(2, reloaded?.appliances?.first()?.quantity)
    }

    @Test
    fun `new room addition persists with unique ID`() {
        val initialRooms = LensRoomStorage.loadRooms()
        val newRoom = Room(
            name = "Study Room",
            order = initialRooms.size,
            status = RoomStatus.PENDING
        )
        val combined = initialRooms + newRoom
        LensRoomStorage.saveRooms(combined)

        val reloaded = LensRoomStorage.loadRooms()
        assertEquals(initialRooms.size + 1, reloaded.size)
        val study = reloaded.find { it.name == "Study Room" }
        assertNotNull(study)
        assertNotEquals(initialRooms[0].id, study?.id)
    }
}

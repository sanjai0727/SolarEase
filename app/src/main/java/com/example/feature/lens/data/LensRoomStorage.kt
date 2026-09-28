package com.example.feature.lens.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.feature.lens.ui.Room
import com.example.feature.lens.ui.RoomStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local persistent storage for SolarEase Lens rooms.
 * Ensures room additions, removals, reorders, and renames persist across app restarts,
 * while maintaining stable room IDs so appliance associations remain intact.
 */
object LensRoomStorage {
    private const val TAG = "LensRoomStorage"
    private const val PREFS_NAME = "SolarEaseLensRoomPrefs"
    private const val KEY_ROOMS = "saved_household_rooms"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var sharedPreferences: SharedPreferences? = null

    fun init(context: Context) {
        if (sharedPreferences != null) return
        try {
            sharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize LensRoomStorage", e)
        }
    }

    fun init(prefs: SharedPreferences) {
        sharedPreferences = prefs
    }

    fun loadRooms(): List<Room> {
        val prefs = sharedPreferences ?: return defaultRooms()
        return try {
            val rawJson = prefs.getString(KEY_ROOMS, null)
            if (!rawJson.isNullOrBlank()) {
                val decoded = json.decodeFromString<List<Room>>(rawJson)
                if (decoded.isNotEmpty()) decoded.sortedBy { it.order } else defaultRooms()
            } else {
                val defaults = defaultRooms()
                saveRooms(defaults)
                defaults
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load rooms from storage, using defaults", e)
            defaultRooms()
        }
    }

    fun saveRooms(rooms: List<Room>) {
        val prefs = sharedPreferences ?: return
        try {
            val serialized = json.encodeToString(rooms)
            prefs.edit().putString(KEY_ROOMS, serialized).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist rooms", e)
        }
    }

    fun defaultRooms(): List<Room> = listOf(
        Room(name = "Bedroom", order = 0, status = RoomStatus.PENDING),
        Room(name = "Hall", order = 1, status = RoomStatus.PENDING),
        Room(name = "Bathroom", order = 2, status = RoomStatus.PENDING),
        Room(name = "Kitchen", order = 3, status = RoomStatus.PENDING)
    )

    fun clear() {
        sharedPreferences?.edit()?.remove(KEY_ROOMS)?.apply()
    }
}

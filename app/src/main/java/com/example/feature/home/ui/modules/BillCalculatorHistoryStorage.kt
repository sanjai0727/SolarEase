package com.example.feature.home.ui.modules

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local persistent storage for Bill Calculator calculation history using SharedPreferences and JSON.
 * Persists calculations across app restart, activity recreation, and device reboot.
 */
object BillCalculatorHistoryStorage {
    private const val TAG = "BillCalcHistoryStorage"
    private const val PREFS_NAME = "SolarEaseBillCalculatorPrefs"
    private const val KEY_CALCULATIONS = "saved_calculations_history"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var sharedPreferences: SharedPreferences? = null

    private val _historyFlow = MutableStateFlow<List<SavedCalculation>>(emptyList())
    val historyFlow: StateFlow<List<SavedCalculation>> = _historyFlow.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences != null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            loadHistory()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize BillCalculatorHistoryStorage", e)
        }
    }

    /**
     * For unit tests or custom shared preferences injection.
     */
    fun init(prefs: SharedPreferences) {
        sharedPreferences = prefs
        loadHistory()
    }

    private fun loadHistory() {
        val prefs = sharedPreferences ?: return
        try {
            val rawJson = prefs.getString(KEY_CALCULATIONS, null)
            if (!rawJson.isNullOrBlank()) {
                val list = json.decodeFromString<List<SavedCalculation>>(rawJson)
                _historyFlow.value = list.sortedByDescending { it.timestamp }
            } else {
                _historyFlow.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load calculation history from storage", e)
            _historyFlow.value = emptyList()
        }
    }

    fun getAll(): List<SavedCalculation> {
        return _historyFlow.value
    }

    fun saveCalculation(calculation: SavedCalculation) {
        // Prepend and ensure newest-first ordering by timestamp
        val updated = (listOf(calculation) + _historyFlow.value)
            .distinctBy { it.id }
            .sortedByDescending { it.timestamp }
        _historyFlow.value = updated
        persist(updated)
    }

    fun deleteCalculation(id: String) {
        val updated = _historyFlow.value.filter { it.id != id }
        _historyFlow.value = updated
        persist(updated)
    }

    fun clearHistory() {
        _historyFlow.value = emptyList()
        persist(emptyList())
    }

    private fun persist(list: List<SavedCalculation>) {
        val prefs = sharedPreferences ?: return
        try {
            val serialized = json.encodeToString(list)
            prefs.edit().putString(KEY_CALCULATIONS, serialized).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist calculation history", e)
        }
    }

    /**
     * Clears in-memory state and preferences.
     */
    fun resetForTesting() {
        _historyFlow.value = emptyList()
        sharedPreferences?.edit()?.clear()?.apply()
    }
}

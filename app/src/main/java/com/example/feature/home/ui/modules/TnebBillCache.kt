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
 * Singleton repository/cache for persisting and retrieving real TNEB bill data across SolarEase modules.
 */
object TnebBillCache {
    private const val TAG = "TnebBillCache"
    private const val PREFS_NAME = "SolarEaseTnebBillPrefs"
    private const val KEY_BILL_DATA = "cached_tneb_bill_data"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _billData = MutableStateFlow<TnebConsumerBillData?>(null)
    val billData: StateFlow<TnebConsumerBillData?> = _billData.asStateFlow()

    fun init(context: Context) {
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_BILL_DATA, null)
            if (!raw.isNullOrBlank()) {
                val parsed = json.decodeFromString<TnebConsumerBillData>(raw)
                _billData.value = parsed
                Log.d(TAG, "Restored cached TNEB bill data for consumer: ${parsed.consumerNumber}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore cached TNEB bill data", e)
        }
    }

    fun save(context: Context?, data: TnebConsumerBillData) {
        _billData.value = data
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val serialized = json.encodeToString(data)
                prefs.edit().putString(KEY_BILL_DATA, serialized).apply()
                Log.d(TAG, "Saved TNEB bill data to cache for consumer: ${data.consumerNumber}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist TNEB bill data", e)
            }
        }
    }

    fun clear(context: Context?) {
        _billData.value = null
        if (context != null) {
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(KEY_BILL_DATA).apply()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear TNEB bill cache", e)
            }
        }
    }
}

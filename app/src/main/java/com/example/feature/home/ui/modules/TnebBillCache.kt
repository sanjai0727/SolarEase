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
 * Ensures bill data survives cold app restarts by storing in SharedPreferences.
 */
object TnebBillCache {
    private const val TAG = "TnebBillCache"
    private const val PREFS_NAME = "SolarEaseTnebBillPrefs"
    private const val KEY_BILL_DATA = "cached_tneb_bill_data"

    private var appContext: Context? = null

    private fun logD(msg: String) { try { Log.d(TAG, msg) } catch (_: Throwable) {} }
    private fun logW(msg: String, tr: Throwable? = null) { try { Log.w(TAG, msg, tr) } catch (_: Throwable) {} }
    private fun logE(msg: String, tr: Throwable? = null) { try { Log.e(TAG, msg, tr) } catch (_: Throwable) {} }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val _billData = MutableStateFlow<TnebConsumerBillData?>(null)
    val billData: StateFlow<TnebConsumerBillData?> = _billData.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val ctx = appContext ?: return
        try {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val raw = prefs.getString(KEY_BILL_DATA, null)
            if (!raw.isNullOrBlank()) {
                val parsed = json.decodeFromString<TnebConsumerBillData>(raw)
                _billData.value = parsed
                logD("Restored cached TNEB bill data for consumer: ${parsed.consumerNumber}")
            }
        } catch (e: Exception) {
            logE("Failed to restore cached TNEB bill data", e)
        }
    }

    fun save(context: Context? = null, data: TnebConsumerBillData) {
        val existing = _billData.value
        val isSameConsumer = existing == null ||
            existing.consumerNumber.isBlank() ||
            data.consumerNumber.isBlank() ||
            existing.consumerNumber == data.consumerNumber

        val mergedBills = if (existing != null && isSameConsumer && existing.bills.isNotEmpty()) {
            val newBills = data.bills
            val existingBills = existing.bills
            // Prioritize freshly retrieved bills at the front (latest/current bill),
            // and preserve previous historical bills that are not in the new list.
            (newBills + existingBills).distinctBy { bill ->
                if (bill.receiptNumber.isNotBlank()) bill.receiptNumber
                else if (bill.assessmentDate.isNotBlank()) bill.assessmentDate
                else bill.id
            }
        } else {
            data.bills
        }
        val finalData = data.copy(bills = mergedBills)
        _billData.value = finalData
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val serialized = json.encodeToString(finalData)
                prefs.edit().putString(KEY_BILL_DATA, serialized).apply()
                logD("Saved TNEB bill data to cache for consumer: ${finalData.consumerNumber} (${mergedBills.size} bills)")
            } catch (e: Exception) {
                logE("Failed to persist TNEB bill data", e)
            }
        } else {
            logW("Context is null, unable to persist TNEB bill data to SharedPreferences")
        }
    }

    fun save(data: TnebConsumerBillData) {
        save(null, data)
    }

    fun hasConnectedBill(): Boolean {
        val data = _billData.value ?: return false
        return data.consumerNumber.isNotBlank() || data.bills.isNotEmpty()
    }

    fun clear(context: Context? = null) {
        _billData.value = null
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            try {
                val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                prefs.edit().remove(KEY_BILL_DATA).apply()
                logD("Cleared cached TNEB bill data")
            } catch (e: Exception) {
                logE("Failed to clear TNEB bill cache", e)
            }
        }
    }
}

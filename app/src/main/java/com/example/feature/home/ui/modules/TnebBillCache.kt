package com.example.feature.home.ui.modules

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.core.session.SecurePreferencesHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Singleton repository/cache for persisting and retrieving real TNEB bill data across SolarEase modules.
 * Stores sensitive utility billing records in hardware-backed EncryptedSharedPreferences.
 */
object TnebBillCache {
    private const val TAG = "TnebBillCache"
    private const val OLD_PLAINTEXT_PREFS = "SolarEaseTnebBillPrefs"
    private const val SECURE_PREFS_NAME = "SolarEaseSecureTnebBillPrefs"
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

    private fun getPrefs(context: Context? = null): SharedPreferences? {
        val ctx = context?.applicationContext ?: appContext ?: return null
        return SecurePreferencesHelper.getEncryptedPrefs(ctx, SECURE_PREFS_NAME)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val ctx = appContext ?: return
        try {
            val prefs = getPrefs(ctx) ?: return

            // Migrate legacy unencrypted preferences if present
            try {
                val legacy = ctx.getSharedPreferences(OLD_PLAINTEXT_PREFS, Context.MODE_PRIVATE)
                val legacyRaw = legacy.getString(KEY_BILL_DATA, null)
                if (!legacyRaw.isNullOrBlank() && prefs.getString(KEY_BILL_DATA, null).isNullOrBlank()) {
                    prefs.edit().putString(KEY_BILL_DATA, legacyRaw).apply()
                    legacy.edit().clear().apply()
                    logD("Migrated legacy TNEB bill cache to EncryptedSharedPreferences")
                }
            } catch (_: Throwable) {}

            val raw = prefs.getString(KEY_BILL_DATA, null)
            if (!raw.isNullOrBlank()) {
                val parsed = json.decodeFromString<TnebConsumerBillData>(raw)
                // Data minimization: ensure sensitive national IDs are not retained
                val sanitized = parsed.copy(aadhar = "", pan = "")
                _billData.value = sanitized
                logD("Restored cached TNEB bill data for consumer: ${sanitized.consumerNumber}")
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
            (newBills + existingBills).distinctBy { bill ->
                if (bill.receiptNumber.isNotBlank()) bill.receiptNumber
                else if (bill.assessmentDate.isNotBlank()) bill.assessmentDate
                else bill.id
            }
        } else {
            data.bills
        }
        // Strict data minimization: drop Aadhaar and PAN before caching
        val finalData = data.copy(bills = mergedBills, aadhar = "", pan = "")
        _billData.value = finalData
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            try {
                val prefs = getPrefs(ctx)
                if (prefs != null) {
                    val serialized = json.encodeToString(finalData)
                    prefs.edit().putString(KEY_BILL_DATA, serialized).apply()
                    logD("Saved encrypted TNEB bill data to cache for consumer: ${finalData.consumerNumber} (${mergedBills.size} bills)")
                }
            } catch (e: Exception) {
                logE("Failed to persist TNEB bill data", e)
            }
        } else {
            logW("Context is null, unable to persist TNEB bill data to EncryptedSharedPreferences")
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
                val prefs = getPrefs(ctx)
                prefs?.edit()?.clear()?.apply()
                val legacy = ctx.getSharedPreferences(OLD_PLAINTEXT_PREFS, Context.MODE_PRIVATE)
                legacy.edit().clear().apply()
                logD("Cleared cached TNEB bill data")
            } catch (e: Exception) {
                logE("Failed to clear TNEB bill cache", e)
            }
        }
    }
}

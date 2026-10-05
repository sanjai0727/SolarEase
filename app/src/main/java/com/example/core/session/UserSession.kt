package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class User(
    val id: String = "",
    val name: String? = null,
    val username: String,
    val email: String? = null,
    val mobileNumber: String?,
    val houseAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val ebServiceNumber: String? = null,
    val tnebRegisteredMobile: String? = null,
    val registrationCompleted: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val installationCompleted: Boolean = false
)

object UserSession {
    var currentUser: User? = null
    var accessToken: String? = null
    var refreshToken: String? = null

    private const val TAG = "UserSession"
    private const val OLD_PLAINTEXT_PREFS = "SolarEaseSessionPrefs"
    private const val SECURE_PREFS_NAME = "SolarEaseSecureSessionPrefs"
    private const val KEY_USER = "current_user"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_EB_SERVICE_NUMBER = "eb_service_number"
    private const val KEY_TNEB_REGISTERED_MOBILE = "tneb_registered_mobile"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        coerceInputValues = true
    }

    private var appContext: Context? = null

    private fun getPrefs(context: Context? = null): SharedPreferences {
        val ctx = context?.applicationContext ?: appContext
            ?: throw IllegalStateException("UserSession has not been initialized with Context")
        return SecurePreferencesHelper.getEncryptedPrefs(ctx, SECURE_PREFS_NAME)
    }

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = getPrefs(context)

        // One-time migration from legacy plaintext SharedPreferences to EncryptedSharedPreferences
        try {
            val legacyPrefs = context.getSharedPreferences(OLD_PLAINTEXT_PREFS, Context.MODE_PRIVATE)
            val legacyAccess = legacyPrefs.getString(KEY_ACCESS_TOKEN, null)
            val legacyRefresh = legacyPrefs.getString(KEY_REFRESH_TOKEN, null)
            val legacyUser = legacyPrefs.getString(KEY_USER, null)
            val legacyEb = legacyPrefs.getString(KEY_EB_SERVICE_NUMBER, null)
            val legacyMob = legacyPrefs.getString(KEY_TNEB_REGISTERED_MOBILE, null)

            if (legacyAccess != null || legacyUser != null || legacyEb != null) {
                val editor = prefs.edit()
                if (legacyAccess != null) editor.putString(KEY_ACCESS_TOKEN, legacyAccess)
                if (legacyRefresh != null) editor.putString(KEY_REFRESH_TOKEN, legacyRefresh)
                if (legacyUser != null) editor.putString(KEY_USER, legacyUser)
                if (legacyEb != null) editor.putString(KEY_EB_SERVICE_NUMBER, legacyEb)
                if (legacyMob != null) editor.putString(KEY_TNEB_REGISTERED_MOBILE, legacyMob)
                editor.apply()

                legacyPrefs.edit().clear().apply()
                Log.i(TAG, "Successfully migrated legacy plaintext session prefs to EncryptedSharedPreferences")
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Legacy preferences migration notice: ${e.message}")
        }

        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userJson = prefs.getString(KEY_USER, null)
        if (userJson != null) {
            try {
                currentUser = json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decode saved user session", e)
            }
        }

        // Resilient recovery: ensure saved ebServiceNumber and tnebRegisteredMobile are never lost
        val fallbackEb = prefs.getString(KEY_EB_SERVICE_NUMBER, null)?.ifBlank { null }
        val fallbackTnebMobile = prefs.getString(KEY_TNEB_REGISTERED_MOBILE, null)?.ifBlank { null }

        currentUser?.let { user ->
            var updated = user
            if (updated.ebServiceNumber.isNullOrBlank() && !fallbackEb.isNullOrBlank()) {
                updated = updated.copy(ebServiceNumber = fallbackEb)
            }
            if (updated.tnebRegisteredMobile.isNullOrBlank() && !fallbackTnebMobile.isNullOrBlank()) {
                updated = updated.copy(tnebRegisteredMobile = fallbackTnebMobile)
            }
            if (updated != user) {
                currentUser = updated
                val editor = prefs.edit()
                editor.putString(KEY_USER, json.encodeToString(updated))
                editor.apply()
            }
        }
    }

    /**
     * Checks if there is a verified, authenticated SolarEase backend session.
     */
    fun hasValidSession(): Boolean {
        return currentUser != null && !accessToken.isNullOrBlank()
    }

    fun getSavedEbServiceNumber(context: Context? = null): String? {
        val ctx = context?.applicationContext ?: appContext
        return currentUser?.ebServiceNumber?.ifBlank { null }
            ?: ctx?.let {
                getPrefs(it).getString(KEY_EB_SERVICE_NUMBER, null)?.ifBlank { null }
            }
    }

    fun getSavedTnebMobile(context: Context? = null): String? {
        val ctx = context?.applicationContext ?: appContext
        return currentUser?.tnebRegisteredMobile?.ifBlank { null }
            ?: ctx?.let {
                getPrefs(it).getString(KEY_TNEB_REGISTERED_MOBILE, null)?.ifBlank { null }
            }
    }

    fun saveExplicitElectricityDetails(context: Context? = null, consumerNumber: String?, tnebMobile: String?) {
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            val prefs = getPrefs(ctx)
            val editor = prefs.edit()
            if (!consumerNumber.isNullOrBlank()) {
                editor.putString(KEY_EB_SERVICE_NUMBER, consumerNumber)
            } else if (consumerNumber != null) {
                editor.remove(KEY_EB_SERVICE_NUMBER)
            }
            if (!tnebMobile.isNullOrBlank()) {
                editor.putString(KEY_TNEB_REGISTERED_MOBILE, tnebMobile)
            } else if (tnebMobile != null) {
                editor.remove(KEY_TNEB_REGISTERED_MOBILE)
            }
            editor.apply()
        }
        currentUser?.let { user ->
            var updated = user
            if (consumerNumber != null) {
                updated = updated.copy(ebServiceNumber = consumerNumber.ifBlank { null })
            }
            if (tnebMobile != null) {
                updated = updated.copy(tnebRegisteredMobile = tnebMobile.ifBlank { null })
            }
            currentUser = updated
        }
    }

    fun saveTokens(context: Context, access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
        val prefs = getPrefs(context)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, access)
            .putString(KEY_REFRESH_TOKEN, refresh)
            .apply()
    }

    fun saveUser(context: Context? = null, user: User) {
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            val prefs = getPrefs(ctx)
            val existingEb = prefs.getString(KEY_EB_SERVICE_NUMBER, null)?.ifBlank { null }
            val existingTnebMobile = prefs.getString(KEY_TNEB_REGISTERED_MOBILE, null)?.ifBlank { null }

            val finalEb = user.ebServiceNumber?.ifBlank { null } ?: existingEb
            val finalTnebMobile = user.tnebRegisteredMobile?.ifBlank { null } ?: existingTnebMobile

            val finalUser = user.copy(
                ebServiceNumber = finalEb,
                tnebRegisteredMobile = finalTnebMobile
            )
            currentUser = finalUser

            val editor = prefs.edit()
            editor.putString(KEY_USER, json.encodeToString(finalUser))
            if (!finalEb.isNullOrBlank()) {
                editor.putString(KEY_EB_SERVICE_NUMBER, finalEb)
            }
            if (!finalTnebMobile.isNullOrBlank()) {
                editor.putString(KEY_TNEB_REGISTERED_MOBILE, finalTnebMobile)
            }
            editor.apply()
        } else {
            currentUser = user
        }
    }

    fun saveUser(user: User) {
        saveUser(null, user)
    }

    fun clear(context: Context) {
        currentUser = null
        accessToken = null
        refreshToken = null
        try {
            val prefs = getPrefs(context)
            prefs.edit().clear().apply()
        } catch (e: Throwable) {
            Log.w(TAG, "Error clearing secure session prefs: ${e.message}")
        }
        try {
            val legacy = context.getSharedPreferences(OLD_PLAINTEXT_PREFS, Context.MODE_PRIVATE)
            legacy.edit().clear().apply()
        } catch (_: Throwable) {}
    }

    fun setProfilePromptDismissed(context: Context, userId: String) {
        try {
            val prefs = getPrefs(context)
            prefs.edit().putBoolean("profile_dismissed_$userId", true).apply()
        } catch (_: Throwable) {}
    }

    fun isProfilePromptDismissed(context: Context, userId: String): Boolean {
        return try {
            val prefs = getPrefs(context)
            prefs.getBoolean("profile_dismissed_$userId", false)
        } catch (_: Throwable) {
            false
        }
    }
}

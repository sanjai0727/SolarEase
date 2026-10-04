package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
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

    private const val PREFS_NAME = "SolarEaseSessionPrefs"
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

    fun init(context: Context) {
        appContext = context.applicationContext
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userJson = prefs.getString(KEY_USER, null)
        if (userJson != null) {
            try {
                currentUser = json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // If user session is null in local prefs, but Firebase session is still active and signed in,
        // restore user session from the active Firebase session to prevent state loss on cold restart.
        if (currentUser == null) {
            try {
                val fbUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                if (fbUser != null) {
                    val restoredUser = User(
                        id = fbUser.uid,
                        name = fbUser.displayName,
                        username = fbUser.displayName?.ifBlank { null } ?: fbUser.phoneNumber ?: "User",
                        email = fbUser.email,
                        mobileNumber = fbUser.phoneNumber
                    )
                    currentUser = restoredUser
                    saveUser(context, restoredUser)
                }
            } catch (_: Throwable) {
                // Ignore in testing environments where FirebaseAuth is not mocked/initialized
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

    fun getSavedEbServiceNumber(context: Context? = null): String? {
        val ctx = context?.applicationContext ?: appContext
        return currentUser?.ebServiceNumber?.ifBlank { null }
            ?: ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.getString(KEY_EB_SERVICE_NUMBER, null)?.ifBlank { null }
    }

    fun getSavedTnebMobile(context: Context? = null): String? {
        val ctx = context?.applicationContext ?: appContext
        return currentUser?.tnebRegisteredMobile?.ifBlank { null }
            ?: ctx?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                ?.getString(KEY_TNEB_REGISTERED_MOBILE, null)?.ifBlank { null }
    }

    fun saveExplicitElectricityDetails(context: Context? = null, consumerNumber: String?, tnebMobile: String?) {
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_ACCESS_TOKEN, access)
            .putString(KEY_REFRESH_TOKEN, refresh)
            .apply()
    }

    fun saveUser(context: Context? = null, user: User) {
        val ctx = context?.applicationContext ?: appContext
        if (ctx != null) {
            val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
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
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_USER)
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_EB_SERVICE_NUMBER)
            .remove(KEY_TNEB_REGISTERED_MOBILE)
            .apply()
    }

    fun setProfilePromptDismissed(context: Context, userId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean("profile_dismissed_$userId", true).apply()
    }

    fun isProfilePromptDismissed(context: Context, userId: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean("profile_dismissed_$userId", false)
    }
}

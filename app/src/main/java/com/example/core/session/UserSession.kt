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

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        accessToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val userJson = prefs.getString(KEY_USER, null)
        if (userJson != null) {
            try {
                currentUser = Json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
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

    fun saveUser(context: Context, user: User) {
        currentUser = user
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER, Json.encodeToString(user)).apply()
    }
    
    fun clear(context: Context) {
        currentUser = null
        accessToken = null
        refreshToken = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER).remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
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

package com.example.core.session

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class User(
    val customerName: String,
    val mobileNumber: String,
    val ebServiceNumber: String? = null,
    val registrationCompleted: Boolean = false,
    val onboardingCompleted: Boolean = false,
    val installationCompleted: Boolean = false
)

object UserSession {
    var currentUser: User? = null

    private const val PREFS_NAME = "SolarEaseSessionPrefs"
    private const val KEY_USER = "current_user"

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userJson = prefs.getString(KEY_USER, null)
        if (userJson != null) {
            try {
                currentUser = Json.decodeFromString<User>(userJson)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun saveUser(context: Context, user: User) {
        currentUser = user
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_USER, Json.encodeToString(user)).apply()
    }
    
    fun clear(context: Context) {
        currentUser = null
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_USER).apply()
    }
}

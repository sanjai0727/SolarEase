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
 * Local persistent storage for SolarEase Assistant chat history using SharedPreferences and JSON.
 * Persists conversations, messages, timestamps, and active session across app restart and screen navigation.
 */
object ChatHistoryStorage {
    private const val TAG = "ChatHistoryStorage"
    private const val PREFS_NAME = "SolarEaseChatHistoryPrefs"
    private const val KEY_SESSIONS = "saved_chat_sessions"
    private const val KEY_ACTIVE_SESSION_PREFIX = "active_session_"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var sharedPreferences: SharedPreferences? = null

    private val _sessionsFlow = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessionsFlow: StateFlow<List<ChatSession>> = _sessionsFlow.asStateFlow()

    fun init(context: Context) {
        if (sharedPreferences != null) return
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPreferences = prefs
            loadSessions()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize ChatHistoryStorage", e)
        }
    }

    /**
     * For unit tests or custom shared preferences injection.
     */
    fun init(prefs: SharedPreferences) {
        sharedPreferences = prefs
        loadSessions()
    }

    private fun loadSessions() {
        val prefs = sharedPreferences ?: return
        try {
            val rawJson = prefs.getString(KEY_SESSIONS, null)
            if (!rawJson.isNullOrBlank()) {
                val list = json.decodeFromString<List<ChatSession>>(rawJson)
                _sessionsFlow.value = list.sortedByDescending { it.timestamp }
            } else {
                _sessionsFlow.value = emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load chat history from storage", e)
            _sessionsFlow.value = emptyList()
        }
    }

    fun getSessionsForUser(userId: String): List<ChatSession> {
        val all = _sessionsFlow.value
        return if (userId.isBlank() || userId == "default_user") {
            all
        } else {
            all.filter { it.userId == userId || it.userId.isBlank() || it.userId == "default_user" }
        }.sortedByDescending { it.timestamp }
    }

    fun getLatestSession(userId: String): ChatSession? {
        return getSessionsForUser(userId).firstOrNull()
    }

    fun getActiveSessionId(userId: String): String? {
        val prefs = sharedPreferences ?: return null
        return prefs.getString(KEY_ACTIVE_SESSION_PREFIX + userId, null)
    }

    fun setActiveSessionId(userId: String, sessionId: String?) {
        val prefs = sharedPreferences ?: return
        val key = KEY_ACTIVE_SESSION_PREFIX + userId
        if (sessionId == null) {
            prefs.edit().remove(key).apply()
        } else {
            prefs.edit().putString(key, sessionId).apply()
        }
    }

    fun saveSession(session: ChatSession) {
        val current = _sessionsFlow.value
        val existingIndex = current.indexOfFirst { it.id == session.id }
        val updated = if (existingIndex >= 0) {
            current.toMutableList().apply { set(existingIndex, session) }
        } else {
            listOf(session) + current
        }.sortedByDescending { it.timestamp }

        _sessionsFlow.value = updated
        persist(updated)
    }

    fun deleteSession(sessionId: String) {
        val updated = _sessionsFlow.value.filter { it.id != sessionId }
        _sessionsFlow.value = updated
        persist(updated)
    }

    fun clearAll() {
        _sessionsFlow.value = emptyList()
        persist(emptyList())
    }

    private fun persist(list: List<ChatSession>) {
        val prefs = sharedPreferences ?: return
        try {
            val serialized = json.encodeToString(list)
            prefs.edit().putString(KEY_SESSIONS, serialized).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist chat sessions", e)
        }
    }

    /**
     * Clears in-memory state and preferences for tests.
     */
    fun resetForTesting() {
        _sessionsFlow.value = emptyList()
        sharedPreferences?.edit()?.clear()?.apply()
        sharedPreferences = null
    }
}

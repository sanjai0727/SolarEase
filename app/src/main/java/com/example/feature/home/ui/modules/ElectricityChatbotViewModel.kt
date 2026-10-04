package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.session.UserSession
import com.example.feature.home.domain.ChatbotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "",
    val title: String,
    val snippet: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String = "citizen",
    val language: String = "en",
    val category: String = "solar-sizing",
    val messages: List<ChatMessage> = emptyList()
)

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val currentMode: String = "citizen",
    val currentLanguage: String = "en",
    val errorMessage: String? = null,
    val lastFailedQuery: String? = null,
    val showHistory: Boolean = false,
    val historySearchQuery: String = "",
    val selectedHistoryCategory: String = "all",
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String? = null
)

class ElectricityChatbotViewModel : ViewModel() {
    private val repository = ChatbotRepository()

    private val _uiState = MutableStateFlow(ChatbotUiState())
    val uiState: StateFlow<ChatbotUiState> = _uiState.asStateFlow()

    init {
        loadPersistedChat()
    }

    private fun getEffectiveUserId(): String {
        val firebaseUid = try {
            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        } catch (_: Throwable) {
            null
        }
        return firebaseUid?.takeIf { it.isNotBlank() }
            ?: UserSession.currentUser?.id?.takeIf { it.isNotBlank() }
            ?: "default_user"
    }

    fun loadPersistedChat() {
        val userId = getEffectiveUserId()
        val userSessions = ChatHistoryStorage.getSessionsForUser(userId)
        val activeSessionId = ChatHistoryStorage.getActiveSessionId(userId)

        val sessionToLoad: ChatSession? = when {
            activeSessionId == "NEW_CHAT" -> null
            !activeSessionId.isNullOrBlank() -> userSessions.firstOrNull { it.id == activeSessionId } ?: userSessions.firstOrNull()
            else -> userSessions.firstOrNull()
        }

        if (sessionToLoad != null) {
            _uiState.update { state ->
                state.copy(
                    messages = sessionToLoad.messages.filter { !it.isLoading },
                    activeSessionId = sessionToLoad.id,
                    currentMode = sessionToLoad.mode,
                    currentLanguage = sessionToLoad.language,
                    sessions = userSessions
                )
            }
        } else {
            _uiState.update { state ->
                state.copy(
                    messages = emptyList(),
                    activeSessionId = null,
                    sessions = userSessions
                )
            }
        }
    }

    fun setMode(mode: String) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(currentLanguage = language) }
    }

    fun toggleHistory(show: Boolean) {
        if (show) {
            val userId = getEffectiveUserId()
            val sessions = ChatHistoryStorage.getSessionsForUser(userId)
            _uiState.update { it.copy(showHistory = true, sessions = sessions) }
        } else {
            _uiState.update { it.copy(showHistory = false) }
        }
    }

    fun setHistorySearchQuery(query: String) {
        _uiState.update { it.copy(historySearchQuery = query) }
    }

    fun setHistoryCategory(category: String) {
        _uiState.update { it.copy(selectedHistoryCategory = category) }
    }

    fun startNewChat() {
        val currentState = _uiState.value
        archiveCurrentSessionIfNeeded(currentState)

        val userId = getEffectiveUserId()
        ChatHistoryStorage.setActiveSessionId(userId, "NEW_CHAT")

        _uiState.update { state ->
            state.copy(
                messages = emptyList(),
                isTyping = false,
                errorMessage = null,
                lastFailedQuery = null,
                activeSessionId = null,
                showHistory = false,
                sessions = ChatHistoryStorage.getSessionsForUser(userId)
            )
        }
    }

    fun loadSession(session: ChatSession) {
        val currentState = _uiState.value
        archiveCurrentSessionIfNeeded(currentState)

        val userId = getEffectiveUserId()
        ChatHistoryStorage.setActiveSessionId(userId, session.id)

        _uiState.update { state ->
            state.copy(
                messages = session.messages.filter { !it.isLoading },
                currentMode = session.mode,
                currentLanguage = session.language,
                activeSessionId = session.id,
                isTyping = false,
                errorMessage = null,
                lastFailedQuery = null,
                showHistory = false,
                sessions = ChatHistoryStorage.getSessionsForUser(userId)
            )
        }
    }

    fun deleteSession(sessionId: String) {
        ChatHistoryStorage.deleteSession(sessionId)
        val userId = getEffectiveUserId()
        val updated = ChatHistoryStorage.getSessionsForUser(userId)

        if (_uiState.value.activeSessionId == sessionId) {
            ChatHistoryStorage.setActiveSessionId(userId, "NEW_CHAT")
            _uiState.update { state ->
                state.copy(
                    messages = emptyList(),
                    isTyping = false,
                    errorMessage = null,
                    lastFailedQuery = null,
                    activeSessionId = null,
                    sessions = updated
                )
            }
        } else {
            _uiState.update { it.copy(sessions = updated) }
        }
    }

    fun retryLastQuery() {
        val query = _uiState.value.lastFailedQuery
        if (!query.isNullOrBlank()) {
            sendMessage(query)
        }
    }

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // Add user message and bot loading message
        _uiState.update { state ->
            state.copy(
                messages = state.messages +
                    ChatMessage(isUser = true, text = query) +
                    ChatMessage(isUser = false, text = "", isLoading = true),
                isTyping = true,
                errorMessage = null,
                lastFailedQuery = query
            )
        }

        viewModelScope.launch {
            val currentState = _uiState.value
            val result = repository.queryChatbot(
                query = query,
                mode = currentState.currentMode,
                language = currentState.currentLanguage
            )

            result.fold(
                onSuccess = { response ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.toMutableList()
                        // Replace the last loading message
                        if (updatedMessages.isNotEmpty() && updatedMessages.last().isLoading) {
                            updatedMessages[updatedMessages.lastIndex] = ChatMessage(
                                isUser = false,
                                text = response.answer
                            )
                        } else {
                            updatedMessages.add(ChatMessage(isUser = false, text = response.answer))
                        }

                        val updatedState = state.copy(
                            messages = updatedMessages,
                            isTyping = false,
                            errorMessage = null,
                            lastFailedQuery = null
                        )
                        updateCurrentSessionInList(updatedState)
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        val updatedMessages = state.messages.toMutableList()
                        if (updatedMessages.isNotEmpty() && updatedMessages.last().isLoading) {
                            updatedMessages.removeLast() // Remove loading indicator on error
                        }
                        val finalState = state.copy(
                            messages = updatedMessages,
                            isTyping = false,
                            errorMessage = error.localizedMessage ?: "Network error. Please retry."
                        )
                        if (updatedMessages.isNotEmpty()) {
                            archiveCurrentSessionIfNeeded(finalState)
                        }
                        finalState
                    }
                }
            )
        }
    }

    private fun archiveCurrentSessionIfNeeded(state: ChatbotUiState) {
        val cleanMessages = state.messages.filter { !it.isLoading }
        val validUserMsg = cleanMessages.firstOrNull { it.isUser && it.text.isNotBlank() } ?: return
        val lastBotMsg = cleanMessages.lastOrNull { !it.isUser && it.text.isNotBlank() }

        val sessionId = state.activeSessionId ?: UUID.randomUUID().toString()
        val userId = getEffectiveUserId()
        val category = determineCategory(validUserMsg.text)

        val newSession = ChatSession(
            id = sessionId,
            userId = userId,
            title = validUserMsg.text.take(60),
            snippet = (lastBotMsg?.text ?: validUserMsg.text).take(90),
            timestamp = System.currentTimeMillis(),
            mode = state.currentMode,
            language = state.currentLanguage,
            category = category,
            messages = cleanMessages
        )

        ChatHistoryStorage.saveSession(newSession)

        val updatedList = ChatHistoryStorage.getSessionsForUser(userId)
        _uiState.update { it.copy(sessions = updatedList) }
    }

    private fun updateCurrentSessionInList(state: ChatbotUiState): ChatbotUiState {
        val cleanMessages = state.messages.filter { !it.isLoading }
        val validUserMsg = cleanMessages.firstOrNull { it.isUser && it.text.isNotBlank() } ?: return state
        val lastBotMsg = cleanMessages.lastOrNull { !it.isUser && it.text.isNotBlank() }

        val sessionId = state.activeSessionId ?: UUID.randomUUID().toString()
        val userId = getEffectiveUserId()
        val category = determineCategory(validUserMsg.text)

        val newSession = ChatSession(
            id = sessionId,
            userId = userId,
            title = validUserMsg.text.take(60),
            snippet = (lastBotMsg?.text ?: validUserMsg.text).take(90),
            timestamp = System.currentTimeMillis(),
            mode = state.currentMode,
            language = state.currentLanguage,
            category = category,
            messages = cleanMessages
        )

        ChatHistoryStorage.saveSession(newSession)
        ChatHistoryStorage.setActiveSessionId(userId, sessionId)

        val updatedList = ChatHistoryStorage.getSessionsForUser(userId)
        return state.copy(sessions = updatedList, activeSessionId = sessionId)
    }

    private fun determineCategory(text: String): String {
        val lower = text.lowercase()
        return when {
            lower.contains("subsidy") || lower.contains("surya") || lower.contains("pm") -> "subsidies"
            lower.contains("bill") || lower.contains("tariff") || lower.contains("slab") || lower.contains("eb") -> "tariffs"
            lower.contains("lens") || lower.contains("scan") || lower.contains("appliance") -> "solar-lens"
            else -> "solar-sizing"
        }
    }
}

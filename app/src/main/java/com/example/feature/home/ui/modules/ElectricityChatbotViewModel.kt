package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.home.domain.ChatbotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val isLoading: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val snippet: String,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String,
    val language: String,
    val category: String = "solar-sizing",
    val messages: List<ChatMessage>
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

    fun setMode(mode: String) {
        _uiState.update { it.copy(currentMode = mode) }
    }

    fun setLanguage(language: String) {
        _uiState.update { it.copy(currentLanguage = language) }
    }

    fun toggleHistory(show: Boolean) {
        _uiState.update { it.copy(showHistory = show) }
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

        _uiState.update { state ->
            state.copy(
                messages = emptyList(),
                isTyping = false,
                errorMessage = null,
                lastFailedQuery = null,
                activeSessionId = null,
                showHistory = false
            )
        }
    }

    fun loadSession(session: ChatSession) {
        val currentState = _uiState.value
        archiveCurrentSessionIfNeeded(currentState)

        _uiState.update { state ->
            state.copy(
                messages = session.messages,
                currentMode = session.mode,
                currentLanguage = session.language,
                activeSessionId = session.id,
                isTyping = false,
                errorMessage = null,
                lastFailedQuery = null,
                showHistory = false
            )
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
                        state.copy(
                            messages = updatedMessages,
                            isTyping = false,
                            errorMessage = error.localizedMessage ?: "Network error. Please retry."
                        )
                    }
                }
            )
        }
    }

    private fun archiveCurrentSessionIfNeeded(state: ChatbotUiState) {
        val validUserMsg = state.messages.firstOrNull { it.isUser && it.text.isNotBlank() } ?: return
        val lastBotMsg = state.messages.lastOrNull { !it.isUser && it.text.isNotBlank() }

        val sessionId = state.activeSessionId ?: UUID.randomUUID().toString()
        val category = determineCategory(validUserMsg.text)

        val newSession = ChatSession(
            id = sessionId,
            title = validUserMsg.text.take(60),
            snippet = (lastBotMsg?.text ?: validUserMsg.text).take(90),
            timestamp = System.currentTimeMillis(),
            mode = state.currentMode,
            language = state.currentLanguage,
            category = category,
            messages = state.messages
        )

        val existingIdx = state.sessions.indexOfFirst { it.id == sessionId }
        val updatedList = if (existingIdx >= 0) {
            state.sessions.toMutableList().apply { set(existingIdx, newSession) }
        } else {
            listOf(newSession) + state.sessions
        }

        _uiState.update { it.copy(sessions = updatedList) }
    }

    private fun updateCurrentSessionInList(state: ChatbotUiState): ChatbotUiState {
        val validUserMsg = state.messages.firstOrNull { it.isUser && it.text.isNotBlank() } ?: return state
        val lastBotMsg = state.messages.lastOrNull { !it.isUser && it.text.isNotBlank() }

        val sessionId = state.activeSessionId ?: UUID.randomUUID().toString()
        val category = determineCategory(validUserMsg.text)

        val newSession = ChatSession(
            id = sessionId,
            title = validUserMsg.text.take(60),
            snippet = (lastBotMsg?.text ?: validUserMsg.text).take(90),
            timestamp = System.currentTimeMillis(),
            mode = state.currentMode,
            language = state.currentLanguage,
            category = category,
            messages = state.messages
        )

        val existingIdx = state.sessions.indexOfFirst { it.id == sessionId }
        val updatedList = if (existingIdx >= 0) {
            state.sessions.toMutableList().apply { set(existingIdx, newSession) }
        } else {
            listOf(newSession) + state.sessions
        }

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

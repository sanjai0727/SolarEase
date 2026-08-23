package com.example.feature.home.ui.modules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.home.domain.ChatbotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatMessage(
    val isUser: Boolean,
    val text: String,
    val isLoading: Boolean = false
)

data class ChatbotUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isTyping: Boolean = false,
    val currentMode: String = "citizen",
    val currentLanguage: String = "en",
    val errorMessage: String? = null
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

    fun sendMessage(query: String) {
        if (query.isBlank()) return

        // Add user message and bot loading message
        _uiState.update { state ->
            state.copy(
                messages = state.messages + 
                    ChatMessage(isUser = true, text = query) + 
                    ChatMessage(isUser = false, text = "", isLoading = true),
                isTyping = true,
                errorMessage = null
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
                        state.copy(messages = updatedMessages, isTyping = false)
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
                            errorMessage = error.localizedMessage ?: "Unknown error occurred"
                        )
                    }
                }
            )
        }
    }
}

package com.example.feature.ebservice.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.feature.ebservice.util.ConsumerNumberDetails
import com.example.feature.ebservice.util.ConsumerNumberParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class EbServiceUiState(
    val rawConsumerNumber: String = "",
    val isValid: Boolean = false,
    val errorMessage: String? = null,
    val parsedDetails: ConsumerNumberDetails? = null
)

class EbServiceViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(EbServiceUiState())
    val uiState: StateFlow<EbServiceUiState> = _uiState.asStateFlow()

    fun updateConsumerNumber(input: String) {
        val rawNumber = input.filter { it.isDigit() }.take(12)
        
        var errorMessage: String? = null
        var isValid = false
        var parsedDetails: ConsumerNumberDetails? = null
        
        if (rawNumber.length in 11..12) {
            val parserResult = ConsumerNumberParser.parse(rawNumber)
            if (parserResult.isSuccess) {
                isValid = true
                parsedDetails = parserResult.getOrNull()
            } else {
                errorMessage = parserResult.exceptionOrNull()?.message
            }
        }
        
        _uiState.update {
            it.copy(
                rawConsumerNumber = rawNumber,
                isValid = isValid,
                errorMessage = errorMessage,
                parsedDetails = parsedDetails
            )
        }
    }

    fun completeRegistration() {
        val currentUser = com.example.core.session.UserSession.currentUser
        if (currentUser != null && _uiState.value.isValid) {
            val updatedUser = currentUser.copy(
                ebServiceNumber = _uiState.value.rawConsumerNumber,
                registrationCompleted = true
            )
            com.example.core.session.UserSession.saveUser(getApplication(), updatedUser)
        }
    }
}

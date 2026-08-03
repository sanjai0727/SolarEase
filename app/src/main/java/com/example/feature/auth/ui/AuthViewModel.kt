package com.example.feature.auth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val message: String? = null) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var otpFailures = 0

    fun sendOtp(phoneNumber: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            delay(1000) // Mock API call
            if (phoneNumber.length >= 10) {
                _uiState.value = AuthUiState.Success("OTP sent successfully")
            } else {
                _uiState.value = AuthUiState.Error("Invalid phone number")
            }
        }
    }

    fun verifyOtp(otp: String, fullName: String? = null, phoneNumber: String? = null) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            delay(1000)
            if (otp == "123456") {
                otpFailures = 0
                if (fullName != null && phoneNumber != null) {
                    val user = com.example.core.session.User(fullName, phoneNumber)
                    com.example.core.session.UserSession.saveUser(getApplication(), user)
                }
                _uiState.value = AuthUiState.Success()
            } else {
                otpFailures++
                if (otpFailures >= 3) {
                    _uiState.value = AuthUiState.Error("Too many failed attempts. Try again in 15 minutes.")
                } else {
                    _uiState.value = AuthUiState.Error("Invalid OTP. Attempts left: ${3 - otpFailures}")
                }
            }
        }
    }

    fun submitProfile(photoRef: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            delay(1000)
            if (photoRef != null) {
                _uiState.value = AuthUiState.Success()
            } else {
                _uiState.value = AuthUiState.Error("Photo is required")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

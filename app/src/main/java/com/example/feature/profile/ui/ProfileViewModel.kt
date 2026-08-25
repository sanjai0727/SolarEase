package com.example.feature.profile.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.RetrofitClient
import com.example.core.network.dto.UserUpdateRequest
import com.example.core.session.User
import com.example.core.session.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        val user = UserSession.currentUser
        if (user != null) {
            _uiState.value = ProfileUiState.Success(user)
            fetchLatestProfile()
        } else {
            _uiState.value = ProfileUiState.Error("User not logged in")
        }
    }

    private fun fetchLatestProfile() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getProfile()
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val sessionUser = User(
                            id = body.id,
                            name = body.name,
                            username = body.username ?: "User",
                            email = body.email,
                            mobileNumber = body.mobileNumber,
                            houseAddress = body.houseAddress,
                            latitude = body.latitude,
                            longitude = body.longitude
                        )
                        UserSession.saveUser(getApplication(), sessionUser)
                        _uiState.value = ProfileUiState.Success(sessionUser)
                    }
                }
            } catch (e: Exception) {
                // Ignore silent fetch failure
            }
        }
    }

    fun updateProfile(name: String, username: String, mobileNumber: String, houseAddress: String, latitude: Double?, longitude: Double?) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val request = UserUpdateRequest(
                    name = name.ifBlank { null },
                    username = username.ifBlank { null },
                    mobileNumber = mobileNumber.ifBlank { null },
                    houseAddress = houseAddress.ifBlank { null },
                    latitude = latitude,
                    longitude = longitude
                )
                val response = RetrofitClient.apiService.updateProfile(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        val sessionUser = User(
                            id = body.id,
                            name = body.name,
                            username = body.username ?: "User",
                            email = body.email,
                            mobileNumber = body.mobileNumber,
                            houseAddress = body.houseAddress,
                            latitude = body.latitude,
                            longitude = body.longitude
                        )
                        UserSession.saveUser(getApplication(), sessionUser)
                        _uiState.value = ProfileUiState.Success(sessionUser)
                    } else {
                        _uiState.value = ProfileUiState.Error("Invalid backend response")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    if (response.code() == 409) {
                        _uiState.value = ProfileUiState.Error("Username or mobile number is already taken.")
                    } else if (response.code() == 422) {
                        _uiState.value = ProfileUiState.Error("Validation error. Please check your inputs.")
                    } else {
                        _uiState.value = ProfileUiState.Error("Update failed: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Network error")
            }
        }
    }
}

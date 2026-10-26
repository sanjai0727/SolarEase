package com.example.feature.profile.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.RetrofitClient
import com.example.core.network.dto.UserUpdateRequest
import com.example.core.session.User
import com.example.core.session.UserSession
import com.example.feature.ebservice.util.ConsumerNumberParser
import com.example.feature.ebservice.util.cleanIndianMobile
import com.example.feature.ebservice.util.isValidIndianMobile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ProfileUiState {
    data object Loading : ProfileUiState()
    data class Success(val user: User) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

data class NotificationPreferences(
    val billReminders: Boolean = true,
    val meterReminders: Boolean = true,
    val mantraReminders: Boolean = true,
    val rewardReminders: Boolean = true,
    val alertReminders: Boolean = true
)

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val notifPrefs: SharedPreferences =
        application.getSharedPreferences("SolarEaseNotificationPrefs", Context.MODE_PRIVATE)

    private val appPrefs: SharedPreferences =
        application.getSharedPreferences("SolarEaseAppPrefs", Context.MODE_PRIVATE)

    private val _notificationPreferences = MutableStateFlow(loadNotificationPreferences())
    val notificationPreferences: StateFlow<NotificationPreferences> = _notificationPreferences.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(appPrefs.getString("app_language", "English") ?: "English")
    val selectedLanguage: StateFlow<String> = _selectedLanguage.asStateFlow()

    private val _biometricLock = MutableStateFlow(appPrefs.getBoolean("biometric_lock", false))
    val biometricLock: StateFlow<Boolean> = _biometricLock.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        var user = UserSession.currentUser
        if (user != null) {
            val savedEb = UserSession.getSavedEbServiceNumber(getApplication())
            val savedTnebMobile = UserSession.getSavedTnebMobile(getApplication())
            if ((user.ebServiceNumber.isNullOrBlank() && !savedEb.isNullOrBlank()) ||
                (user.tnebRegisteredMobile.isNullOrBlank() && !savedTnebMobile.isNullOrBlank())
            ) {
                user = user.copy(
                    ebServiceNumber = user.ebServiceNumber?.ifBlank { null } ?: savedEb,
                    tnebRegisteredMobile = user.tnebRegisteredMobile?.ifBlank { null } ?: savedTnebMobile
                )
                UserSession.saveUser(getApplication(), user)
            }
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
                        val current = UserSession.currentUser
                        val sessionUser = User(
                            id = body.id,
                            name = body.name,
                            username = body.username ?: current?.username ?: "User",
                            email = body.email ?: current?.email,
                            mobileNumber = body.mobileNumber ?: current?.mobileNumber,
                            houseAddress = body.houseAddress ?: current?.houseAddress,
                            latitude = body.latitude ?: current?.latitude,
                            longitude = body.longitude ?: current?.longitude,
                            ebServiceNumber = current?.ebServiceNumber ?: UserSession.getSavedEbServiceNumber(getApplication()),
                            tnebRegisteredMobile = current?.tnebRegisteredMobile ?: UserSession.getSavedTnebMobile(getApplication()),
                            registrationCompleted = current?.registrationCompleted ?: false,
                            onboardingCompleted = current?.onboardingCompleted ?: false,
                            installationCompleted = current?.installationCompleted ?: false
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

    fun updateProfile(
        name: String,
        username: String,
        mobileNumber: String,
        houseAddress: String,
        latitude: Double?,
        longitude: Double?,
        email: String? = null,
        onComplete: ((Boolean, String?) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val current = UserSession.currentUser
            // Optimistically update local session so UI updates immediately
            val localUpdated = current?.copy(
                name = name.ifBlank { null },
                username = username.ifBlank { current.username },
                mobileNumber = mobileNumber.ifBlank { null },
                email = email?.ifBlank { null } ?: current.email,
                houseAddress = houseAddress.ifBlank { null },
                latitude = latitude ?: current.latitude,
                longitude = longitude ?: current.longitude
            ) ?: User(
                username = username,
                name = name.ifBlank { null },
                mobileNumber = mobileNumber.ifBlank { null },
                email = email?.ifBlank { null },
                houseAddress = houseAddress.ifBlank { null },
                latitude = latitude,
                longitude = longitude
            )
            UserSession.saveUser(getApplication(), localUpdated)
            _uiState.value = ProfileUiState.Success(localUpdated)

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
                        val sessionUser = localUpdated.copy(
                            id = body.id,
                            name = body.name,
                            username = body.username ?: localUpdated.username,
                            email = body.email ?: localUpdated.email,
                            mobileNumber = body.mobileNumber ?: localUpdated.mobileNumber,
                            houseAddress = body.houseAddress ?: localUpdated.houseAddress,
                            latitude = body.latitude ?: localUpdated.latitude,
                            longitude = body.longitude ?: localUpdated.longitude
                        )
                        UserSession.saveUser(getApplication(), sessionUser)
                        _uiState.value = ProfileUiState.Success(sessionUser)
                        onComplete?.invoke(true, null)
                    } else {
                        onComplete?.invoke(true, null)
                    }
                } else {
                    val msg = if (response.code() == 409) {
                        "Username or mobile number is already taken."
                    } else if (response.code() == 422) {
                        "Validation error. Please check your inputs."
                    } else {
                        "Update failed: ${response.code()}"
                    }
                    onComplete?.invoke(false, msg)
                }
            } catch (e: Exception) {
                // Keep local changes since backend might be offline in development
                onComplete?.invoke(true, null)
            }
        }
    }

    fun updateElectricityDetails(
        consumerNumber: String,
        tnebMobile: String?,
        onResult: (Boolean, String?) -> Unit
    ) {
        val cleanConsumer = consumerNumber.filter { it.isDigit() }
        if (cleanConsumer.length !in 10..12) {
            onResult(false, "Consumer number must be 10 to 12 digits.")
            return
        }
        val parseResult = ConsumerNumberParser.parse(cleanConsumer)
        if (parseResult.isFailure) {
            onResult(false, parseResult.exceptionOrNull()?.message ?: "Invalid Consumer Number.")
            return
        }

        val cleanMobile = tnebMobile?.let { cleanIndianMobile(it) }?.ifBlank { null }
        if (cleanMobile != null) {
            if (!isValidIndianMobile(cleanMobile)) {
                onResult(false, "Please enter a valid 10-digit Indian mobile number.")
                return
            }
        }

        val current = UserSession.currentUser
        if (current != null) {
            val updated = current.copy(
                ebServiceNumber = cleanConsumer,
                tnebRegisteredMobile = cleanMobile,
                registrationCompleted = true
            )
            UserSession.saveExplicitElectricityDetails(getApplication(), cleanConsumer, cleanMobile)
            UserSession.saveUser(getApplication(), updated)
            _uiState.value = ProfileUiState.Success(updated)
            onResult(true, null)
        } else {
            onResult(false, "No active user session.")
        }
    }

    fun updateEbServiceNumber(
        consumerNumber: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        val currentTnebMobile = UserSession.getSavedTnebMobile(getApplication())
        updateElectricityDetails(consumerNumber, currentTnebMobile, onResult)
    }

    private fun loadNotificationPreferences(): NotificationPreferences {
        return NotificationPreferences(
            billReminders = notifPrefs.getBoolean("bill_reminders", true),
            meterReminders = notifPrefs.getBoolean("meter_reminders", true),
            mantraReminders = notifPrefs.getBoolean("mantra_reminders", true),
            rewardReminders = notifPrefs.getBoolean("reward_reminders", true),
            alertReminders = notifPrefs.getBoolean("alert_reminders", true)
        )
    }

    fun setNotificationPreference(
        key: String,
        value: Boolean
    ) {
        notifPrefs.edit().putBoolean(key, value).apply()
        _notificationPreferences.value = loadNotificationPreferences()
    }

    fun setAppLanguage(language: String) {
        appPrefs.edit().putString("app_language", language).apply()
        _selectedLanguage.value = language
    }

    fun setBiometricLock(enabled: Boolean) {
        appPrefs.edit().putBoolean("biometric_lock", enabled).apply()
        _biometricLock.value = enabled
    }

    fun signOut(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                com.example.core.network.RetrofitClient.apiService.logout()
            } catch (e: Exception) {
                Log.w("ProfileViewModel", "Backend logout failed or offline: ${e.message}")
            } finally {
                UserSession.clear(context)
                com.example.feature.home.ui.modules.TnebBillCache.clear(context)
                com.example.feature.home.ui.modules.MeterReadingCache.clear(context)
                com.example.feature.home.ui.modules.BillCalculatorHistoryStorage.clearHistory()
                com.example.feature.lens.data.LensRoomStorage.clear()
                com.example.feature.home.ui.modules.ChatHistoryStorage.clearAll()
                try {
                    com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                } catch (_: Throwable) {}
                onComplete()
            }
        }
    }
}

package com.example.feature.auth.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val message: String? = null) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var verificationId: String? = null
    private var forceResendingToken: PhoneAuthProvider.ForceResendingToken? = null
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Auto-retrieval or instant verification
                    signInWithPhoneAuthCredential(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    val message = when (e) {
                        is FirebaseAuthInvalidCredentialsException -> "Invalid phone number."
                        is com.google.firebase.FirebaseTooManyRequestsException -> "Too many requests. Try again later."
                        else -> e.localizedMessage ?: "Verification failed."
                    }
                    _uiState.value = AuthUiState.Error(message)
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    this@AuthViewModel.verificationId = verificationId
                    this@AuthViewModel.forceResendingToken = token
                    _uiState.value = AuthUiState.Success("OTP sent successfully")
                }
            }

            val formattedNumber = if (phoneNumber.startsWith("+")) phoneNumber else "+91$phoneNumber"
            
            val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(formattedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)

            if (forceResendingToken != null) {
                optionsBuilder.setForceResendingToken(forceResendingToken!!)
            }
            
            PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
        }
    }

    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            val id = verificationId
            if (id == null) {
                _uiState.value = AuthUiState.Error("Verification ID missing. Please request a new OTP.")
                return@launch
            }
            
            try {
                val credential = PhoneAuthProvider.getCredential(id, otp)
                signInWithPhoneAuthCredential(credential)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error("Invalid OTP format.")
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState.Success("Authentication successful")
                } else {
                    val message = when (task.exception) {
                        is FirebaseAuthInvalidCredentialsException -> "Invalid OTP or expired."
                        else -> task.exception?.localizedMessage ?: "Authentication failed."
                    }
                    _uiState.value = AuthUiState.Error(message)
                }
            }
    }

    fun submitProfile(ebNumber: String, photoRef: String?) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            kotlinx.coroutines.delay(1000)
            if (ebNumber.isNotBlank() && photoRef != null) {
                _uiState.value = AuthUiState.Success()
            } else {
                _uiState.value = AuthUiState.Error("EB number and photo are required")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

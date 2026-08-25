package com.example.feature.auth.ui

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import android.util.Log
import kotlinx.coroutines.delay
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

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    init {
        Log.d("AuthLifecycle", "AuthViewModel created: ${this.hashCode()}")
    }

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    fun sendOtp(phoneNumber: String, activity: Activity) {
        Log.d("AuthLifecycle", "sendOtp() called on ViewModel: ${this.hashCode()}")
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            
            Log.d("AuthViewModel", "Firebase App Name: ${auth.app.name}")
            Log.d("AuthViewModel", "Firebase Project ID: ${auth.app.options.projectId}")
            Log.d("AuthViewModel", "Firebase Application ID: ${auth.app.options.applicationId}")
            Log.d("AuthViewModel", "Phone number before normalize: $phoneNumber")

            if (phoneNumber.length < 10) {
                _uiState.value = AuthUiState.Error("Invalid phone number")
                return@launch
            }

            val normalizedNumber = if (phoneNumber.startsWith("+91")) {
                phoneNumber
            } else {
                "+91$phoneNumber"
            }

            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(normalizedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        Log.d("AuthLifecycle", "onVerificationCompleted() on ViewModel: ${this.hashCode()}")
                        signInWithCredential(credential, null, normalizedNumber)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        Log.e("AuthLifecycle", "onVerificationFailed() on ViewModel: ${this.hashCode()}")
                        Log.e("AuthViewModel", "FirebaseException class: ${e.javaClass.name}")
                        Log.e("AuthViewModel", "FirebaseException message: ${e.message}")
                        if (e is FirebaseAuthException) {
                            Log.e("AuthViewModel", "FirebaseAuth errorCode: ${e.errorCode}")
                        }
                        _uiState.value = AuthUiState.Error(e.message ?: "Verification failed")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        Log.d("AuthLifecycle", "onCodeSent() on ViewModel: ${this.hashCode()}")
                        Log.d("AuthLifecycle", "Verification ID received: ${verificationId.take(5)}...")
                        storedVerificationId = verificationId
                        resendToken = token
                        _uiState.value = AuthUiState.Success("OTP sent successfully")
                    }
                })
                .build()
                
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    fun verifyOtp(otp: String, username: String? = null, phoneNumber: String? = null) {
        Log.d("AuthLifecycle", "verifyOtp() called on ViewModel: ${this.hashCode()}")
        val verificationId = storedVerificationId
        Log.d("AuthLifecycle", "storedVerificationId is ${if (verificationId == null) "NULL" else "NOT NULL"}")
        
        if (verificationId == null) {
            _uiState.value = AuthUiState.Error("Please request OTP first.")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                Log.d("AuthLifecycle", "Calling PhoneAuthProvider.getCredential() on ViewModel: ${this.hashCode()}")
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                signInWithCredential(credential, username, phoneNumber)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Invalid OTP.")
            }
        }
    }

    private fun signInWithCredential(credential: PhoneAuthCredential, username: String?, phoneNumber: String?) {
        Log.d("AuthLifecycle", "signInWithCredential() called on ViewModel: ${this.hashCode()}")
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AuthLifecycle", "signInWithCredential success on ViewModel: ${this.hashCode()}")
                    val user = task.result?.user
                    handleFirebaseUser(user, username, isSignUp = false)
                } else {
                    Log.e("AuthLifecycle", "signInWithCredential failed on ViewModel: ${this.hashCode()}")
                    if (task.exception is FirebaseAuthException) {
                        val e = task.exception as FirebaseAuthException
                        Log.e("AuthLifecycle", "ErrorCode: ${e.errorCode}, Message: ${e.message}")
                    }
                    _uiState.value = AuthUiState.Error(task.exception?.message ?: "Authentication failed")
                }
            }
    }

    private fun handleFirebaseUser(user: FirebaseUser?, username: String? = null, isSignUp: Boolean = false) {
        Log.d("AuthFlow", "Firebase current user available")
        if (user != null) {
            // Obtain the ID token
            user.getIdToken(true).addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val idToken = tokenTask.result?.token
                    if (idToken != null) {
                        Log.d("AuthFlow", "Firebase ID token obtained, length: ${idToken.length}")
                        Log.d("AuthFlow", "Sending token to backend")
                        viewModelScope.launch {
                            try {
                                val response = com.example.core.network.RetrofitClient.apiService.verifyFirebaseToken(
                                    "Bearer $idToken",
                                    com.example.core.network.dto.FirebaseTokenRequest(
                                        firebaseIdToken = idToken,
                                        username = username,
                                        is_signup = isSignUp
                                    )
                                )
                                if (response.isSuccessful) {
                                    Log.d("AuthFlow", "Backend verification successful")
                                    val body = response.body()
                                    if (body != null) {
                                        com.example.core.session.UserSession.saveTokens(getApplication(), body.accessToken, body.refreshToken)
                                        val sessionUser = com.example.core.session.User(
                                            id = body.user.id,
                                            name = body.user.name,
                                            username = body.user.username ?: "User",
                                            email = body.user.email,
                                            mobileNumber = body.user.mobileNumber,
                                            houseAddress = body.user.houseAddress
                                        )
                                        com.example.core.session.UserSession.saveUser(getApplication(), sessionUser)
                                        Log.d("AuthFlow", "User session created")
                                        _uiState.value = AuthUiState.Success()
                                    } else {
                                        _uiState.value = AuthUiState.Error("Invalid backend response")
                                    }
                                } else {
                                    val errorBody = response.errorBody()?.string()
                                    Log.e("AuthLifecycle", "Backend /auth/firebase FAILED. Status: ${response.code()}, Error: $errorBody")
                                    var errorMessage = "Backend error: ${response.code()}"
                                    if (response.code() == 409) errorMessage = "An account already exists. Please sign in instead."
                                    if (response.code() == 401) errorMessage = "Invalid or expired authentication session. Please try again."
                                    
                                    try {
                                        if (errorBody != null) {
                                            val json = org.json.JSONObject(errorBody)
                                            if (json.has("detail")) {
                                                errorMessage = json.getString("detail")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        // ignore parsing error
                                    }
                                    _uiState.value = AuthUiState.Error(errorMessage)
                                }
                            } catch (e: Exception) {
                                _uiState.value = AuthUiState.Error(e.message ?: "Network error connecting to backend")
                            }
                        }
                    } else {
                        _uiState.value = AuthUiState.Error("Failed to obtain Firebase ID token")
                    }
                } else {
                    _uiState.value = AuthUiState.Error(tokenTask.exception?.message ?: "Failed to get Firebase ID token")
                }
            }
        } else {
            _uiState.value = AuthUiState.Error("Authentication succeeded but user is null")
        }
    }

    fun signInWithGoogle(googleIdToken: String, username: String? = null, isSignUp: Boolean = false) {
        Log.d("AuthLifecycle", "signInWithGoogle() called on ViewModel")
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    Log.d("AuthFlow", "Google credential received")
                    Log.d("AuthFlow", "Firebase authentication successful")
                    handleFirebaseUser(user, username, isSignUp)
                } else {
                    Log.e("AuthLifecycle", "signInWithGoogle failed")
                    if (task.exception is FirebaseAuthException) {
                        val e = task.exception as FirebaseAuthException
                        Log.e("AuthLifecycle", "ErrorCode: ${e.errorCode}, Message: ${e.message}")
                    }
                    _uiState.value = AuthUiState.Error(task.exception?.message ?: "Google Authentication failed")
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

    fun setLoading() {
        _uiState.value = AuthUiState.Loading
    }

    fun setError(message: String) {
        _uiState.value = AuthUiState.Error(message)
    }
}

package com.example.feature.auth.ui

import android.app.Activity
import android.app.Application
import android.util.Base64
import android.util.Log
import android.util.Patterns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.network.ApiService
import com.example.core.network.RetrofitClient
import com.example.core.network.dto.FirebaseTokenRequest
import com.example.core.network.dto.ResolveIdentifierRequest
import com.example.core.session.User
import com.example.core.session.UserSession
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AuthUiState {
    data object Idle : AuthUiState()
    data object Loading : AuthUiState()
    data class Success(val message: String? = null) : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel @JvmOverloads constructor(
    application: Application,
    private val auth: FirebaseAuth? = null,
    private val apiService: ApiService = RetrofitClient.apiService
) : AndroidViewModel(application) {

    private val firebaseAuth: FirebaseAuth
        get() = auth ?: FirebaseAuth.getInstance()

    init {
        Log.d("AuthLifecycle", "AuthViewModel created: ${this.hashCode()}")
    }


    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Account Linking State
    private val _showLinkAccountDialog = MutableStateFlow(false)
    val showLinkAccountDialog = _showLinkAccountDialog.asStateFlow()

    private val _pendingLinkingEmail = MutableStateFlow<String?>(null)
    val pendingLinkingEmail = _pendingLinkingEmail.asStateFlow()

    private var pendingGoogleCredential: AuthCredential? = null

    // Forgot Password State
    private val _showForgotPasswordDialog = MutableStateFlow(false)
    val showForgotPasswordDialog = _showForgotPasswordDialog.asStateFlow()

    private val _forgotPasswordStatus = MutableStateFlow<String?>(null)
    val forgotPasswordStatus = _forgotPasswordStatus.asStateFlow()

    // Phone Auth
    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    // -------------------------------------------------------------
    //  EMAIL SIGN-UP
    // -------------------------------------------------------------
    fun signUpWithEmail(username: String, email: String, password: String) {
        val cleanUsername = username.trim()
        val cleanEmail = email.trim().lowercase()

        // 1. Username validation: 3-30 chars, alphanumeric + underscore, at least one letter
        if (cleanUsername.length !in 3..30 ||
            !cleanUsername.matches(Regex("^[a-zA-Z0-9_]{3,30}$")) ||
            !cleanUsername.any { it.isLetter() }
        ) {
            _uiState.value = AuthUiState.Error(
                "Username must be 3-30 characters (letters, numbers, underscores) and contain at least one letter."
            )
            return
        }

        // 2. Email format validation
        if (!Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiState.value = AuthUiState.Error("Please enter a valid email address.")
            return
        }

        // 3. Password policy: min 8 characters, at least 1 letter and 1 number
        if (password.length < 8 || !password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            _uiState.value = AuthUiState.Error(
                "Password must be at least 8 characters long and contain both letters and numbers."
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            // Step 4: Pre-check username uniqueness on backend
            try {
                val checkRes = apiService.checkUsername(cleanUsername)
                if (checkRes.isSuccessful && checkRes.body()?.available == false) {
                    _uiState.value = AuthUiState.Error(
                        checkRes.body()?.message ?: "This username is already taken. Please choose another."
                    )
                    return@launch
                }
            } catch (e: Exception) {
                Log.w("AuthViewModel", "Pre-check username warning: ${e.message}")
            }

            // Step 5: Create user in Firebase Authentication
            firebaseAuth.createUserWithEmailAndPassword(cleanEmail, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = task.result?.user
                        if (firebaseUser != null) {
                            firebaseUser.getIdToken(false).addOnCompleteListener { tokenTask ->
                                if (tokenTask.isSuccessful) {
                                    val idToken = tokenTask.result?.token
                                    if (idToken != null) {
                                        viewModelScope.launch {
                                            try {
                                                val response = apiService.verifyFirebaseToken(
                                                    "Bearer $idToken",
                                                    FirebaseTokenRequest(
                                                        firebaseIdToken = idToken,
                                                        username = cleanUsername,
                                                        is_signup = true
                                                    )
                                                )
                                                if (response.isSuccessful) {
                                                    val body = response.body()
                                                    if (body != null) {
                                                        UserSession.saveTokens(
                                                            getApplication(),
                                                            body.accessToken,
                                                            body.refreshToken
                                                        )
                                                        val sessionUser = User(
                                                            id = body.user.id,
                                                            name = body.user.name,
                                                            username = body.user.username ?: cleanUsername,
                                                            email = body.user.email,
                                                            mobileNumber = body.user.mobileNumber,
                                                            houseAddress = body.user.houseAddress,
                                                            ebServiceNumber = UserSession.getSavedEbServiceNumber(getApplication()),
                                                            tnebRegisteredMobile = UserSession.getSavedTnebMobile(getApplication())
                                                        )
                                                        UserSession.saveUser(getApplication(), sessionUser)
                                                        _uiState.value = AuthUiState.Success()
                                                    } else {
                                                        rollbackFirebaseUser(firebaseUser, "Invalid backend response")
                                                    }
                                                } else {
                                                    val errorMsg = parseErrorMessage(
                                                        response.errorBody()?.string(),
                                                        response.code()
                                                    )
                                                    // Rollback Firebase user so no orphaned Firebase accounts exist
                                                    rollbackFirebaseUser(firebaseUser, errorMsg)
                                                }
                                            } catch (e: Exception) {
                                                rollbackFirebaseUser(
                                                    firebaseUser,
                                                    e.message ?: "Network error connecting to backend"
                                                )
                                            }
                                        }
                                    } else {
                                        rollbackFirebaseUser(firebaseUser, "Failed to obtain Firebase ID token")
                                    }
                                } else {
                                    rollbackFirebaseUser(
                                        firebaseUser,
                                        tokenTask.exception?.message ?: "Failed to get Firebase token"
                                    )
                                }
                            }
                        } else {
                            _uiState.value = AuthUiState.Error("Registration succeeded but user is null")
                        }
                    } else {
                        val ex = task.exception
                        val msg = if (ex is FirebaseAuthUserCollisionException) {
                            "An account with this email already exists. Please sign in or continue with Google."
                        } else {
                            ex?.message ?: "Registration failed."
                        }
                        _uiState.value = AuthUiState.Error(msg)
                    }
                }
        }
    }

    private fun rollbackFirebaseUser(user: FirebaseUser, reason: String) {
        user.delete().addOnCompleteListener {
            _uiState.value = AuthUiState.Error(reason)
        }
    }

    // -------------------------------------------------------------
    //  EMAIL OR USERNAME SIGN-IN
    // -------------------------------------------------------------
    fun signInWithEmailOrUsername(identifier: String, password: String) {
        val cleanIdentifier = identifier.trim()
        if (cleanIdentifier.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your username or email and password.")
            return
        }

        _uiState.value = AuthUiState.Loading

        if (cleanIdentifier.contains("@")) {
            // Directly email sign in
            signInWithEmail(cleanIdentifier.lowercase(), password)
        } else {
            // Username sign in: resolve identifier to email address first
            viewModelScope.launch {
                try {
                    val resolveRes = apiService.resolveIdentifier(
                        ResolveIdentifierRequest(cleanIdentifier.lowercase())
                    )
                    if (resolveRes.isSuccessful && resolveRes.body() != null) {
                        val resolvedEmail = resolveRes.body()!!.email
                        signInWithEmail(resolvedEmail, password)
                    } else {
                        _uiState.value = AuthUiState.Error("Invalid username or password.")
                    }
                } catch (e: Exception) {
                    _uiState.value = AuthUiState.Error("Invalid username or password.")
                }
            }
        }
    }

    private fun signInWithEmail(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    handleFirebaseUser(user, isSignUp = false)
                } else {
                    val ex = task.exception
                    Log.e("AuthViewModel", "Sign in failed: ${ex?.message}")
                    _uiState.value = AuthUiState.Error("Invalid username/email or password.")
                }
            }
    }

    // -------------------------------------------------------------
    //  GOOGLE AUTHENTICATION & ACCOUNT LINKING
    // -------------------------------------------------------------
    fun signInWithGoogle(googleIdToken: String, username: String? = null, isSignUp: Boolean = false) {
        val credential = GoogleAuthProvider.getCredential(googleIdToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    handleFirebaseUser(user, username, isSignUp)
                } else {
                    val exception = task.exception
                    if (exception is FirebaseAuthUserCollisionException) {
                        // Account exists with different credential (e.g. Email/Password)
                        pendingGoogleCredential = credential
                        val email = exception.email ?: extractEmailFromGoogleToken(googleIdToken)
                        _pendingLinkingEmail.value = email
                        _showLinkAccountDialog.value = true
                        _uiState.value = AuthUiState.Idle
                    } else {
                        _uiState.value = AuthUiState.Error(
                            exception?.message ?: "Google authentication failed"
                        )
                    }
                }
            }
    }

    fun confirmAccountLinking(password: String) {
        val email = _pendingLinkingEmail.value
        val credential = pendingGoogleCredential
        if (email.isNullOrBlank() || credential == null) {
            _uiState.value = AuthUiState.Error("Linking session expired. Please try again.")
            _showLinkAccountDialog.value = false
            return
        }

        if (password.isBlank()) {
            _uiState.value = AuthUiState.Error("Please enter your password.")
            return
        }

        _uiState.value = AuthUiState.Loading

        // Step 1: Authenticate with existing email/password account to verify account ownership
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { signInTask ->
                if (signInTask.isSuccessful) {
                    val currentUser = firebaseAuth.currentUser
                    if (currentUser == null) {
                        _uiState.value = AuthUiState.Error("Authentication succeeded but user is null")
                        return@addOnCompleteListener
                    }

                    // Step 2: Link Google credential to the authenticated Firebase account
                    currentUser.linkWithCredential(credential)
                        .addOnCompleteListener { linkTask ->
                            if (linkTask.isSuccessful) {
                                pendingGoogleCredential = null
                                _pendingLinkingEmail.value = null
                                _showLinkAccountDialog.value = false

                                // Step 3: Sync session with backend to get SolarEase tokens and preserve profile
                                handleFirebaseUser(currentUser, isSignUp = false)
                            } else {
                                val linkEx = linkTask.exception
                                Log.e("AuthViewModel", "Account linking failed: ${linkEx?.message}")
                                _uiState.value = AuthUiState.Error(
                                    linkEx?.message ?: "Failed to link Google account to existing profile."
                                )
                            }
                        }
                } else {
                    val ex = signInTask.exception
                    Log.e("AuthViewModel", "Sign in for linking failed: ${ex?.message}")
                    _uiState.value = AuthUiState.Error("Incorrect password for $email.")
                }
            }
    }

    fun cancelAccountLinking() {
        pendingGoogleCredential = null
        _pendingLinkingEmail.value = null
        _showLinkAccountDialog.value = false
        _uiState.value = AuthUiState.Idle
    }

    private fun extractEmailFromGoogleToken(token: String): String? {
        return try {
            val parts = token.split(".")
            if (parts.size > 1) {
                val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE))
                JSONObject(payloadJson).optString("email").ifBlank { null }
            } else null
        } catch (_: Exception) {
            null
        }
    }

    // -------------------------------------------------------------
    //  PASSWORD RECOVERY
    // -------------------------------------------------------------
    fun sendPasswordReset(email: String) {
        val cleanEmail = email.trim()
        if (cleanEmail.isBlank()) {
            _forgotPasswordStatus.value = "Please enter your registered email address."
            return
        }

        if (!cleanEmail.contains("@")) {
            _forgotPasswordStatus.value = "Please enter your registered email address for password recovery."
            return
        }

        firebaseAuth.sendPasswordResetEmail(cleanEmail.lowercase())
            .addOnCompleteListener { _ ->
                // Always show neutral confirmation as per requirement 5
                _forgotPasswordStatus.value =
                    "If an account is associated with $cleanEmail, a password reset link has been sent to your inbox."
            }
    }

    fun openForgotPasswordDialog() {
        _forgotPasswordStatus.value = null
        _showForgotPasswordDialog.value = true
    }

    fun closeForgotPasswordDialog() {
        _showForgotPasswordDialog.value = false
        _forgotPasswordStatus.value = null
    }

    // -------------------------------------------------------------
    //  EDGE CASE: ADD PASSWORD CREDENTIAL TO GOOGLE ACCOUNT
    // -------------------------------------------------------------
    fun addPasswordToCurrentAccount(password: String) {
        val currentUser = firebaseAuth.currentUser
        if (currentUser == null || currentUser.email == null) {
            _uiState.value = AuthUiState.Error("No active user session to add password.")
            return
        }
        if (password.length < 8 || !password.any { it.isLetter() } || !password.any { it.isDigit() }) {
            _uiState.value = AuthUiState.Error("Password must be at least 8 characters and include letters and numbers.")
            return
        }
        _uiState.value = AuthUiState.Loading
        val credential = EmailAuthProvider.getCredential(currentUser.email!!, password)
        currentUser.linkWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _uiState.value = AuthUiState.Success("Password credential successfully added!")
                } else {
                    _uiState.value = AuthUiState.Error(task.exception?.message ?: "Failed to set password.")
                }
            }
    }

    // -------------------------------------------------------------
    //  BACKEND SESSION SYNC
    // -------------------------------------------------------------
    private fun handleFirebaseUser(user: FirebaseUser?, username: String? = null, isSignUp: Boolean = false) {
        if (user != null) {
            user.getIdToken(false).addOnCompleteListener { tokenTask ->
                if (tokenTask.isSuccessful) {
                    val idToken = tokenTask.result?.token
                    if (idToken != null) {
                        viewModelScope.launch {
                            try {
                                val response = apiService.verifyFirebaseToken(
                                    "Bearer $idToken",
                                    FirebaseTokenRequest(
                                        firebaseIdToken = idToken,
                                        username = username,
                                        is_signup = isSignUp
                                    )
                                )
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    if (body != null) {
                                        UserSession.saveTokens(getApplication(), body.accessToken, body.refreshToken)
                                        val sessionUser = User(
                                            id = body.user.id,
                                            name = body.user.name,
                                            username = body.user.username ?: "User",
                                            email = body.user.email,
                                            mobileNumber = body.user.mobileNumber,
                                            houseAddress = body.user.houseAddress,
                                            ebServiceNumber = UserSession.getSavedEbServiceNumber(getApplication()),
                                            tnebRegisteredMobile = UserSession.getSavedTnebMobile(getApplication())
                                        )
                                        UserSession.saveUser(getApplication(), sessionUser)
                                        _uiState.value = AuthUiState.Success()
                                    } else {
                                        _uiState.value = AuthUiState.Error("Invalid backend response")
                                    }
                                } else {
                                    val errorMsg = parseErrorMessage(
                                        response.errorBody()?.string(),
                                        response.code()
                                    )
                                    _uiState.value = AuthUiState.Error(errorMsg)
                                }
                            } catch (e: Exception) {
                                _uiState.value = AuthUiState.Error(
                                    e.message ?: "Network error connecting to backend"
                                )
                            }
                        }
                    } else {
                        _uiState.value = AuthUiState.Error("Failed to obtain Firebase ID token")
                    }
                } else {
                    _uiState.value = AuthUiState.Error(
                        tokenTask.exception?.message ?: "Failed to get Firebase ID token"
                    )
                }
            }
        } else {
            _uiState.value = AuthUiState.Error("Authentication succeeded but user is null")
        }
    }

    private fun parseErrorMessage(errorBody: String?, statusCode: Int): String {
        var errorMessage = "Backend error: $statusCode"
        if (statusCode == 409) errorMessage = "An account with this email or username already exists. Please sign in instead."
        if (statusCode == 401) errorMessage = "Invalid or expired authentication session. Please try again."
        if (errorBody != null) {
            try {
                val json = JSONObject(errorBody)
                if (json.has("detail")) {
                    errorMessage = json.getString("detail")
                }
            } catch (_: Exception) {}
        }
        return errorMessage
    }

    // -------------------------------------------------------------
    //  PHONE AUTH (PRESERVED)
    // -------------------------------------------------------------
    fun sendOtp(phoneNumber: String, activity: Activity) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading

            if (phoneNumber.length < 10) {
                _uiState.value = AuthUiState.Error("Invalid phone number")
                return@launch
            }

            val normalizedNumber = if (phoneNumber.startsWith("+91")) {
                phoneNumber
            } else {
                "+91$phoneNumber"
            }

            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(normalizedNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        signInWithPhoneCredential(credential, null, normalizedNumber)
                    }

                    override fun onVerificationFailed(e: FirebaseException) {
                        _uiState.value = AuthUiState.Error(e.message ?: "Verification failed")
                    }

                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        storedVerificationId = verificationId
                        resendToken = token
                        _uiState.value = AuthUiState.Success("OTP sent successfully")
                    }
                })
                .build()

            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    fun verifyOtp(otp: String, username: String? = null, phoneNumber: String? = null, isSignUp: Boolean = true) {
        val verificationId = storedVerificationId
        if (verificationId == null) {
            _uiState.value = AuthUiState.Error("Please request OTP first.")
            return
        }

        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                signInWithPhoneCredential(credential, username, phoneNumber, isSignUp = isSignUp)
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Invalid OTP.")
            }
        }
    }

    private fun signInWithPhoneCredential(
        credential: PhoneAuthCredential,
        username: String?,
        phoneNumber: String?,
        isSignUp: Boolean = true
    ) {
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    handleFirebaseUser(user, username, isSignUp = isSignUp)
                } else {
                    _uiState.value = AuthUiState.Error(task.exception?.message ?: "Authentication failed")
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

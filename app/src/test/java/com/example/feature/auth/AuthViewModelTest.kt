package com.example.feature.auth

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.ApiService
import com.example.core.network.dto.CheckUsernameResponse
import com.example.core.network.dto.ResolveIdentifierRequest
import com.example.core.network.dto.ResolveIdentifierResponse
import com.example.feature.auth.ui.AuthUiState
import com.example.feature.auth.ui.AuthViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeApiService: FakeApiService

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        application = ApplicationProvider.getApplicationContext()
        val options = com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("1:123456789:android:abcdef")
            .setApiKey("AIzaSyD-fakeApiKeyForTesting1234567890")
            .setProjectId("solarease-test")
            .build()
        if (com.google.firebase.FirebaseApp.getApps(application).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(application, options)
        }
        fakeApiService = FakeApiService()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // -------------------------------------------------------------
    // SIGN-UP VALIDATION TESTS
    // -------------------------------------------------------------
    @Test
    fun signUp_rejectsShortUsername() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signUpWithEmail("ab", "user@example.com", "Password123!")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("Username must be 3-30 characters"))
    }

    @Test
    fun signUp_rejectsUsernameWithoutLetters() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signUpWithEmail("123456", "user@example.com", "Password123!")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("contain at least one letter"))
    }

    @Test
    fun signUp_rejectsInvalidEmailFormat() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signUpWithEmail("sanjai_dev", "invalid-email-format", "Password123!")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("valid email address"))
    }

    @Test
    fun signUp_rejectsShortPassword() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signUpWithEmail("sanjai_dev", "user@example.com", "Pass1")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("at least 8 characters"))
    }

    @Test
    fun signUp_rejectsPasswordWithoutNumbers() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signUpWithEmail("sanjai_dev", "user@example.com", "PasswordOnly")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("both letters and numbers"))
    }

    @Test
    fun signUp_whenUsernameAlreadyTaken_stopsAndReportsError() = runTest {
        fakeApiService.usernameAvailable = false
        fakeApiService.usernameMessage = "This username is already taken. Please choose another."

        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signUpWithEmail("existing_user", "newuser@example.com", "Password123!")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertEquals("This username is already taken. Please choose another.", msg)
    }

    // -------------------------------------------------------------
    // SIGN-IN TESTS
    // -------------------------------------------------------------
    @Test
    fun signIn_withBlankCredentials_setsError() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.signInWithEmailOrUsername("", "")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("Please enter your username or email"))
    }

    @Test
    fun signIn_withUsername_resolvesIdentifierViaBackend() = runTest {
        fakeApiService.resolvedEmail = "sanjai@example.com"
        val viewModel = AuthViewModel(application, apiService = fakeApiService)

        viewModel.signInWithEmailOrUsername("sanjai123", "Secret123!")
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("sanjai123", fakeApiService.lastResolvedIdentifier)
    }

    @Test
    fun signIn_withUsername_whenResolutionFails_returnsGenericError() = runTest {
        fakeApiService.resolveShouldFail = true
        val viewModel = AuthViewModel(application, apiService = fakeApiService)

        viewModel.signInWithEmailOrUsername("unknown_user", "Secret123!")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertEquals("Invalid username or password.", msg)
    }

    @Test
    fun signIn_withEmail_doesNotCallResolveIdentifier() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)

        viewModel.signInWithEmailOrUsername("sanjai@example.com", "Secret123!")
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(fakeApiService.lastResolvedIdentifier)
    }

    // -------------------------------------------------------------
    // FORGOT PASSWORD TESTS
    // -------------------------------------------------------------
    @Test
    fun forgotPassword_withBlankEmail_setsWarning() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.sendPasswordReset("  ")

        assertEquals(
            "Please enter your registered email address.",
            viewModel.forgotPasswordStatus.value
        )
    }

    @Test
    fun forgotPassword_withUsername_requestsRegisteredEmail() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.sendPasswordReset("sanjai123")

        assertEquals(
            "Please enter your registered email address for password recovery.",
            viewModel.forgotPasswordStatus.value
        )
    }

    @Test
    fun forgotPassword_dialogVisibilityControls() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        assertFalse(viewModel.showForgotPasswordDialog.value)

        viewModel.openForgotPasswordDialog()
        assertTrue(viewModel.showForgotPasswordDialog.value)

        viewModel.closeForgotPasswordDialog()
        assertFalse(viewModel.showForgotPasswordDialog.value)
    }

    // -------------------------------------------------------------
    // ACCOUNT LINKING TESTS
    // -------------------------------------------------------------
    @Test
    fun accountLinking_cancelLinking_clearsState() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        viewModel.cancelAccountLinking()

        assertFalse(viewModel.showLinkAccountDialog.value)
        assertNull(viewModel.pendingLinkingEmail.value)
        assertEquals(AuthUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun accountLinking_confirmLinking_withBlankPassword_setsError() = runTest {
        val viewModel = AuthViewModel(application, apiService = fakeApiService)
        // With no pending email or credentials:
        viewModel.confirmAccountLinking("password123")

        assertTrue(viewModel.uiState.value is AuthUiState.Error)
        val msg = (viewModel.uiState.value as AuthUiState.Error).message
        assertTrue(msg.contains("Linking session expired"))
    }

    @Test
    fun viewModel_hasAndroidViewModelSingleArgConstructor() {
        val constructor = AuthViewModel::class.java.getConstructor(Application::class.java)
        val vm = constructor.newInstance(application)
        assertEquals(AuthUiState.Idle, vm.uiState.value)
    }
}

// -----------------------------------------------------------------
// Fake ApiService for unit tests
// -----------------------------------------------------------------
class FakeApiService : ApiService {
    var usernameAvailable: Boolean = true
    var usernameMessage: String = "Username is available"
    var lastResolvedIdentifier: String? = null
    var resolvedEmail: String? = null
    var resolveShouldFail: Boolean = false

    override suspend fun checkUsername(username: String): Response<CheckUsernameResponse> {
        return Response.success(CheckUsernameResponse(available = usernameAvailable, message = usernameMessage))
    }

    override suspend fun resolveIdentifier(request: ResolveIdentifierRequest): Response<ResolveIdentifierResponse> {
        lastResolvedIdentifier = request.identifier
        if (resolveShouldFail || resolvedEmail == null) {
            val errorBody = "{\"detail\":\"Invalid username or password.\"}".toResponseBody("application/json".toMediaType())
            return Response.error(404, errorBody)
        }
        return Response.success(ResolveIdentifierResponse(email = resolvedEmail!!))
    }

    override suspend fun verifyFirebaseToken(
        authHeader: String,
        request: com.example.core.network.dto.FirebaseTokenRequest
    ): Response<com.example.core.network.dto.TokenResponse> {
        throw NotImplementedError()
    }

    override suspend fun queryChatbot(
        request: com.example.core.network.dto.ChatQueryRequest
    ): Response<com.example.core.network.dto.ChatResponse> {
        throw NotImplementedError()
    }

    override suspend fun updateProfile(
        request: com.example.core.network.dto.UserUpdateRequest
    ): Response<com.example.core.network.dto.UserResponse> {
        throw NotImplementedError()
    }

    override suspend fun getProfile(): Response<com.example.core.network.dto.UserResponse> {
        throw NotImplementedError()
    }

    override suspend fun refreshToken(
        request: com.example.core.network.dto.RefreshTokenRequest
    ): Response<com.example.core.network.dto.TokenResponse> {
        throw NotImplementedError()
    }

    override suspend fun logout(): Response<Unit> {
        return Response.success(Unit)
    }
}

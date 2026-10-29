package com.example.feature.auth

import android.app.Application
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import com.example.feature.auth.ui.AuthMainRoute
import com.example.feature.auth.ui.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h915dp-xxhdpi")
class AuthMainScreenLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var fakeApiService: FakeApiService
    private lateinit var viewModel: AuthViewModel

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
        viewModel = AuthViewModel(application, apiService = fakeApiService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun signIn_showsContinueWithGoogleFirst_thenOrDivider_thenFormFields() {
        composeTestRule.setContent {
            AuthMainRoute(
                onNavigateToPhoneAuth = {},
                onNavigateToHome = {},
                viewModel = viewModel
            )
        }

        // 1. Verify Continue with Google button is present
        val googleButton = composeTestRule.onNodeWithText("Continue with Google")
        googleButton.assertExists()

        // 2. Verify "or" divider is present
        val orDivider = composeTestRule.onNodeWithText("or")
        orDivider.assertExists()

        // 3. Verify Sign In form fields are present
        val identifierField = composeTestRule.onNodeWithText("Username or email address")
        identifierField.assertExists()

        val passwordField = composeTestRule.onNodeWithText("Password")
        passwordField.assertExists()

        // The primary Sign In button (last node with "Sign In", distinct from tab)
        val signInButton = composeTestRule.onAllNodesWithText("Sign In").onLast()
        signInButton.assertExists()

        // 4. Verify vertical layout order:
        // Google button < "or" divider < Identifier < Password < Sign In button
        val googleBounds = googleButton.getBoundsInRoot()
        val orBounds = orDivider.getBoundsInRoot()
        val identifierBounds = identifierField.getBoundsInRoot()
        val passwordBounds = passwordField.getBoundsInRoot()
        val signInBounds = signInButton.getBoundsInRoot()

        assertTrue(
            "Expected Google button top (${googleBounds.top}) to be above OR divider top (${orBounds.top})",
            googleBounds.top < orBounds.top
        )
        assertTrue(
            "Expected OR divider top (${orBounds.top}) to be above identifier field top (${identifierBounds.top})",
            orBounds.top < identifierBounds.top
        )
        assertTrue(
            "Expected identifier field top (${identifierBounds.top}) to be above password field top (${passwordBounds.top})",
            identifierBounds.top < passwordBounds.top
        )
        assertTrue(
            "Expected password field top (${passwordBounds.top}) to be above Sign In button top (${signInBounds.top})",
            passwordBounds.top < signInBounds.top
        )
    }

    @Test
    fun createAccount_showsContinueWithGoogleFirst_thenOrDivider_thenCreateAccountForm() {
        composeTestRule.setContent {
            AuthMainRoute(
                onNavigateToPhoneAuth = {},
                onNavigateToHome = {},
                viewModel = viewModel
            )
        }

        // Switch to Create Account mode via segmented switcher (first node is the tab)
        val createAccountTab = composeTestRule.onAllNodesWithText("Create Account").onFirst()
        createAccountTab.performClick()
        composeTestRule.waitForIdle()

        // 1. Verify Continue with Google button is STILL at top
        val googleButton = composeTestRule.onNodeWithText("Continue with Google")
        googleButton.assertExists()

        // 2. Verify "or" divider is present
        val orDivider = composeTestRule.onNodeWithText("or")
        orDivider.assertExists()

        // 3. Verify Create Account form fields are displayed
        val usernameField = composeTestRule.onNodeWithText("Username")
        usernameField.assertExists()

        val emailField = composeTestRule.onNodeWithText("Email address")
        emailField.assertExists()

        val passwordField = composeTestRule.onNodeWithText("Password")
        passwordField.assertExists()

        // The primary Create Account button (last node with "Create Account", distinct from tab)
        val createAccountButton = composeTestRule.onAllNodesWithText("Create Account").onLast()
        createAccountButton.assertExists()

        // 4. Verify vertical layout order:
        // Google button < "or" divider < Username < Email < Password < Create Account button
        val googleBounds = googleButton.getBoundsInRoot()
        val orBounds = orDivider.getBoundsInRoot()
        val usernameBounds = usernameField.getBoundsInRoot()
        val emailBounds = emailField.getBoundsInRoot()
        val passwordBounds = passwordField.getBoundsInRoot()
        val buttonBounds = createAccountButton.getBoundsInRoot()

        assertTrue(
            "Expected Google button top (${googleBounds.top}) to be above OR divider top (${orBounds.top})",
            googleBounds.top < orBounds.top
        )
        assertTrue(
            "Expected OR divider top (${orBounds.top}) to be above username field top (${usernameBounds.top})",
            orBounds.top < usernameBounds.top
        )
        assertTrue(
            "Expected username field top (${usernameBounds.top}) to be above email field top (${emailBounds.top})",
            usernameBounds.top < emailBounds.top
        )
        assertTrue(
            "Expected email field top (${emailBounds.top}) to be above password field top (${passwordBounds.top})",
            emailBounds.top < passwordBounds.top
        )
        assertTrue(
            "Expected password field top (${passwordBounds.top}) to be above Create Account button top (${buttonBounds.top})",
            passwordBounds.top < buttonBounds.top
        )
    }

    @Test
    fun switchingModes_maintainsContinueWithGoogleAtTopWithoutFlickering() {
        composeTestRule.setContent {
            AuthMainRoute(
                onNavigateToPhoneAuth = {},
                onNavigateToHome = {},
                viewModel = viewModel
            )
        }

        val initialGoogleTop = composeTestRule.onNodeWithText("Continue with Google").getBoundsInRoot().top

        // Switch to Create Account
        composeTestRule.onAllNodesWithText("Create Account").onFirst().performClick()
        composeTestRule.waitForIdle()

        val createAccountGoogleTop = composeTestRule.onNodeWithText("Continue with Google").getBoundsInRoot().top

        // Switch back to Sign In
        composeTestRule.onAllNodesWithText("Sign In").onFirst().performClick()
        composeTestRule.waitForIdle()

        val signInGoogleTop = composeTestRule.onNodeWithText("Continue with Google").getBoundsInRoot().top

        // Google button stays stably at the exact same vertical position
        assertTrue(
            "Expected Google button top in Create Account ($createAccountGoogleTop) to match Sign In ($initialGoogleTop)",
            Math.abs(createAccountGoogleTop.value - initialGoogleTop.value) < 1.0f
        )
        assertTrue(
            "Expected Google button top after switching back ($signInGoogleTop) to match Sign In ($initialGoogleTop)",
            Math.abs(signInGoogleTop.value - initialGoogleTop.value) < 1.0f
        )
    }

    @Test
    fun forgotPassword_linkIsPresentAndTriggersDialogState() {
        composeTestRule.setContent {
            AuthMainRoute(
                onNavigateToPhoneAuth = {},
                onNavigateToHome = {},
                viewModel = viewModel
            )
        }

        val forgotPassword = composeTestRule.onNodeWithText("Forgot password?")
        forgotPassword.assertExists()
        forgotPassword.performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertTrue(
            "Expected forgot password dialog to be opened in ViewModel",
            viewModel.showForgotPasswordDialog.value
        )
    }
}

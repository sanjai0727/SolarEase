package com.example.feature.auth.ui

import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.window.Dialog
import com.example.core.legal.LegalComplianceDialog
import com.example.core.legal.LegalSection
import com.example.ui.theme.rememberIsReducedMotionEnabled
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

enum class AuthMode {
    SIGN_IN, SIGN_UP
}

@Composable
fun AuthMainRoute(
    onNavigateToPhoneAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showLinkAccountDialog by viewModel.showLinkAccountDialog.collectAsStateWithLifecycle()
    val pendingLinkingEmail by viewModel.pendingLinkingEmail.collectAsStateWithLifecycle()
    val showForgotPasswordDialog by viewModel.showForgotPasswordDialog.collectAsStateWithLifecycle()
    val forgotPasswordStatus by viewModel.forgotPasswordStatus.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var authMode by rememberSaveable { mutableStateOf(AuthMode.SIGN_IN) }

    // Form inputs
    var identifier by rememberSaveable { mutableStateOf("") } // username or email for sign-in
    var username by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    // Dialog inputs
    var linkingPassword by rememberSaveable { mutableStateOf("") }
    var linkingPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var forgotPasswordEmail by rememberSaveable { mutableStateOf("") }
    var activeLegalSection by remember { mutableStateOf<LegalSection?>(null) }

    val focusManager = LocalFocusManager.current

    // Launch Google Auth
    val submitGoogleAuth: () -> Unit = {
        if (uiState !is AuthUiState.Loading) {
            val activity = context as? Activity
            if (activity != null) {
                coroutineScope.launch {
                    viewModel.setLoading()
                    val credentialManager = CredentialManager.create(context)

                    val googleIdOption = GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(context.getString(R.string.default_web_client_id))
                        .setAutoSelectEnabled(false)
                        .build()

                    val request = GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build()

                    try {
                        val result = credentialManager.getCredential(
                            request = request,
                            context = activity
                        )
                        val credential = result.credential
                        if (credential is androidx.credentials.CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.signInWithGoogle(
                                googleIdToken = googleIdTokenCredential.idToken,
                                isSignUp = authMode == AuthMode.SIGN_UP
                            )
                        } else {
                            viewModel.setError("Unexpected credential type")
                        }
                    } catch (e: GetCredentialCancellationException) {
                        viewModel.resetState()
                    } catch (e: androidx.credentials.exceptions.NoCredentialException) {
                        android.util.Log.w("AuthMainScreen", "No credentials available for Google Sign-In", e)
                        viewModel.setError(
                            "No Google account credentials available on this device. Please check your Google account in Device Settings or sign in with email and password below."
                        )
                    } catch (e: GetCredentialException) {
                        val msg = e.errorMessage?.toString() ?: (e.message ?: "Google Sign-In failed")
                        android.util.Log.e("AuthMainScreen", "GetCredentialException: $msg", e)
                        if (msg.contains("28444") || msg.contains("Developer console")) {
                            viewModel.setError(
                                "Google Sign-In setup error (10: [28444]): The signing key (SHA-1) of this build must be registered in the Firebase Console."
                            )
                        } else {
                            viewModel.setError(msg)
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        val msg = e.message ?: "An error occurred during Google Sign-In"
                        android.util.Log.e("AuthMainScreen", "Google Sign-In general exception", e)
                        viewModel.setError(msg)
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message == null) {
            viewModel.resetState()
            onNavigateToHome()
        }
    }

    // SolarEase Visual Tokens
    val primaryGreen = MaterialTheme.colorScheme.primary
    val backgroundCream = MaterialTheme.colorScheme.background
    val textPrimary = MaterialTheme.colorScheme.onBackground
    val textSecondary = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val inputBackground = MaterialTheme.colorScheme.surface

    Scaffold(
        containerColor = backgroundCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // -------------------------------------------------------------
            //  1. GREEN ABSTRACT WAVE HEADER
            // -------------------------------------------------------------
            GreenAbstractWaveHeader()

            Spacer(modifier = Modifier.height(16.dp))

            // -------------------------------------------------------------
            //  2. HEADINGS & SHORT SUBTITLE
            // -------------------------------------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (authMode == AuthMode.SIGN_IN) "Sign in to" else "Create account with",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "SolarEase",
                    style = MaterialTheme.typography.headlineMedium,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = primaryGreen,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Green Accent Bar
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(primaryGreen)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Understand your energy.\nTake control of your usage.",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.5.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // -------------------------------------------------------------
            //  3. ANIMATED SEGMENTED SWITCHER (Sign In / Create Account)
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedSegmentedSwitcher(
                    selectedMode = authMode,
                    onModeSelected = { newMode ->
                        viewModel.resetState()
                        authMode = newMode
                    },
                    modifier = Modifier.widthIn(max = 440.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // -------------------------------------------------------------
            //  4. FORM CARD CONTAINER WITH ANIMATED TRANSITIONS
            // -------------------------------------------------------------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, outlineBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // ERROR BANNER
                    if (uiState is AuthUiState.Error) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = (uiState as AuthUiState.Error).message,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // SUCCESS BANNER (e.g. Password reset link sent)
                    if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .semantics { liveRegion = LiveRegionMode.Polite },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, primaryGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = (uiState as AuthUiState.Success).message!!,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                        }
                    }

                    // =========================================================
                    // A. CONTINUE WITH GOOGLE — FIRST
                    // =========================================================
                    OutlinedButton(
                        onClick = submitGoogleAuth,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = textPrimary
                        ),
                        border = BorderStroke(1.dp, outlineBorder),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 1.dp,
                            pressedElevation = 2.dp
                        ),
                        enabled = uiState !is AuthUiState.Loading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary,
                                fontSize = 14.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // =========================================================
                    // A2. EXPLORE AS GUEST (Zero-Friction Activation)
                    // =========================================================
                    OutlinedButton(
                        onClick = onNavigateToHome,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = primaryGreen
                        ),
                        border = BorderStroke(1.dp, primaryGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = "Explore SolarEase as Guest",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = primaryGreen,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // =========================================================
                    // B. OR DIVIDER
                    // =========================================================
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = outlineBorder,
                            thickness = 1.dp
                        )
                        Text(
                            text = "or",
                            color = textSecondary,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            color = outlineBorder,
                            thickness = 1.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // =========================================================
                    // C. USERNAME/EMAIL AND PASSWORD — ALTERNATIVE
                    // DYNAMIC ANIMATED FORM CONTENT
                    // =========================================================
                    AnimatedContent(
                        targetState = authMode,
                        transitionSpec = {
                            if (targetState == AuthMode.SIGN_UP) {
                                (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { width -> width / 3 } +
                                        fadeIn(tween(200)))
                                    .togetherWith(
                                        slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { width -> -width / 3 } +
                                                fadeOut(tween(150))
                                    )
                            } else {
                                (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) { width -> -width / 3 } +
                                        fadeIn(tween(200)))
                                    .togetherWith(
                                        slideOutHorizontally(tween(200, easing = FastOutSlowInEasing)) { width -> width / 3 } +
                                                fadeOut(tween(150))
                                    )
                            }
                        },
                        label = "authFormTransition"
                    ) { mode ->
                        Column(modifier = Modifier.fillMaxWidth()) {
                            if (mode == AuthMode.SIGN_IN) {
                                // =========================================
                                // SIGN-IN FORM
                                // =========================================
                                Text(
                                    text = "Username or email address",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = identifier,
                                    onValueChange = { identifier = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "Enter your username or email",
                                            color = textSecondary.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryGreen,
                                        unfocusedBorderColor = outlineBorder,
                                        focusedContainerColor = inputBackground,
                                        unfocusedContainerColor = inputBackground,
                                        cursorColor = primaryGreen
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Password",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textPrimary,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "Forgot password?",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = primaryGreen,
                                        fontSize = 12.sp,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            forgotPasswordEmail = if (identifier.contains("@")) identifier else ""
                                            viewModel.openForgotPasswordDialog()
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "Enter your password",
                                            color = textSecondary.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                tint = textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryGreen,
                                        unfocusedBorderColor = outlineBorder,
                                        focusedContainerColor = inputBackground,
                                        unfocusedContainerColor = inputBackground,
                                        cursorColor = primaryGreen
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        viewModel.signInWithEmailOrUsername(identifier, password)
                                    })
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Primary Sign In Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.signInWithEmailOrUsername(identifier, password)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    enabled = uiState !is AuthUiState.Loading
                                ) {
                                    if (uiState is AuthUiState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Sign In",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Switcher link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Don't have an account? ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = "Create Account",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryGreen,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            viewModel.resetState()
                                            authMode = AuthMode.SIGN_UP
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = onNavigateToPhoneAuth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, outlineBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = textPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = primaryGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sign in with Phone / OTP",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }

                            } else {
                                // =========================================
                                // CREATE ACCOUNT FORM
                                // =========================================
                                Text(
                                    text = "Username",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = username,
                                    onValueChange = { username = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "e.g. solar_champion",
                                            color = textSecondary.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryGreen,
                                        unfocusedBorderColor = outlineBorder,
                                        focusedContainerColor = inputBackground,
                                        unfocusedContainerColor = inputBackground,
                                        cursorColor = primaryGreen
                                    ),
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )
                                Text(
                                    text = "3-30 characters (letters, numbers, underscore). Must contain a letter.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Email address",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "name@example.com",
                                            color = textSecondary.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryGreen,
                                        unfocusedBorderColor = outlineBorder,
                                        focusedContainerColor = inputBackground,
                                        unfocusedContainerColor = inputBackground,
                                        cursorColor = primaryGreen
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Email,
                                        imeAction = ImeAction.Next
                                    ),
                                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Password",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textPrimary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    placeholder = {
                                        Text(
                                            "Create a password",
                                            color = textSecondary.copy(alpha = 0.6f),
                                            fontSize = 14.sp
                                        )
                                    },
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                                tint = textSecondary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = primaryGreen,
                                        unfocusedBorderColor = outlineBorder,
                                        focusedContainerColor = inputBackground,
                                        unfocusedContainerColor = inputBackground,
                                        cursorColor = primaryGreen
                                    ),
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Password,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(onDone = {
                                        focusManager.clearFocus()
                                        viewModel.signUpWithEmail(username, email, password)
                                    })
                                )
                                Text(
                                    text = "Make sure it's at least 8 characters including letters and numbers.",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = textSecondary,
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                // Primary Create Account Button
                                Button(
                                    onClick = {
                                        focusManager.clearFocus()
                                        viewModel.signUpWithEmail(username, email, password)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                    enabled = uiState !is AuthUiState.Loading
                                ) {
                                    if (uiState is AuthUiState.Loading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            strokeWidth = 2.dp
                                        )
                                    } else {
                                        Text(
                                            text = "Create Account",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontSize = 15.sp
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Switcher link
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Already have an account? ",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        color = textSecondary
                                    )
                                    Text(
                                        text = "Sign In",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = primaryGreen,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            viewModel.resetState()
                                            authMode = AuthMode.SIGN_IN
                                        }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = onNavigateToPhoneAuth,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, outlineBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = textPrimary
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = primaryGreen
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Sign up with Phone / OTP",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(thickness = 0.5.dp, color = outlineBorder.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Statutory Consent & Legal Links (DPDP Act 2023 § 5 & § 6)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "By continuing, you consent to personal data processing under India's DPDP Act, 2023.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Terms of Service",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryGreen,
                                modifier = Modifier.clickable {
                                    activeLegalSection = LegalSection.TERMS_OF_SERVICE
                                }
                            )
                            Text(
                                text = "  •  ",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                            Text(
                                text = "Privacy Policy",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryGreen,
                                modifier = Modifier.clickable {
                                    activeLegalSection = LegalSection.PRIVACY_POLICY
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))
        }
    }

    if (activeLegalSection != null) {
        LegalComplianceDialog(
            initialSection = activeLegalSection!!,
            onDismiss = { activeLegalSection = null }
        )
    }

    // -----------------------------------------------------------------
    //  ACCOUNT LINKING DIALOG
    // -----------------------------------------------------------------
    if (showLinkAccountDialog) {
        Dialog(onDismissRequest = { viewModel.cancelAccountLinking() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, outlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Link Existing Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "An account associated with ${pendingLinkingEmail ?: "this email"} already exists. To securely link your Google account to your SolarEase profile, enter your password.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        lineHeight = 18.sp,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = linkingPassword,
                        onValueChange = { linkingPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Password", color = textSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        visualTransformation = if (linkingPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { linkingPasswordVisible = !linkingPasswordVisible }) {
                                Icon(
                                    imageVector = if (linkingPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (linkingPasswordVisible) "Hide password" else "Show password",
                                    tint = textSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = {
                            focusManager.clearFocus()
                            viewModel.confirmAccountLinking(linkingPassword)
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = outlineBorder,
                            focusedContainerColor = inputBackground,
                            unfocusedContainerColor = inputBackground,
                            cursorColor = primaryGreen
                        )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = {
                                linkingPassword = ""
                                viewModel.cancelAccountLinking()
                            },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, outlineBorder)
                        ) {
                            Text("Cancel", color = textSecondary, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                viewModel.confirmAccountLinking(linkingPassword)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            enabled = uiState !is AuthUiState.Loading && linkingPassword.isNotBlank()
                        ) {
                            if (uiState is AuthUiState.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Link Account", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // -----------------------------------------------------------------
    //  FORGOT PASSWORD DIALOG
    // -----------------------------------------------------------------
    if (showForgotPasswordDialog) {
        Dialog(onDismissRequest = { viewModel.closeForgotPasswordDialog() }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, outlineBorder)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "Reset Password",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Enter your registered email address and we will send you a password reset link.",
                        style = MaterialTheme.typography.bodySmall,
                        color = textSecondary,
                        lineHeight = 18.sp,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (forgotPasswordStatus != null) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 14.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            border = BorderStroke(1.dp, primaryGreen.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = forgotPasswordStatus!!,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = forgotPasswordEmail,
                        onValueChange = { forgotPasswordEmail = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("name@example.com", color = textSecondary.copy(alpha = 0.6f)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryGreen,
                            unfocusedBorderColor = outlineBorder,
                            focusedContainerColor = inputBackground,
                            unfocusedContainerColor = inputBackground,
                            cursorColor = primaryGreen
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.closeForgotPasswordDialog() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, outlineBorder)
                        ) {
                            Text("Close", color = textSecondary, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Button(
                            onClick = {
                                viewModel.sendPasswordReset(forgotPasswordEmail)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                            enabled = forgotPasswordEmail.isNotBlank()
                        ) {
                            Text("Send Link", color = MaterialTheme.colorScheme.onPrimary, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
//  GREEN ABSTRACT WAVE HEADER
// ---------------------------------------------------------------------
@Composable
private fun GreenAbstractWaveHeader(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "SolarEase Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "SolarEase",
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// ---------------------------------------------------------------------
//  ANIMATED SEGMENTED SWITCHER (Interactive Left-to-Right Pill)
// ---------------------------------------------------------------------
@Composable
private fun AnimatedSegmentedSwitcher(
    selectedMode: AuthMode,
    onModeSelected: (AuthMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryGreen = MaterialTheme.colorScheme.primary
    val textMuted = MaterialTheme.colorScheme.onSurfaceVariant
    val trackBackground = MaterialTheme.colorScheme.surfaceVariant
    val trackBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    val indicatorBg = MaterialTheme.colorScheme.surface
    val indicatorBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val reducedMotion = rememberIsReducedMotionEnabled()

    // 0f = SIGN_IN (left), 1f = SIGN_UP (right)
    val targetPosition = if (selectedMode == AuthMode.SIGN_IN) 0f else 1f
    val animatedOffset by animateFloatAsState(
        targetValue = targetPosition,
        animationSpec = tween(
            durationMillis = if (reducedMotion) 0 else 260,
            easing = FastOutSlowInEasing
        ),
        label = "indicatorOffset"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackBackground)
            .border(1.dp, trackBorder, RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        val totalWidth = maxWidth
        val tabWidth = (totalWidth - 4.dp) / 2

        // Animated Sliding Selection Indicator (Pill adapting to surface color)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (animatedOffset * (tabWidth + 4.dp).toPx()).roundToInt(),
                        y = 0
                    )
                }
                .width(tabWidth)
                .fillMaxHeight()
                .shadow(elevation = 2.dp, shape = RoundedCornerShape(11.dp))
                .clip(RoundedCornerShape(11.dp))
                .background(indicatorBg)
                .border(0.5.dp, indicatorBorder, RoundedCornerShape(11.dp))
        )

        // Tab Labels (Equal Width)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sign In Option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics(mergeDescendants = true) {
                        role = Role.Tab
                        selected = selectedMode == AuthMode.SIGN_IN
                        stateDescription = if (selectedMode == AuthMode.SIGN_IN) "Selected" else "Not selected"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) {
                        onModeSelected(AuthMode.SIGN_IN)
                    },
                contentAlignment = Alignment.Center
            ) {
                val signInColor by animateColorAsState(
                    targetValue = if (selectedMode == AuthMode.SIGN_IN) primaryGreen else textMuted,
                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
                    label = "signInTextColor"
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.5.sp,
                    fontWeight = if (selectedMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                    color = signInColor
                )
            }

            // Create Account Option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .semantics(mergeDescendants = true) {
                        role = Role.Tab
                        selected = selectedMode == AuthMode.SIGN_UP
                        stateDescription = if (selectedMode == AuthMode.SIGN_UP) "Selected" else "Not selected"
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) {
                        onModeSelected(AuthMode.SIGN_UP)
                    },
                contentAlignment = Alignment.Center
            ) {
                val signUpColor by animateColorAsState(
                    targetValue = if (selectedMode == AuthMode.SIGN_UP) primaryGreen else textMuted,
                    animationSpec = tween(durationMillis = if (reducedMotion) 0 else 200),
                    label = "signUpTextColor"
                )
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.5.sp,
                    fontWeight = if (selectedMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
                    color = signUpColor
                )
            }
        }
    }
}

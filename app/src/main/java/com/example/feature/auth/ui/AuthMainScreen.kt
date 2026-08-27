package com.example.feature.auth.ui

import android.app.Activity
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.ui.components.StepProgress
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


enum class AuthMode {
    SIGN_UP, SIGN_IN
}

enum class ActiveAuthAction {
    NONE, GOOGLE, FACEBOOK
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthMainRoute(
    onNavigateToPhoneAuth: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var authMode by rememberSaveable { mutableStateOf(AuthMode.SIGN_UP) }
    var username by rememberSaveable { mutableStateOf("") }
    var activeAuthAction by rememberSaveable { mutableStateOf(ActiveAuthAction.NONE) }

    var usernameError by rememberSaveable { mutableStateOf<String?>(null) }

    val usernameRegex = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,30}$".toRegex()
    val isUsernameValid = usernameError == null

    val submitAuth: () -> Unit = {
        if (uiState !is AuthUiState.Loading) {
            val sanitized = username.filterNot { it.isWhitespace() }
            username = sanitized
            var isValid = true
            if (authMode == AuthMode.SIGN_UP) {
                if (!usernameRegex.matches(sanitized)) {
                    usernameError = "Username must be 3–30 characters and can contain letters, numbers, and underscores."
                    isValid = false
                } else {
                    usernameError = null
                }
            }
            if (isValid) {
                Log.d("AuthFlow", "Username validation passed")
                Log.d("AuthFlow", "Google authentication started")
                activeAuthAction = ActiveAuthAction.GOOGLE
                val activity = context as? Activity
                if (activity != null) {
                    coroutineScope.launch {
                        viewModel.setLoading()
                        val credentialManager = CredentialManager.create(context)

                        val googleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(context.getString(R.string.default_web_client_id))
                            .setAutoSelectEnabled(false)
                            .build()

                        val request: GetCredentialRequest = GetCredentialRequest.Builder()
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
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken, sanitized, authMode == AuthMode.SIGN_UP)
                            } else {
                                Log.e("AuthMainRoute", "Unexpected credential type")
                                viewModel.setError("Unexpected credential type")
                            }
                        } catch (e: GetCredentialException) {
                            Log.e(
                                "AuthMainRoute",
                                "GetCredentialException: type=${e::class.java.name}, message=${e.message}, errorMessage=${e.errorMessage}",
                                e
                            )
                            viewModel.setError(
                                e.errorMessage?.toString() ?: "Sign in cancelled"
                            )
                        } catch (e: Exception) {
                            Log.e(
                                "AuthMainRoute",
                                "Exception: type=${e::class.java.name}, message=${e.message}",
                                e
                            )
                            viewModel.setError(e.message ?: "An error occurred")
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(uiState) {
        if (uiState !is AuthUiState.Loading) {
            activeAuthAction = ActiveAuthAction.NONE
        }
        if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message == null) {
            viewModel.resetState()
            Log.d("AuthFlow", "Navigating to Home")
            onNavigateToHome()
        }
    }

    val primaryGreen = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        //  HERO SECTION â€” Clean abstract energy-tech background
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAbstractEnergyBackground(size)
            }

            // SolarEase logo and app name â€” primary focal point
            Column(
                modifier = Modifier
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo container
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WbSunny,
                        contentDescription = "SolarEase Logo",
                        tint = primaryGreen,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // App name
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(
                            color = Color(0xFF1A1A1A),
                            fontWeight = FontWeight.Bold
                        )) {
                            append("Solar")
                        }
                        withStyle(style = SpanStyle(
                            color = primaryGreen,
                            fontWeight = FontWeight.Bold
                        )) {
                            append("Ease")
                        }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 22.sp
                )
            }
        }

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        //  WAVE TRANSITION â€” smooth blend into white
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .offset(y = (-32).dp)
        ) {
            val w = size.width
            val h = size.height
            val wavePath = Path().apply {
                moveTo(0f, 0f)
                cubicTo(w * 0.30f, h * 1.2f, w * 0.70f, h * -0.2f, w, h * 0.35f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wavePath, Color.White)
        }

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        //  WELCOME TEXT â€” Dynamic based on auth mode
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated heading
            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    (fadeIn(tween(250, easing = EaseOutCubic)) +
                        slideInHorizontally(tween(300, easing = EaseOutCubic)) { if (targetState == AuthMode.SIGN_IN) -40 else 40 })
                        .togetherWith(
                            fadeOut(tween(150, easing = EaseInCubic)) +
                                slideOutHorizontally(tween(200, easing = EaseInCubic)) { if (targetState == AuthMode.SIGN_IN) 40 else -40 }
                        )
                },
                label = "welcomeHeading"
            ) { mode ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (mode == AuthMode.SIGN_IN) "Welcome back to" else "Start your journey with",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A1A1A),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SolarEase",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryGreen,
                        fontSize = 34.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Green accent line
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryGreen)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Animated subtitle
            AnimatedContent(
                targetState = authMode,
                transitionSpec = {
                    fadeIn(tween(280, 80, easing = EaseOutCubic))
                        .togetherWith(fadeOut(tween(120, easing = EaseInCubic)))
                },
                label = "welcomeSubtitle"
            ) { mode ->
                Text(
                    text = if (mode == AuthMode.SIGN_IN)
                        "Continue understanding your energy.\nTake control of your usage."
                    else
                        "Understand your energy.\nTake control of your usage.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF8A8A8A),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        //  AUTH SECTION
        // â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .padding(horizontal = 24.dp)
        ) {
            // â”€â”€ Sliding Segmented Tab Control â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            SlidingSegmentedControl(
                selectedMode = authMode,
                onModeChange = { authMode = it },
                primaryGreen = primaryGreen
            )

            Spacer(modifier = Modifier.height(24.dp))

            // â”€â”€ Username Field â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            OutlinedTextField(
                value = username,
                onValueChange = { newUsername ->
                    val sanitized = newUsername.filterNot { it.isWhitespace() }
                    username = sanitized
                    if (usernameError != null && usernameRegex.matches(sanitized)) {
                        usernameError = null
                    }
                },
                label = { Text("Enter your username") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Icon",
                        tint = primaryGreen
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                        submitAuth()
                    }
                ),
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryGreen,
                    focusedLabelColor = primaryGreen,
                    unfocusedBorderColor = Color(0xFFE0E0E0),
                    unfocusedContainerColor = Color(0xFFFAFAFA),
                    focusedContainerColor = Color.White
                ),
                modifier = Modifier.fillMaxWidth(),
                isError = usernameError != null,
                supportingText = {
                    if (usernameError != null) {
                        Text(usernameError!!)
                    }
                }
            )

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // â”€â”€ Google Auth Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Surface(
                onClick = {
                    keyboardController?.hide()
                    submitAuth()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = primaryGreen,
                shadowElevation = 4.dp,
                enabled = uiState !is AuthUiState.Loading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState is AuthUiState.Loading && activeAuthAction == ActiveAuthAction.GOOGLE) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) "Signing up..." else "Signing in...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_google),
                            contentDescription = "Google Icon",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) "Sign up with Google" else "Continue with Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // â”€â”€ Facebook Auth Button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Surface(
                onClick = {
                    val sanitized = username.filterNot { it.isWhitespace() }
                    username = sanitized
                    if (authMode == AuthMode.SIGN_UP) {
                        if (!usernameRegex.matches(sanitized)) {
                            usernameError = "Username must be 3–30 characters and can contain letters, numbers, and underscores."
                            return@Surface
                        }
                        usernameError = null
                    }
                    Log.d("AuthFlow", "Username validation passed")
                    activeAuthAction = ActiveAuthAction.FACEBOOK
                    Log.d("AuthMainRoute", "Sign in with Facebook clicked (Not Implemented)")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                border = ButtonDefaults.outlinedButtonBorder,
                enabled = uiState !is AuthUiState.Loading
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (uiState is AuthUiState.Loading && activeAuthAction == ActiveAuthAction.FACEBOOK) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = primaryGreen,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) "Signing up..." else "Signing in...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1E1E)
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_facebook),
                            contentDescription = "Facebook Icon",
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) "Sign up with Facebook" else "Continue with Facebook",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E1E1E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // â”€â”€ Toggle Sign In / Sign Up â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (authMode == AuthMode.SIGN_UP) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF8A8A8A)
                )
                Text(
                    text = if (authMode == AuthMode.SIGN_UP) "Sign in" else "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryGreen,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        authMode = if (authMode == AuthMode.SIGN_UP) AuthMode.SIGN_IN else AuthMode.SIGN_UP
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
//  SLIDING SEGMENTED CONTROL
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

@Composable
private fun SlidingSegmentedControl(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    primaryGreen: Color
) {
    val tabIndex = if (selectedMode == AuthMode.SIGN_IN) 0 else 1

    // Animate the indicator offset as a fraction: 0f = left, 1f = right
    val indicatorFraction by animateFloatAsState(
        targetValue = tabIndex.toFloat(),
        animationSpec = tween(320, easing = EaseOutQuart),
        label = "tabSlide"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF0F0F0))
            .padding(4.dp)
    ) {
        val containerWidth = maxWidth
        val tabWidth = (containerWidth - 4.dp) / 2  // half minus spacing

        // Sliding indicator
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (indicatorFraction * (tabWidth + 4.dp).toPx()).roundToInt(),
                        y = 0
                    )
                }
                .width(tabWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White)
        )

        // Tab labels
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Sign In
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(AuthMode.SIGN_IN) },
                contentAlignment = Alignment.Center
            ) {
                val textColor by animateColorAsState(
                    targetValue = if (selectedMode == AuthMode.SIGN_IN) primaryGreen else Color(0xFF999999),
                    animationSpec = tween(250),
                    label = "signInColor"
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedMode == AuthMode.SIGN_IN) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }

            // Create Account
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onModeChange(AuthMode.SIGN_UP) },
                contentAlignment = Alignment.Center
            ) {
                val textColor by animateColorAsState(
                    targetValue = if (selectedMode == AuthMode.SIGN_UP) primaryGreen else Color(0xFF999999),
                    animationSpec = tween(250),
                    label = "signUpColor"
                )
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selectedMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}


// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•
//  ABSTRACT ENERGY BACKGROUND â€” Clean, premium, modern
// â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•

private fun DrawScope.drawAbstractEnergyBackground(canvasSize: Size) {
    val w = canvasSize.width
    val h = canvasSize.height

    // â”€â”€ Base gradient â€” soft green atmospheric tone â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0.0f to Color(0xFFECF5ED),
                0.4f to Color(0xFFD5EBDA),
                0.75f to Color(0xFFBBDEC2),
                1.0f to Color(0xFFA2D3AB)
            ),
            startY = 0f,
            endY = h
        ),
        size = canvasSize
    )

    // â”€â”€ Subtle radial glow â€” warm center light â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.0f to Color(0x14FFFFFF),
                0.5f to Color(0x08FFFFFF),
                1.0f to Color.Transparent
            ),
            center = Offset(w * 0.5f, h * 0.35f),
            radius = w * 0.55f
        ),
        radius = w * 0.55f,
        center = Offset(w * 0.5f, h * 0.35f)
    )

    // â”€â”€ Flowing shape â€” gentle curve, lower-left â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val flowPath1 = Path().apply {
        moveTo(0f, h * 0.65f)
        cubicTo(w * 0.25f, h * 0.50f, w * 0.55f, h * 0.70f, w * 0.85f, h * 0.55f)
        cubicTo(w * 0.95f, h * 0.50f, w, h * 0.52f, w, h * 0.55f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        flowPath1,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF8FCC93).copy(alpha = 0.35f),
                Color(0xFF6ABF6E).copy(alpha = 0.25f)
            ),
            startY = h * 0.50f,
            endY = h
        )
    )

    // â”€â”€ Flowing shape â€” softer curve, overlapping â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    val flowPath2 = Path().apply {
        moveTo(0f, h * 0.78f)
        cubicTo(w * 0.30f, h * 0.65f, w * 0.60f, h * 0.80f, w, h * 0.70f)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(
        flowPath2,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF6ABF6E).copy(alpha = 0.20f),
                Color(0xFF4CAF50).copy(alpha = 0.15f)
            ),
            startY = h * 0.65f,
            endY = h
        )
    )
}


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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
    var authMode by rememberSaveable { mutableStateOf(AuthMode.SIGN_UP) }
    var activeAuthAction by rememberSaveable { mutableStateOf(ActiveAuthAction.NONE) }

    val submitAuth: () -> Unit = {
        if (uiState !is AuthUiState.Loading) {
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

                    Log.d("AuthFlowDiagnose", "AUTH STEP 1: Google credential request started")
                    try {
                        val result = kotlinx.coroutines.withTimeout(15000L) {
                            credentialManager.getCredential(
                                request = request,
                                context = activity
                            )
                        }
                        Log.d("AuthFlowDiagnose", "AUTH STEP 2: Google credential received")
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
                            Log.e("AuthMainRoute", "Unexpected credential type")
                            viewModel.setError("Unexpected credential type")
                        }
                    } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                        Log.e("AuthFlowDiagnose", "AUTH ERROR: TimeoutCancellationException (Credential Manager timed out)")
                        viewModel.setError("Sign in timed out. Please try again.")
                    } catch (e: GetCredentialException) {
                        Log.e("AuthFlowDiagnose", "AUTH ERROR: GetCredentialException")
                        Log.e("AuthFlowDiagnose", "AUTH ERROR TYPE: ${e::class.java.name}")
                        Log.e("AuthFlowDiagnose", "AUTH ERROR MESSAGE: ${e.message}")
                        Log.e(
                            "AuthMainRoute",
                            "GetCredentialException: type=${e::class.java.name}, message=${e.message}, errorMessage=${e.errorMessage}",
                            e
                        )
                        viewModel.setError(
                            e.errorMessage?.toString() ?: "Sign in cancelled"
                        )
                    } catch (e: Exception) {
                        Log.e("AuthFlowDiagnose", "AUTH ERROR: General Exception in Google Auth")
                        Log.e("AuthFlowDiagnose", "AUTH ERROR TYPE: ${e::class.java.name}")
                        Log.e("AuthFlowDiagnose", "AUTH ERROR MESSAGE: ${e.message}")
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

    LaunchedEffect(uiState) {
        if (uiState !is AuthUiState.Loading) {
            activeAuthAction = ActiveAuthAction.NONE
        }
        if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message == null) {
            viewModel.resetState()
            Log.d("AuthFlowDiagnose", "AUTH STEP 12: Navigating to Home")
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
        // -------------------------------------------------------------
        //  HERO SECTION - Clean abstract energy-tech background with bottom wave
        // -------------------------------------------------------------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawAbstractEnergyBackground(size)
            }

            // SolarEase logo and app name - primary focal point
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
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
                            color = Color(0xFF191C18),
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

            // Smooth wave transition into surface
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .align(Alignment.BottomCenter)
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
        }

        // -------------------------------------------------------------
        //  CONTENT SECTION - Welcome Text & Authentication Controls
        // -------------------------------------------------------------
        Column(
            modifier = Modifier
                .fillMaxWidth()
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
                        color = Color(0xFF191C18),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "SolarEase",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryGreen,
                        fontSize = 32.sp,
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
                    color = Color(0xFF747873),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sliding Segmented Tab Control
            SlidingSegmentedControl(
                selectedMode = authMode,
                onModeChange = { authMode = it },
                primaryGreen = primaryGreen
            )

            if (uiState is AuthUiState.Error) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Google Auth Button
            Surface(
                onClick = {
                    submitAuth()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                color = primaryGreen,
                shadowElevation = 2.dp,
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

            Spacer(modifier = Modifier.height(24.dp))

            // Toggle Sign In / Sign Up
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (authMode == AuthMode.SIGN_UP) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF747873)
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

// -------------------------------------------------------------
//  SLIDING SEGMENTED CONTROL
// -------------------------------------------------------------

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


// -------------------------------------------------------------
//  ABSTRACT ENERGY BACKGROUND â€” Clean, premium, modern
// -------------------------------------------------------------

private fun DrawScope.drawAbstractEnergyBackground(canvasSize: Size) {
    val w = canvasSize.width
    val h = canvasSize.height

    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
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

    // -------------------------------------------------------------
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


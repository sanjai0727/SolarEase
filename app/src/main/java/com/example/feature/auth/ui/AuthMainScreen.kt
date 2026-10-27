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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
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
                        val result = credentialManager.getCredential(
                            request = request,
                            context = activity
                        )
                        Log.d("AuthFlowDiagnose", "AUTH STEP 2: Google credential received")
                        val credential = result.credential

                        if (credential is androidx.credentials.CustomCredential &&
                            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            val googleIdTokenCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.signInWithGoogle(googleIdTokenCredential.idToken, null, authMode == AuthMode.SIGN_UP)
                        } else {
                            Log.e("AuthMainRoute", "Unexpected credential type")
                            viewModel.setError("Unexpected credential type")
                        }
                    } catch (e: GetCredentialCancellationException) {
                        Log.d("AuthFlowDiagnose", "AUTH CANCELLED: User dismissed Google Sign-In prompt")
                        viewModel.resetState()
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
                            e.errorMessage?.toString() ?: (e.message ?: "Google Sign-In failed")
                        )
                    } catch (e: CancellationException) {
                        throw e
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

    val primaryGreen = Color(0xFF386A20)
    val backgroundCream = Color(0xFFFBFCF3)
    val textPrimary = Color(0xFF191C18)
    val textSecondary = Color(0xFF747873)
    val outlineBorder = Color(0xFFDDE4D5)

    Scaffold(
        containerColor = backgroundCream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // -------------------------------------------------------------
            //  BRANDING - Compact Logo and Wordmark
            // -------------------------------------------------------------
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White)
                    .border(1.dp, outlineBorder, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = "SolarEase Logo",
                    tint = primaryGreen,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(
                        style = SpanStyle(
                            color = textPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Solar")
                    }
                    withStyle(
                        style = SpanStyle(
                            color = primaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
                        append("Ease")
                    }
                },
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 24.sp,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(36.dp))

            // -------------------------------------------------------------
            //  HEADINGS - Dynamic Animated Heading & Short Subtitle
            // -------------------------------------------------------------
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
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (mode == AuthMode.SIGN_IN) "Welcome back" else "Create your account",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Clean short supporting sentence
            Text(
                text = if (authMode == AuthMode.SIGN_IN)
                    "Sign in to track your solar energy and manage your usage."
                else
                    "Understand your energy and take control of your usage.",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp,
                color = textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // -------------------------------------------------------------
            //  SLIDING SEGMENTED CONTROL (Sign In / Create Account)
            // -------------------------------------------------------------
            SlidingSegmentedControl(
                selectedMode = authMode,
                onModeChange = { authMode = it },
                primaryGreen = primaryGreen
            )

            Spacer(modifier = Modifier.height(28.dp))

            // -------------------------------------------------------------
            //  ERROR STATE (If any)
            // -------------------------------------------------------------
            if (uiState is AuthUiState.Error) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = (uiState as AuthUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            }

            // -------------------------------------------------------------
            //  PRIMARY GOOGLE AUTH BUTTON
            // -------------------------------------------------------------
            Surface(
                onClick = { submitAuth() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
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
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (authMode == AuthMode.SIGN_UP) "Signing up..." else "Signing in...",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            fontSize = 15.sp
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
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // -------------------------------------------------------------
            //  SIMPLE ACCOUNT NAVIGATION LINK
            // -------------------------------------------------------------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (authMode == AuthMode.SIGN_UP) "Already have an account? " else "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    color = textSecondary
                )
                Text(
                    text = if (authMode == AuthMode.SIGN_UP) "Sign in" else "Sign up",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
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

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// -----------------------------------------------------------------
//  SLIDING SEGMENTED CONTROL (iOS-style Segmented Pill)
// -----------------------------------------------------------------

@Composable
private fun SlidingSegmentedControl(
    selectedMode: AuthMode,
    onModeChange: (AuthMode) -> Unit,
    primaryGreen: Color
) {
    val tabIndex = if (selectedMode == AuthMode.SIGN_IN) 0 else 1

    val indicatorFraction by animateFloatAsState(
        targetValue = tabIndex.toFloat(),
        animationSpec = tween(280, easing = EaseOutQuart),
        label = "tabSlide"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFEEF3EA))
            .border(1.dp, Color(0xFFDDE4D5), RoundedCornerShape(14.dp))
            .padding(4.dp)
    ) {
        val containerWidth = maxWidth
        val tabWidth = (containerWidth - 4.dp) / 2

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
                .clip(RoundedCornerShape(11.dp))
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
                    targetValue = if (selectedMode == AuthMode.SIGN_IN) primaryGreen else Color(0xFF747873),
                    animationSpec = tween(200),
                    label = "signInColor"
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
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
                    targetValue = if (selectedMode == AuthMode.SIGN_UP) primaryGreen else Color(0xFF747873),
                    animationSpec = tween(200),
                    label = "signUpColor"
                )
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = if (selectedMode == AuthMode.SIGN_UP) FontWeight.Bold else FontWeight.Medium,
                    color = textColor
                )
            }
        }
    }
}

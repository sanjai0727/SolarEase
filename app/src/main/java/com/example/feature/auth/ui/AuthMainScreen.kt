package com.example.feature.auth.ui

import android.app.Activity
import android.util.Log
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
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


enum class AuthMode {
    SIGN_UP, SIGN_IN
}

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
    var username by rememberSaveable { mutableStateOf("") }

    var usernameError by rememberSaveable { mutableStateOf<String?>(null) }
    
    val usernameRegex = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,30}$".toRegex()
    val isUsernameValid = usernameError == null

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message == null) {
            viewModel.resetState()
            Log.d("AuthFlow", "Navigating to Home")
            onNavigateToHome()
        }
    }

    val backgroundColor = Color(0xFFFDFBF7)
    val primaryGreen = MaterialTheme.colorScheme.primary

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        

        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(primaryGreen.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = primaryGreen,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (authMode == AuthMode.SIGN_UP) "Create Your Account" else "Welcome Back",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = if (authMode == AuthMode.SIGN_UP) "Join SolarEase and start your solar journey" else "Sign in to continue your solar journey",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = { newUsername ->
                username = newUsername
                if (usernameError != null && usernameRegex.matches(newUsername.trim())) {
                    usernameError = null
                }
            },
            label = { Text("Enter your username") },
            leadingIcon = {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Person,
                    contentDescription = "User Icon",
                    tint = primaryGreen
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryGreen,
                focusedLabelColor = primaryGreen
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
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            onClick = {
                
                if (authMode == AuthMode.SIGN_UP) {
                    val trimmed = username.trim()
                    if (!usernameRegex.matches(trimmed)) {
                        usernameError = "Username must be 3–30 characters and can contain letters, numbers, and underscores."
                        return@Surface
                    }
                    usernameError = null
                }
                Log.d("AuthFlow", "Username validation passed")
                Log.d("AuthFlow", "Google authentication started")
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
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken, username, authMode == AuthMode.SIGN_UP)
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
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100.dp),
            color = primaryGreen,
            shadowElevation = 2.dp,
            enabled = uiState !is AuthUiState.Loading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
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
                        modifier = Modifier.size(24.dp)
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

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            onClick = {
                
                if (authMode == AuthMode.SIGN_UP) {
                    val trimmed = username.trim()
                    if (!usernameRegex.matches(trimmed)) {
                        usernameError = "Username must be 3–30 characters and can contain letters, numbers, and underscores."
                        return@Surface
                    }
                    usernameError = null
                }
                Log.d("AuthFlow", "Username validation passed")
Log.d("AuthMainRoute", "Sign in with Facebook clicked (Not Implemented)")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(100.dp),
            color = Color.White,
            shadowElevation = 2.dp,
            enabled = uiState !is AuthUiState.Loading
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
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
                        modifier = Modifier.size(24.dp)
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

        Spacer(modifier = Modifier.height(48.dp))

        Spacer(modifier = Modifier.height(32.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (authMode == AuthMode.SIGN_UP) "Already have an account? " else "Don't have an account? ",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF666666)
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

        Spacer(modifier = Modifier.height(24.dp))
    }
}
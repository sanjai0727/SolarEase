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
import androidx.compose.material3.*
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

@Composable
fun AuthMainRoute(
    onNavigateToPhoneAuth: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var fullName by rememberSaveable { mutableStateOf("") }

    val isFullNameValid = fullName.isBlank() || (fullName.trim().length in 3..60 &&
        fullName.all { it.isLetter() || it.isWhitespace() || it == '.' })

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success && (uiState as AuthUiState.Success).message == null) {
            viewModel.resetState()
            onNavigateToProfile()
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
        StepProgress(currentStep = 1, totalSteps = 11)

        Spacer(modifier = Modifier.height(32.dp))

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
            text = "Welcome to SolarEase",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E1E1E),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Enter your details to start your solar journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFF666666),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryGreen,
                focusedLabelColor = primaryGreen
            ),
            modifier = Modifier.fillMaxWidth(),
            isError = fullName.isNotEmpty() && !isFullNameValid,
            supportingText = {
                if (fullName.isNotEmpty() && !isFullNameValid) {
                    Text("Enter a valid name")
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

        Text(
            text = "Continue with",
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Surface(
            onClick = {
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
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
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
                        color = primaryGreen
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.drawable.ic_google),
                        contentDescription = "Google Icon",
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Sign in with Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1E1E1E)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Surface(
            onClick = {
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
                Image(
                    painter = painterResource(id = R.drawable.ic_facebook),
                    contentDescription = "Facebook Icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Sign in with Facebook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1E1E1E)
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        val termsText = buildAnnotatedString {
            append("By continuing, you agree to our\n")
            withStyle(style = SpanStyle(color = primaryGreen, fontWeight = FontWeight.Medium)) {
                append("Terms of Service")
            }
            append(" and ")
            withStyle(style = SpanStyle(color = primaryGreen, fontWeight = FontWeight.Medium)) {
                append("Privacy Policy")
            }
            append(".")
        }

        Text(
            text = termsText,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF888888),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
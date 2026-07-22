package com.example.feature.auth.ui

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StepProgress

fun Context.findActivity(): android.app.Activity? = when (this) {
    is android.app.Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AuthPhoneRoute(
    onNavigateToOtp: (String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var phoneNumber by remember { mutableStateOf("") }
    var hasInteracted by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val activity = context.findActivity()

    val isValid = phoneNumber.length == 10 && phoneNumber.matches(Regex("^[6-9]\\d{9}$"))
    val localErrorMessage = when {
        phoneNumber.isEmpty() -> "Please enter your mobile number."
        !phoneNumber.matches(Regex("^[6-9].*")) -> "Please enter a valid Indian mobile number."
        phoneNumber.length < 10 -> "Mobile number must contain exactly 10 digits."
        else -> null
    }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onNavigateToOtp(phoneNumber)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        StepProgress(currentStep = 1, totalSteps = 10)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Welcome to SolarEase",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "Enter your phone number to start your solar journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue -> 
                hasInteracted = true
                val filtered = newValue.filter { it.isDigit() }
                if (filtered.length <= 10) {
                    phoneNumber = filtered
                }
            },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            isError = (hasInteracted && !isValid) || uiState is AuthUiState.Error,
            supportingText = {
                if (uiState is AuthUiState.Error) {
                    Text((uiState as AuthUiState.Error).message)
                } else if (hasInteracted && localErrorMessage != null) {
                    Text(localErrorMessage)
                }
            }
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = { 
                if (activity != null) {
                    viewModel.sendOtp(phoneNumber, activity) 
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 8.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
            enabled = uiState !is AuthUiState.Loading && isValid
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Send OTP", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

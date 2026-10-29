package com.example.feature.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.StepProgress

@Composable
fun AuthPhoneRoute(
    onNavigateToOtp: (String, String) -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var fullName by rememberSaveable { mutableStateOf("") }
    var phoneNumber by rememberSaveable { mutableStateOf("") }

    val isFullNameValid = fullName.isNotBlank() && 
        fullName.trim().length in 3..60 && 
        fullName.all { it.isLetter() || it.isWhitespace() || it == '.' }
    val isPhoneNumberValid = phoneNumber.length == 10 && phoneNumber.firstOrNull()?.let { it in listOf('6', '7', '8', '9') } == true

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            viewModel.resetState()
            onNavigateToOtp(fullName.trim(), phoneNumber)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        StepProgress(currentStep = 1, totalSteps = 11)
        
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
            text = "Enter your details to start your solar journey.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            isError = fullName.isNotEmpty() && !isFullNameValid,
            supportingText = {
                if (fullName.isNotEmpty() && !isFullNameValid) {
                    Text("Enter a valid name (3-60 characters, letters/spaces/periods only)")
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phoneNumber,
            onValueChange = { newValue -> 
                val filtered = newValue.filter { it.isDigit() }.take(10)
                phoneNumber = filtered 
            },
            label = { Text("Mobile Number *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth(),
            isError = uiState is AuthUiState.Error || (phoneNumber.isNotEmpty() && !isPhoneNumberValid),
            supportingText = {
                if (uiState is AuthUiState.Error) {
                    Text((uiState as AuthUiState.Error).message)
                } else if (phoneNumber.isNotEmpty() && !isPhoneNumberValid) {
                    Text("Enter a valid 10-digit mobile number.", color = MaterialTheme.colorScheme.error)
                }
            }
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        val context = androidx.compose.ui.platform.LocalContext.current
        Button(
            onClick = {
                val activity = context as? android.app.Activity
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
            enabled = uiState !is AuthUiState.Loading && isFullNameValid && isPhoneNumberValid
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text("Get OTP", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

package com.example.feature.home.ui.modules

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillStatusRoute(
    onNavigateBack: () -> Unit,
    viewModel: TnebBillStatusViewModel = viewModel()
) {
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val isChecking by viewModel.isChecking.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Electricity Bill Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFF5F7FB),
                    titleContentColor = Color(0xFF1E222D),
                    navigationIconContentColor = Color(0xFF1E222D)
                )
            )
        },
        containerColor = Color(0xFFF5F7FB)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ── Hero / Introduction ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE3F2FD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = Color(0xFF1976D2),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Check Your Bill Status",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1E222D)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Check your latest TNPDCL bill securely. We don't store your connection details.",
                fontSize = 15.sp,
                color = Color(0xFF7D8492),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Temporary Validation State (Phase 1) ──────────────────────
            if (isChecking) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFF1E7038),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Validating details...",
                            color = Color(0xFF1E7038),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Future phases will fetch the actual TNPDCL bill data.",
                            fontSize = 12.sp,
                            color = Color(0xFF2E7D32),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { viewModel.resetValidationState() }) {
                            Text("Cancel", color = Color(0xFF1E7038))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Input Fields ──────────────────────────────────────────────
            OutlinedTextField(
                value = serviceConnectionNumber,
                onValueChange = { viewModel.updateServiceConnectionNumber(it) },
                label = { Text("Service Connection Number") },
                placeholder = { Text("e.g. 04234567891") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E7038),
                    focusedLabelColor = Color(0xFF1E7038),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = registeredMobileNumber,
                onValueChange = { viewModel.updateRegisteredMobileNumber(it) },
                label = { Text("Registered Mobile Number") },
                placeholder = { Text("10-digit mobile number") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF1E7038),
                    focusedLabelColor = Color(0xFF1E7038),
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Important Disclaimer ──────────────────────────────────────
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFF8E1)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(20.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "The mobile number must exactly match the number registered with this service connection on TNPDCL.",
                        fontSize = 13.sp,
                        color = Color(0xFF5D4037),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Primary CTA ───────────────────────────────────────────────
            Button(
                onClick = { viewModel.checkBillStatus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isValid && !isChecking,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E7038),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFA5D6A7),
                    disabledContentColor = Color.White
                )
            ) {
                Text(
                    text = "Check Bill",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

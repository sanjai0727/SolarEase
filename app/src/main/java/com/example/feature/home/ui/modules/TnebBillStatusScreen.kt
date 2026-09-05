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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    val captchaImageBase64 by viewModel.captchaImageBase64.collectAsState()
    val captchaInput by viewModel.captchaInput.collectAsState()
    val pageLoadError by viewModel.pageLoadError.collectAsState()

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
                text = "Check your latest TNPDCL bill securely. Your details are used securely to retrieve your TNPDCL bill.",
                fontSize = 15.sp,
                color = Color(0xFF7D8492),
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(32.dp))

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

            val submitState by viewModel.submitState.collectAsState()

            // ── Temporary Validation State (Phase 1) ──────────────────────
            if (isChecking && captchaImageBase64 == null && !pageLoadError) {
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
                            text = "Loading TNPDCL Portal...",
                            color = Color(0xFF1E7038),
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Connecting securely to fetch the CAPTCHA.",
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

            // ── Error State (Page Load Error or General Error) ─────────────
            if (pageLoadError || submitState == SubmitState.ERROR) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Unable to connect to TNPDCL. Please try again.",
                                color = Color(0xFFB71C1C),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Invalid CAPTCHA State ──────────────────────────────────────
            if (submitState == SubmitState.INVALID_CAPTCHA) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Invalid CAPTCHA. Please refresh the CAPTCHA and try again.",
                            color = Color(0xFFB71C1C),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // ── Invalid Details State ──────────────────────────────────────
            if (submitState == SubmitState.INVALID_DETAILS) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "The details you entered do not match TNPDCL records.",
                            color = Color(0xFFB71C1C),
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // ── Success State ──────────────────────────────────────────────
            if (submitState == SubmitState.SUCCESS) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Bill details retrieved successfully.",
                            color = Color(0xFF1B5E20),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── CAPTCHA Display & Input ──────────────────────────────────
            if (captchaImageBase64 != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Enter the CAPTCHA shown below",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E222D)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            // Decode Base64 to Bitmap safely
                            val bitmap = remember(captchaImageBase64) {
                                try {
                                    val b64 = captchaImageBase64!!
                                    val imageBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                    android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                                } catch (e: Exception) {
                                    null
                                }
                            }
                            
                            if (bitmap != null) {
                                androidx.compose.foundation.Image(
                                    bitmap = bitmap,
                                    contentDescription = "CAPTCHA Image",
                                    modifier = Modifier
                                        .height(50.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                            } else {
                                Text("Failed to decode CAPTCHA", color = Color.Red)
                            }
                            
                            Spacer(modifier = Modifier.width(16.dp))
                            
                            IconButton(
                                onClick = { viewModel.requestCaptchaRefresh() },
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(Color(0xFFF5F7FB), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh CAPTCHA",
                                    tint = Color(0xFF1E7038)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        OutlinedTextField(
                            value = captchaInput,
                            onValueChange = { viewModel.updateCaptchaInput(it) },
                            placeholder = { Text("CAPTCHA Code") },
                            modifier = Modifier.fillMaxWidth(0.7f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFF1E7038),
                                focusedLabelColor = Color(0xFF1E7038),
                                unfocusedContainerColor = Color(0xFFF5F7FB),
                                focusedContainerColor = Color(0xFFF5F7FB)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Primary CTA ───────────────────────────────────────────────
            val isInitialCheckEnabled = isValid && !isChecking && captchaImageBase64 == null
            val isSubmitCaptchaEnabled = isValid && isChecking && captchaImageBase64 != null && captchaInput.isNotEmpty() && submitState != SubmitState.SUBMITTING
            val hasCaptcha = captchaImageBase64 != null || isChecking
            
            Button(
                onClick = { 
                    if (isInitialCheckEnabled || isSubmitCaptchaEnabled) {
                        viewModel.checkBillStatus() 
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = isInitialCheckEnabled || isSubmitCaptchaEnabled,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1E7038),
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFA5D6A7),
                    disabledContentColor = Color.White
                )
            ) {
                if (submitState == SubmitState.SUBMITTING) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (hasCaptcha) "Check Bill Status" else "Get CAPTCHA",
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
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Hidden WebView for TNPDCL Integration (Phase 2) ───────────
            if (isChecking) {
                TnebBillStatusWebView(
                    viewModel = viewModel,
                    modifier = Modifier.size(1.dp)
                )
            }
        }
    }
}

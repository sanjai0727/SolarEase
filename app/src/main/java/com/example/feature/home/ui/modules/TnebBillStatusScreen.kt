package com.example.feature.home.ui.modules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.ebservice.util.formatConsumerNumber

// Official SolarEase Design Palette
private val SolarEaseEmerald = Color(0xFF386A20)
private val SolarEaseEmeraldDark = Color(0xFF2D5518)
private val SolarEaseEmeraldContainer = Color(0xFFE5F2D9)
private val SoftBackground = Color(0xFFFBFCF3)
private val InfoBlueContainer = Color(0xFFEAF3FF)
private val InfoBlueText = Color(0xFF18324B)
private val CardBorderColor = Color(0xFFDDE4D5)
private val TextDark = Color(0xFF191C18)
private val TextMuted = Color(0xFF747873)
private val ErrorContainer = Color(0xFFFFDAD6)
private val ErrorText = Color(0xFFBA1A1A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillStatusRoute(
    onNavigateBack: () -> Unit,
    onNavigateToSolarLens: () -> Unit = {},
    onReturnToHome: () -> Unit = onNavigateBack,
    viewModel: TnebBillStatusViewModel = viewModel()
) {
    val submitState by viewModel.submitState.collectAsState()
    val isChecking by viewModel.isChecking.collectAsState()
    val captchaImageBase64 by viewModel.captchaImageBase64.collectAsState()
    val pageLoadError by viewModel.pageLoadError.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            submitState == SubmitState.SUCCESS -> {
                BackHandler(enabled = true) {
                    viewModel.resetValidationState()
                    onReturnToHome()
                }
                TnebBillDashboard(
                    onNavigateBack = {
                        viewModel.resetValidationState()
                        onReturnToHome()
                    },
                    viewModel = viewModel,
                    onNavigateToSolarLens = onNavigateToSolarLens,
                    onCheckBillAgain = {
                        viewModel.resetValidationState()
                    },
                    onReturnToHome = {
                        viewModel.resetValidationState()
                        onReturnToHome()
                    }
                )
            }
            submitState == SubmitState.SUBMITTING -> {
                BackHandler(enabled = true) {
                    viewModel.resetValidationState()
                }
                TnebBillLoadingScreen(
                    viewModel = viewModel,
                    onCancel = { viewModel.resetValidationState() }
                )
            }
            submitState == SubmitState.ERROR || submitState == SubmitState.PARSING_ERROR || submitState == SubmitState.INVALID_DETAILS || pageLoadError -> {
                BackHandler(enabled = true) {
                    viewModel.resetValidationState()
                }
                TnebBillErrorScreen(
                    viewModel = viewModel,
                    onRetry = { viewModel.checkBillStatus() },
                    onEditDetails = { viewModel.resetValidationState() },
                    onNavigateBack = { viewModel.resetValidationState() }
                )
            }
            captchaImageBase64 != null -> {
                BackHandler(enabled = true) {
                    viewModel.resetValidationState()
                }
                // Screen 2: CAPTCHA Verification
                TnebCaptchaVerificationScreen(
                    viewModel = viewModel,
                    onNavigateBack = { viewModel.resetValidationState() }
                )
            }
            else -> {
                // Screen 1: Electricity Bill Status Entry
                TnebBillStatusEntryScreen(
                    viewModel = viewModel,
                    onNavigateBack = onNavigateBack
                )
            }
        }

        // Hidden WebView kept alive for background scraping & session sync
        if (isChecking) {
            TnebBillStatusWebView(
                viewModel = viewModel,
                modifier = Modifier.size(1.dp)
            )
        }
    }
}

// ==============================================================================
// SCREEN 1: ELECTRICITY BILL STATUS ENTRY (Stitch 50860a8bc4474202a5ad720a7e287184)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillStatusEntryScreen(
    viewModel: TnebBillStatusViewModel,
    onNavigateBack: () -> Unit
) {
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val isValid by viewModel.isValid.collectAsState()
    val isChecking by viewModel.isChecking.collectAsState()
    var showHelpDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Electricity Bill Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(Icons.Default.HelpOutline, contentDescription = "Help")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftBackground,
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark,
                    actionIconContentColor = TextDark
                )
            )
        },
        containerColor = SoftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(InfoBlueContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = InfoBlueText,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Check Your Bill Status",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Instant TNPDCL utility ledger synchronization",
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Input Container Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Field 1: Service Connection Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Service Connection Number",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Text(
                            text = if (serviceConnectionNumber.length in 11..12) "${serviceConnectionNumber.length} Digits" else "${serviceConnectionNumber.length}/12 Digits",
                            fontSize = 11.sp,
                            color = if (serviceConnectionNumber.length in 11..12) SolarEaseEmerald else TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = serviceConnectionNumber,
                        onValueChange = { viewModel.updateServiceConnectionNumber(it) },
                        placeholder = { Text("e.g. 042345678901", color = Color(0xFFA0AEC0)) },
                        leadingIcon = {
                            Icon(Icons.Default.Bolt, contentDescription = null, tint = SolarEaseEmerald)
                        },
                        trailingIcon = {
                            if (serviceConnectionNumber.length in 11..12) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = SolarEaseEmerald)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolarEaseEmerald,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color(0xFFFAFCFF),
                            unfocusedContainerColor = Color(0xFFFAFCFF)
                        )
                    )

                    Text(
                        text = "Found on your physical electricity bill or meter card",
                        fontSize = 12.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Field 2: Registered Mobile Number
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Registered Mobile Number",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Text(
                            text = "${registeredMobileNumber.length}/10 Digits",
                            fontSize = 11.sp,
                            color = if (registeredMobileNumber.length == 10) SolarEaseEmerald else TextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = registeredMobileNumber,
                        onValueChange = { viewModel.updateRegisteredMobileNumber(it) },
                        placeholder = { Text("10-digit mobile number", color = Color(0xFFA0AEC0)) },
                        leadingIcon = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                            ) {
                                Text(
                                    text = "+91",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(18.dp)
                                        .background(CardBorderColor)
                                )
                            }
                        },
                        trailingIcon = {
                            if (registeredMobileNumber.length == 10) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Valid", tint = SolarEaseEmerald)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolarEaseEmerald,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color(0xFFFAFCFF),
                            unfocusedContainerColor = Color(0xFFFAFCFF)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reassurance Callout
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = InfoBlueContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = InfoBlueText,
                        modifier = Modifier
                            .size(20.dp)
                            .padding(top = 1.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "The mobile number must match the phone registered with your TNPDCL service connection to retrieve your billing records.",
                        fontSize = 13.sp,
                        color = Color(0xFF0F3A66),
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // How to find link
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showHelpDialog = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = SolarEaseEmerald,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Don't know your Consumer Number? How to find it",
                    color = SolarEaseEmerald,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary CTA Button
            Button(
                onClick = { viewModel.checkBillStatus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = isValid && !isChecking,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarEaseEmerald,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFA5D6A7),
                    disabledContentColor = Color.White
                )
            ) {
                if (isChecking) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Loading TNPDCL Portal...", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                } else {
                    Text("Get Security CAPTCHA", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Trust & Security Footer
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF718096),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Official TNPDCL Consumer Portal Integration • 256-bit Encrypted",
                    fontSize = 11.sp,
                    color = Color(0xFF718096)
                )
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = { Text("Find Your Consumer Number", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        "Your 11 to 12-digit Service Connection Number is composed of:\n\n" +
                                "• Digits 1–2: Region Code\n" +
                                "• Digits 3–5: Division Code\n" +
                                "• Digits 6–8: Subdivision Code\n" +
                                "• Remaining digits: Consumer Number\n\n" +
                                "Example: 042345678901 or 09440233135 (without spaces).\n" +
                                "It is printed at the top of any physical white meter card or previous receipt.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Got It", color = SolarEaseEmerald, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// ==============================================================================
// SCREEN 2: CAPTCHA VERIFICATION (Stitch ac97834a9f094760890c6d9ecceb77d2)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebCaptchaVerificationScreen(
    viewModel: TnebBillStatusViewModel,
    onNavigateBack: () -> Unit
) {
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val captchaImageBase64 by viewModel.captchaImageBase64.collectAsState()
    val captchaInput by viewModel.captchaInput.collectAsState()
    val submitState by viewModel.submitState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Verification", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Cancel", color = SolarEaseEmerald, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftBackground,
                    titleContentColor = TextDark,
                    navigationIconContentColor = TextDark
                )
            )
        },
        containerColor = SoftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Step Progress Chip
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE2E8F0))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(SolarEaseEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Step 2 of 2 • TNPDCL Portal Sync",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Verified Consumer Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SolarEaseEmeraldContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = SolarEaseEmerald)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Service: ${formatConsumerNumber(serviceConnectionNumber)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextDark
                            )
                            Text(
                                text = "Mobile: +91 ${registeredMobileNumber.take(5)} ${registeredMobileNumber.takeLast(5)}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    TextButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp), tint = SolarEaseEmerald)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = SolarEaseEmerald, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Invalid CAPTCHA feedback alert if applicable
            if (submitState == SubmitState.INVALID_CAPTCHA) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = ErrorContainer),
                    border = BorderStroke(1.dp, Color(0xFFFFCDD2))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = ErrorText)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Invalid CAPTCHA code entered. Please tap reload to get a new code and try again.",
                            color = ErrorText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 18.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Main CAPTCHA Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Enter Security Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Type the characters shown in the security image below.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // CAPTCHA Preview & Reload Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val bitmap = remember(captchaImageBase64) {
                            try {
                                val b64 = captchaImageBase64 ?: ""
                                val imageBytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                                android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)?.asImageBitmap()
                            } catch (e: Exception) {
                                null
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(200.dp)
                                .height(60.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F0FE))
                                .border(1.dp, Color(0xFFC5D9F8), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bitmap != null) {
                                Image(
                                    bitmap = bitmap,
                                    contentDescription = "CAPTCHA Code",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                                )
                            } else {
                                CircularProgressIndicator(color = SolarEaseEmerald, modifier = Modifier.size(24.dp))
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        IconButton(
                            onClick = { viewModel.requestCaptchaRefresh() },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF1F5F9))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reload CAPTCHA",
                                tint = SolarEaseEmerald,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // CAPTCHA Input Field
                    OutlinedTextField(
                        value = captchaInput,
                        onValueChange = { viewModel.updateCaptchaInput(it.uppercase()) },
                        placeholder = { Text("CAPTCHA Code", color = Color(0xFFA0AEC0)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 2.sp
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SolarEaseEmerald,
                            unfocusedBorderColor = CardBorderColor,
                            focusedContainerColor = Color(0xFFFAFCFF),
                            unfocusedContainerColor = Color(0xFFFAFCFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Code is case-sensitive • Numbers & Letters",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Primary Check Bill Status Button
            Button(
                onClick = { viewModel.checkBillStatus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = captchaInput.isNotBlank() && submitState != SubmitState.SUBMITTING,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SolarEaseEmerald,
                    contentColor = Color.White,
                    disabledContainerColor = Color(0xFFA5D6A7),
                    disabledContentColor = Color.White
                )
            ) {
                Text("Check Bill Status", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ==============================================================================
// STATE SCREEN 1: RETRIEVING BILL DETAILS (Stitch 3a1c2d1a3d7b42149e113ccf8bde739b)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillLoadingScreen(
    viewModel: TnebBillStatusViewModel,
    onCancel: () -> Unit
) {
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Retrieving Bill Details", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftBackground,
                    titleContentColor = TextDark
                )
            )
        },
        containerColor = SoftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Consumer pill
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = InfoBlueContainer,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = InfoBlueText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Service: ${serviceConnectionNumber.take(4)} • TNPDCL Sync",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = InfoBlueText
                    )
                }
            }

            // Main Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SolarEaseEmeraldContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = SolarEaseEmerald,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(60.dp)
                        )
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(28.dp))
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Connecting to TNPDCL Portal...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Securely synchronizing your latest utility billing ledger and tariff slab.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // 3-step checklist
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFAFCFF), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SolarEaseEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Security verification validated", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextDark)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = SolarEaseEmerald, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Fetching bi-monthly ledger...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = SolarEaseEmerald)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(18.dp).clip(CircleShape).background(Color(0xFFE2E8F0)))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Computing Solar Rooftop savings", fontSize = 13.sp, color = TextMuted)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "This typically takes 3 to 5 seconds depending on official utility server response.",
                        fontSize = 12.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = onCancel) {
                Text("Cancel & Return", color = SolarEaseEmerald, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ==============================================================================
// STATE SCREEN 2: PORTAL ERROR & RETRY (Stitch 4b816b118d57435a928fd69aaf83deb3)
// ==============================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TnebBillErrorScreen(
    viewModel: TnebBillStatusViewModel,
    onRetry: () -> Unit,
    onEditDetails: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val serviceConnectionNumber by viewModel.serviceConnectionNumber.collectAsState()
    val registeredMobileNumber by viewModel.registeredMobileNumber.collectAsState()
    val submitState by viewModel.submitState.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val titleText = when (submitState) {
        SubmitState.INVALID_DETAILS -> "Invalid Consumer Details"
        SubmitState.PARSING_ERROR -> "Bill Processing Error"
        else -> "Unable to Retrieve Bill"
    }

    val descriptionText = when (submitState) {
        SubmitState.INVALID_DETAILS -> errorMessage ?: "The Service Connection Number and Registered Mobile Number combination was not found in TNEB records."
        SubmitState.PARSING_ERROR -> errorMessage ?: "The bill status page was reached, but the bill details could not be parsed. The portal layout may have changed."
        else -> errorMessage ?: "The official TNPDCL utility portal is temporarily unreachable or undergoing maintenance. Your entered credentials have been preserved."
    }

    val errorIcon = when (submitState) {
        SubmitState.INVALID_DETAILS -> Icons.Default.Warning
        SubmitState.PARSING_ERROR -> Icons.Default.ErrorOutline
        else -> Icons.Default.CloudOff
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bill Status", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SoftBackground,
                    titleContentColor = TextDark
                )
            )
        },
        containerColor = SoftBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, CardBorderColor)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(ErrorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(errorIcon, contentDescription = null, tint = ErrorText, modifier = Modifier.size(32.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = descriptionText,
                        fontSize = 13.sp,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Diagnostic & credentials context
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, CardBorderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Service No: ${serviceConnectionNumber.ifBlank { "Not available" }}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextDark
                            )
                            Text(
                                text = "Mobile: ${if (registeredMobileNumber.isNotBlank()) "+91 $registeredMobileNumber" else "Not available"}",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onRetry,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SolarEaseEmerald)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Retrieval", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onEditDetails,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, SolarEaseEmerald),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SolarEaseEmerald)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Consumer Details", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Support help link
            Text(
                text = "Need help? Contact TNEB Minnagam 1912 Helpline",
                fontSize = 12.sp,
                color = SolarEaseEmerald,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

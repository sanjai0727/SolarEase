package com.example.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ElectricMeter
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Edit
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.DailyConsumptionGraph
import com.example.feature.home.ui.modules.MeterReadingGraph
import com.example.feature.home.ui.modules.MeterReadingProgressionGraph
import androidx.compose.material.icons.filled.History
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.theme.*
import java.util.Locale

// ─── Stitch Material 3 Home Tokens ──────────────────────────────────────────────
private object HomeColors {
    val Background = Color(0xFFFBFCF3)
    val Primary = Color(0xFF386A20)
    val PrimaryContainer = Color(0xFFE8F5E1)
    val TextPrimary = Color(0xFF191C18)
    val TextSecondary = Color(0xFF5F665B)
    val Border = Color(0xFFE1E5DA)
    val CardBg = Color(0xFFFFFFFF)
    val SurfaceSecondary = Color(0xFFF1F5E9)
    val AccentOrange = Color(0xFFB85C00)
    val Success = Color(0xFF217346)
    val ChartHighlight = Color(0xFFF4B183)
    val LightBlue = Color(0xFFEEF4FF)
}

// ─── Main Home Screen Entry Route ──────────────────────────────────────────────

@Composable
fun HomeRoute(
    onNavigateToBillCalculator: () -> Unit,
    onNavigateToElectricityChatbot: () -> Unit,
    onNavigateToSolarEaseLens: () -> Unit,
    onNavigateToTnebBillStatus: () -> Unit,
    onNavigateToBillHistory: () -> Unit = {},
    onNavigateToSolarPanel: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToRecommends: () -> Unit = {},
    onNavigateToActivity: () -> Unit = {},
    onNavigateToSolar: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = UserSession.currentUser

    var showAccountPreview by remember { mutableStateOf(false) }
    var showProfilePrompt by remember { mutableStateOf(false) }

    LaunchedEffect(currentUser) {
        if (currentUser != null && !UserSession.isProfilePromptDismissed(context, currentUser.id)) {
            if (currentUser.houseAddress.isNullOrBlank() || currentUser.email.isNullOrBlank()) {
                showProfilePrompt = true
            }
        }
    }

    if (showProfilePrompt) {
        AlertDialog(
            onDismissRequest = {
                showProfilePrompt = false
                currentUser?.id?.let { UserSession.setProfilePromptDismissed(context, it) }
            },
            title = { Text("Complete Your Profile", fontWeight = FontWeight.SemiBold, color = StitchOnSurface) },
            text = { Text("Add your email and installation address to calculate precise local solar insolation.", color = StitchOnSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    showProfilePrompt = false
                    currentUser?.id?.let { UserSession.setProfilePromptDismissed(context, it) }
                    onNavigateToProfile()
                }) {
                    Text("Complete Now", color = StitchPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showProfilePrompt = false
                    currentUser?.id?.let { UserSession.setProfilePromptDismissed(context, it) }
                }) {
                    Text("Later", color = StitchOnSurfaceVariant)
                }
            }
        )
    }

    if (showAccountPreview) {
        AlertDialog(
            onDismissRequest = { showAccountPreview = false },
            title = { Text("Account Details", fontWeight = FontWeight.SemiBold, color = StitchOnSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Name: ${currentUser?.name ?: currentUser?.username ?: "Guest"}", fontWeight = FontWeight.Medium, color = StitchOnSurface)
                    if (!currentUser?.email.isNullOrBlank()) {
                        Text(text = "Email: ${currentUser?.email}", color = StitchOnSurfaceVariant, fontSize = 13.sp)
                    }
                    if (!currentUser?.houseAddress.isNullOrBlank()) {
                        Text(text = "Address: ${currentUser?.houseAddress}", color = StitchOnSurfaceVariant, fontSize = 13.sp)
                    }
                    if (!currentUser?.ebServiceNumber.isNullOrBlank()) {
                        Text(text = "EB Service Number: ${currentUser?.ebServiceNumber}", color = StitchPrimaryContainer, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccountPreview = false
                    onNavigateToProfile()
                }) {
                    Text("Manage Profile", color = StitchPrimaryContainer, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountPreview = false }) {
                    Text("Close", color = StitchOnSurfaceVariant)
                }
            }
        )
    }

    Scaffold(
        containerColor = HomeColors.Background,
        bottomBar = {
            SolarEaseBottomNavigation(
                currentTab = SolarEaseNavTab.HOME,
                onNavigateToHome = {},
                onNavigateToRecommends = onNavigateToRecommends,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── 1. Top Bar / App Branding ───────────────────────────
            StitchTopBar(
                userLocation = uiState.userAddress,
                userInitial = uiState.userInitial,
                onProfileClick = { showAccountPreview = true },
                onNotificationsClick = {}
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── 2. Greeting & Live System Synchronization Status ────
                GreetingHeader(
                    greeting = uiState.greeting,
                    userName = uiState.userName,
                    hasConnectedBill = uiState.hasConnectedBill
                )

                // ── 2. Primary Energy Status Card (Home Energy) ─────────
                PrimaryEnergyCard(
                    uiState = uiState,
                    onConnectBill = onNavigateToTnebBillStatus,
                    onViewBillHistory = onNavigateToBillHistory,
                    onMeterInputChanged = { viewModel.onMeterInputChanged(it) },
                    onSaveMeterReading = { viewModel.saveCurrentMeterReading() },
                    onUpdateTodayReading = { viewModel.updateTodayMeterReading() },
                    onStartEditingReading = { viewModel.startEditingReading() },
                    onCancelEditingReading = { viewModel.cancelEditingReading() },
                    onEditCurrentReading = { viewModel.initiateEditCurrentReading() }
                )

                // ── 3. Meter Reading Progression Graph ────────────────
                MeterReadingProgressionGraph(
                    points = uiState.meterGraphPoints,
                    billData = uiState.billData,
                    onEditReading = { viewModel.onInitiateEditReading(it) }
                )

                // ── 4. REAP Energy Profile / Household Classification ───
                ReapClassificationCard(
                    uiState = uiState,
                    onLearnMore = onNavigateToTnebBillStatus
                )

                // ── 5. KNOW YOUR CONSUMPTION (Solar Lens Promotion) ─────
                KnowYourConsumptionCard(
                    uiState = uiState,
                    onOpenSolarLens = onNavigateToSolarEaseLens
                )

                // ── 6. Bill History / Check Dues ────────────────────────
                BillActionsSection(
                    onViewBillHistory = onNavigateToBillHistory,
                    onCheckDues = onNavigateToTnebBillStatus
                )

                // ── 7. Explore Tools (Bill Status, Chatbot, Solar Lens, Calculator)
                QuickToolsSection(
                    onBillStatus = onNavigateToTnebBillStatus,
                    onChatbot = onNavigateToElectricityChatbot,
                    onSolarLens = onNavigateToSolarEaseLens,
                    onCalculator = onNavigateToBillCalculator,
                    hasConnectedBill = uiState.hasConnectedBill,
                    tariffCode = uiState.tariffCode
                )

                // ── 8. Recent Household Activity / Event Log ────────────
                RecentActivitySection(
                    activities = uiState.recentActivities
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        // ── Dialog 1: Reading Lower Than Previous ──────────────────────
        val lowerDialog = uiState.lowerThanPreviousDialog
        if (lowerDialog != null) {
            val prevFormatted = String.format(Locale.getDefault(), "%,.0f", lowerDialog.previousReading.readingValue)
            AlertDialog(
                onDismissRequest = { viewModel.dismissLowerThanPreviousDialog() },
                title = {
                    Text(
                        text = "Reading Lower Than Previous",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                },
                text = {
                    Text(
                        text = "The reading you entered is lower than your last recorded reading ($prevFormatted kWh). If the previous reading was entered incorrectly, you can correct it.",
                        fontSize = 13.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 18.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onConfirmCorrectPreviousReading() },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Correct Previous Reading", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissLowerThanPreviousDialog() }
                    ) {
                        Text("Cancel", fontSize = 13.sp, color = StitchOnSurfaceVariant)
                    }
                },
                containerColor = StitchSurfaceContainerLowest,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // ── Dialog 2: Edit Previous Reading Confirmation ───────────────
        val confirmData = uiState.historicalEditConfirmDialog
        if (confirmData != null) {
            val dateLabel = confirmData.reading.displayDate.ifBlank { confirmData.reading.readingDate }
            val reasons = listOf("Entered by mistake", "Reading was recorded incorrectly", "Other")

            AlertDialog(
                onDismissRequest = { viewModel.dismissHistoricalEditConfirmDialog() },
                title = {
                    Text(
                        text = "Edit Previous Reading?",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "You're changing the meter reading recorded on $dateLabel. This may affect your consumption, estimated bill, and analytics.",
                            fontSize = 13.sp,
                            color = StitchOnSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        Text(
                            text = "Reason for correction",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            reasons.forEach { reason ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable { viewModel.onHistoricalReasonSelected(reason) }
                                        .padding(vertical = 2.dp, horizontal = 4.dp)
                                ) {
                                    RadioButton(
                                        selected = confirmData.selectedReason == reason,
                                        onClick = { viewModel.onHistoricalReasonSelected(reason) },
                                        colors = RadioButtonDefaults.colors(selectedColor = StitchPrimaryContainer)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = reason,
                                        fontSize = 13.sp,
                                        color = StitchOnSurface
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.onContinueHistoricalEdit() },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Continue", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissHistoricalEditConfirmDialog() }
                    ) {
                        Text("Cancel", fontSize = 13.sp, color = StitchOnSurfaceVariant)
                    }
                },
                containerColor = StitchSurfaceContainerLowest,
                shape = RoundedCornerShape(16.dp)
            )
        }

        // ── Dialog 3: Correct Meter Reading Entry ──────────────────────
        val inputData = uiState.historicalCorrectionInputDialog
        if (inputData != null) {
            val dateLabel = inputData.reading.displayDate.ifBlank { inputData.reading.readingDate }
            val lowerFmt = String.format(Locale.getDefault(), "%,.0f", inputData.lowerBound)
            val upperFmt = if (inputData.upperBound != null) String.format(Locale.getDefault(), "%,.0f", inputData.upperBound) else null

            val rangeHint = if (upperFmt != null) {
                "Allowed range: $lowerFmt – $upperFmt kWh"
            } else {
                "Allowed range: ≥ $lowerFmt kWh"
            }

            AlertDialog(
                onDismissRequest = { viewModel.dismissHistoricalCorrectionInputDialog() },
                title = {
                    Text(
                        text = "Correct Meter Reading",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "$dateLabel · Recorded: ${String.format(Locale.getDefault(), "%,.0f", inputData.reading.readingValue)} kWh",
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant
                        )

                        Text(
                            text = rangeHint,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchPrimaryContainer
                        )

                        OutlinedTextField(
                            value = inputData.inputText,
                            onValueChange = { viewModel.onHistoricalCorrectionInputChanged(it) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                Text(
                                    text = "kWh",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = StitchOnSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            isError = inputData.validationError != null,
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (inputData.validationError != null) {
                            Text(
                                text = inputData.validationError,
                                fontSize = 11.sp,
                                color = StitchError
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { viewModel.saveHistoricalCorrection() },
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { viewModel.dismissHistoricalCorrectionInputDialog() }
                    ) {
                        Text("Cancel", fontSize = 13.sp, color = StitchOnSurfaceVariant)
                    }
                },
                containerColor = StitchSurfaceContainerLowest,
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

// ─── 1. Top Bar ────────────────────────────────────────────────────────────────

@Composable
private fun StitchTopBar(
    userLocation: String?,
    userInitial: Char,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = HomeColors.Background.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand & Location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                // SolarEase Icon Container
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HomeColors.Primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = "SolarEase",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SolarEase",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.TextPrimary,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(HomeColors.PrimaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HOME",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.Primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    if (!userLocation.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = HomeColors.Primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = userLocation,
                                fontSize = 11.sp,
                                color = HomeColors.TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "REAP Solar Intelligence",
                            fontSize = 11.sp,
                            color = HomeColors.TextSecondary,
                            letterSpacing = 0.2.sp
                        )
                    }
                }
            }

            // Notification & Profile Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = HomeColors.TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(HomeColors.AccentOrange)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(HomeColors.Primary)
                        .border(1.dp, HomeColors.Border, CircleShape)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── 2. Greeting Header ────────────────────────────────────────────────────────

@Composable
private fun GreetingHeader(
    greeting: String,
    userName: String?,
    hasConnectedBill: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(HomeColors.Success)
                            .alpha(alphaAnim)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasConnectedBill) "SITE OVERVIEW" else "REAP HOUSEHOLD ONBOARDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.Success,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!userName.isNullOrBlank()) "$greeting, $userName" else "$greeting 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeColors.TextPrimary,
                    letterSpacing = (-0.5).sp
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(HomeColors.SurfaceSecondary)
                    .border(1.dp, HomeColors.Border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = HomeColors.Primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = if (hasConnectedBill) {
                "Your rooftop telemetry and electricity baseline are synchronized."
            } else {
                "Let's understand your home's energy footprint and build your path to clean independence."
            },
            fontSize = 13.sp,
            color = HomeColors.TextSecondary,
            lineHeight = 18.sp
        )
    }
}

// ─── 3. Primary Energy Card ────────────────────────────────────────────────────

@Composable
private fun PrimaryEnergyCard(
    uiState: HomeUiState,
    onConnectBill: () -> Unit,
    onViewBillHistory: () -> Unit,
    onMeterInputChanged: (String) -> Unit,
    onSaveMeterReading: () -> Unit,
    onUpdateTodayReading: () -> Unit,
    onStartEditingReading: () -> Unit,
    onCancelEditingReading: () -> Unit,
    onEditCurrentReading: () -> Unit = {}
) {
    var isDrawerExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        if (!uiState.hasConnectedBill) {
            // ==================================================================
            // STATE A: NEW USER / BILL NOT CONNECTED
            // ==================================================================
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(HomeColors.SurfaceSecondary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricMeter,
                                contentDescription = null,
                                tint = HomeColors.Primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Your Home Energy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeColors.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(HomeColors.SurfaceSecondary)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Standby",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeColors.TextSecondary
                        )
                    }
                }

                // Illustrated zero-state inner slot
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = HomeColors.SurfaceSecondary
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(HomeColors.CardBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricMeter,
                                contentDescription = null,
                                tint = HomeColors.Primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No electricity data connected yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect your consumer service number to reveal your bi-monthly tariff slabs, actual consumption, and peak load profile.",
                            fontSize = 12.sp,
                            color = HomeColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 17.sp
                        )
                    }
                }

                Button(
                    onClick = onConnectBill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeColors.Primary,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Connect TNEB Bill",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        } else if (!uiState.hasCurrentReading) {
            // ==================================================================
            // STATE B: BILL CONNECTED, FIRST CURRENT READING NOT ENTERED
            // ==================================================================
            val baseline = uiState.meterBaseline
            val baselineKwh = baseline?.baselineReading ?: 0.0
            val baselineDate = baseline?.baselineDate?.ifBlank { "01 Sep 2026" } ?: "01 Sep 2026"
            val prevBilledUnits = baseline?.previousBilledUnits ?: uiState.unitsConsumed

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header with Bill Connected Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR HOME ENERGY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.TextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(HomeColors.PrimaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = HomeColors.Success,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Bill Connected ✓",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.Success,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Baseline Starting Point Info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = HomeColors.SurfaceSecondary,
                    border = BorderStroke(1.dp, HomeColors.Border)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "LAST BILL READING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (baselineKwh > 0.0) "${String.format(Locale.getDefault(), "%,.0f", baselineKwh)} kWh" else "Baseline Pending",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.TextPrimary
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "LAST BILL DATE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.TextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = baselineDate,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = HomeColors.TextPrimary
                                )
                            }
                        }

                        if (prevBilledUnits != null && prevBilledUnits > 0.0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = HomeColors.Border)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Previous billed units: ${String.format(Locale.getDefault(), "%,.0f", prevBilledUnits)} kWh (Historical baseline)",
                                fontSize = 11.sp,
                                color = HomeColors.TextSecondary
                            )
                        }
                    }
                }

                // Prompt to enter today's meter reading
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Enter today's meter reading",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeColors.TextPrimary
                    )

                    OutlinedTextField(
                        value = uiState.meterInputText,
                        onValueChange = onMeterInputChanged,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = if (baselineKwh > 0.0) "e.g. ${String.format(Locale.getDefault(), "%.0f", baselineKwh + 132)}" else "e.g. 12582",
                                color = HomeColors.TextSecondary.copy(alpha = 0.6f)
                            )
                        },
                        trailingIcon = {
                            Text(
                                text = "kWh",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeColors.TextSecondary,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = uiState.meterValidationError != null,
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = HomeColors.Primary,
                            unfocusedBorderColor = HomeColors.Border
                        )
                    )

                    if (uiState.meterValidationError != null) {
                        Text(
                            text = uiState.meterValidationError,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }

                    Button(
                        onClick = onSaveMeterReading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomeColors.Primary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Save Today's Reading",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Two Separate Actions: View Bill History & Check Dues
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewBillHistory,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, HomeColors.Border)
                    ) {
                        Text(
                            text = "View Bill History",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeColors.TextPrimary
                        )
                    }

                    Button(
                        onClick = onConnectBill,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomeColors.AccentOrange,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Check Dues",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        } else {
            // ==================================================================
            // STATE C & D: CURRENT CYCLE LIVE DASHBOARD / TODAY'S READING SAVED
            // Matches Stitch homescreen2 (compact collapsed) -> homescreen1 (drawer)
            // ==================================================================
            val metrics = uiState.currentEnergyMetrics
            val units = metrics?.unitsConsumed ?: 0.0
            val currentReadingVal = metrics?.currentReading ?: 0.0
            val avgDaily = metrics?.avgUnitsPerDay ?: 0.0
            val estBill = metrics?.estimatedBill ?: 0.0
            val periodDisplay = metrics?.billingPeriodDisplay ?: "Current billing cycle"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header with Live Tracking Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR CURRENT ENERGY",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.TextSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(HomeColors.PrimaryContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(HomeColors.Success)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (uiState.isTodayReadingEntered) "TODAY SAVED ✓" else "LIVE TRACKING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.Primary,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Primary Numbers Display (Current-Cycle Consumption & Estimated Bill)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = if (units % 1.0 == 0.0) units.toInt().toString() else String.format(Locale.getDefault(), "%.1f", units),
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.TextPrimary,
                                letterSpacing = (-1).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "kWh",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = HomeColors.TextSecondary,
                                modifier = Modifier.padding(bottom = 3.dp)
                            )
                        }
                        Text(
                            text = "Consumed since last bill",
                            fontSize = 11.sp,
                            color = HomeColors.TextSecondary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (estBill > 0.0) "₹${String.format(Locale.getDefault(), "%,.0f", estBill)}" else "₹0",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.AccentOrange
                        )
                        Text(
                            text = "Estimated Bill",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            color = HomeColors.TextSecondary
                        )
                    }
                }

                // Collapsed Context Banner (Cycle & Tariff)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = HomeColors.SurfaceSecondary
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Cycle: $periodDisplay",
                            fontSize = 11.sp,
                            color = HomeColors.TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.tariffCategory.ifBlank { "LT-IA • Domestic" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeColors.Primary
                        )
                    }
                }

                // Interactive Collapse/Expand Dropdown Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isDrawerExpanded = !isDrawerExpanded },
                    shape = RoundedCornerShape(8.dp),
                    color = HomeColors.SurfaceSecondary,
                    border = BorderStroke(1.dp, HomeColors.Border)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDrawerExpanded || uiState.isEditingReading) "Hide Details" else "View Meter Readings & Entry",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (isDrawerExpanded || uiState.isEditingReading) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isDrawerExpanded || uiState.isEditingReading) "Collapse" else "Expand",
                            tint = HomeColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Collapsible Drawer (Expanded State from homescreen1)
                AnimatedVisibility(visible = isDrawerExpanded || uiState.isEditingReading) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3 Telemetry Metric Sub-Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Current Reading Sub-Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = HomeColors.SurfaceSecondary,
                                border = BorderStroke(1.dp, HomeColors.Border)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "CURRENT",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = HomeColors.TextSecondary
                                        )
                                        if (!uiState.isTodayReadingEntered) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit previous reading",
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .clickable { onEditCurrentReading() },
                                                tint = HomeColors.Primary
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%,.0f", currentReadingVal)} kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.TextPrimary
                                    )
                                }
                            }

                            // Average Daily Sub-Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = HomeColors.SurfaceSecondary,
                                border = BorderStroke(1.dp, HomeColors.Border)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "AVERAGE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.Success
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${String.format(Locale.getDefault(), "%.1f", avgDaily)} kWh/d",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.TextPrimary
                                    )
                                }
                            }

                            // Elapsed Days Sub-Card
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                color = HomeColors.SurfaceSecondary,
                                border = BorderStroke(1.dp, HomeColors.Border)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = "ELAPSED",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.Primary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${metrics?.daysElapsed ?: 1} Days",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.TextPrimary
                                    )
                                }
                            }
                        }

                        // Progress Track
                        val progressFraction = (units / 1000.0).toFloat().coerceIn(0.05f, 1f)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(HomeColors.Border)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(progressFraction)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(HomeColors.Primary)
                            )
                        }

                        // Meter Reading Entry / Status / Edit
                        if (uiState.isTodayReadingEntered) {
                            if (uiState.isEditingReading) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = HomeColors.SurfaceSecondary,
                                    border = BorderStroke(1.dp, HomeColors.Border)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "Edit Today's Reading",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = HomeColors.TextPrimary
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedTextField(
                                                value = uiState.meterInputText,
                                                onMeterInputChanged,
                                                modifier = Modifier.weight(1f),
                                                trailingIcon = {
                                                    Text(
                                                        text = "kWh",
                                                        fontSize = 11.sp,
                                                        color = HomeColors.TextSecondary,
                                                        modifier = Modifier.padding(end = 8.dp)
                                                    )
                                                },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                singleLine = true,
                                                shape = RoundedCornerShape(6.dp),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedBorderColor = HomeColors.Primary,
                                                    unfocusedBorderColor = HomeColors.Border
                                                )
                                            )
                                            Button(
                                                onClick = onUpdateTodayReading,
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = HomeColors.Primary,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Text("Update", fontSize = 12.sp)
                                            }
                                            TextButton(onClick = onCancelEditingReading) {
                                                Text("Cancel", fontSize = 12.sp, color = HomeColors.TextSecondary)
                                            }
                                        }
                                        if (uiState.meterValidationError != null) {
                                            Text(
                                                text = uiState.meterValidationError,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(HomeColors.PrimaryContainer)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = HomeColors.Success,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Today's reading (${String.format(Locale.getDefault(), "%,.0f", uiState.todayEnteredReading ?: currentReadingVal)} kWh) logged",
                                            fontSize = 11.sp,
                                            color = HomeColors.Primary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    TextButton(
                                        onClick = onStartEditingReading,
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            modifier = Modifier.size(12.dp),
                                            tint = HomeColors.Primary
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "Edit",
                                            fontSize = 11.sp,
                                            color = HomeColors.Primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        } else {
                            // Reading not yet entered today
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                color = HomeColors.SurfaceSecondary,
                                border = BorderStroke(1.dp, HomeColors.Border)
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Enter today's meter reading",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.TextPrimary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = uiState.meterInputText,
                                            onValueChange = onMeterInputChanged,
                                            modifier = Modifier.weight(1f),
                                            placeholder = {
                                                Text(
                                                    text = "e.g. ${String.format(Locale.getDefault(), "%.0f", currentReadingVal + 5)}",
                                                    fontSize = 12.sp,
                                                    color = HomeColors.TextSecondary.copy(alpha = 0.6f)
                                                )
                                            },
                                            trailingIcon = {
                                                Text(
                                                    text = "kWh",
                                                    fontSize = 11.sp,
                                                    color = HomeColors.TextSecondary,
                                                    modifier = Modifier.padding(end = 8.dp)
                                                )
                                            },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(6.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = HomeColors.Primary,
                                                unfocusedBorderColor = HomeColors.Border
                                            )
                                        )
                                        Button(
                                            onClick = onSaveMeterReading,
                                            shape = RoundedCornerShape(6.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = HomeColors.Primary,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Text("Save", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                        }
                                    }
                                    if (uiState.meterValidationError != null) {
                                        Text(
                                            text = uiState.meterValidationError,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── 4. REAP Classification Card ──────────────────────────────────────────────

@Composable
private fun ReapClassificationCard(
    uiState: HomeUiState,
    onLearnMore: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = null,
                        tint = HomeColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Your Energy Profile",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeColors.TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(HomeColors.PrimaryContainer)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.reapTier == ReapTier.PENDING) "Pending" else uiState.reapTier.code,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.Primary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (uiState.reapTier == ReapTier.PENDING) {
                Text(
                    text = "Your home will be classified into one of the 4 REAP benchmark tiers based on your recorded bi-monthly cycle units.",
                    fontSize = 12.sp,
                    color = HomeColors.TextSecondary,
                    lineHeight = 17.sp
                )

                // 4-tier category preview chips in 2x2 grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReapTierChip(
                            modifier = Modifier.weight(1f),
                            code = "ECH",
                            range = "≤ 500 u",
                            title = "Conservation",
                            subtitle = "Energy Saver Base",
                            tint = HomeColors.Primary
                        )
                        ReapTierChip(
                            modifier = Modifier.weight(1f),
                            code = "EEH",
                            range = "500–800 u",
                            title = "Efficiency",
                            subtitle = "Targeted Optimization",
                            tint = HomeColors.Success
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ReapTierChip(
                            modifier = Modifier.weight(1f),
                            code = "EOH",
                            range = "800–1000 u",
                            title = "Optimisation",
                            subtitle = "Load Balancing",
                            tint = HomeColors.AccentOrange
                        )
                        ReapTierChip(
                            modifier = Modifier.weight(1f),
                            code = "ETH",
                            range = ">1000 u",
                            title = "Transition",
                            subtitle = "High Solar Yield",
                            tint = HomeColors.Primary
                        )
                    }
                }
            } else {
                // Connected Tier Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = uiState.reapTier.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.TextPrimary
                    )
                    Text(
                        text = uiState.reapTier.description,
                        fontSize = 12.sp,
                        color = HomeColors.TextSecondary,
                        lineHeight = 17.sp
                    )

                    if (uiState.reapTier.targetUnits != null && uiState.unitsConsumed != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = HomeColors.SurfaceSecondary,
                            border = BorderStroke(1.dp, HomeColors.Border)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Current: ${uiState.unitsConsumed.toInt()} kWh",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HomeColors.TextPrimary
                                    )
                                    Text(
                                        text = "Target: ${uiState.reapTier.targetUnits.toInt()} kWh",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HomeColors.Primary
                                    )
                                }
                                val targetFraction = (uiState.reapTier.targetUnits / uiState.unitsConsumed).toFloat().coerceIn(0.1f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(HomeColors.Border)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(targetFraction)
                                            .fillMaxHeight()
                                            .background(HomeColors.Primary)
                                    )
                                }
                                Text(
                                    text = "Targeting ${uiState.reapTier.targetUnits.toInt()} kWh brings your home into the lower tariff slab, unlocking subsidized baseline units.",
                                    fontSize = 11.sp,
                                    color = HomeColors.TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReapTierChip(
    modifier: Modifier = Modifier,
    code: String,
    range: String,
    title: String,
    subtitle: String,
    tint: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = HomeColors.SurfaceSecondary,
        border = BorderStroke(1.dp, HomeColors.Border)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = code, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = tint)
                Text(text = range, fontSize = 10.sp, color = HomeColors.TextSecondary)
            }
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = HomeColors.TextPrimary)
            Text(text = subtitle, fontSize = 10.sp, color = HomeColors.TextSecondary, maxLines = 1)
        }
    }
}

// ─── 5. KNOW YOUR CONSUMPTION (Solar Lens Promotion Card) ──────────────────────

@Composable
private fun KnowYourConsumptionCard(
    uiState: HomeUiState,
    onOpenSolarLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentKwh = uiState.currentBillingCycleKwh
    val kwhText = if (currentKwh > 0.0) {
        if (currentKwh % 1.0 == 0.0) "${currentKwh.toInt()}" else String.format(Locale.getDefault(), "%.1f", currentKwh)
    } else null

    val headingText = if (kwhText != null) {
        "Find where your $kwhText kWh is going"
    } else {
        "Find where your electricity is going"
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.SurfaceSecondary,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "KNOW YOUR CONSUMPTION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HomeColors.Primary,
                letterSpacing = 0.8.sp
            )

            Text(
                text = headingText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = HomeColors.TextPrimary
            )

            Text(
                text = "Run an appliance audit to discover which heavy loads (AC, water heater, pump) can be optimized to lower your bill into the next lower tariff slab.",
                fontSize = 12.sp,
                color = HomeColors.TextSecondary,
                lineHeight = 17.sp
            )

            Button(
                onClick = onOpenSolarLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeColors.Primary,
                    contentColor = Color.White
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Start Appliance Audit with Solar Lens",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

// ─── 6. Bill History / Check Dues Section ─────────────────────────────────────

@Composable
private fun BillActionsSection(
    onViewBillHistory: () -> Unit,
    onCheckDues: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = HomeColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "BILL & PAYMENT ACTIONS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.Primary,
                        letterSpacing = 0.6.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onViewBillHistory,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, HomeColors.Border)
                ) {
                    Text(
                        text = "View Bill History",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeColors.TextPrimary
                    )
                }

                Button(
                    onClick = onCheckDues,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeColors.AccentOrange,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = "Check Dues",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── 6. REAP Energy Journey Stepper (Preserved for Profile screen relocation) ───

@Suppress("unused")
@Composable
private fun ReapJourneyStepper(activeStage: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Energy Journey",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "The 6-Stage REAP Framework",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant
                    )
                }

                Text(
                    text = "$activeStage of 6 Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimaryContainer
                )
            }

            // 6-Stage Rail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val stages = listOf(
                    "01 Measure" to 1,
                    "02 Reduce" to 2,
                    "03 Optimise" to 3,
                    "04 Generate" to 4,
                    "05 Store" to 5,
                    "06 Monitor" to 6
                )

                stages.forEach { (label, stageNum) ->
                    val isDone = stageNum < activeStage
                    val isActive = stageNum == activeStage
                    val isFuture = stageNum > activeStage

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> StitchPrimaryContainer
                                        isActive -> StitchPrimaryFixed
                                        else -> StitchSurfaceContainerLow
                                    }
                                )
                                .then(
                                    if (isActive) Modifier.border(2.dp, StitchPrimaryContainer, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            } else {
                                Text(
                                    text = stageNum.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) StitchOnPrimaryFixedVariant else StitchOnSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = label.substringAfter(" "),
                            fontSize = 9.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) StitchPrimaryContainer else StitchOnSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ─── 7. Solar Lens Hero Card ──────────────────────────────────────────────────

@Suppress("unused")
@Composable
private fun SolarLensHeroCard(
    uiState: HomeUiState,
    onLaunchLens: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchPrimaryContainer.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraOutdoor,
                            contentDescription = null,
                            tint = StitchPrimaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "INTELLIGENT AUDIT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            letterSpacing = 0.6.sp
                        )
                        Text(
                            text = "Solar Lens • AI Load Audit",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(StitchPrimaryContainer)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "v2.4 CV",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Real status banner if audited
            if (uiState.hasCompletedAudit) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StitchSurfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${uiState.appliances.size} Active Appliances Profiled",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = StitchOnSurface
                            )
                            Text(
                                text = "Optical CV model verified",
                                fontSize = 11.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.2f kW", uiState.totalConnectedLoadKw),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchPrimaryContainer
                            )
                            Text(
                                text = "Connected Load",
                                fontSize = 10.sp,
                                color = StitchOnSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Feature Highlights
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureBullet(
                        icon = Icons.Default.ViewInAr,
                        text = "Room-by-room CV spatial detection"
                    )
                    FeatureBullet(
                        icon = Icons.Default.Sensors,
                        text = "Appliance wattage profiling & inductive spikes"
                    )
                    FeatureBullet(
                        icon = Icons.Default.Tune,
                        text = "Precision multi-tier solar array sizing recommendation"
                    )
                }
            }

            Button(
                onClick = onLaunchLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = StitchPrimaryContainer,
                    contentColor = StitchOnPrimary
                )
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (uiState.hasCompletedAudit) "Open Solar Lens" else "Launch Solar Lens",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureBullet(
    icon: ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = StitchPrimaryContainer,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            fontSize = 12.sp,
            color = StitchOnSurfaceVariant
        )
    }
}

// ─── 7. Quick Tools Section (2x2 Grid) ─────────────────────────────────────────

@Composable
private fun QuickToolsSection(
    onBillStatus: () -> Unit,
    onChatbot: () -> Unit,
    onSolarLens: () -> Unit,
    onCalculator: () -> Unit,
    hasConnectedBill: Boolean,
    tariffCode: String = "LT-IA"
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Explore Tools",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeColors.TextPrimary
            )
            Text(
                text = "QUICK ACCESS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = HomeColors.TextSecondary,
                letterSpacing = 0.8.sp
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Bill Status",
                subtitle = if (hasConnectedBill) "TNEB $tariffCode" else "Check dues & units",
                icon = Icons.Default.ReceiptLong,
                containerBg = HomeColors.PrimaryContainer,
                iconTint = HomeColors.Primary,
                onClick = onBillStatus
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Chatbot",
                subtitle = "Ask Energy AI",
                icon = Icons.Default.SmartToy,
                containerBg = HomeColors.LightBlue,
                iconTint = Color(0xFF005683),
                onClick = onChatbot
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Solar Lens",
                subtitle = "AI load & audit",
                icon = Icons.Default.CameraOutdoor,
                containerBg = HomeColors.PrimaryContainer,
                iconTint = HomeColors.Success,
                onClick = onSolarLens
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Calculator",
                subtitle = "Solar sizing & savings",
                icon = Icons.Default.Calculate,
                containerBg = Color(0xFFFFEED9),
                iconTint = HomeColors.AccentOrange,
                onClick = onCalculator
            )
        }
    }
}

@Composable
private fun QuickToolCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerBg: Color = HomeColors.SurfaceSecondary,
    iconTint: Color = HomeColors.Primary,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = tween(100),
        label = "toolScale"
    )

    Surface(
        onClick = onClick,
        modifier = modifier.scale(scale),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        interactionSource = interactionSource,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(containerBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = HomeColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = HomeColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── 9. Recent Activity Section ───────────────────────────────────────────────

@Composable
private fun RecentActivitySection(
    activities: List<HomeActivityEvent>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Activity",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeColors.TextPrimary
                )
                Text(
                    text = "LOG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = HomeColors.TextSecondary,
                    letterSpacing = 0.8.sp
                )
            }

            if (activities.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = HomeColors.SurfaceSecondary,
                    border = BorderStroke(1.dp, HomeColors.Border)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No energy assessments yet",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Your connected bill cycles, appliance audits, and savings milestones will populate here automatically.",
                            fontSize = 11.sp,
                            color = HomeColors.TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    activities.forEachIndexed { index, event ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(if (event.isPositive) HomeColors.Success else HomeColors.AccentOrange)
                                )
                                Column {
                                    Text(
                                        text = event.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = HomeColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${event.subtitle} • ${event.timeLabel}",
                                        fontSize = 11.sp,
                                        color = HomeColors.TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (!event.metricLabel.isNullOrBlank()) {
                                Text(
                                    text = event.metricLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (event.isPositive) HomeColors.Success else HomeColors.AccentOrange
                                )
                            }
                        }

                        if (index < activities.lastIndex) {
                            HorizontalDivider(
                                color = HomeColors.Border,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 10. Stitch Bottom Navigation (Replaced with SolarEaseBottomNavigation) ───────


@Composable
private fun StitchNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 26.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(if (isSelected) StitchPrimaryFixed else Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isSelected) StitchOnPrimaryFixedVariant else StitchOnSurfaceVariant,
                modifier = Modifier.size(19.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) StitchPrimaryContainer else StitchOnSurfaceVariant
        )
    }
}

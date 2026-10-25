package com.example.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.material.icons.filled.CameraAlt
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.text.font.FontFamily
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
import com.example.ui.components.subtleTopRightGradient
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
    onNavigateToMeterScanner: () -> Unit = {},
    viewModel: HomeViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val currentUser = UserSession.currentUser

    var showAccountPreview by remember { mutableStateOf(false) }
    var showProfilePrompt by remember { mutableStateOf(false) }
    var showInsightsAndTools by rememberSaveable { mutableStateOf(false) }
    var showBillHelp by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }

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
        topBar = {
            StitchTopBar(
                userInitial = uiState.userInitial,
                onProfileClick = { showAccountPreview = true },
                onNotificationsClick = {}
            )
        },
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val step = uiState.currentOnboardingStep

                // ── Onboarding Progress Stepper Banner ───────────────────
                OnboardingProgressBanner(
                    currentStep = step,
                    isAuditCompleted = uiState.hasCompletedAudit
                )

                // ── Greeting Header ──────────────────────────────────────
                GreetingHeader(
                    greeting = uiState.greeting,
                    userName = uiState.userName,
                    onboardingStep = step
                )

                if (step == 1) {
                    // ── STEP 1: Connect TNEB Bill ────────────────────────────
                    PrimaryEnergyCard(
                        uiState = uiState,
                        onConnectBill = onNavigateToTnebBillStatus,
                        onViewBillHistory = onNavigateToBillHistory,
                        onMeterInputChanged = { viewModel.onMeterInputChanged(it) },
                        onSaveMeterReading = { viewModel.saveCurrentMeterReading() },
                        onUpdateTodayReading = { viewModel.updateTodayMeterReading() },
                        onStartEditingReading = { viewModel.startEditingReading() },
                        onCancelEditingReading = { viewModel.cancelEditingReading() },
                        onEditCurrentReading = { viewModel.initiateEditCurrentReading() },
                        onScanMeterReading = onNavigateToMeterScanner
                    )

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = HomeColors.CardBg,
                        border = BorderStroke(1.dp, HomeColors.Border),
                        shadowElevation = 0.dp,
                        onClick = { showBillHelp = !showBillHelp }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Default.Help, contentDescription = null, tint = HomeColors.Primary)
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Need help with your electricity bill?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = HomeColors.TextPrimary)
                                Text(if (showBillHelp) "Hide bill help" else "Find your service number and learn how it works", fontSize = 12.sp, color = HomeColors.TextSecondary)
                            }
                            Icon(if (showBillHelp) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (showBillHelp) "Hide bill help" else "Show bill help", tint = HomeColors.Primary)
                        }
                    }

                    AnimatedVisibility(visible = showBillHelp, enter = fadeIn(animationSpec = tween(180)), exit = fadeOut(animationSpec = tween(120))) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            WhyConnectBillSection()
                            WhereToFindConsumerNumberCard()
                            ReapClassificationCard(uiState = uiState, onLearnMore = onNavigateToTnebBillStatus)
                        }
                    }
                } else if (step == 2) {
                    // ── STEP 2: Track Usage / Baseline Calibration ───────────
                    SyncedBillSummaryStrip(
                        uiState = uiState,
                        onViewBillHistory = onNavigateToBillHistory,
                        onCheckDues = onNavigateToTnebBillStatus
                    )

                    PrimaryEnergyCard(
                        uiState = uiState,
                        onConnectBill = onNavigateToTnebBillStatus,
                        onViewBillHistory = onNavigateToBillHistory,
                        onMeterInputChanged = { viewModel.onMeterInputChanged(it) },
                        onSaveMeterReading = { viewModel.saveCurrentMeterReading() },
                        onUpdateTodayReading = { viewModel.updateTodayMeterReading() },
                        onStartEditingReading = { viewModel.startEditingReading() },
                        onCancelEditingReading = { viewModel.cancelEditingReading() },
                        onEditCurrentReading = { viewModel.initiateEditCurrentReading() },
                        onScanMeterReading = onNavigateToMeterScanner
                    )

                    MeterCalibrationProgressCard(
                        recordedCount = uiState.recordedReadingsCount,
                        todayEnteredReading = uiState.todayEnteredReading,
                        baselineKwh = uiState.meterBaseline?.baselineReading ?: 0.0
                    )

                    LockedStep3PreviewCard()
                } else {
                    // ── STEP 3: Audit Loads & Full Dashboard ─────────────────
                    if (!uiState.hasCompletedAudit) {
                        SolarLensPriorityHeroCard(
                            uiState = uiState,
                            onOpenSolarLens = onNavigateToSolarEaseLens
                        )
                    } else {
                        CompletedAuditSummaryCard(
                            uiState = uiState,
                            onOpenSolarLens = onNavigateToSolarEaseLens
                        )
                    }

                    PrimaryEnergyCard(
                        uiState = uiState,
                        onConnectBill = onNavigateToTnebBillStatus,
                        onViewBillHistory = onNavigateToBillHistory,
                        onMeterInputChanged = { viewModel.onMeterInputChanged(it) },
                        onSaveMeterReading = { viewModel.saveCurrentMeterReading() },
                        onUpdateTodayReading = { viewModel.updateTodayMeterReading() },
                        onStartEditingReading = { viewModel.startEditingReading() },
                        onCancelEditingReading = { viewModel.cancelEditingReading() },
                        onEditCurrentReading = { viewModel.initiateEditCurrentReading() },
                        onScanMeterReading = onNavigateToMeterScanner
                    )

                    // Keep the dashboard focused; secondary tools remain available below.
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = HomeColors.CardBg,
                        border = BorderStroke(1.dp, HomeColors.Border),
                        shadowElevation = 0.dp,
                        onClick = { showInsightsAndTools = !showInsightsAndTools }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(HomeColors.SurfaceSecondary), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Analytics, contentDescription = null, tint = HomeColors.Primary, modifier = Modifier.size(22.dp))
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Usage insights & tools", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = HomeColors.TextPrimary)
                                Text(if (showInsightsAndTools) "Hide extra details" else "View usage trends, bills and helpful tools", fontSize = 12.sp, color = HomeColors.TextSecondary)
                            }
                            Icon(if (showInsightsAndTools) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (showInsightsAndTools) "Collapse insights and tools" else "Expand insights and tools", tint = HomeColors.Primary)
                        }
                    }

                    AnimatedVisibility(visible = showInsightsAndTools, enter = fadeIn(animationSpec = tween(180)), exit = fadeOut(animationSpec = tween(120))) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            MeterReadingProgressionGraph(points = uiState.meterGraphPoints, billData = uiState.billData, onEditReading = { viewModel.onInitiateEditReading(it) })
                            ReapClassificationCard(uiState = uiState, onLearnMore = onNavigateToTnebBillStatus)
                            BillActionsSection(onViewBillHistory = onNavigateToBillHistory, onCheckDues = onNavigateToTnebBillStatus)
                            QuickToolsSection(onBillStatus = onNavigateToTnebBillStatus, onChatbot = onNavigateToElectricityChatbot, onSolarLens = onNavigateToSolarEaseLens, onCalculator = onNavigateToBillCalculator, hasConnectedBill = true, tariffCode = uiState.tariffCode)
                            RecentActivitySection(activities = uiState.recentActivities)
                        }
                    }
                }

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
    userInitial: Char,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = HomeColors.Background,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand: SolarEase icon, App name, and HOME badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "SolarEase",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.TextPrimary,
                        letterSpacing = (-0.3).sp
                    )
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
    onboardingStep: Int
) {
    // Keep the status marker steady; constant motion adds unnecessary visual noise.
    val alphaAnim = 1f

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
                        text = when (onboardingStep) {
                            1 -> "GET STARTED"
                            2 -> "BILL CONNECTED"
                            else -> "HOME ENERGY OVERVIEW"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.Success,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when (onboardingStep) {
                        1 -> if (!userName.isNullOrBlank()) "Welcome, $userName!" else "Welcome to SolarEase!"
                        2 -> "Your electricity connection is ready!"
                        else -> if (!userName.isNullOrBlank()) "$greeting, $userName! ☀️" else "$greeting! ☀️"
                    },
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
            text = when (onboardingStep) {
                1 -> "Connect your electricity bill to get started."
                2 -> "Enter the number shown on your electricity meter to track your usage."
                else -> "See how much electricity you use and explore ways to save."
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
    onEditCurrentReading: () -> Unit = {},
    onScanMeterReading: () -> Unit = {}
) {
    var isDrawerExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(16.dp),
                    radiusMultiplier = 0.70f
                )
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
                            text = "Not Connected",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HomeColors.AccentOrange
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
                            text = "Connect Your EB Bill",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.TextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect your 11–12 digit consumer number to establish your baseline tariff schedule (LT-1A Domestic) and billing cycle.",
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
        } else if (!uiState.hasSufficientReadings) {
            // ==================================================================
            // STATE B: BILL CONNECTED, BASELINE CALIBRATION (READINGS < 2)
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
                // Header with Live Baseline Capture Badge
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
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = HomeColors.Primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Today's Meter Reading",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.TextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF3C7))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Live Baseline Capture",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB45309),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Text(
                    text = "Enter the current total reading displayed on your physical digital electricity meter.",
                    fontSize = 12.sp,
                    color = HomeColors.TextSecondary,
                    lineHeight = 17.sp
                )

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

                // If user entered a reading today and is not currently in edit mode, show recorded state
                if (uiState.isTodayReadingEntered && !uiState.isEditingReading) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = HomeColors.PrimaryContainer.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, HomeColors.Success.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TODAY'S RECORDED READING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.Success
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${String.format(Locale.getDefault(), "%,.0f", uiState.todayEnteredReading ?: baselineKwh)} kWh",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.TextPrimary
                                )
                            }
                            TextButton(onClick = onStartEditingReading) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = HomeColors.Primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Edit",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HomeColors.Primary
                                )
                            }
                        }
                    }
                } else {
                    // Prompt to enter today's meter reading
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (uiState.isEditingReading) "Edit today's meter reading" else "Enter cumulative units from meter display",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeColors.TextPrimary,
                                modifier = Modifier.weight(1f, fill = false)
                            )

                            IconButton(
                                onClick = onScanMeterReading,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Scan meter reading",
                                    tint = HomeColors.Primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = uiState.meterInputText,
                            onValueChange = onMeterInputChanged,
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text(
                                    text = if (baselineKwh > 0.0) "e.g. ${String.format(Locale.getDefault(), "%.0f", baselineKwh + 15)}" else "e.g. 42352",
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

                        if (uiState.isEditingReading) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onCancelEditingReading,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, HomeColors.Border)
                                ) {
                                    Text("Cancel", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HomeColors.TextPrimary)
                                }
                                Button(
                                    onClick = onUpdateTodayReading,
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HomeColors.Primary,
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Update", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        } else {
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
                                    text = "Save Reading →",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Tip: Record your meter reading regularly to understand how much electricity your home uses each day.",
                    fontSize = 11.sp,
                    color = HomeColors.TextSecondary,
                    lineHeight = 15.sp
                )

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
}

// ─── 3B. Progressive Onboarding Components (3-Stage Single-Screen Experience) ─

@Composable
private fun OnboardingProgressBanner(
    currentStep: Int,
    isAuditCompleted: Boolean = false,
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
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(HomeColors.PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = HomeColors.Primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = when (currentStep) {
                            1 -> "STEP 1 OF 3 • CONNECT YOUR BILL"
                            2 -> "STEP 2 OF 3 • ADD METER READING"
                            else -> "STEP 3 OF 3 • CHECK HOME ENERGY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.Primary,
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        currentStep == 1 -> Color(0xFFFEF3C7)
                        currentStep == 2 -> Color(0xFFE0F2FE)
                        isAuditCompleted -> HomeColors.PrimaryContainer
                        else -> Color(0xFFE8F5E9)
                    }
                ) {
                    Text(
                        text = when {
                            currentStep == 1 -> "33% Ready"
                            currentStep == 2 -> "66% Complete"
                            isAuditCompleted -> "100% Ready ✨"
                            else -> "Final Setup Step"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            currentStep == 1 -> Color(0xFFB45309)
                            currentStep == 2 -> Color(0xFF0369A1)
                            else -> HomeColors.Primary
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            val targetProgress = when (currentStep) {
                1 -> 0.33f
                2 -> 0.66f
                else -> 1.0f
            }
            val animatedProgress by animateFloatAsState(
                targetValue = targetProgress,
                animationSpec = tween(500),
                label = "onboardingProgress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = HomeColors.Primary,
                trackColor = HomeColors.SurfaceSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OnboardingStepChip(
                    modifier = Modifier.weight(1f),
                    stepNumber = "1",
                    label = "Connect Bill",
                    isCompleted = currentStep > 1,
                    isActive = currentStep == 1
                )
                OnboardingStepChip(
                    modifier = Modifier.weight(1f),
                    stepNumber = "2",
                    label = "Meter Log",
                    isCompleted = currentStep > 2,
                    isActive = currentStep == 2
                )
                OnboardingStepChip(
                    modifier = Modifier.weight(1f),
                    stepNumber = "3",
                    label = "Solar Lens",
                    isCompleted = isAuditCompleted,
                    isActive = currentStep == 3 && !isAuditCompleted
                )
            }
        }
    }
}

@Composable
private fun OnboardingStepChip(
    stepNumber: String,
    label: String,
    isCompleted: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = when {
            isActive -> HomeColors.PrimaryContainer.copy(alpha = 0.7f)
            isCompleted -> HomeColors.SurfaceSecondary
            else -> Color(0xFFF8FAFC)
        },
        border = BorderStroke(
            1.dp,
            when {
                isActive -> HomeColors.Primary.copy(alpha = 0.5f)
                isCompleted -> HomeColors.Success.copy(alpha = 0.3f)
                else -> HomeColors.Border.copy(alpha = 0.6f)
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = HomeColors.Success,
                    modifier = Modifier.size(13.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .clip(CircleShape)
                        .background(
                            if (isActive) HomeColors.Primary else Color(0xFF94A3B8)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNumber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    isActive -> HomeColors.Primary
                    isCompleted -> HomeColors.TextPrimary
                    else -> HomeColors.TextSecondary
                },
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WhyConnectBillSection(
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(HomeColors.PrimaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = HomeColors.Primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "Why connect your electricity bill?",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.TextPrimary
                    )
                    Text(
                        text = "SolarEase is designed around real consumption, not rough guesses",
                        fontSize = 11.sp,
                        color = HomeColors.TextSecondary
                    )
                }
            }

            HorizontalDivider(color = HomeColors.Border, thickness = 1.dp)

            BenefitRow(
                icon = Icons.Default.Speed,
                iconTint = HomeColors.Primary,
                iconBg = HomeColors.PrimaryContainer,
                title = "Accurate Energy Baseline",
                description = "Links your bi-monthly kWh usage to establish how much electricity your household actually consumes."
            )

            BenefitRow(
                icon = Icons.Default.PriceCheck,
                iconTint = HomeColors.AccentOrange,
                iconBg = Color(0xFFFFF3E0),
                title = "TNEB Tariff & Slab Calculation",
                description = "Calculates your cost using Tamil Nadu domestic slab rates, fixed charges, and the 100 free units subsidy."
            )

            BenefitRow(
                icon = Icons.Default.Bolt,
                iconTint = HomeColors.Success,
                iconBg = Color(0xFFE8F5E9),
                title = "Zero-Waste Solar Sizing",
                description = "Recommends the right rooftop solar kW size to offset your bill without wasting money on unneeded panels."
            )
        }
    }
}

@Composable
private fun BenefitRow(
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = HomeColors.TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = HomeColors.TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun WhereToFindConsumerNumberCard(
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        color = HomeColors.SurfaceSecondary,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = null,
                        tint = HomeColors.Primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Where to find your 11–12 digit consumer number?",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeColors.TextPrimary
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = HomeColors.TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = HomeColors.CardBg,
                        border = BorderStroke(1.dp, HomeColors.Border)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Standard Format Example:",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeColors.TextSecondary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "04-021-008-1422",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = HomeColors.Primary,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Region (2) - Section (3) - Distribution (3) - Service No. (4)",
                                fontSize = 10.sp,
                                color = HomeColors.TextSecondary
                            )
                        }
                    }

                    Text(
                        text = "• On your physical EB Card / White Card: Look for the 11 or 12 digit number at the top.\n" +
                               "• On TNEB SMS or receipt: It is listed as the Consumer No. or Service No.\n" +
                               "• Preserve leading zeros: If your number starts with 0 (e.g., 04), be sure to include it.",
                        fontSize = 11.sp,
                        color = HomeColors.TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SyncedBillSummaryStrip(
    uiState: HomeUiState,
    onViewBillHistory: () -> Unit,
    onCheckDues: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = HomeColors.CardBg,
        border = BorderStroke(1.dp, HomeColors.Border),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(HomeColors.Success)
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "TNEB Bill Linked",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.TextPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = HomeColors.PrimaryContainer
                        ) {
                            Text(
                                text = "Tariff ${uiState.tariffCode}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HomeColors.Primary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                    val displayNum = uiState.consumerNumberFormatted.ifBlank {
                        uiState.billData?.consumerNumber ?: "Connected"
                    }
                    Text(
                        text = "Consumer No: $displayNum",
                        fontSize = 11.sp,
                        color = HomeColors.TextSecondary
                    )
                }
            }

            TextButton(
                onClick = onViewBillHistory,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "View Bill Details →",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeColors.Primary
                )
            }
        }
    }
}

@Composable
private fun MeterCalibrationProgressCard(
    recordedCount: Int,
    todayEnteredReading: Double?,
    baselineKwh: Double,
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = Color(0xFF0369A1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Meter Calibration Progress",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.TextPrimary
                        )
                        Text(
                            text = if (recordedCount >= 1 || todayEnteredReading != null) {
                                "1 of 2 readings recorded"
                            } else {
                                "0 of 2 readings recorded"
                            },
                            fontSize = 11.sp,
                            color = HomeColors.TextSecondary
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (recordedCount >= 1 || todayEnteredReading != null) {
                        Color(0xFFE0F2FE)
                    } else {
                        Color(0xFFFEF3C7)
                    }
                ) {
                    Text(
                        text = if (recordedCount >= 1 || todayEnteredReading != null) "In Calibration" else "Action Needed",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (recordedCount >= 1 || todayEnteredReading != null) Color(0xFF0369A1) else Color(0xFFB45309),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Visual calibration timeline steps
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Step 1 dot
                val step1Done = recordedCount >= 1 || todayEnteredReading != null
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(if (step1Done) HomeColors.Success else Color(0xFFCBD5E1)),
                    contentAlignment = Alignment.Center
                ) {
                    if (step1Done) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    } else {
                        Text("1", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (step1Done) "First Reading" else "Enter 1st Reading",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (step1Done) HomeColors.TextPrimary else HomeColors.TextSecondary
                )

                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (step1Done) HomeColors.Success.copy(alpha = 0.5f) else Color(0xFFE2E8F0),
                    thickness = 2.dp
                )

                // Step 2 dot
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCBD5E1)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("2", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Second Reading",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HomeColors.TextSecondary
                )
            }

            // Explanation box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = HomeColors.SurfaceSecondary
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (recordedCount >= 1 || todayEnteredReading != null) {
                            "Why is a 2nd reading required?"
                        } else {
                            "How calibration works:"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.TextPrimary
                    )
                    Text(
                        text = if (recordedCount >= 1 || todayEnteredReading != null) {
                            "A single reading records cumulative kWh. To calculate your daily burn rate and unlock consumption graphs and appliance audit, SolarEase requires a second reading logged on a later date (e.g. tomorrow)."
                        } else {
                            "Enter the cumulative reading directly from your physical meter box above. This will serve as your starting point."
                        },
                        fontSize = 11.sp,
                        color = HomeColors.TextSecondary,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedStep3PreviewCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE2E8F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Step 3: Audit with Solar Lens",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "Unlocks after 2nd meter reading is saved",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE2E8F0)
                ) {
                    Text(
                        text = "Locked 🔒",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Text(
                text = "Once your second reading establishes your household burn rate, you'll be able to audit power-hungry appliances using camera-assisted Solar Lens.",
                fontSize = 11.sp,
                color = Color(0xFF94A3B8),
                lineHeight = 15.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Air Conditioner", "Water Heater", "Water Pump").forEach { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFFEDF2F7),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Text(
                            text = tag,
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SolarLensPriorityHeroCard(
    uiState: HomeUiState,
    onOpenSolarLens: () -> Unit,
    modifier: Modifier = Modifier
) {
    val consumedUnits = uiState.currentEnergyMetrics?.unitsConsumed ?: 0.0

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF1E3A2F),
        shadowElevation = 2.dp
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
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFF2E5A44)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF74D99F),
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "PRIORITY ACTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF74D99F),
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Step 3 of 3",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Find Where Your Electricity Goes",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (consumedUnits > 0.0) {
                        "You've consumed ${String.format(Locale.getDefault(), "%.1f", consumedUnits)} kWh since your baseline. Scan your appliances with Solar Lens to audit high-power loads before sizing solar."
                    } else {
                        "Audit your high-power appliances with Solar Lens to understand individual device consumption before sizing your rooftop solar system."
                    },
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 17.sp
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ApplianceTag(label = "Air Conditioner")
                ApplianceTag(label = "Water Heater")
                ApplianceTag(label = "Water Pump")
            }

            Button(
                onClick = onOpenSolarLens,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF74D99F),
                    contentColor = Color(0xFF0F291E)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Appliance Audit with Solar Lens",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ApplianceTag(
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun CompletedAuditSummaryCard(
    uiState: HomeUiState,
    onOpenSolarLens: () -> Unit,
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(HomeColors.PrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = HomeColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Appliance Audit Completed",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HomeColors.TextPrimary
                        )
                        val applianceCount = uiState.appliances.size
                        val kwText = String.format(Locale.getDefault(), "%.1f", uiState.totalConnectedLoadKw)
                        Text(
                            text = "$applianceCount appliances cataloged • $kwText kW connected load",
                            fontSize = 11.sp,
                            color = HomeColors.TextSecondary
                        )
                    }
                }

                TextButton(
                    onClick = onOpenSolarLens,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Edit Audit →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HomeColors.Primary
                    )
                }
            }

            Text(
                text = "Your household appliance load has been analyzed. Sizing recommendations now incorporate your actual daytime vs nighttime load profile.",
                fontSize = 11.sp,
                color = HomeColors.TextSecondary,
                lineHeight = 15.sp
            )
        }
    }
}

// ─── 4. REAP Classification Card ──────────────────────────────────────────────

@Composable
private fun ReapClassificationCard(
    uiState: HomeUiState,
    onLearnMore: () -> Unit
) {
    val isPendingOrCalibrating = uiState.currentOnboardingStep < 3 || uiState.reapTier == ReapTier.PENDING

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
                        text = if (isPendingOrCalibrating) "Your Energy Profile — Pending" else "Your Energy Profile",
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
                        text = if (isPendingOrCalibrating) "Pending" else uiState.reapTier.code,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = HomeColors.Primary,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            if (isPendingOrCalibrating) {
                Text(
                    text = "Your profile will be calculated after we record your electricity consumption.",
                    fontSize = 12.sp,
                    color = HomeColors.TextSecondary,
                    lineHeight = 17.sp
                )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .subtleTopRightGradient(
                    shape = RoundedCornerShape(16.dp),
                    radiusMultiplier = 0.75f
                )
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
                            text = "Your bill updates, home checks, and savings progress will appear here.",
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

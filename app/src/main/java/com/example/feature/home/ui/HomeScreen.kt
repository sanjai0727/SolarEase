package com.example.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.session.UserSession
import com.example.feature.home.ui.modules.MeterReadingProgressionGraph
import com.example.feature.home.ui.modules.PendingBillUiState
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.components.SolarEaseNavigationRail
import com.example.ui.theme.*
import java.util.Locale

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
                    Text("Complete Now", color = StitchPrimary, fontWeight = FontWeight.Bold)
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
                        Text(text = "EB Service Number: ${currentUser?.ebServiceNumber}", color = StitchPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showAccountPreview = false
                    onNavigateToProfile()
                }) {
                    Text("Manage Profile", color = StitchPrimary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAccountPreview = false }) {
                    Text("Close", color = StitchOnSurfaceVariant)
                }
            }
        )
    }

    val layoutInfo = rememberSolarEaseWindowLayoutInfo()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            StitchTopBar(
                userInitial = uiState.userInitial,
                onProfileClick = { showAccountPreview = true },
                onNotificationsClick = {}
            )
        },
        bottomBar = {
            if (!layoutInfo.useNavigationRail) {
                SolarEaseBottomNavigation(
                    currentTab = SolarEaseNavTab.HOME,
                    onNavigateToHome = {},
                    onNavigateToRecommends = onNavigateToRecommends,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (layoutInfo.useNavigationRail) {
                SolarEaseNavigationRail(
                    currentTab = SolarEaseNavTab.HOME,
                    onNavigateToHome = {},
                    onNavigateToRecommends = onNavigateToRecommends,
                    onNavigateToProfile = onNavigateToProfile
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = layoutInfo.maxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ── Modern Clean Welcoming Header ────────────────────────
                    ModernHomeHeader(
                        userName = uiState.userName,
                        hasConnectedBill = uiState.hasConnectedBill,
                        consumerNumber = uiState.consumerNumberFormatted.ifBlank { currentUser?.ebServiceNumber }
                    )

                    // ── Financial Action: Pending Electricity Bill / Status ───
                    when (val billState = uiState.pendingBillState) {
                        is PendingBillUiState.Pending -> {
                            PendingElectricityBillHeroCard(
                                state = billState,
                                onViewBillDetails = onNavigateToBillHistory
                            )
                        }
                        is PendingBillUiState.Loading -> {
                            BillStatusLoadingCard()
                        }
                        is PendingBillUiState.RetrievalFailed -> {
                            BillRetrievalFailedCard(
                                state = billState,
                                onCheckBillStatus = onNavigateToTnebBillStatus,
                                onRetry = { viewModel.retryBillFetch() }
                            )
                        }
                        is PendingBillUiState.Paid, PendingBillUiState.None -> {
                            // No warning displayed
                        }
                    }

                    // ── Hero Section (Bill Not Connected vs Active Energy Tracker) ──
                    if (!uiState.hasConnectedBill) {
                        ConnectBillHeroCard(
                            onConnectBill = onNavigateToTnebBillStatus,
                            onLearnMore = { showBillHelp = !showBillHelp }
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.dp,
                            onClick = { showBillHelp = !showBillHelp }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.Help, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Need help with your electricity bill?", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(if (showBillHelp) "Hide bill help" else "Find your service number and learn how TNEB slabs work", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(if (showBillHelp) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (showBillHelp) "Hide bill help" else "Show bill help", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        AnimatedVisibility(visible = showBillHelp, enter = fadeIn(animationSpec = tween(180)), exit = fadeOut(animationSpec = tween(120))) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                WhyConnectBillSection()
                                WhereToFindConsumerNumberCard()
                                ReapClassificationCard(uiState = uiState, onLearnMore = onNavigateToTnebBillStatus)
                            }
                        }
                    } else {
                        // Bill Connected: Synced Bill Summary + Primary Energy Meter Tracker
                        if (uiState.pendingBillState !is PendingBillUiState.Pending) {
                            SyncedBillSummaryStrip(
                                uiState = uiState,
                                onViewBillHistory = onNavigateToBillHistory,
                                onCheckDues = onNavigateToTnebBillStatus
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
                    }

                    // ── Core Utility Bento Grid (Always Accessible to All Users) ───
                    QuickToolsSection(
                        onBillStatus = onNavigateToTnebBillStatus,
                        onChatbot = onNavigateToElectricityChatbot,
                        onSolarLens = onNavigateToSolarEaseLens,
                        onCalculator = onNavigateToBillCalculator,
                        hasConnectedBill = uiState.hasConnectedBill,
                        tariffCode = uiState.tariffCode
                    )

                    // ── Solar Lens & Appliance Audit Feature Showcase ────────
                    if (uiState.hasCompletedAudit) {
                        CompletedAuditSummaryCard(
                            uiState = uiState,
                            onOpenSolarLens = onNavigateToSolarEaseLens
                        )
                    } else if (uiState.currentBillingCycleKwh > 0.0) {
                        KnowYourConsumptionCard(
                            uiState = uiState,
                            onOpenSolarLens = onNavigateToSolarEaseLens
                        )
                    } else {
                        SolarLensShowcaseCard(
                            onOpenSolarLens = onNavigateToSolarEaseLens
                        )
                    }

                    // ── Meter Progression Graph (When Readings Are Available) ─
                    if (uiState.hasConnectedBill && uiState.meterGraphPoints.isNotEmpty()) {
                        MeterReadingProgressionGraph(
                            points = uiState.meterGraphPoints,
                            billData = uiState.billData,
                            onEditReading = { viewModel.onInitiateEditReading(it) }
                        )
                    }

                    // ── Connected User Extra Actions & Activity ──────────────
                    if (uiState.hasConnectedBill) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .semantics {
                                    stateDescription = if (showInsightsAndTools) "Expanded" else "Collapsed"
                                },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                            shadowElevation = 0.dp,
                            onClick = { showInsightsAndTools = !showInsightsAndTools }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text("Usage insights & billing history", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                    Text(if (showInsightsAndTools) "Hide details" else "View tariff classification, past bills, and activity", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Icon(if (showInsightsAndTools) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = if (showInsightsAndTools) "Collapse insights" else "Expand insights", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        AnimatedVisibility(visible = showInsightsAndTools, enter = fadeIn(animationSpec = tween(180)), exit = fadeOut(animationSpec = tween(120))) {
                            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ReapClassificationCard(uiState = uiState, onLearnMore = onNavigateToTnebBillStatus)
                                BillActionsSection(onViewBillHistory = onNavigateToBillHistory, onCheckDues = onNavigateToTnebBillStatus)
                                if (uiState.recentActivities.isNotEmpty()) {
                                    RecentActivitySection(activities = uiState.recentActivities)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimary,
                            contentColor = StitchOnPrimary
                        ),
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
                                        colors = RadioButtonDefaults.colors(selectedColor = StitchPrimary)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimary,
                            contentColor = StitchOnPrimary
                        ),
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
                            color = StitchPrimary
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchPrimary,
                            contentColor = StitchOnPrimary
                        ),
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

// ─── Top Bar ──────────────────────────────────────────────────────────────────

@Composable
private fun StitchTopBar(
    userInitial: Char,
    onProfileClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Brand: SolarEase icon, App name, and HOME badge
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = "SolarEase",
                        tint = MaterialTheme.colorScheme.onPrimary,
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
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        letterSpacing = 0.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "HOME",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Notification & Profile Avatar
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = onNotificationsClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SolarEaseSemanticColors.WarningText)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .semantics(mergeDescendants = true) {
                            contentDescription = "Account details"
                            role = Role.Button
                        }
                        .clickable(role = Role.Button) { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = userInitial.toString(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

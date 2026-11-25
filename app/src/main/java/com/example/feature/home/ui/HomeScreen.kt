package com.example.feature.home.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraOutdoor
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.SolarPower
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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
    var showNewUserGuide by rememberSaveable { mutableStateOf(true) }
    var wasTodayReadingEntered by remember { mutableStateOf(uiState.isTodayReadingEntered) }
    var showExploreToolsPopup by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isTodayReadingEntered) {
        if (!wasTodayReadingEntered && uiState.isTodayReadingEntered && uiState.hasConnectedBill) {
            showExploreToolsPopup = true
        }
        wasTodayReadingEntered = uiState.isTodayReadingEntered
    }

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
            text = { Text("Add your email and service address to personalize your electricity bill tracking and slab alerts.", color = StitchOnSurfaceVariant) },
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

    if (showExploreToolsPopup) {
        val isDark = LocalIsDarkTheme.current
        AlertDialog(
            onDismissRequest = { showExploreToolsPopup = false },
            containerColor = if (isDark) MaterialTheme.colorScheme.surface else SolarBrandGlassCardBg,
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Reading Saved!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = StitchOnSurface
                        )
                        Text(
                            text = "Explore tools to analyze your usage",
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExploreToolSuggestionRow(
                        icon = Icons.Default.Calculate,
                        title = "Bill Calculator",
                        subtitle = "Estimate TNEB slab cost & units",
                        onClick = {
                            showExploreToolsPopup = false
                            onNavigateToBillCalculator()
                        }
                    )
                    ExploreToolSuggestionRow(
                        icon = Icons.Default.SmartToy,
                        title = "Energy AI Chatbot",
                        subtitle = "Ask about tariffs, slabs & savings",
                        onClick = {
                            showExploreToolsPopup = false
                            onNavigateToElectricityChatbot()
                        }
                    )
                    ExploreToolSuggestionRow(
                        icon = Icons.Default.CameraOutdoor,
                        title = "Appliance Audit",
                        subtitle = "Identify heavy power-drawing devices",
                        onClick = {
                            showExploreToolsPopup = false
                            onNavigateToSolarEaseLens()
                        }
                    )
                    ExploreToolSuggestionRow(
                        icon = Icons.Default.ReceiptLong,
                        title = "Bill Status & Ledger",
                        subtitle = "Check dues & past billing history",
                        onClick = {
                            showExploreToolsPopup = false
                            onNavigateToTnebBillStatus()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showExploreToolsPopup = false }) {
                    Text(
                        text = "Maybe Later",
                        color = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    if (showAccountPreview) {
        AlertDialog(
            onDismissRequest = { showAccountPreview = false },
            title = { Text("Account Details", fontWeight = FontWeight.SemiBold, color = StitchOnSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val displayName = currentUser?.name?.takeIf { it.isNotBlank() }
                        ?: currentUser?.username?.takeIf { it.isNotBlank() }
                        ?: "Guest"
                    val serviceNumber = currentUser?.ebServiceNumber?.takeIf { it.isNotBlank() }
                        ?: uiState.consumerNumberFormatted.takeIf { it.isNotBlank() }
                        ?: "Not Connected"

                    Text(
                        text = "Name: $displayName",
                        fontWeight = FontWeight.Medium,
                        color = StitchOnSurface,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Service Number: $serviceNumber",
                        color = if (uiState.hasConnectedBill) StitchPrimary else StitchOnSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }
            },
            confirmButton = {
                if (uiState.hasConnectedBill) {
                    TextButton(onClick = {
                        showAccountPreview = false
                        onNavigateToProfile()
                    }) {
                        Text("Manage Profile", color = StitchPrimary, fontWeight = FontWeight.Bold)
                    }
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
        containerColor = if (LocalIsDarkTheme.current) MaterialTheme.colorScheme.background else SolarBrandBackground,
        topBar = {
            StitchTopBar(
                userInitial = uiState.userInitial,
                onProfileClick = { showAccountPreview = true },
                onNotificationsClick = {}
            )
        },
        bottomBar = {
            if (uiState.hasConnectedBill && !layoutInfo.useNavigationRail) {
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
            if (uiState.hasConnectedBill && layoutInfo.useNavigationRail) {
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

                    val currentPendingState = uiState.pendingBillState

                    if (!uiState.hasConnectedBill) {
                        // ════════════════════════════════════════════════════════════
                        // ── 1. FIRST-TIME USER — BILL NOT CONNECTED ──────────────────
                        // ════════════════════════════════════════════════════════════

                        // Optional connection / retrieval failure alert
                        if (currentPendingState is PendingBillUiState.RetrievalFailed) {
                            BillRetrievalFailedCard(
                                state = currentPendingState,
                                onCheckBillStatus = onNavigateToTnebBillStatus,
                                onRetry = { viewModel.retryBillFetch() }
                            )
                        }

                        // ── 3. Connect Your Electricity Bill Hero Card ──────────────
                        StaggeredHomeItem(index = 0) {
                            ConnectBillHeroCard(
                                onConnectBill = onNavigateToTnebBillStatus,
                                onLearnMore = { showBillHelp = !showBillHelp }
                            )
                        }

                        // ── 4. Need Help With Your Electricity Bill (Expandable) ─────
                        StaggeredHomeItem(index = 1) {
                            val helpChevronRotation by animateFloatAsState(
                                targetValue = if (showBillHelp) 180f else 0f,
                                animationSpec = motionTween(durationMillis = 200),
                                label = "billHelpChevron"
                            )
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .semantics {
                                        stateDescription = if (showBillHelp) "Expanded" else "Collapsed"
                                    },
                                shape = RoundedCornerShape(20.dp),
                                color = OceanGlassCardBg,
                                border = BorderStroke(1.dp, OceanGlassCardBorder),
                                shadowElevation = 0.dp,
                                onClick = { showBillHelp = !showBillHelp }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 18.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(OceanAquaContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Help,
                                            contentDescription = null,
                                            tint = OceanTeal,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "Need help with your electricity bill?",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = OceanDeepNavy
                                        )
                                        Text(
                                            text = if (showBillHelp) "Hide bill help" else "Find your service number and learn how TNEB slabs work",
                                            fontSize = 12.sp,
                                            color = OceanTextSecondary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ExpandMore,
                                        contentDescription = if (showBillHelp) "Hide bill help" else "Show bill help",
                                        tint = OceanTeal,
                                        modifier = Modifier.graphicsLayer { rotationZ = helpChevronRotation }
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = showBillHelp,
                            enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                            exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                WhyConnectBillSection()
                                WhereToFindConsumerNumberCard()
                            }
                        }

                        // ── 5. New User Quick Start Guide ───────────────────────────
                        StaggeredHomeItem(index = 2) {
                            NewUserQuickStartGuideCard(
                                isExpanded = showNewUserGuide,
                                onToggleExpand = { showNewUserGuide = !showNewUserGuide },
                                onConnectBill = onNavigateToTnebBillStatus
                            )
                        }

                        // IMPORTANT: On first-time screen, hide completely:
                        // - Explore Tools
                        // - Meter reading & entry
                        // - Usage insights graph
                        // - Bill & payment actions
                        // - Find where your electricity goes card
                        // - Recent activity

                    } else {
                        // ════════════════════════════════════════════════════════════
                        // ── 2. EXISTING USER — BILL CONNECTED ────────────────────────
                        // ════════════════════════════════════════════════════════════

                        // Optional bill retrieval failure state
                        if (currentPendingState is PendingBillUiState.RetrievalFailed) {
                            BillRetrievalFailedCard(
                                state = currentPendingState,
                                onCheckBillStatus = onNavigateToTnebBillStatus,
                                onRetry = { viewModel.retryBillFetch() }
                            )
                        }

                        // ── 3. Hero Card: Previous Bill Consumption & Dues ───────────
                        StaggeredHomeItem(index = 0) {
                            PreviousBillConsumptionHeroCard(
                                uiState = uiState,
                                onViewBillDetails = onNavigateToBillHistory
                            )
                        }

                        // ── 4. Today's Meter Reading & Entry ─────────────────────────
                        StaggeredHomeItem(index = 1) {
                            HomeMeterReadingEntryCard(
                                uiState = uiState,
                                onMeterInputChanged = { viewModel.onMeterInputChanged(it) },
                                onSaveMeterReading = { viewModel.saveCurrentMeterReading() },
                                onUpdateTodayReading = { viewModel.updateTodayMeterReading() },
                                onStartEditingReading = { viewModel.startEditingReading() },
                                onCancelEditingReading = { viewModel.cancelEditingReading() },
                                onScanMeterReading = onNavigateToMeterScanner
                            )
                        }

                        // ── 5. Usage Insights: Consumption Graph ─────────────────────
                        StaggeredHomeItem(index = 2) {
                            MeterReadingProgressionGraph(
                                points = uiState.meterGraphPoints,
                                billData = uiState.billData,
                                onEditReading = { viewModel.onInitiateEditReading(it) },
                                onViewReadingDetails = onNavigateToBillHistory
                            )
                        }

                        // ── 6. Bill and Payment Actions ──────────────────────────────
                        StaggeredHomeItem(index = 3) {
                            BillActionsSection(
                                onViewBillHistory = onNavigateToBillHistory,
                                onCheckDues = onNavigateToTnebBillStatus
                            )
                        }

                        // ── 7. Find Where Your Previous Billed Consumption Went ──────
                        StaggeredHomeItem(index = 4) {
                            KnowYourConsumptionCard(
                                uiState = uiState,
                                onOpenSolarLens = onNavigateToSolarEaseLens
                            )
                        }

                        // ── 8. Explore Tools: Collapsible Dropdown (Default Collapsed)
                        StaggeredHomeItem(index = 5) {
                            CollapsibleExploreToolsSection(
                                onBillStatus = onNavigateToTnebBillStatus,
                                onChatbot = onNavigateToElectricityChatbot,
                                onSolarLens = onNavigateToSolarEaseLens,
                                onCalculator = onNavigateToBillCalculator,
                                hasConnectedBill = true,
                                tariffCode = uiState.tariffCode
                            )
                        }

                        // Completely removed:
                        // - Recent Activity section
                        // - Separate TNEB Bill Linked card
                        // - Energy Conservation Home card
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
                        painter = androidx.compose.ui.res.painterResource(com.example.R.drawable.ic_solarease_logo),
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

/**
 * Accessibility-aware staggered entrance wrapper for Home cards.
 * Gently fades and slides content into place on initial composition.
 */
@Composable
private fun StaggeredHomeItem(
    index: Int,
    content: @Composable () -> Unit
) {
    val isReducedMotion = LocalIsReducedMotionEnabled.current
    var visible by remember { mutableStateOf(isReducedMotion) }
    LaunchedEffect(Unit) {
        if (!isReducedMotion) {
            visible = true
        }
    }

    if (isReducedMotion) {
        content()
    } else {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(
                    durationMillis = 280,
                    delayMillis = (index * 55).coerceAtMost(300),
                    easing = SolarEaseMotion.easingDecelerate
                )
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = (index * 55).coerceAtMost(300),
                    easing = SolarEaseMotion.easingDecelerate
                ),
                initialOffsetY = { it / 10 }
            )
        ) {
            content()
        }
    }
}

@Composable
private fun ExploreToolSuggestionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val accentGreen = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) MaterialTheme.colorScheme.surfaceVariant else SolarBrandGreenLight.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, if (isDark) MaterialTheme.colorScheme.outlineVariant else SolarBrandGlassCardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else Color(0xFFFCFDF9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentGreen,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = StitchOnSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = accentGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun NewUserQuickStartGuideCard(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onConnectBill: () -> Unit
) {
    val isDark = LocalIsDarkTheme.current
    val accentGreen = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
    val cardGradient = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF102A22), Color(0xFF0C1D18)))
    } else {
        Brush.linearGradient(listOf(Color(0xFFFCFDF9), Color(0xFFF1F7F1)))
    }
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = motionTween(durationMillis = 200),
        label = "guideChevron"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = OceanGlassCardBg,
        border = BorderStroke(1.dp, OceanGlassCardBorder),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(OceanAquaContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = null,
                        tint = accentGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "New User Quick Start Guide",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = OceanDeepNavy
                    )
                    Text(
                        text = "3 simple steps to monitor bills & save power",
                        fontSize = 12.sp,
                        color = OceanTextSecondary
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Hide guide" else "Show guide",
                    tint = accentGreen,
                    modifier = Modifier.graphicsLayer { rotationZ = chevronRotation }
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                exit = shrinkVertically(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    GuideStepRow(
                        step = "1",
                        title = "Connect Your TNEB Bill",
                        description = "Link your 11–12 digit consumer number to fetch your tariff schedule and unlock Recommends & Profile."
                    )
                    GuideStepRow(
                        step = "2",
                        title = "Log Daily Meter Readings",
                        description = "Type or scan your meter kWh reading daily to view your weekly graph and avoid high slab jumps."
                    )
                    GuideStepRow(
                        step = "3",
                        title = "Use Smart Tools & Guidelines",
                        description = "Estimate slab costs in Calculator, audit heavy appliances, and check off daily Energy Mantras."
                    )

                    Button(
                        onClick = onConnectBill,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Start Step 1: Connect TNEB Bill →",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GuideStepRow(
    step: String,
    title: String,
    description: String
) {
    val isDark = LocalIsDarkTheme.current
    val accentGreen = if (isDark) MaterialTheme.colorScheme.primary else SolarBrandGreen
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isDark) MaterialTheme.colorScheme.surfaceVariant else Color(0xFFFCFDF9))
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (isDark) MaterialTheme.colorScheme.primaryContainer else SolarBrandGreenLight),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentGreen
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface
            )
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = StitchOnSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}

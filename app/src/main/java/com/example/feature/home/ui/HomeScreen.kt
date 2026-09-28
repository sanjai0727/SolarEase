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
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.session.UserSession
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
        containerColor = StitchSurface,
        bottomBar = {
            StitchBottomNavigation(
                onHome = {},
                onSolarLens = onNavigateToSolarEaseLens,
                onCalculator = onNavigateToBillCalculator,
                onProfile = onNavigateToProfile
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

                // ── 3. Primary Energy Status Card (Connected vs Zero-State) ─
                PrimaryEnergyCard(
                    uiState = uiState,
                    onConnectBill = onNavigateToTnebBillStatus,
                    onViewBillHistory = onNavigateToBillHistory
                )

                // ── 4. REAP Energy Profile / Household Classification ───
                ReapClassificationCard(
                    uiState = uiState,
                    onLearnMore = onNavigateToTnebBillStatus
                )

                // ── 5. Recommended Next Step Action Hero ────────────────
                RecommendedActionCard(
                    uiState = uiState,
                    onConnectBill = onNavigateToTnebBillStatus,
                    onOpenCalculator = onNavigateToBillCalculator,
                    onOpenSolarLens = onNavigateToSolarEaseLens,
                    onExploreSolar = onNavigateToSolarPanel
                )

                // ── 6. "Your Energy Journey" (6-Stage REAP Stepper) ─────
                ReapJourneyStepper(
                    activeStage = uiState.activeJourneyStage
                )

                // ── 7. Solar Lens AI Load Audit Discovery Card ──────────
                SolarLensHeroCard(
                    uiState = uiState,
                    onLaunchLens = onNavigateToSolarEaseLens
                )

                // ── 8. Quick Access Tools (2x2 Grid) ────────────────────
                QuickToolsSection(
                    onBillStatus = onNavigateToTnebBillStatus,
                    onCalculator = onNavigateToBillCalculator,
                    onSolarLens = onNavigateToSolarEaseLens,
                    onChatbot = onNavigateToElectricityChatbot,
                    hasConnectedBill = uiState.hasConnectedBill
                )

                // ── 9. Recent Household Activity / Event Log ────────────
                RecentActivitySection(
                    activities = uiState.recentActivities
                )

                Spacer(modifier = Modifier.height(24.dp))
            }
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
        color = StitchSurface.copy(alpha = 0.95f),
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
                        .background(StitchPrimaryContainer),
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
                            color = StitchOnSurface,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(StitchPrimaryFixed.copy(alpha = 0.6f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "HOME",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnPrimaryFixedVariant,
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
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = userLocation,
                                fontSize = 11.sp,
                                color = StitchOnSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "REAP Solar Intelligence",
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant,
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
                            tint = StitchOnSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(StitchPrimaryContainer)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(StitchPrimary)
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userInitial.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnPrimary
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
                            .background(StitchPrimaryContainer)
                            .alpha(alphaAnim)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasConnectedBill) "SITE OVERVIEW" else "REAP HOUSEHOLD ONBOARDING",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchPrimaryContainer,
                        letterSpacing = 0.8.sp
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (!userName.isNullOrBlank()) "$greeting, $userName" else "$greeting 👋",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.5).sp
                )
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(StitchSurfaceContainerLow),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = StitchPrimaryContainer,
                    modifier = Modifier.size(20.dp)
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
            color = StitchOnSurfaceVariant,
            lineHeight = 18.sp
        )
    }
}

// ─── 3. Primary Energy Card ────────────────────────────────────────────────────

@Composable
private fun PrimaryEnergyCard(
    uiState: HomeUiState,
    onConnectBill: () -> Unit,
    onViewBillHistory: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
        shadowElevation = 0.dp
    ) {
        if (!uiState.hasConnectedBill || uiState.unitsConsumed == null) {
            // Stitch New User Standby Zero-State
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(StitchSurfaceContainerLow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricMeter,
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = "Your Home Energy",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StitchSurfaceContainerHigh)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Standby",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchOnSurfaceVariant
                        )
                    }
                }

                // Illustrated zero-state inner slot
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceContainerLow
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
                                .background(StitchSurfaceContainerLowest),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ElectricMeter,
                                contentDescription = null,
                                tint = StitchPrimaryContainer,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No electricity data connected yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Connect your consumer service number to reveal your bi-monthly tariff slabs, actual consumption, and peak load profile.",
                            fontSize = 12.sp,
                            color = StitchOnSurfaceVariant,
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
                        containerColor = StitchPrimaryContainer,
                        contentColor = StitchOnPrimary
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
        } else {
            // Real TNEB Synced State
            val units = uiState.unitsConsumed
            val dailyAvg = String.format(Locale.getDefault(), "%.1f", units / 60.0)

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
                    Text(
                        text = "YOUR HOME ENERGY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurfaceVariant,
                        letterSpacing = 0.8.sp
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(StitchPrimaryFixed)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(StitchPrimary)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "TNEB SYNCED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnPrimaryFixedVariant,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // Numbers Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (units % 1.0 == 0.0) units.toInt().toString() else String.format(Locale.getDefault(), "%.1f", units),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface,
                            letterSpacing = (-1).sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "kWh",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Normal,
                            color = StitchOnSurfaceVariant
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = uiState.amountDue ?: "₹0.00",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isPaid) StitchPrimaryContainer else StitchError
                        )
                        Text(
                            text = if (uiState.isPaid) "Paid • Nil Dues" else (uiState.dueDate?.let { "Due $it" } ?: "Current Bill"),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (uiState.isPaid) StitchPrimaryContainer else StitchError
                        )
                    }
                }

                // Progress Track
                val progressFraction = (units / 1200.0).toFloat().coerceIn(0.1f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(StitchSurfaceContainerHigh)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(StitchPrimaryContainer)
                    )
                }

                // Cycle and daily rate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = uiState.billingPeriod?.let { "Cycle: $it" } ?: "Bi-monthly billing cycle",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant
                    )
                    Text(
                        text = "$dailyAvg kWh/day",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                }

                // Tariff Slab Slot
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchSurfaceContainerLow)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TARIFF SLAB",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurfaceVariant,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = uiState.tariffCategory,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                    }
                }

                // Action to view details
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
                        border = BorderStroke(1.dp, StitchOutline)
                    ) {
                        Text(
                            text = "View Bill History",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StitchOnSurface
                        )
                    }

                    Button(
                        onClick = onConnectBill,
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchSurfaceContainerLow,
                            contentColor = StitchPrimaryContainer
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
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        tint = StitchPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Your Energy Profile",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = StitchOnSurface
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (uiState.reapTier == ReapTier.PENDING) StitchSecondaryContainer else StitchPrimaryFixed)
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (uiState.reapTier == ReapTier.PENDING) "Classification Pending" else uiState.reapTier.code,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (uiState.reapTier == ReapTier.PENDING) StitchSecondary else StitchOnPrimaryFixedVariant
                    )
                }
            }

            if (uiState.reapTier == ReapTier.PENDING) {
                Text(
                    text = "Your home will be classified into one of the 4 REAP benchmark tiers based on your recorded bi-monthly cycle units.",
                    fontSize = 12.sp,
                    color = StitchOnSurfaceVariant,
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
                            tint = StitchPrimaryContainer
                        )
                        ReapTierChip(
                            modifier = Modifier.weight(1f),
                            code = "EEH",
                            range = "500–800 u",
                            title = "Efficiency",
                            subtitle = "Targeted Optimization",
                            tint = StitchTertiary
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
                            tint = StitchSecondary
                        )
                        ReapTierChip(
                            modifier = Modifier.weight(1f),
                            code = "ETH",
                            range = ">1000 u",
                            title = "Transition",
                            subtitle = "High Solar Yield",
                            tint = StitchPrimary
                        )
                    }
                }
            } else {
                // Connected Tier Breakdown
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = uiState.reapTier.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = uiState.reapTier.description,
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    if (uiState.reapTier.targetUnits != null && uiState.unitsConsumed != null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            color = StitchSurfaceContainerLow
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
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StitchOnSurface
                                    )
                                    Text(
                                        text = "Target: ${uiState.reapTier.targetUnits.toInt()} kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = StitchPrimaryContainer
                                    )
                                }
                                val targetFraction = (uiState.reapTier.targetUnits / uiState.unitsConsumed).toFloat().coerceIn(0.1f, 1f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(StitchSurfaceContainerHigh)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(targetFraction)
                                            .fillMaxHeight()
                                            .background(StitchPrimaryContainer)
                                    )
                                }
                                Text(
                                    text = "Targeting ${uiState.reapTier.targetUnits.toInt()} kWh brings your home into the lower tariff slab, unlocking subsidized baseline units.",
                                    fontSize = 11.sp,
                                    color = StitchOnSurfaceVariant,
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
        color = StitchSurfaceContainerLow
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
                Text(text = range, fontSize = 10.sp, color = StitchOnSurfaceVariant)
            }
            Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
            Text(text = subtitle, fontSize = 10.sp, color = StitchOnSurfaceVariant, maxLines = 1)
        }
    }
}

// ─── 5. Recommended Action Hero Card ──────────────────────────────────────────

@Composable
private fun RecommendedActionCard(
    uiState: HomeUiState,
    onConnectBill: () -> Unit,
    onOpenCalculator: () -> Unit,
    onOpenSolarLens: () -> Unit,
    onExploreSolar: () -> Unit
) {
    when {
        // Step 1: Measure Baseline (New User)
        !uiState.hasConnectedBill -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLow,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECOMMENDED ACTION",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StitchPrimaryFixed)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Step 01",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnPrimaryFixedVariant
                            )
                        }
                    }

                    Text(
                        text = "Measure Your Baseline",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Retrieve your latest consumer receipt or calculate your estimated monthly units to establish your home's initial energy baseline.",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onConnectBill,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = StitchPrimaryContainer,
                                contentColor = StitchOnPrimary
                            )
                        ) {
                            Text("Connect Bill", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = onOpenCalculator,
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, StitchOutline)
                        ) {
                            Text("Use Calculator", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                        }
                    }
                }
            }
        }

        // Step 2: Reduce Loads (Bill Connected, Audit Pending)
        !uiState.hasCompletedAudit -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerHigh,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECOMMENDED NEXT STEP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryContainer,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StitchPrimaryFixed)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "02 Reduce",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchOnPrimaryFixedVariant
                            )
                        }
                    }

                    Text(
                        text = "Find where your ${uiState.unitsConsumed?.toInt() ?: ""} kWh is going",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface
                    )
                    Text(
                        text = "Run an appliance audit to discover which heavy loads (AC, water heater, pump) can be optimized to lower your bill into the next lower tariff slab.",
                        fontSize = 12.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 17.sp
                    )

                    Button(
                        onClick = onOpenSolarLens,
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
                            Text("Start Appliance Audit with Solar Lens", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Step 3 & 4: Optimise & Generate (Audit Completed)
        else -> {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchPrimary,
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "03 OPTIMISE & 04 GENERATE",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchPrimaryFixed,
                            letterSpacing = 0.8.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "HIGHEST ROI",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    Text(
                        text = "Size Solar for your optimized baseline",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    val capacityStr = uiState.recommendedSolarCapacityKw?.let { String.format(Locale.getDefault(), "%.1f kW", it) } ?: "Rooftop"
                    Text(
                        text = "Sizing solar after appliance profiling recommends a $capacityStr array, maximizing generation self-consumption and reducing payback period.",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        lineHeight = 17.sp
                    )

                    Button(
                        onClick = onExploreSolar,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StitchSurfaceContainerLowest,
                            contentColor = StitchPrimary
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Explore Custom Solar System", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// ─── 6. REAP Energy Journey Stepper ───────────────────────────────────────────

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

// ─── 8. Quick Tools Section (2x2 Grid) ─────────────────────────────────────────

@Composable
private fun QuickToolsSection(
    onBillStatus: () -> Unit,
    onCalculator: () -> Unit,
    onSolarLens: () -> Unit,
    onChatbot: () -> Unit,
    hasConnectedBill: Boolean
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
                color = StitchOnSurface
            )
            Text(
                text = "QUICK ACCESS",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurfaceVariant,
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
                subtitle = if (hasConnectedBill) "TNEB LT-1A" else "Check dues & units",
                icon = Icons.Default.ReceiptLong,
                iconTint = StitchPrimaryContainer,
                onClick = onBillStatus
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Calculator",
                subtitle = "Estimate monthly cost",
                icon = Icons.Default.Calculate,
                iconTint = StitchTertiary,
                onClick = onCalculator
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Energy Audit",
                subtitle = "Scan appliance loads",
                icon = Icons.Default.CameraOutdoor,
                iconTint = StitchPrimaryContainer,
                onClick = onSolarLens
            )
            QuickToolCard(
                modifier = Modifier.weight(1f),
                title = "Chatbot",
                subtitle = "Ask Energy AI",
                icon = Icons.Default.SmartToy,
                iconTint = StitchSecondary,
                onClick = onChatbot
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
    iconTint: Color,
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
        shape = RoundedCornerShape(14.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
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
                    .background(StitchSurfaceContainerLow),
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
                color = StitchOnSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 11.sp,
                color = StitchOnSurfaceVariant,
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
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
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
                    color = StitchOnSurface
                )
                Text(
                    text = "LOG",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurfaceVariant,
                    letterSpacing = 0.8.sp
                )
            }

            if (activities.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = StitchSurfaceContainerLow
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
                            color = StitchOnSurface
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Your connected bill cycles, appliance audits, and savings milestones will populate here automatically.",
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant,
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
                                        .background(if (event.isPositive) StitchPrimaryContainer else StitchError)
                                )
                                Column {
                                    Text(
                                        text = event.title,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = StitchOnSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${event.subtitle} • ${event.timeLabel}",
                                        fontSize = 11.sp,
                                        color = StitchOnSurfaceVariant,
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
                                    color = if (event.isPositive) StitchPrimaryContainer else StitchOnSurface
                                )
                            }
                        }

                        if (index < activities.lastIndex) {
                            HorizontalDivider(
                                color = StitchSurfaceContainer,
                                thickness = 1.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 10. Stitch Bottom Navigation ─────────────────────────────────────────────

@Composable
private fun StitchBottomNavigation(
    onHome: () -> Unit,
    onSolarLens: () -> Unit,
    onCalculator: () -> Unit,
    onProfile: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StitchSurface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, StitchOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StitchNavItem(
                icon = Icons.Default.Home,
                label = "Home",
                isSelected = true,
                onClick = onHome
            )
            StitchNavItem(
                icon = Icons.Default.CameraOutdoor,
                label = "Solar Lens",
                isSelected = false,
                onClick = onSolarLens
            )
            StitchNavItem(
                icon = Icons.Default.Calculate,
                label = "Calculator",
                isSelected = false,
                onClick = onCalculator
            )
            StitchNavItem(
                icon = Icons.Default.Person,
                label = "Profile",
                isSelected = false,
                onClick = onProfile
            )
        }
    }
}

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

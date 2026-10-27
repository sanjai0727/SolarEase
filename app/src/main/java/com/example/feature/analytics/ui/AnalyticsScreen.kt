package com.example.feature.analytics.ui

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.analytics.model.*
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.max

// Stitch Color Palette Accents
private val StitchBlueAccent = Color(0xFF2563EB)
private val StitchBlueContainer = Color(0xFFEFF4FF)
private val StitchEmeraldGreen = Color(0xFF047857)
private val StitchEmeraldContainer = Color(0xFFECFDF5)
private val StitchAmberAccent = Color(0xFFD97706)
private val StitchAmberContainer = Color(0xFFFFFBEB)
private val StitchPurpleAccent = Color(0xFF7C3AED)
private val StitchPurpleContainer = Color(0xFFF5F3FF)

@Composable
fun AnalyticsRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTnebBillStatus: () -> Unit,
    onNavigateToLogMeterReading: () -> Unit,
    viewModel: AnalyticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreen(
        uiState = uiState,
        onRangeSelected = { viewModel.setRange(it) },
        onDailyPointSelected = { viewModel.selectDailyPoint(it) },
        onNavigateToHome = onNavigateToHome,
        onNavigateToRecommends = onNavigateToRecommends,
        onNavigateToProfile = onNavigateToProfile,
        onNavigateToTnebBillStatus = onNavigateToTnebBillStatus,
        onNavigateToLogMeterReading = onNavigateToLogMeterReading
    )
}

@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onDailyPointSelected: (DailyUsagePoint?) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToRecommends: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToTnebBillStatus: () -> Unit,
    onNavigateToLogMeterReading: () -> Unit
) {
    Scaffold(
        containerColor = StitchSurface,
        topBar = {
            AnalyticsTopBar(
                userLocation = uiState.userLocation,
                onProfileClick = onNavigateToProfile
            )
        },
        bottomBar = {
            SolarEaseBottomNavigation(
                currentTab = SolarEaseNavTab.ANALYTICS,
                onNavigateToHome = onNavigateToHome,
                onNavigateToAnalytics = { /* Already on Analytics */ },
                onNavigateToRecommends = onNavigateToRecommends,
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = StitchPrimaryContainer)
                }
            } else if (!uiState.hasData) {
                EmptyAnalyticsView(
                    onConnectBill = onNavigateToTnebBillStatus,
                    onLogMeter = onNavigateToLogMeterReading
                )
            } else {
                PopulatedAnalyticsView(
                    uiState = uiState,
                    onRangeSelected = onRangeSelected,
                    onDailyPointSelected = onDailyPointSelected
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR COMPONENT
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AnalyticsTopBar(
    userLocation: String,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = StitchSurface.copy(alpha = 0.95f),
        border = BorderStroke(1.dp, StitchOutline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(StitchPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = "SolarEase",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SolarEase",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        letterSpacing = (-0.3).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = StitchPrimary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = userLocation,
                            fontSize = 11.sp,
                            color = StitchSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(StitchPrimary)
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// 1. EMPTY / NOT CONNECTED STATE (Exact Stitch Implementation)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyAnalyticsView(
    onConnectBill: () -> Unit,
    onLogMeter: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Analytics Context & Category Header
        Column(modifier = Modifier.padding(top = 2.dp, bottom = 2.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StitchPrimaryContainer)
                )
                Text(
                    text = "TNEB PORTAL SYNC • READY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchPrimaryContainer,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "Energy Analytics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StitchOnSurface,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Understand how your home's electricity usage is changing.",
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant
            )
        }

        // Hero Empty State Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StitchSurfaceContainerLowest,
            border = BorderStroke(1.dp, StitchOutline),
            shadowElevation = 0.5.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Central Badge (Circle 64dp with QueryStats & Bolt badge)
                Box(
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF005683)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QueryStats,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .clip(CircleShape)
                            .background(StitchPrimaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                Text(
                    text = "Your energy analytics will appear here",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Connect your electricity bill and start recording meter readings to build your consumption history and unlock seasonal trend analysis.",
                    fontSize = 13.sp,
                    color = StitchOnSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Action Group
                Button(
                    onClick = onConnectBill,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryContainer)
                ) {
                    Text("Connect Electricity Bill", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }

                OutlinedButton(
                    onClick = onLogMeter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, StitchOutline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = StitchOnSurface)
                ) {
                    Icon(Icons.Default.Speed, contentDescription = null, tint = StitchPrimaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Log First Meter Reading", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(Icons.Default.Verified, contentDescription = null, tint = StitchPrimaryContainer, modifier = Modifier.size(14.dp))
                    Text(
                        text = "Standard Tamil Nadu (TNEB) Tariff Format Supported",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
            }
        }

        // Teaser / Forecast Preview Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = StitchSurfaceContainerLowest,
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = null, tint = Color(0xFF005683), modifier = Modifier.size(16.dp))
                        Text(
                            text = "TELEMETRY FORECAST PREVIEW",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StitchOnSurface,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StitchSurfaceContainerHigh
                    ) {
                        Text(
                            text = "Sample Data",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = StitchOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(StitchSurfaceContainerLow),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val path = Path().apply {
                            moveTo(0f, size.height * 0.75f)
                            cubicTo(
                                size.width * 0.25f, size.height * 0.55f,
                                size.width * 0.5f, size.height * 0.75f,
                                size.width * 0.75f, size.height * 0.25f
                            )
                            lineTo(size.width, size.height * 0.35f)
                        }
                        drawPath(
                            path = path,
                            color = StitchPrimaryContainer.copy(alpha = 0.35f),
                            style = Stroke(width = 2.5f)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = StitchPrimaryContainer, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("12-Month Consumer Archive", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "Connect your TNEB consumer number to automatically import past 12-month billing records.",
                            fontSize = 11.sp,
                            color = StitchOnSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // "What you'll see here" Section (3 items matching Stitch)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("What you'll see here", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                Text("ZERO OVERHEAD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StitchPrimaryContainer, letterSpacing = 0.8.sp)
            }

            EmptyBenefitCard(
                icon = Icons.Default.CalendarViewWeek,
                title = "Bi-Monthly Consumption Trends",
                description = "Track exactly how many units you consume each cycle with automated bi-monthly differential curves.",
                iconBg = Color(0xFF97F5CC),
                iconTint = Color(0xFF047857)
            )

            EmptyBenefitCard(
                icon = Icons.Default.ReceiptLong,
                title = "Bill Amount vs. Usage",
                description = "See how appliance changes impact your bi-monthly electricity bill across shifting seasonal rate slabs.",
                iconBg = Color(0xFFDAE2FD),
                iconTint = Color(0xFF2563EB)
            )

            EmptyBenefitCard(
                icon = Icons.Default.Insights,
                title = "Personalized Energy Insights",
                description = "Pinpoint high-consumption periods and identify seasonal spikes before your meter cycles.",
                iconBg = Color(0xFFCCE5FF),
                iconTint = Color(0xFF005683)
            )
        }

        // Verified Security Banner (Stitch bottom banner)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = StitchSurfaceContainerLow
        ) {
            Row(
                modifier = Modifier.padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(StitchSurfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = StitchOnSurfaceVariant, modifier = Modifier.size(16.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Privacy First Storage", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                    Text(
                        "Your billing identifiers are locally encrypted and used solely for calculating rooftop solar ROI.",
                        fontSize = 11.sp,
                        color = StitchOnSurfaceVariant,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun EmptyBenefitCard(
    icon: ImageVector,
    title: String,
    description: String,
    iconBg: Color,
    iconTint: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(description, fontSize = 12.sp, color = StitchOnSurfaceVariant, lineHeight = 16.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 2. POPULATED / REAL DATA STATE (Deep Analysis)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PopulatedAnalyticsView(
    uiState: AnalyticsUiState,
    onRangeSelected: (AnalyticsRange) -> Unit,
    onDailyPointSelected: (DailyUsagePoint?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Header & Range Switcher
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Energy Analytics",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = StitchOnSurface,
                        letterSpacing = (-0.3).sp
                    )
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(StitchBlueContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = StitchBlueAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = StitchSurfaceContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(12.dp))
                        Text("Updated today", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = StitchOnSurfaceVariant)
                    }
                }
            }

            Text(
                text = "Understand how your home's electricity usage is changing.",
                fontSize = 13.sp,
                color = StitchOnSurfaceVariant
            )

            // Range Segmented Switcher (2M | 4M | 6M Active)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = StitchSurfaceContainerLow
            ) {
                Row(modifier = Modifier.padding(2.dp)) {
                    AnalyticsRange.entries.forEach { range ->
                        val isSelected = uiState.selectedRange == range
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) StitchSurfaceContainerLowest else Color.Transparent)
                                .clickable { onRangeSelected(range) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range.label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) StitchBlueAccent else StitchOnSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Section 2: Current Cycle Overview (Deep-Analysis Card)
        val cycle = uiState.currentCycle
        if (cycle != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ElectricMeter, contentDescription = null, tint = StitchBlueAccent, modifier = Modifier.size(18.dp))
                                Text(cycle.periodDisplay, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                                Surface(shape = RoundedCornerShape(4.dp), color = StitchBlueContainer) {
                                    Text("Active Cycle", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StitchBlueAccent, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                }
                            }
                            Text(
                                text = "Previous Paid Bill (${cycle.prevBillDate}) → Current Meter Reading (${cycle.currentDate})",
                                fontSize = 10.sp,
                                color = StitchSecondary
                            )
                        }
                        Text(cycle.tariffCategory, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = StitchOnSurfaceVariant)
                    }

                    // Key metrics 4-grid (gap-space-xs = 4dp)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MetricBox(
                                modifier = Modifier.weight(1f),
                                title = "CONSUMPTION",
                                value = "${cycle.unitsConsumed.toInt()}",
                                unit = "kWh",
                                subtitle = "${cycle.daysElapsed} of 60 days recorded",
                                icon = Icons.Default.Bolt,
                                accentColor = StitchBlueAccent,
                                containerColor = StitchBlueContainer
                            )
                            MetricBox(
                                modifier = Modifier.weight(1f),
                                title = "DAILY AVERAGE",
                                value = String.format(Locale.US, "%.1f", cycle.avgUnitsPerDay),
                                unit = "kWh/day",
                                subtitle = "Normal consumption pace",
                                icon = Icons.Default.Speed,
                                accentColor = StitchEmeraldGreen,
                                containerColor = StitchEmeraldContainer
                            )
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            MetricBox(
                                modifier = Modifier.weight(1f),
                                title = "METER PROGRESSION",
                                value = "${String.format(Locale.getDefault(), "%,.0f", cycle.baselineReading)} → ${String.format(Locale.getDefault(), "%,.0f", cycle.currentReading)}",
                                unit = "",
                                subtitle = "Net Δ ${cycle.unitsConsumed.toInt()} units",
                                icon = Icons.Default.Pin,
                                accentColor = StitchSecondary,
                                containerColor = StitchSurfaceContainerLow,
                                isValueLarge = false
                            )
                            MetricBox(
                                modifier = Modifier.weight(1f),
                                title = "ESTIMATED BILL",
                                value = if (cycle.estimatedBill > 0) "₹${cycle.estimatedBill.toInt()}" else "₹0",
                                unit = cycle.slabLabel,
                                subtitle = cycle.freeAllowanceDescription,
                                icon = Icons.Default.Payments,
                                accentColor = StitchAmberAccent,
                                containerColor = StitchAmberContainer
                            )
                        }
                    }

                    // Free Allowance Status Bar
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(Icons.Default.Verified, contentDescription = null, tint = StitchEmeraldGreen, modifier = Modifier.size(15.dp))
                                    Text("Free Allowance Remaining", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = StitchOnSurface)
                                }
                                Text("${cycle.freeAllowanceRemaining.toInt()} kWh left in tier", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchEmeraldGreen)
                            }
                            val progress = (cycle.unitsConsumed / cycle.freeAllowanceTotal).coerceIn(0.0, 1.0).toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(2.5.dp))
                                    .background(StitchSurfaceContainer)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(progress)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(2.5.dp))
                                        .background(StitchEmeraldGreen)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Allowance: ${cycle.freeAllowanceTotal.toInt()} kWh − Current: ${cycle.unitsConsumed.toInt()} kWh",
                                    fontSize = 10.sp,
                                    color = StitchOnSurfaceVariant,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = StitchEmeraldContainer) {
                                    Text(cycle.freeAllowanceDescription, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchEmeraldGreen, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 3: Daily Consumption (Main Blue Line/Area Graph)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StitchSurfaceContainerLowest,
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Daily Consumption", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                        Text("Detailed day-by-day meter progression & daily burn rate", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }
                    val periodBadge = cycle?.periodDisplay?.uppercase(Locale.US) ?: "CURRENT CYCLE"
                    Surface(shape = RoundedCornerShape(12.dp), color = StitchBlueContainer) {
                        Text(periodBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchBlueAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                }

                // Interactive Tooltip Pill (Sep 14 / Selected Day)
                val activePoint = uiState.selectedDailyPoint ?: uiState.dailyPoints.maxByOrNull { it.dailyKwh }
                if (activePoint != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0B1C30),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                                Text(activePoint.dateLabel, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Text(
                                text = "Daily Consumption: ${String.format(Locale.US, "%.1f", activePoint.dailyKwh)} kWh (Δ since previous day)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFEFF4FF)
                            )
                            Text(
                                text = "Meter Reading: ${String.format(Locale.getDefault(), "%,.0f", activePoint.cumulativeKwh)} kWh (Cumulative)",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }

                // Smooth Vector Graph Canvas (Exact Stitch Dimensions & Grid)
                DailyConsumptionCanvas(
                    points = uiState.dailyPoints,
                    avgKwh = cycle?.avgUnitsPerDay ?: 0.0,
                    selectedPoint = activePoint,
                    onPointSelected = onDailyPointSelected
                )

                // Section 4: Daily Usage Insights (3 compact metric boxes)
                val summary = uiState.dailySummary
                if (summary != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        DailyInsightMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "HIGHEST DAY",
                            value = "${String.format(Locale.US, "%.1f", summary.highestDay?.dailyKwh ?: 0.0)} kWh",
                            date = summary.highestDay?.dateLabel ?: "N/A",
                            valueColor = StitchBlueAccent
                        )
                        DailyInsightMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "LOWEST DAY",
                            value = "${String.format(Locale.US, "%.1f", summary.lowestDay?.dailyKwh ?: 0.0)} kWh",
                            date = summary.lowestDay?.dateLabel ?: "N/A",
                            valueColor = StitchEmeraldGreen
                        )
                        DailyInsightMiniCard(
                            modifier = Modifier.weight(1f),
                            title = "CYCLE DAILY AVG",
                            value = "${String.format(Locale.US, "%.1f", summary.avgDailyKwh)} kWh",
                            date = "${summary.totalDays} days avg",
                            valueColor = StitchOnSurface
                        )
                    }
                }
            }
        }

        // Section 5: Billing Cycle Analysis (Math Flow & Stats)
        if (cycle != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text("Billing Cycle Analysis", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                        Text("How current cycle consumption was calculated", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }

                    // Visual Math Flow Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("PREVIOUS BILL READING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant)
                                Text("${String.format(Locale.getDefault(), "%,.0f", cycle.baselineReading)} kWh", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                                Text(cycle.prevBillDate, fontSize = 10.sp, color = StitchSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = StitchBlueAccent, modifier = Modifier.size(16.dp))
                                Text("${cycle.unitsConsumed.toInt()} kWh", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StitchBlueAccent)
                                Text("over ${cycle.daysElapsed} days", fontSize = 10.sp, color = StitchSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("CURRENT READING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant)
                                Text("${String.format(Locale.getDefault(), "%,.0f", cycle.currentReading)} kWh", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                                Text(cycle.currentDate, fontSize = 10.sp, color = StitchSecondary)
                            }
                        }
                    }

                    // Supplementary Cycle Stats Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        CycleMiniStat(modifier = Modifier.weight(1f), label = "Days Elapsed", value = "${cycle.daysElapsed} / 60")
                        CycleMiniStat(modifier = Modifier.weight(1f), label = "Burn Rate", value = "${String.format(Locale.US, "%.1f", cycle.avgUnitsPerDay)} kWh/d")
                        CycleMiniStat(modifier = Modifier.weight(1f), label = "Subsidized Buffer", value = "${cycle.freeAllowanceRemaining.toInt()} kWh", valueColor = StitchEmeraldGreen)
                    }
                }
            }
        }

        // Section 6: Current vs Previous Billing Cycle
        val comparison = uiState.cycleComparison
        if (comparison != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Compared to Previous Cycle", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                        Surface(shape = RoundedCornerShape(12.dp), color = StitchSurfaceContainer) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = if (comparison.isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                    contentDescription = null,
                                    tint = if (comparison.isIncrease) StitchBlueAccent else StitchEmeraldGreen,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${if (comparison.isIncrease) "+" else "-"}${comparison.deltaUnits.toInt()} kWh (${String.format(Locale.US, "%.1f", comparison.deltaPercent)}%)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchOnSurface
                                )
                            }
                        }
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("CURRENT (DAY ${cycle?.daysElapsed ?: 1})", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant)
                                Text("${comparison.currentUnits.toInt()} kWh", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                            }
                            Box(modifier = Modifier.height(28.dp).width(1.dp).background(StitchOutline))
                            Column {
                                Text("PREVIOUS (EQ DAY)", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant)
                                Text("${comparison.prevUnitsEq.toInt()} kWh", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant)
                            }
                            Box(modifier = Modifier.height(28.dp).width(1.dp).background(StitchOutline))
                            Column(horizontalAlignment = Alignment.End) {
                                Text("NET DELTA", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant)
                                Text(
                                    text = "${if (comparison.isIncrease) "+" else "-"}${comparison.deltaUnits.toInt()} kWh",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (comparison.isIncrease) StitchBlueAccent else StitchEmeraldGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurfaceContainer)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(15.dp))
                        Text("Consumption aligns with seasonal temperature fluctuations.", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }
                }
            }
        }

        // Section 7: Consumption History (Bar Chart)
        if (uiState.historicalCycles.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Consumption History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                            Text("Billed units across completed bi-monthly cycles", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                        }
                        Surface(shape = RoundedCornerShape(12.dp), color = StitchBlueContainer) {
                            Text("UNIT: kWh", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = StitchBlueAccent, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }

                    // Bar Chart Canvas
                    ConsumptionHistoryBarChart(
                        cycles = uiState.historicalCycles,
                        avgUnits = uiState.historyAvgUnits
                    )

                    // Legend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(StitchBlueAccent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Billed Units (kWh)", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                        Spacer(modifier = Modifier.width(16.dp))
                        Box(modifier = Modifier.width(16.dp).height(2.dp).background(StitchBlueAccent))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3-Cycle Avg (${uiState.historyAvgUnits.toInt()} kWh)", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }
                }
            }
        }

        // Section 8: Bill & Consumption History (Compact Analytical Ledger)
        if (uiState.historicalCycles.isNotEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Bill & Consumption History", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                            Text("Historical cycle dues & unit correlation", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                        }
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(20.dp))
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        uiState.historicalCycles.forEach { item ->
                            HistoricalCycleRow(item = item)
                        }
                    }
                }
            }
        }

        // Section 9: Bill vs Consumption Correlation
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = StitchSurfaceContainerLowest,
            border = BorderStroke(1.dp, StitchOutline)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column {
                    Text("Bill vs. Consumption Correlation", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                    Text("How unit volume shifts tariff slab rates", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    uiState.tariffSlabs.forEach { slab ->
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(slab.name, fontSize = 12.sp, color = StitchOnSurfaceVariant)
                                Text(slab.rateStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(slab.tierColorHex))
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(3.5.dp))
                                    .background(StitchSurfaceContainer)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(slab.fillFraction.coerceIn(0f, 1f))
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(3.5.dp))
                                        .background(Color(slab.tierColorHex))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 10: Bill & Consumption Breakdown
        val breakdown = uiState.billBreakdown
        if (breakdown != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = StitchSurfaceContainerLowest,
                border = BorderStroke(1.dp, StitchOutline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Column {
                        Text("Bill & Consumption Breakdown", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                        Text("Factual breakdown of current billing cycle estimation", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = StitchSurfaceContainerLow
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            BreakdownRow(label = "Current Cycle Consumption", value = "${breakdown.cycleConsumptionKwh.toInt()} kWh", isBold = true)
                            BreakdownRow(label = "Applicable Free Allowance (TNEB LT-1A)", value = "${breakdown.freeAllowanceKwh.toInt()} kWh (100% subsidized)", valueColor = StitchEmeraldGreen)
                            BreakdownRow(label = "Chargeable Units", value = "${breakdown.chargeableUnitsKwh.toInt()} kWh")
                            HorizontalDivider(color = StitchOutline)
                            BreakdownRow(
                                label = "Estimated Energy Charges",
                                value = if (breakdown.estimatedEnergyCharge > 0) "₹${String.format(Locale.US, "%.2f", breakdown.estimatedEnergyCharge)}" else "₹0.00 (within subsidized tier)",
                                valueColor = StitchEmeraldGreen
                            )
                            BreakdownRow(label = "Fixed & Statutory Charges", value = "₹0.00")
                            HorizontalDivider(color = StitchOutline)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Estimated Dues", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                                Text(
                                    text = if (breakdown.totalEstimatedDues > 0) "₹${String.format(Locale.US, "%.2f", breakdown.totalEstimatedDues)}" else "₹0.00",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StitchEmeraldGreen
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StitchSurfaceContainer)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StitchSecondary, modifier = Modifier.size(14.dp))
                        Text("Slab rates and fixed tariff dues activate once usage exceeds remaining free allowance.", fontSize = 10.sp, color = StitchOnSurfaceVariant)
                    }
                }
            }
        }

        // Section 11: Data-Driven Energy Insights
        if (uiState.insights.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Consumption Trend", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                        Text("Consumption is increasing across recent billing cycles.", fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = StitchBlueContainer) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = StitchBlueAccent, modifier = Modifier.size(12.dp))
                            Text(uiState.trendPercentageText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = StitchBlueAccent)
                        }
                    }
                }

                uiState.insights.forEach { insight ->
                    EnergyInsightCard(insight = insight)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// REUSABLE POPULATED SUBCOMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MetricBox(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    unit: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    containerColor: Color,
    isValueLarge: Boolean = true
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = accentColor, letterSpacing = 0.5.sp)
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(3.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = value,
                    fontSize = if (isValueLarge) 20.sp else 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchOnSurface,
                    letterSpacing = (-0.2).sp
                )
                if (unit.isNotBlank()) {
                    Spacer(modifier = Modifier.width(3.dp))
                    if (unit.startsWith("Slab")) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = StitchEmeraldContainer
                        ) {
                            Text(
                                text = unit,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = StitchEmeraldGreen,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    } else {
                        Text(unit, fontSize = 11.sp, color = StitchOnSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, fontSize = 10.sp, color = accentColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun DailyInsightMiniCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    date: String,
    valueColor: Color
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = StitchSurfaceContainerLow
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = StitchOnSurfaceVariant, letterSpacing = 0.5.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = valueColor)
            Spacer(modifier = Modifier.height(1.dp))
            Text(date, fontSize = 9.sp, color = StitchSecondary)
        }
    }
}

@Composable
private fun CycleMiniStat(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    valueColor: Color = StitchOnSurface
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = StitchSurfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 9.sp, color = StitchOnSurfaceVariant)
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
private fun HistoricalCycleRow(item: HistoricalCycleItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = StitchSurfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(item.period, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (item.isCurrent) StitchBlueContainer else StitchSurfaceContainer
                    ) {
                        Text(
                            text = item.status,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (item.isCurrent) StitchBlueAccent else StitchOnSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (item.isCurrent) StitchBlueAccent else StitchOnSurface)
                    Text(" / ${item.units.toInt()} kWh", fontSize = 12.sp, color = StitchOnSurfaceVariant)
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(2.5.dp))
                    .background(StitchSurfaceContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(item.ratio)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(if (item.isCurrent) StitchBlueAccent else StitchSecondary)
                )
            }
        }
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    valueColor: Color = StitchOnSurface,
    isBold: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = StitchOnSurfaceVariant)
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.SemiBold,
            color = valueColor
        )
    }
}

@Composable
private fun EnergyInsightCard(insight: EnergyInsightItem) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = StitchSurfaceContainerLowest,
        border = BorderStroke(1.dp, StitchOutline)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(insight.accentColorHex).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                val icon = when (insight.iconType) {
                    "north_east" -> Icons.Default.NorthEast
                    "speed" -> Icons.Default.Speed
                    "trending_up" -> Icons.Default.TrendingUp
                    "energy_savings_leaf" -> Icons.Default.EnergySavingsLeaf
                    else -> Icons.Default.Bolt
                }
                Icon(icon, contentDescription = null, tint = Color(insight.accentColorHex), modifier = Modifier.size(18.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(insight.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = StitchOnSurface)
                Spacer(modifier = Modifier.height(2.dp))
                Text(insight.description, fontSize = 12.sp, color = StitchOnSurfaceVariant, lineHeight = 16.sp)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CHARTS (Canvas Smooth Vector Implementations matching Stitch SVG)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DailyConsumptionCanvas(
    points: List<DailyUsagePoint>,
    avgKwh: Double,
    selectedPoint: DailyUsagePoint?,
    onPointSelected: (DailyUsagePoint) -> Unit
) {
    val maxVal = max(20.0, points.maxOfOrNull { it.dailyKwh }?.let { it * 1.25 } ?: 20.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(175.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isNotEmpty()) {
                                val chartW = size.width - 55f
                                val startX = 35f
                                val stepX = chartW / (points.size.coerceAtLeast(2) - 1)
                                val tappedIndex = ((offset.x - startX + stepX / 2f) / stepX).toInt().coerceIn(0, points.lastIndex)
                                onPointSelected(points[tappedIndex])
                            }
                        }
                    }
            ) {
                val chartW = size.width - 55f
                val chartH = size.height - 30f
                val startX = 35f
                val startY = 10f

                // 1. Gridlines (5 horizontal dashed lines for 0, 5, 10, 15, 20)
                val gridSteps = 4
                for (i in 0..gridSteps) {
                    val y = startY + (chartH / gridSteps) * i
                    drawLine(
                        color = Color(0xFFE5EEFF),
                        start = Offset(startX, y),
                        end = Offset(startX + chartW, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                if (points.size >= 2) {
                    val stepX = chartW / (points.size - 1)

                    // 2. Average line across cycle
                    val avgY = startY + chartH - ((avgKwh / maxVal) * chartH).toFloat().coerceIn(0f, chartH)
                    drawLine(
                        color = Color(0xFF60A5FA).copy(alpha = 0.7f),
                        start = Offset(startX, avgY),
                        end = Offset(startX + chartW, avgY),
                        strokeWidth = 1.2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )

                    // 3. Smooth Area & Cubic Bezier Line
                    val linePath = Path()
                    val areaPath = Path()

                    points.forEachIndexed { i, p ->
                        val x = startX + i * stepX
                        val y = startY + chartH - ((p.dailyKwh / maxVal) * chartH).toFloat().coerceIn(0f, chartH)
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            areaPath.moveTo(x, startY + chartH)
                            areaPath.lineTo(x, y)
                        } else {
                            val prevX = startX + (i - 1) * stepX
                            val prevY = startY + chartH - ((points[i - 1].dailyKwh / maxVal) * chartH).toFloat().coerceIn(0f, chartH)
                            val cx1 = (prevX + x) / 2f
                            linePath.cubicTo(cx1, prevY, cx1, y, x, y)
                            areaPath.cubicTo(cx1, prevY, cx1, y, x, y)
                        }
                    }

                    areaPath.lineTo(startX + chartW, startY + chartH)
                    areaPath.close()

                    // Gradient Area under curve
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                StitchBlueAccent.copy(alpha = 0.35f),
                                StitchBlueAccent.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = startY,
                            endY = startY + chartH
                        )
                    )

                    // Main Curve Line
                    drawPath(
                        path = linePath,
                        color = StitchBlueAccent,
                        style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Data Point Nodes
                    points.forEachIndexed { i, p ->
                        val x = startX + i * stepX
                        val y = startY + chartH - ((p.dailyKwh / maxVal) * chartH).toFloat().coerceIn(0f, chartH)
                        val isSelected = selectedPoint?.dateLabel == p.dateLabel

                        if (isSelected) {
                            drawCircle(color = StitchBlueAccent, radius = 6f, center = Offset(x, y))
                            drawCircle(color = Color.White, radius = 4f, center = Offset(x, y))
                        } else {
                            drawCircle(color = Color.White, radius = 3.5f, center = Offset(x, y))
                            drawCircle(color = StitchBlueAccent, radius = 3.5f, center = Offset(x, y), style = Stroke(width = 1.5f))
                        }
                    }
                }
            }

            // Y-axis Scale Labels Overlay
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(20, 15, 10, 5, 0).forEach { scaleVal ->
                    Text(
                        text = "$scaleVal",
                        fontSize = 9.sp,
                        color = StitchSecondary,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // Bottom Date Labels Row
        if (points.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 32.dp, end = 20.dp, top = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val displayPoints = if (points.size <= 5) points else {
                    listOf(
                        points.first(),
                        points[points.size / 4],
                        points[points.size / 2],
                        points[points.size * 3 / 4],
                        points.last()
                    )
                }
                displayPoints.forEach { p ->
                    val isSelected = selectedPoint?.dateLabel == p.dateLabel
                    Text(
                        text = p.dateLabel,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) StitchBlueAccent else StitchOnSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConsumptionHistoryBarChart(
    cycles: List<HistoricalCycleItem>,
    avgUnits: Double
) {
    val maxUnits = max(600.0, cycles.maxOfOrNull { it.units }?.let { it * 1.25 } ?: 600.0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Active bar tooltip if current cycle exists
        val activeCycle = cycles.firstOrNull { it.isCurrent } ?: cycles.lastOrNull()
        if (activeCycle != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0B1C30)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                        Text(
                            text = "${activeCycle.period}: ${activeCycle.units.toInt()} kWh · ${activeCycle.amount}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val chartW = size.width - 45f
                val chartH = size.height - 20f
                val startX = 35f
                val startY = 10f

                // Gridlines (4 horizontal lines)
                val steps = 3
                for (i in 0..steps) {
                    val y = startY + (chartH / steps) * i
                    drawLine(
                        color = Color(0xFFE5EEFF),
                        start = Offset(startX, y),
                        end = Offset(startX + chartW, y),
                        strokeWidth = 1f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                    )
                }

                // Average line
                val avgY = startY + chartH - ((avgUnits / maxUnits) * chartH).toFloat().coerceIn(0f, chartH)
                drawLine(
                    color = StitchBlueAccent.copy(alpha = 0.6f),
                    start = Offset(startX, avgY),
                    end = Offset(startX + chartW, avgY),
                    strokeWidth = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                )

                // Draw Bars
                val n = cycles.size.coerceAtLeast(1)
                val barWidth = (chartW / n * 0.45f).coerceIn(24f, 44f)
                val spacing = chartW / n

                cycles.forEachIndexed { i, c ->
                    val barH = ((c.units / maxUnits) * chartH).toFloat().coerceIn(10f, chartH)
                    val x = startX + i * spacing + (spacing - barWidth) / 2f
                    val y = startY + chartH - barH

                    val barColor = if (c.isCurrent) StitchBlueAccent else Color(0xFF93C5FD)

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = androidx.compose.ui.geometry.Size(barWidth, barH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }

            // Y-axis Scale Labels Overlay
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 4.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(600, 400, 200, 0).forEach { scaleVal ->
                    Text(
                        text = "$scaleVal",
                        fontSize = 9.sp,
                        color = StitchSecondary,
                        modifier = Modifier.width(28.dp),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        // Cycle labels & units below
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 12.dp, top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            cycles.forEach { c ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${c.units.toInt()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (c.isCurrent) StitchBlueAccent else StitchOnSurfaceVariant
                    )
                    Text(
                        text = c.period,
                        fontSize = 10.sp,
                        color = StitchOnSurfaceVariant
                    )
                }
            }
        }
    }
}

package com.example.feature.recommends.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.home.ui.modules.BillingComparisonState
import com.example.feature.home.ui.modules.BillingCycleComparison
import com.example.feature.home.ui.modules.BillingCycleItem
import com.example.feature.home.ui.modules.BillingCycleStatus
import com.example.feature.recommends.model.*
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.theme.*

@Composable
fun RecommendsRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSolarLens: () -> Unit = {},
    viewModel: RecommendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    RecommendsScreen(
        uiState = uiState,
        onMantraClick = { id -> viewModel.toggleMantraCheckIn(context, id) },
        onNavigateToHome = onNavigateToHome,
        onNavigateToProfile = onNavigateToProfile,
        onPreviousCycle = { viewModel.selectPreviousCycle() },
        onNextCycle = { viewModel.selectNextCycle() }
    )
}

@Composable
fun RecommendsScreen(
    uiState: RecommendsUiState,
    onMantraClick: (Int) -> Unit,
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onPreviousCycle: () -> Unit = {},
    onNextCycle: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            RecommendsTopBar(
                userAddress = uiState.userAddress,
                onProfileClick = onNavigateToProfile
            )
        },
        bottomBar = {
            SolarEaseBottomNavigation(
                currentTab = SolarEaseNavTab.RECOMMENDS,
                onNavigateToHome = onNavigateToHome,
                onNavigateToRecommends = { /* Already on Recommends */ },
                onNavigateToProfile = onNavigateToProfile
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Hero Header Section with Eco Illustration
            RecommendsHeroHeader()

            // 2. Energy Credits & Reward Summary Card
            EnergyCreditsCard(energyCredits = uiState.energyCredits)

            // 3. Today's Progress & Streak Cards (2-column grid)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TodayProgressCard(
                    modifier = Modifier.weight(1f),
                    checkedCount = uiState.todayProgressChecked,
                    totalCount = uiState.todayProgressTotal
                )
                StreakCard(
                    modifier = Modifier.weight(1f),
                    streakDays = uiState.streakDays
                )
            }

            // 4. Last Billing Cycle (Verified Savings Card with Navigation)
            BillingSavingsCard(
                savings = uiState.billingCycleSavings,
                availableCycles = uiState.availableCycles,
                selectedCycleIndex = uiState.selectedCycleIndex,
                cycleComparison = uiState.cycleComparison,
                onPreviousCycle = onPreviousCycle,
                onNextCycle = onNextCycle
            )

            // 5. 9 Energy-Saving Mantras Section
            MantrasSection(
                mantras = uiState.mantras,
                onMantraClick = onMantraClick
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TOP BAR
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecommendsTopBar(
    userAddress: String,
    onProfileClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = Color(0xFFF8FAFC).copy(alpha = 0.95f),
        border = BorderStroke(0.5.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
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
                        .clip(CircleShape)
                        .background(Color(0xFF047857).copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SolarPower,
                        contentDescription = "SolarEase",
                        tint = Color(0xFF047857),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = "SolarEase",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = (-0.2).sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = userAddress,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF047857))
                    .clickable { onProfileClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 1. HERO HEADER SECTION WITH ECO ILLUSTRATION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RecommendsHeroHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        Text(
            text = "Recommends",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF0F172A),
            letterSpacing = (-0.3).sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Follow the 9 Energy-Saving Mantras and build your energy-saving journey.",
            fontSize = 12.sp,
            color = Color(0xFF64748B),
            lineHeight = 16.sp
        )
    }
}

// EcoCottageIllustration removed — header is now text-only per design spec

// ─────────────────────────────────────────────────────────────────────────────
// 2. ENERGY CREDITS CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun EnergyCreditsCard(energyCredits: EnergyCreditsData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Energy Credits Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFBBF24), Color(0xFFF59E0B))
                                )
                            )
                            .border(2.dp, Color(0xFFFDE68A), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Column {
                        Text(
                            text = "${energyCredits.totalCredits}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            lineHeight = 26.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Energy Credits",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .height(36.dp)
                        .width(1.dp)
                        .background(Color(0xFFF1F5F9))
                )

                // Right: Next Reward
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF7ED)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CardGiftcard,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Next Reward",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            text = "${energyCredits.creditsToUnlock} credits to unlock",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            // Bottom Progress Bar
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "${energyCredits.progressCredits} / ${energyCredits.nextRewardTarget}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.align(Alignment.End)
                )

                val progress = (energyCredits.progressCredits.toFloat() / energyCredits.nextRewardTarget.toFloat())
                    .coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF16A34A))
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. TODAY'S PROGRESS & STREAK CARDS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TodayProgressCard(
    modifier: Modifier = Modifier,
    checkedCount: Int,
    totalCount: Int
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFECFDF5)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Today's Progress",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$checkedCount / $totalCount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Mantras",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val progress = (checkedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color(0xFFF1F5F9))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color(0xFF16A34A))
                )
            }
        }
    }
}

@Composable
private fun StreakCard(
    modifier: Modifier = Modifier,
    streakDays: Int
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
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
                            .background(Color(0xFFFFFBEB)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color(0xFFF97316),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = if (streakDays > 0) "$streakDays Day Streak" else "7 Day Streak",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E293B)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(14.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (streakDays > 0) "You've checked in for $streakDays consecutive days!" else "Start your first streak",
                fontSize = 11.sp,
                color = Color(0xFF64748B),
                lineHeight = 15.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 4. LAST BILLING CYCLE (VERIFIED SAVINGS CARD)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BillingSavingsCard(
    savings: BillingCycleSavings,
    availableCycles: List<BillingCycleItem> = emptyList(),
    selectedCycleIndex: Int = 0,
    cycleComparison: BillingCycleComparison? = null,
    onPreviousCycle: () -> Unit = {},
    onNextCycle: () -> Unit = {}
) {
    val comparison = cycleComparison
    val selectedCycle = comparison?.selectedCycle
        ?: availableCycles.getOrNull(selectedCycleIndex)
    val previousCycle = comparison?.previousCycle
        ?: (if (selectedCycleIndex > 0) availableCycles.getOrNull(selectedCycleIndex - 1) else null)

    val isCurrent = selectedCycle?.status == BillingCycleStatus.CURRENT
    val canGoBack = selectedCycleIndex > 0
    val canGoForward = selectedCycleIndex < availableCycles.lastIndex

    // Fallbacks from savings if availableCycles is empty
    val prevUnits = previousCycle?.consumptionKwh ?: savings.previousUnits
    val prevPeriod = previousCycle?.periodLabel?.takeIf { it.isNotBlank() } ?: savings.previousPeriod
    val currUnits = selectedCycle?.consumptionKwh ?: savings.currentUnits
    val currPeriod = selectedCycle?.periodLabel?.takeIf { it.isNotBlank() } ?: savings.currentPeriod

    val state = comparison?.state ?: if (savings.isConsumptionIncreased) BillingComparisonState.COMPLETED_INCREASED else BillingComparisonState.COMPLETED_SAVED

    // Card background & borders based on state
    val isIncreased = when (state) {
        BillingComparisonState.CURRENT_HIGHER,
        BillingComparisonState.COMPLETED_INCREASED -> true
        else -> false
    }

    val isCompletedSaved = state == BillingComparisonState.COMPLETED_SAVED
    val isCurrentLower = state == BillingComparisonState.CURRENT_LOWER
    val isSteadyOrNoChange = state == BillingComparisonState.COMPLETED_NO_CHANGE

    val cardBg = when {
        isIncreased -> Color(0xFFFFFBEB)
        isCompletedSaved -> Color(0xFFF0FDF4)
        isCurrentLower -> Color(0xFFF0F9FF)
        else -> Color(0xFFF8FAFC)
    }

    val cardBorder = when {
        isIncreased -> Color(0xFFFDE68A)
        isCompletedSaved -> Color(0xFFA7F3D0)
        isCurrentLower -> Color(0xFFBAE6FD)
        else -> Color(0xFFE2E8F0)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Title and Cycle Navigation
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
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = if (isIncreased) Color(0xFFD97706) else Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Last Billing Cycle",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                // Cycle Navigation: < [Period (Live/Completed)] >
                if (availableCycles.size > 1) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = onPreviousCycle,
                            enabled = canGoBack,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous cycle",
                                tint = if (canGoBack) Color(0xFF0F766E) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isCurrent) Color(0xFFD1FAE5) else Color(0xFFE2E8F0),
                            border = BorderStroke(0.5.dp, if (isCurrent) Color(0xFF6EE7B7) else Color(0xFFCBD5E1))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (isCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF059669))
                                    )
                                }
                                Text(
                                    text = if (isCurrent) "$currPeriod • Live" else "$currPeriod • Done",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) Color(0xFF065F46) else Color(0xFF334155)
                                )
                            }
                        }

                        IconButton(
                            onClick = onNextCycle,
                            enabled = canGoForward,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next cycle",
                                tint = if (canGoForward) Color(0xFF0F766E) else Color(0xFFCBD5E1),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Two Equal-Width Columns for Previous Cycle and Current / Selected Cycle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Previous Cycle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Previous Cycle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${prevUnits.toInt()} kWh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Text(
                        text = prevPeriod,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Middle arrow indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isIncreased -> Color(0xFFFEF3C7)
                                isCompletedSaved -> Color(0xFFD1FAE5)
                                isCurrentLower -> Color(0xFFE0F2FE)
                                else -> Color(0xFFF1F5F9)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            isIncreased -> Icons.Default.ArrowUpward
                            isCompletedSaved -> Icons.Default.ArrowDownward
                            isCurrentLower -> Icons.Default.ArrowDownward
                            else -> Icons.Default.Remove
                        },
                        contentDescription = null,
                        tint = when {
                            isIncreased -> Color(0xFFD97706)
                            isCompletedSaved -> Color(0xFF059669)
                            isCurrentLower -> Color(0xFF0284C7)
                            else -> Color(0xFF64748B)
                        },
                        modifier = Modifier.size(14.dp)
                    )
                }

                // Column 2: Current / Selected Cycle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = if (isCurrent) "Current Cycle" else "Selected Cycle",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "${currUnits.toInt()} kWh",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1E293B)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = currPeriod,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                        if (isCurrent) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFD1FAE5)
                            ) {
                                Text(
                                    text = "Live",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFE2E8F0)
                            ) {
                                Text(
                                    text = "Completed",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Divider separating cycle columns from comparison section
            HorizontalDivider(
                color = if (isIncreased) Color(0xFFFDE68A).copy(alpha = 0.7f) else Color(0xFFBBF7D0).copy(alpha = 0.7f),
                thickness = 0.8.dp
            )

            // Dedicated Full-Width Comparison Section
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(
                    1.dp,
                    when {
                        isIncreased -> Color(0xFFFDE68A)
                        isCompletedSaved -> Color(0xFFD1FAE5)
                        isCurrentLower -> Color(0xFFBAE6FD)
                        else -> Color(0xFFE2E8F0)
                    }
                ),
                shadowElevation = 0.5.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Comparison Icon Box
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (state) {
                                    BillingComparisonState.CURRENT_HIGHER,
                                    BillingComparisonState.COMPLETED_INCREASED -> Color(0xFFFEF3C7)
                                    BillingComparisonState.COMPLETED_SAVED -> Color(0xFFD1FAE5)
                                    BillingComparisonState.CURRENT_LOWER,
                                    BillingComparisonState.CURRENT_NO_READINGS -> Color(0xFFE0F2FE)
                                    BillingComparisonState.COMPLETED_NO_CHANGE -> Color(0xFFF1F5F9)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when (state) {
                                BillingComparisonState.CURRENT_HIGHER,
                                BillingComparisonState.COMPLETED_INCREASED -> Icons.Default.TrendingUp
                                BillingComparisonState.COMPLETED_SAVED -> Icons.Default.ElectricBolt
                                BillingComparisonState.CURRENT_LOWER -> Icons.Default.TrendingDown
                                BillingComparisonState.CURRENT_NO_READINGS -> Icons.Default.Info
                                BillingComparisonState.COMPLETED_NO_CHANGE -> Icons.Default.BarChart
                            },
                            contentDescription = null,
                            tint = when (state) {
                                BillingComparisonState.CURRENT_HIGHER,
                                BillingComparisonState.COMPLETED_INCREASED -> Color(0xFFD97706)
                                BillingComparisonState.COMPLETED_SAVED -> Color(0xFF059669)
                                BillingComparisonState.CURRENT_LOWER,
                                BillingComparisonState.CURRENT_NO_READINGS -> Color(0xFF0284C7)
                                BillingComparisonState.COMPLETED_NO_CHANGE -> Color(0xFF475569)
                            },
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Comparison Text Information
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        when (state) {
                            BillingComparisonState.CURRENT_HIGHER -> {
                                val diff = comparison?.differenceUnits?.toInt() ?: (currUnits - prevUnits).toInt()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "↑ +$diff kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706)
                                    )
                                    Text(
                                        text = "• Consumption Increased",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = comparison?.secondaryMessage ?: "${currUnits.toInt()} kWh consumed so far, based on daily meter readings.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = comparison?.comparisonNote ?: "Already +$diff kWh higher than previous cycle",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            BillingComparisonState.CURRENT_LOWER -> {
                                val diff = comparison?.differenceUnits?.toInt() ?: (prevUnits - currUnits).toInt()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "↓ $diff kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7)
                                    )
                                    Text(
                                        text = "• Below Previous Cycle",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0369A1)
                                    )
                                }
                                Text(
                                    text = comparison?.secondaryMessage ?: "${currUnits.toInt()} kWh so far, based on daily meter readings.",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = comparison?.comparisonNote ?: "Current cycle still in progress",
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            BillingComparisonState.COMPLETED_SAVED -> {
                                val saved = comparison?.savedUnits?.toInt() ?: savings.savedUnits.toInt()
                                val credits = comparison?.savedCredits ?: savings.savedCredits
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "↓ $saved kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF059669)
                                    )
                                    Text(
                                        text = "• Saved",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF15803D)
                                    )
                                }
                                Text(
                                    text = if (credits > 0) "+$credits Energy Credits" else comparison?.secondaryMessage ?: "Historical Baseline",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (credits > 0) Color(0xFF059669) else Color(0xFF64748B)
                                )
                                Text(
                                    text = comparison?.comparisonNote ?: "$saved kWh lower than $prevPeriod",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            BillingComparisonState.COMPLETED_INCREASED -> {
                                val diff = comparison?.differenceUnits?.toInt() ?: savings.differenceUnits.toInt()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "↑ +$diff kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFD97706)
                                    )
                                    Text(
                                        text = "• Consumption Increased",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Text(
                                    text = comparison?.secondaryMessage ?: "No Energy Credits",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = comparison?.comparisonNote ?: "Consumption increased by $diff kWh compared with the previous cycle",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            BillingComparisonState.COMPLETED_NO_CHANGE -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "→ 0 kWh",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF475569)
                                    )
                                    Text(
                                        text = "• No Change",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF334155)
                                    )
                                }
                                Text(
                                    text = comparison?.secondaryMessage ?: "No Energy Credits",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = comparison?.comparisonNote ?: "0 kWh difference compared with the previous cycle",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                            BillingComparisonState.CURRENT_NO_READINGS -> {
                                Text(
                                    text = comparison?.primaryMessage ?: "Record your meter reading to track this cycle",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0369A1)
                                )
                                Text(
                                    text = comparison?.secondaryMessage ?: "Opening reading: ${selectedCycle?.openingMeterReading?.toInt() ?: 0} kWh",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF475569)
                                )
                                Text(
                                    text = comparison?.comparisonNote ?: "Log daily readings on Home to track consumption in real-time",
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. 9 ENERGY-SAVING MANTRAS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MantrasSection(
    mantras: List<MantraItem>,
    onMantraClick: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = "9 Energy-Saving Mantras",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                letterSpacing = (-0.2).sp
            )
            Text(
                text = "Check in regularly, build the habit and reduce your electricity consumption.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            mantras.forEach { item ->
                MantraCard(
                    item = item,
                    onClick = { onMantraClick(item.id) }
                )
            }
        }
    }
}

@Composable
private fun MantraCard(
    item: MantraItem,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
        shadowElevation = 0.5.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Top row: Number + Icon + Title + Action button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Number index
                Text(
                    text = item.number,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Icon Circle
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(item.iconBgColorHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.iconEmoji,
                        fontSize = 19.sp
                    )
                }

                // Title + Description
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.description,
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        lineHeight = 15.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Action / Check in button (top-right aligned)
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(top = 1.dp)
                ) {
                    if (item.isCheckedIn) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF16A34A),
                            modifier = Modifier.clickable { onClick() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "Checked in",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 1
                                )
                            }
                        }
                        if (item.checkInTimeDisplay != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = item.checkInTimeDisplay,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onClick,
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF16A34A)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF15803D)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (item.isOneTimeAction) "Mark Done" else "Check In",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Bottom row: Potential savings + Frequency badge
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 60.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (item.potentialSavings != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = item.potentialSavings,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Frequency badge — always horizontal, never wraps
                val (badgeBg, badgeText) = when (item.frequencyBadge) {
                    "Daily" -> Pair(Color(0xFFEFF6FF), Color(0xFF2563EB))
                    "Weekly" -> Pair(Color(0xFFFAF5FF), Color(0xFF9333EA))
                    "Monthly" -> Pair(Color(0xFFFFF1F2), Color(0xFFE11D48))
                    else -> Pair(Color(0xFFFFFBEB), Color(0xFFB45309))
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeBg
                ) {
                    Text(
                        text = item.frequencyBadge,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = badgeText,
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

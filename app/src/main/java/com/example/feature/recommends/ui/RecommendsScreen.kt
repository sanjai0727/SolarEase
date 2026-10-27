package com.example.feature.recommends.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.ui.platform.testTag
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun RecommendsRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSolarLens: () -> Unit = {},
    onNavigateToSolarVendor: () -> Unit = {},
    viewModel: RecommendsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(context) {
        viewModel.initPreferences(context)
    }

    RecommendsScreen(
        uiState = uiState,
        onMantraClick = { id -> viewModel.toggleMantraCheckIn(context, id) },
        onToggleMantrasExpanded = { viewModel.toggleMantrasExpanded(context) },
        onNavigateToHome = onNavigateToHome,
        onNavigateToProfile = onNavigateToProfile,
        onPreviousCycle = { viewModel.selectPreviousCycle() },
        onNextCycle = { viewModel.selectNextCycle() },
        onNavigateToSolarVendor = onNavigateToSolarVendor
    )
}

@Composable
fun RecommendsScreen(
    uiState: RecommendsUiState,
    onMantraClick: (Int) -> Unit,
    onToggleMantrasExpanded: () -> Unit = {},
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onPreviousCycle: () -> Unit = {},
    onNextCycle: () -> Unit = {},
    onNavigateToSolarVendor: () -> Unit = {}
) {
    Scaffold(
        containerColor = StitchSurface,
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
        var showRulesAndHistorySheet by remember { mutableStateOf(false) }

        if (showRulesAndHistorySheet) {
            CoinsRulesAndHistoryBottomSheet(
                coinBalance = uiState.coinBalance,
                streakState = uiState.streakState,
                transactions = uiState.coinTransactions,
                onDismiss = { showRulesAndHistorySheet = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Header Section with Eco Illustration
            RecommendsHeroHeader()

            // 2. Compact SolarEase Coins Card
            SolarEaseCoinsCard(
                coinBalance = uiState.coinBalance,
                streakState = uiState.streakState,
                recentTransactions = uiState.coinTransactions.take(3),
                verificationResult = uiState.billVerificationResult,
                onViewHistoryClick = { showRulesAndHistorySheet = true }
            )

            // 3. Last Billing Cycle (Verified Savings Card with Navigation)
            BillingSavingsCard(
                savings = uiState.billingCycleSavings,
                availableCycles = uiState.availableCycles,
                selectedCycleIndex = uiState.selectedCycleIndex,
                cycleComparison = uiState.cycleComparison,
                onPreviousCycle = onPreviousCycle,
                onNextCycle = onNextCycle
            )

            // 4. Today's Mantra Progress (Full-width compact card)
            TodayProgressCard(
                modifier = Modifier.fillMaxWidth(),
                checkedCount = uiState.todayProgressChecked,
                totalCount = uiState.todayProgressTotal
            )

            // 5. 9 Energy-Saving Mantras Section
            MantrasSection(
                mantras = uiState.mantras,
                isExpanded = uiState.isMantrasExpanded,
                onToggleExpand = onToggleMantrasExpanded,
                onMantraClick = onMantraClick
            )

            Spacer(modifier = Modifier.height(24.dp))
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
                            text = userAddress,
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

// ─────────────────────────────────────────────────────────────────────────────
// 2. SOLAREASE COINS CARD & WALLET COMPONENTS
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SolarEaseCoinsCard(
    coinBalance: Int,
    streakState: StreakState,
    recentTransactions: List<CoinTransaction>,
    verificationResult: BillVerificationResult?,
    onViewHistoryClick: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFFBFCF3),
        border = BorderStroke(1.dp, Color(0xFFDDE5D6)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (isExpanded) 14.dp else 0.dp)
        ) {
            // Header Row: Coin Balance (Left) & Streak Badge + Chevron (Right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Coins Icon & Balance
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isExpanded) 44.dp else 38.dp)
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
                            text = "🪙",
                            fontSize = if (isExpanded) 20.sp else 18.sp
                        )
                    }

                    Column {
                        Text(
                            text = "$coinBalance",
                            fontSize = if (isExpanded) 26.sp else 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF191C18),
                            lineHeight = if (isExpanded) 28.sp else 22.sp
                        )
                        Text(
                            text = if (coinBalance == 1) "SolarEase Coin" else "SolarEase Coins",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF43483E)
                        )
                    }
                }

                // Right: Consecutive Streak Pill + Expand/Collapse Chevron
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, Color(0xFFFFEDD5))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFEA580C),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (streakState.currentStreak > 0) "${streakState.currentStreak} Day Streak" else "0 Day Streak",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC2410C)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isExpanded = !isExpanded },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse SolarEase Coins card" else "Expand SolarEase Coins card",
                            tint = Color(0xFF43483E),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.padding(top = 14.dp)
                ) {
                    // Bill Verification Status Pill
                    BillVerificationStatusPill(verificationResult = verificationResult)

                    // 7-Step Segmented Streak Tracker
                    SevenDayStreakTracker(streakState = streakState)

                    // Divider
                    HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.8.dp)

                    // Recent Transactions Mini-Feed
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Activity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF191C18)
                            )
                            Text(
                                text = if (recentTransactions.isNotEmpty()) "Latest ${recentTransactions.size}" else "",
                                fontSize = 11.sp,
                                color = Color(0xFF73796E)
                            )
                        }

                        if (recentTransactions.isEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "No coin transactions yet. Check in a mantra to earn your first coin!",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B),
                                    lineHeight = 15.sp
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                recentTransactions.forEach { tx ->
                                    MiniTransactionRow(tx = tx)
                                }
                            }
                        }
                    }

                    // Action Button: Earning Rules & History
                    OutlinedButton(
                        onClick = onViewHistoryClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF386A20)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF386A20)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
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
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Earning Rules & History",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class StatusPillConfig(
    val bg: Color,
    val border: Color,
    val tint: Color,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val text: String
)

@Composable
private fun BillVerificationStatusPill(verificationResult: BillVerificationResult?) {
    val status = verificationResult?.status ?: BillVerificationStatus.WAITING_FOR_BILL

    val config = when (status) {
        BillVerificationStatus.WAITING_FOR_BILL -> StatusPillConfig(
            bg = Color(0xFFF1F5F9),
            border = Color(0xFFE2E8F0),
            tint = Color(0xFF64748B),
            icon = Icons.Outlined.HourglassEmpty,
            text = "Waiting for next bill data"
        )
        BillVerificationStatus.PROCESSING -> StatusPillConfig(
            bg = Color(0xFFEFF6FF),
            border = Color(0xFFDBEAFE),
            tint = Color(0xFF2563EB),
            icon = Icons.Outlined.Sync,
            text = "Evaluating electricity consumption..."
        )
        BillVerificationStatus.VERIFIED_REDUCTION -> {
            val reduction = verificationResult?.reductionPercent?.roundToInt() ?: 0
            val bonus = verificationResult?.reductionBonusEarned ?: 0
            val process = verificationResult?.billProcessedRewardEarned ?: 0
            StatusPillConfig(
                bg = Color(0xFFF0FDF4),
                border = Color(0xFFDCFCE7),
                tint = Color(0xFF16A34A),
                icon = Icons.Default.CheckCircle,
                text = "Verified: $reduction% reduction • +${bonus + process} coins"
            )
        }
        BillVerificationStatus.NO_QUALIFYING_REDUCTION -> {
            val reduction = verificationResult?.reductionPercent?.roundToInt() ?: 0
            val text = if (reduction > 0) {
                "Reduction ($reduction%) below 5% threshold • +${verificationResult?.billProcessedRewardEarned ?: 0} bill coins"
            } else {
                "No consumption reduction • +${verificationResult?.billProcessedRewardEarned ?: 0} bill coins"
            }
            StatusPillConfig(
                bg = Color(0xFFF8FAFC),
                border = Color(0xFFE2E8F0),
                tint = Color(0xFF64748B),
                icon = Icons.Outlined.Info,
                text = text
            )
        }
        BillVerificationStatus.INSUFFICIENT_HISTORY -> StatusPillConfig(
            bg = Color(0xFFFFFBEB),
            border = Color(0xFFFEF3C7),
            tint = Color(0xFFD97706),
            icon = Icons.Outlined.History,
            text = "Not enough bill history to verify yet (+${verificationResult?.billProcessedRewardEarned ?: 0} coins)"
        )
        BillVerificationStatus.INVALID_BILL -> StatusPillConfig(
            bg = Color(0xFFFEF2F2),
            border = Color(0xFFFEE2E2),
            tint = Color(0xFFDC2626),
            icon = Icons.Outlined.Warning,
            text = "Bill record has invalid or missing consumption"
        )
        BillVerificationStatus.PROCESSING_ERROR -> StatusPillConfig(
            bg = Color(0xFFFEF2F2),
            border = Color(0xFFFEE2E2),
            tint = Color(0xFFDC2626),
            icon = Icons.Outlined.ErrorOutline,
            text = "Consumption baseline calculation error"
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = config.bg,
        border = BorderStroke(1.dp, config.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = config.icon,
                contentDescription = null,
                tint = config.tint,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = config.text,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1E293B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SevenDayStreakTracker(streakState: StreakState) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "7-Day Milestone Progress",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF191C18)
            )
            Text(
                text = "Day ${streakState.daysInCurrentCycle} of 7 • Cycle ${streakState.currentMilestoneCycle}",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF386A20)
            )
        }

        // 7 Segmented Day Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (day in 1..7) {
                val isCompleted = day <= streakState.daysInCurrentCycle
                val isMilestoneDay = day == 7

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when {
                                isMilestoneDay && isCompleted -> Color(0xFFF59E0B)
                                isMilestoneDay -> Color(0xFFFFFBEB)
                                isCompleted -> Color(0xFF386A20)
                                else -> Color(0xFFF1F5F9)
                            }
                        )
                        .border(
                            1.dp,
                            when {
                                isMilestoneDay && isCompleted -> Color(0xFFD97706)
                                isMilestoneDay -> Color(0xFFFDE68A)
                                isCompleted -> Color(0xFF2D5519)
                                else -> Color(0xFFE2E8F0)
                            },
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isMilestoneDay) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardGiftcard,
                                contentDescription = "Milestone Reward",
                                tint = if (isCompleted) Color.White else Color(0xFFD97706),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "+5",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCompleted) Color.White else Color(0xFFB45309)
                            )
                        }
                    } else if (isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed Day $day",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "D$day",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        // Milestone Subtext
        Text(
            text = if (streakState.daysInCurrentCycle == 7) {
                "🎉 Cycle ${streakState.currentMilestoneCycle} complete! +5 bonus coins awarded. Check in tomorrow to advance into Cycle ${streakState.currentMilestoneCycle + 1}!"
            } else {
                "${7 - streakState.daysInCurrentCycle} day(s) until your next 5-coin milestone bonus."
            },
            fontSize = 11.sp,
            color = Color(0xFF64748B),
            lineHeight = 15.sp
        )
    }
}

@Composable
private fun MiniTransactionRow(tx: CoinTransaction) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when (tx.eventType) {
                                CoinEventType.STREAK_MILESTONE -> Color(0xFFFEF3C7)
                                CoinEventType.BILL_PROCESSED,
                                CoinEventType.VERIFIED_CONSUMPTION_REDUCTION -> Color(0xFFEFF6FF)
                                else -> Color(0xFFDCFCE7)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (tx.eventType) {
                            CoinEventType.STREAK_MILESTONE -> Icons.Default.CardGiftcard
                            CoinEventType.BILL_PROCESSED,
                            CoinEventType.VERIFIED_CONSUMPTION_REDUCTION -> Icons.Outlined.TrendingDown
                            else -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = when (tx.eventType) {
                            CoinEventType.STREAK_MILESTONE -> Color(0xFFD97706)
                            CoinEventType.BILL_PROCESSED,
                            CoinEventType.VERIFIED_CONSUMPTION_REDUCTION -> Color(0xFF2563EB)
                            else -> Color(0xFF16A34A)
                        },
                        modifier = Modifier.size(13.dp)
                    )
                }

                Column {
                    Text(
                        text = tx.description,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C18),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTransactionTimestamp(tx.timestamp),
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = "+${tx.amount}",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF386A20)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 3. TODAY'S PROGRESS CARD (FULL-WIDTH COMPACT)
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
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF386A20).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF386A20),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Today's Mantra Progress",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF191C18)
                    )
                }

                Text(
                    text = "$checkedCount of $totalCount completed",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF386A20)
                )
            }

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
                        .background(Color(0xFF386A20))
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// EARNING RULES & HISTORY BOTTOM SHEET
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CoinsRulesAndHistoryBottomSheet(
    coinBalance: Int,
    streakState: StreakState,
    transactions: List<CoinTransaction>,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTabIndex by remember { mutableIntStateOf(0) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SolarEase Coins & Rules",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Balance Banner
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF386A20)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Available Balance",
                            fontSize = 12.sp,
                            color = Color(0xFFE2E8F0)
                        )
                        Text(
                            text = "$coinBalance Coins",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "${streakState.currentStreak} Day Streak",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Tab Row
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = Color(0xFF386A20),
                divider = { HorizontalDivider(color = Color(0xFFE2E8F0)) }
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = {
                        Text(
                            text = "Activity History (${transactions.size})",
                            fontWeight = if (selectedTabIndex == 0) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = {
                        Text(
                            text = "Earning Rules",
                            fontWeight = if (selectedTabIndex == 1) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = 380.dp)
            ) {
                if (selectedTabIndex == 0) {
                    // Full Transaction History
                    if (transactions.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ReceiptLong,
                                    contentDescription = null,
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No transactions recorded yet",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Complete daily mantras or verify electric bills to earn SolarEase Coins.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                lineHeight = 16.sp
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            transactions.forEach { tx ->
                                MiniTransactionRow(tx = tx)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // Earning Rules Breakdown
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        EarningRuleItem(
                            icon = Icons.Outlined.CheckCircle,
                            iconTint = Color(0xFF16A34A),
                            title = "Daily Energy-Saving Activity • 1 Coin",
                            description = "Check in any of the 9 Energy-Saving Mantras or record an eligible daily meter reading. Awarded once per calendar day (unchecking a mantra does not revoke an earned coin)."
                        )

                        EarningRuleItem(
                            icon = Icons.Outlined.LocalFireDepartment,
                            iconTint = Color(0xFFEA580C),
                            title = "7-Day Streak Milestone • 5 Bonus Coins",
                            description = "Complete each 7-day cycle of consecutive qualifying activity days. Missing a full calendar day resets the active streak when you next qualify."
                        )

                        EarningRuleItem(
                            icon = Icons.Default.Description,
                            iconTint = Color(0xFF2563EB),
                            title = "Newly Validated Bill Processing • 10 Coins",
                            description = "Awarded once for each newly received, successfully validated electricity bill from your utility service."
                        )

                        EarningRuleItem(
                            icon = Icons.Outlined.TrendingDown,
                            iconTint = Color(0xFF059669),
                            title = "Verified Consumption Reduction • Up to 50 Coins",
                            description = "Compares normalized daily consumption (kWh/day) against the average of up to 3 preceding valid bills:\n• 5% to <10% reduction: 15 bonus coins\n• 10% to <20% reduction: 30 bonus coins\n• 20% or greater reduction: 50 bonus coins\n• Below 5% reduction: No reduction bonus\nOnly the highest qualifying tier is awarded per bill."
                        )

                        EarningRuleItem(
                            icon = Icons.Outlined.Info,
                            iconTint = Color(0xFF64748B),
                            title = "Historical Baseline Policy",
                            description = "Pre-existing cached bills on first activation establish your historical baseline and do not earn retroactive rewards. If prior bill history is insufficient, shows 'Not enough bill history to verify yet.'"
                        )

                        // Disclaimer Card
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Important Disclaimer",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Redemption is not available yet. Coins currently track your qualifying activities and verified rewards. SolarEase does not promise cash conversion, payouts, or external redemption at this stage.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF92400E),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun EarningRuleItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    description: String
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF191C18)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 15.sp
                )
            }
        }
    }
}

private fun formatTransactionTimestamp(timestampMs: Long): String {
    return try {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(timestampMs))
    } catch (_: Exception) {
        ""
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
    var isExpanded by remember { mutableStateOf(false) }

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

    // Difference calculation: difference = currentCycleConsumption - previousCycleConsumption
    val hasValidComparison = previousCycle != null || savings.previousUnits > 0f
    val consumptionDiff = currUnits - prevUnits
    val isDecrease = hasValidComparison && (consumptionDiff < -0.01f || state == BillingComparisonState.COMPLETED_SAVED || state == BillingComparisonState.CURRENT_LOWER)
    val isIncreaseDiff = hasValidComparison && (consumptionDiff > 0.01f || state == BillingComparisonState.COMPLETED_INCREASED || state == BillingComparisonState.CURRENT_HIGHER)
    val absDiffKwh = kotlin.math.abs(consumptionDiff).toInt()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row: Title & Expand/Collapse Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
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
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Last Billing Cycle",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Last Billing Cycle card" else "Expand Last Billing Cycle card",
                        tint = Color(0xFF475569),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Collapsed Summary (Always visible):
            // Previous Cycle column | Middle Difference Pill | Current/Selected Cycle column
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Middle Difference Pill (compact rounded pill with arrow and kWh difference)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = when {
                        isIncreaseDiff -> Color(0xFFFEF3C7)
                        isDecrease -> Color(0xFFD1FAE5)
                        else -> Color(0xFFF1F5F9)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isIncreaseDiff -> Color(0xFFFDE68A)
                            isDecrease -> Color(0xFFA7F3D0)
                            else -> Color(0xFFE2E8F0)
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                isIncreaseDiff -> Icons.Default.ArrowUpward
                                isDecrease -> Icons.Default.ArrowDownward
                                else -> Icons.Default.Remove
                            },
                            contentDescription = null,
                            tint = when {
                                isIncreaseDiff -> Color(0xFFD97706)
                                isDecrease -> Color(0xFF059669)
                                else -> Color(0xFF64748B)
                            },
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = when {
                                isIncreaseDiff -> "$absDiffKwh kWh"
                                isDecrease -> "$absDiffKwh kWh"
                                hasValidComparison -> "0 kWh"
                                else -> "—"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isIncreaseDiff -> Color(0xFFB45309)
                                isDecrease -> Color(0xFF047857)
                                else -> Color(0xFF475569)
                            }
                        )
                    }
                }

                // Column 2: Selected / Current Cycle
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalAlignment = Alignment.End
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
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
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
                                    text = "Done",
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

            // Expanded Section: Cycle Selector & Full Comparison Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cycle Navigation (< [Period • Live/Done] >)
                    if (availableCycles.size > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select Cycle",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B)
                            )
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

                    // Divider separating cycle columns from comparison section
                    HorizontalDivider(
                        color = if (isIncreased) Color(0xFFFDE68A).copy(alpha = 0.7f) else Color(0xFFBBF7D0).copy(alpha = 0.7f),
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(top = 2.dp)
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
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 5. 9 ENERGY-SAVING MANTRAS SECTION
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun MantrasSection(
    mantras: List<MantraItem>,
    isExpanded: Boolean = true,
    onToggleExpand: () -> Unit = {},
    onMantraClick: (Int) -> Unit
) {
    val checkedCount = mantras.count { it.isCheckedIn }
    val totalCount = mantras.size

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Header Row: Heading + Description (Left) & Progress Pill + Chevron (Right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
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

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (!isExpanded) {
                    Surface(
                        modifier = Modifier.testTag("mantras_progress_pill"),
                        shape = RoundedCornerShape(20.dp),
                        color = if (checkedCount == totalCount) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (checkedCount == totalCount) Color(0xFFBBF7D0) else Color(0xFFE2E8F0)
                        )
                    ) {
                        Text(
                            text = "$checkedCount of $totalCount completed",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (checkedCount == totalCount) Color(0xFF16A34A) else Color(0xFF64748B),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleExpand,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                        contentDescription = if (isExpanded) "Collapse 9 Energy-Saving Mantras" else "Expand 9 Energy-Saving Mantras",
                        tint = Color(0xFF43483E),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                mantras.forEach { item ->
                    MantraCard(
                        item = item,
                        onClick = { onMantraClick(item.id) }
                    )
                }
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
                                    text = "Checked In",
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
                                text = "Check In",
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

// ─────────────────────────────────────────────────────────────────────────────
// 6. SOLAR VENDOR / SOLAR POTENTIAL CARD
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SolarVendorCard(
    onExploreSolar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onExploreSolar),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF7FAF4),
        border = BorderStroke(1.dp, Color(0xFFD6E4CF)),
        shadowElevation = 0.5.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Subtle primary green accent solar visual
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE8F5E1))
                    .border(1.dp, Color(0xFFC7DEC0), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SolarPower,
                    contentDescription = "Solar Potential",
                    tint = Color(0xFF386A20),
                    modifier = Modifier.size(22.dp)
                )
            }

            Text(
                text = "How much could your home reduce its carbon footprint?",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                lineHeight = 21.sp
            )

            Text(
                text = "Explore your solar potential and connect with trusted solar vendors near you.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Button(
                onClick = onExploreSolar,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF386A20),
                    contentColor = Color.White
                ),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    text = "Explore Solar",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

package com.example.feature.recommends.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.recommends.model.RecommendsUiState
import com.example.feature.recommends.ui.modules.BillingSavingsCard
import com.example.feature.recommends.ui.modules.CoinsRulesAndHistoryBottomSheet
import com.example.feature.recommends.ui.modules.MantrasSection
import com.example.feature.recommends.ui.modules.RecommendsHeroHeader
import com.example.feature.recommends.ui.modules.RecommendsTopBar
import com.example.feature.recommends.ui.modules.SolarEaseCoinsCard
import com.example.feature.recommends.ui.modules.TodayProgressCard
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.components.SolarEaseNavigationRail
import com.example.ui.theme.SolarEaseTheme
import com.example.ui.theme.rememberSolarEaseWindowLayoutInfo

@Composable
fun RecommendsRoute(
    onNavigateToHome: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToAnalytics: () -> Unit = {},
    onNavigateToSolarLens: () -> Unit = {},
    onNavigateToSolarEstimate: () -> Unit = {},
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
        onNavigateToSolarEstimate = onNavigateToSolarEstimate
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
    onNavigateToSolarEstimate: () -> Unit = {}
) {
    val layoutInfo = rememberSolarEaseWindowLayoutInfo()
    val colors = SolarEaseTheme.colors

    Scaffold(
        containerColor = colors.surface,
        topBar = {
            RecommendsTopBar(
                userAddress = uiState.userAddress,
                onProfileClick = onNavigateToProfile
            )
        },
        bottomBar = {
            if (!layoutInfo.useNavigationRail) {
                SolarEaseBottomNavigation(
                    currentTab = SolarEaseNavTab.RECOMMENDS,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToRecommends = { /* Already on Recommends */ },
                    onNavigateToProfile = onNavigateToProfile
                )
            }
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

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (layoutInfo.useNavigationRail) {
                SolarEaseNavigationRail(
                    currentTab = SolarEaseNavTab.RECOMMENDS,
                    onNavigateToHome = onNavigateToHome,
                    onNavigateToRecommends = { /* Already on Recommends */ },
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
                if (layoutInfo.useTwoColumnGrid) {
                    // Adaptive Two-Column Layout on Wide Screens (Tablet / Desktop >= 600dp)
                    Row(
                        modifier = Modifier
                            .widthIn(max = layoutInfo.maxContentWidth)
                            .fillMaxWidth()
                            .padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Left Column: Hero, Coins Card, Billing Savings Card
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            RecommendsHeroHeader()

                            SolarEaseCoinsCard(
                                coinBalance = uiState.coinBalance,
                                streakState = uiState.streakState,
                                recentTransactions = uiState.coinTransactions.take(3),
                                verificationResult = uiState.billVerificationResult,
                                onViewHistoryClick = { showRulesAndHistorySheet = true }
                            )

                            BillingSavingsCard(
                                savings = uiState.billingCycleSavings,
                                availableCycles = uiState.availableCycles,
                                selectedCycleIndex = uiState.selectedCycleIndex,
                                cycleComparison = uiState.cycleComparison,
                                onPreviousCycle = onPreviousCycle,
                                onNextCycle = onNextCycle
                            )
                        }

                        // Right Column: Today's Progress & Mantras Section
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            TodayProgressCard(
                                modifier = Modifier.fillMaxWidth(),
                                checkedCount = uiState.todayProgressChecked,
                                totalCount = uiState.todayProgressTotal
                            )

                            MantrasSection(
                                mantras = uiState.mantras,
                                isExpanded = uiState.isMantrasExpanded,
                                onToggleExpand = onToggleMantrasExpanded,
                                onMantraClick = onMantraClick
                            )
                            Spacer(modifier = Modifier.height(48.dp))
                        }
                    }
                } else {
                    // Phone Portrait / Compact Layout: Sequential Single Column
                    Column(
                        modifier = Modifier
                            .widthIn(max = layoutInfo.maxContentWidth)
                            .fillMaxWidth()
                            .padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. Hero Header Section
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
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        }
    }
}

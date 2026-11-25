package com.example.feature.recommends.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.feature.recommends.model.RecommendsUiState
import com.example.feature.recommends.ui.modules.MantrasSection
import com.example.feature.recommends.ui.modules.RecommendsHeroHeader
import com.example.feature.recommends.ui.modules.RecommendsTopBar
import com.example.feature.recommends.ui.modules.TodayProgressCard
import com.example.ui.components.SolarEaseBottomNavigation
import com.example.ui.components.SolarEaseNavTab
import com.example.ui.components.SolarEaseNavigationRail
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.LocalIsReducedMotionEnabled
import com.example.ui.theme.SolarBrandBackground
import com.example.ui.theme.SolarEaseMotion
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
    val isDark = LocalIsDarkTheme.current

    Scaffold(
        containerColor = if (isDark) MaterialTheme.colorScheme.background else SolarBrandBackground,
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
                Column(
                    modifier = Modifier
                        .widthIn(max = layoutInfo.maxContentWidth)
                        .fillMaxWidth()
                        .padding(horizontal = layoutInfo.horizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Hero Header Section
                    StaggeredRecommendsItem(index = 0) {
                        RecommendsHeroHeader()
                    }

                    // 2. Today's Mantra Progress (Full-width compact card)
                    StaggeredRecommendsItem(index = 1) {
                        TodayProgressCard(
                            modifier = Modifier.fillMaxWidth(),
                            checkedCount = uiState.todayProgressChecked,
                            totalCount = uiState.todayProgressTotal
                        )
                    }

                    // 3. 9 Energy-Saving Mantras Section
                    StaggeredRecommendsItem(index = 2) {
                        MantrasSection(
                            mantras = uiState.mantras,
                            isExpanded = uiState.isMantrasExpanded,
                            onToggleExpand = onToggleMantrasExpanded,
                            onMantraClick = onMantraClick
                        )
                    }

                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }
}

@Composable
private fun StaggeredRecommendsItem(
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
                    delayMillis = (index * 55).coerceAtMost(240),
                    easing = SolarEaseMotion.easingDecelerate
                )
            ) + slideInVertically(
                animationSpec = tween(
                    durationMillis = 300,
                    delayMillis = (index * 55).coerceAtMost(240),
                    easing = SolarEaseMotion.easingDecelerate
                ),
                initialOffsetY = { it / 10 }
            )
        ) {
            content()
        }
    }
}

package com.example.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.toRoute
import com.example.feature.auth.ui.AuthViewModel
import com.example.feature.auth.ui.AuthPhoneRoute
import com.example.feature.auth.ui.AuthMainRoute
import com.example.feature.auth.ui.AuthOtpRoute

import com.example.feature.dashboard.ui.DashboardRoute
import com.example.feature.systemdetails.ui.SystemDetailsRoute
import com.example.feature.performance.ui.PerformanceRoute
import com.example.feature.financial.ui.FinancialRoute
import com.example.feature.environmental.ui.EnvironmentalRoute
import com.example.feature.home.ui.HomeRoute
import com.example.feature.home.ui.modules.BillCalculatorRoute
import com.example.feature.home.ui.modules.ElectricityChatbotRoute
import com.example.feature.lens.ui.LensLandingScreen
import com.example.feature.lens.ui.LensScreen
import com.example.feature.lens.ui.LensViewModel

import com.example.feature.lens.ui.ApplianceResultScreen
import com.example.core.network.RetrofitClient
import com.example.feature.lens.data.LensRepository
import com.example.feature.home.ui.modules.TnebBillStatusRoute
import com.example.feature.home.ui.modules.BillHistoryRoute
import com.example.feature.home.ui.modules.SolarPanelRoute
import com.example.feature.ebservice.ui.EbServiceNumberRoute
import com.example.feature.assessment.ui.CustomerCategoryRoute
import com.example.feature.assessment.ui.SystemTypeRoute
import com.example.feature.assessment.ui.MonthlyConsumptionRoute
import com.example.feature.assessment.ui.MountTypeRoute
import com.example.feature.assessment.ui.AvailableAreaRoute
import com.example.feature.assessment.ui.RecommendationRoute
import com.example.feature.assessment.ui.PanelSelectionRoute
import com.example.feature.assessment.ui.NearbyVendorsRoute
import com.example.feature.assessment.ui.VendorProfileRoute
import com.example.feature.assessment.ui.EstimateRequestRoute
import com.example.feature.assessment.ui.RequestConfirmationRoute
import com.example.feature.assessment.ui.SolarVendorViewModel
import com.example.feature.assessment.ui.AssessmentInputRoute
import com.example.feature.assessment.ui.CostEstimationRoute
import com.example.feature.assessment.util.SolarEstimateResult
import com.example.feature.order.ui.PaymentOptionRoute
import com.example.feature.order.ui.OrderSummaryRoute
import com.example.feature.order.ui.SiteSurveyStatusRoute
import com.example.feature.order.ui.InstallationTrackingRoute

private fun isTopLevel(destination: NavDestination): Boolean {
    return destination.hasRoute<Screen.Home>() ||
           destination.hasRoute<Screen.Recommends>() ||
           destination.hasRoute<Screen.Profile>() ||
           destination.hasRoute<Screen.Analytics>()
}

private fun isTopLevelTransition(initial: NavDestination, target: NavDestination): Boolean {
    return isTopLevel(initial) && isTopLevel(target)
}

private fun NavHostController.navigateToTopLevel(destination: Screen) {
    if (destination == Screen.Home) {
        navigate(Screen.Home) {
            popUpTo(Screen.Home) {
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    } else {
        navigate(destination) {
            popUpTo(Screen.Home) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
}

@Composable
fun SolarEaseNavGraph(navController: NavHostController, startDestination: Any = Screen.AuthGraph) {
    val solarVendorViewModel: SolarVendorViewModel = viewModel()
    NavHost(
        navController = navController,
        startDestination = startDestination,
        enterTransition = {
            if (isTopLevelTransition(initialState.destination, targetState.destination)) {
                EnterTransition.None
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        },
        exitTransition = {
            if (isTopLevelTransition(initialState.destination, targetState.destination)) {
                ExitTransition.None
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        },
        popEnterTransition = {
            if (isTopLevelTransition(initialState.destination, targetState.destination)) {
                EnterTransition.None
            } else {
                slideInHorizontally(
                    initialOffsetX = { fullWidth -> -fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        },
        popExitTransition = {
            if (isTopLevelTransition(initialState.destination, targetState.destination)) {
                ExitTransition.None
            } else {
                slideOutHorizontally(
                    targetOffsetX = { fullWidth -> fullWidth },
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                )
            }
        }
    ) {
        navigation<Screen.AuthGraph>(startDestination = Screen.AuthMain) {
            composable<Screen.AuthMain> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.AuthGraph>()
                }
                val authViewModel: AuthViewModel = viewModel(parentEntry)
                AuthMainRoute(
                    viewModel = authViewModel,
                    onNavigateToPhoneAuth = {
                        navController.navigate(Screen.AuthPhone)
                    },
                    onNavigateToHome = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.AuthGraph) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.AuthPhone> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.AuthGraph>()
                }
                val authViewModel: AuthViewModel = viewModel(parentEntry)
                AuthPhoneRoute(
                    viewModel = authViewModel,
                    onNavigateToOtp = { fullName, phone ->
                        navController.navigate(Screen.AuthOtp(fullName, phone))
                    }
                )
            }

            composable<Screen.AuthOtp> { backStackEntry ->
                val args = backStackEntry.toRoute<Screen.AuthOtp>()
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.AuthGraph>()
                }
                val authViewModel: AuthViewModel = viewModel(parentEntry)
                AuthOtpRoute(
                    fullName = args.fullName,
                    phoneNumber = args.phoneNumber,
                    viewModel = authViewModel,
                    onNavigateToHome = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.AuthGraph) { inclusive = true }
                        }
                    },
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }
        }

        composable<Screen.Home> {
            HomeRoute(
                onNavigateToBillCalculator = { navController.navigate(Screen.BillCalculator) },
                onNavigateToElectricityChatbot = { navController.navigate(Screen.ElectricityChatbot) },
                onNavigateToSolarEaseLens = { navController.navigate(Screen.SolarEaseLens) },
                onNavigateToTnebBillStatus = { navController.navigate(Screen.TnebBillStatus) },
                onNavigateToBillHistory = { navController.navigate(Screen.BillHistory) },
                onNavigateToSolarPanel = { navController.navigate(Screen.SolarPanel) },
                onNavigateToProfile = { navController.navigateToTopLevel(Screen.Profile) },
                onNavigateToRecommends = { navController.navigateToTopLevel(Screen.Recommends) },
                onNavigateToMeterScanner = { navController.navigate(Screen.MeterScanner) }
            )
        }

        composable<Screen.Analytics> {
            com.example.feature.analytics.ui.AnalyticsRoute(
                onNavigateToHome = {
                    navController.navigateToTopLevel(Screen.Home)
                },
                onNavigateToRecommends = {
                    navController.navigateToTopLevel(Screen.Recommends)
                },
                onNavigateToProfile = {
                    navController.navigateToTopLevel(Screen.Profile)
                },
                onNavigateToTnebBillStatus = {
                    navController.navigate(Screen.TnebBillStatus)
                },
                onNavigateToLogMeterReading = {
                    navController.navigateToTopLevel(Screen.Home)
                }
            )
        }

        composable<Screen.Recommends> {
            com.example.feature.recommends.ui.RecommendsRoute(
                onNavigateToHome = {
                    navController.navigateToTopLevel(Screen.Home)
                },
                onNavigateToProfile = {
                    navController.navigateToTopLevel(Screen.Profile)
                },
                onNavigateToSolarLens = {
                    navController.navigate(Screen.SolarEaseLens)
                },
                onNavigateToSolarEstimate = {
                    navController.navigate(Screen.EbServiceNumber)
                }
            )
        }
        
        composable<Screen.BillCalculator> {
            BillCalculatorRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSolarLens = { navController.navigate(Screen.SolarEaseLens) }
            )
        }
        
        composable<Screen.ElectricityChatbot> {
            ElectricityChatbotRoute(onNavigateBack = { navController.popBackStack() })
        }
        
        navigation<Screen.SolarEaseLens>(startDestination = Screen.LensMain) {
            composable<Screen.LensMain> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.SolarEaseLens>()
                }
                val lensViewModel: LensViewModel = viewModel(
                    parentEntry,
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return LensViewModel(LensRepository(RetrofitClient.lensApi)) as T
                        }
                    }
                )
                LensLandingScreen(
                    viewModel = lensViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onOpenCamera = {
                        navController.navigate(Screen.LensCamera)
                    },
                    onNavigateToReview = { navController.navigate(Screen.LensReview) { launchSingleTop = true } },
                    onNavigateToAnalytics = { navController.navigate(Screen.LensResult) { launchSingleTop = true } }
                )
            }

            composable<Screen.LensCamera> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.SolarEaseLens>()
                }
                val lensViewModel: LensViewModel = viewModel(
                    parentEntry,
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                            return LensViewModel(LensRepository(RetrofitClient.lensApi)) as T
                        }
                    }
                )
                LensScreen(
                    viewModel = lensViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToReview = {
                        navController.navigate(Screen.LensReview) {
                            popUpTo(Screen.LensCamera) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
            


            composable<Screen.LensReview> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.SolarEaseLens>()
                }
                val lensViewModel: LensViewModel = viewModel(parentEntry)
                com.example.feature.lens.ui.ApplianceReviewScreen(
                    viewModel = lensViewModel,
                    onNavigateBack = {
                        navController.popBackStack(Screen.LensMain, false)
                    },
                    onNavigateToResult = { navController.navigate(Screen.LensResult) }
                )
            }

            composable<Screen.LensResult> { backStackEntry ->
                val parentEntry = remember(backStackEntry) {
                    navController.getBackStackEntry<Screen.SolarEaseLens>()
                }
                val lensViewModel: LensViewModel = viewModel(parentEntry)
                ApplianceResultScreen(
                    viewModel = lensViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onScanAnother = { 
                        lensViewModel.startNewAudit()
                        navController.popBackStack(Screen.LensMain, false)
                    },
                    onNavigateHome = {
                        navController.navigate(Screen.Home) {
                            popUpTo(Screen.Home) { inclusive = false }
                        }
                    }
                )
            }
        }
        
        composable<Screen.TnebBillStatus> {
            TnebBillStatusRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSolarLens = { navController.navigate(Screen.SolarEaseLens) },
                onReturnToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Screen.MeterScanner> {
            com.example.feature.meterreading.ui.MeterScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onReadingConfirmed = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.BillHistory> {
            BillHistoryRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSolarLens = { navController.navigate(Screen.SolarEaseLens) }
            )
        }
        
        composable<Screen.SolarPanel> {
            SolarPanelRoute(onNavigateBack = { navController.popBackStack() })
        }

        // ── Stitch 12-Step Solar Vendor Journey ──────────────────────────────
        // Step 1: Consumer Number
        composable<Screen.EbServiceNumber> {
            EbServiceNumberRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.CustomerCategory) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 2: Property Type / Category
        composable<Screen.CustomerCategory> {
            CustomerCategoryRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.SystemType) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 3: Solar System Type
        composable<Screen.SystemType> {
            SystemTypeRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.MonthlyConsumption) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 4: Monthly Electricity Consumption
        composable<Screen.MonthlyConsumption> {
            MonthlyConsumptionRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.MountType) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 5: Installation Location / Mount Type
        composable<Screen.MountType> {
            MountTypeRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.AvailableArea) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 6: Available Installation Area
        composable<Screen.AvailableArea> {
            AvailableAreaRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.Recommendation) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 7: Solar System Recommendation
        composable<Screen.Recommendation> {
            RecommendationRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = { navController.navigate(Screen.PanelSelection) },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 8: DCR or Non-DCR Panel Selection -> Proceeds to Cost Estimation
        composable<Screen.PanelSelection> {
            PanelSelectionRoute(
                vendorViewModel = solarVendorViewModel,
                onNavigateNext = {
                    val s = solarVendorViewModel.state.value
                    navController.navigate(
                        Screen.CostEstimation(
                            monthlyConsumption = s.monthlyConsumptionKwh,
                            availableArea = s.availableAreaSqM,
                            mountType = s.mountType,
                            systemType = s.systemType,
                            panelType = s.panelType,
                            customerType = s.propertyType
                        )
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 9: Cost Estimation Breakdown
        composable<Screen.CostEstimation> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.CostEstimation>()
            CostEstimationRoute(
                monthlyConsumption = route.monthlyConsumption,
                availableArea = route.availableArea,
                mountType = route.mountType,
                systemType = route.systemType,
                panelType = route.panelType,
                customerType = route.customerType,
                onNavigateNext = { result ->
                    navController.navigate(
                        Screen.PaymentOption(
                            recommendedCapacityKW = result.recommendedCapacityKW,
                            requiredArea = result.requiredArea,
                            availableArea = result.availableArea,
                            areaStatus = result.areaStatus,
                            systemType = result.systemType,
                            mountType = result.mountType,
                            panelType = result.panelType,
                            customerType = result.customerType,
                            baseRatePerKW = result.baseRatePerKW,
                            panelsCost = result.panelsCost,
                            mountingCost = result.mountingCost,
                            labourCost = result.labourCost,
                            foundationCost = result.foundationCost,
                            grossTotal = result.grossTotal,
                            subsidyEligible = result.subsidyEligible,
                            subsidyReason = result.subsidyReason,
                            subsidyAmount = result.subsidyAmount,
                            netPayable = result.netPayable,
                            annualSavings = result.annualSavings,
                            estimatedPaybackYears = result.estimatedPaybackYears
                        )
                    )
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // Step 10: Payment & Financing Options
        composable<Screen.PaymentOption> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.PaymentOption>()
            val estimateResult = remember(route) {
                SolarEstimateResult(
                    availableArea = route.availableArea,
                    mountType = route.mountType,
                    systemType = route.systemType,
                    panelType = route.panelType,
                    customerType = route.customerType,
                    recommendedCapacityKW = route.recommendedCapacityKW,
                    requiredArea = route.requiredArea,
                    areaStatus = route.areaStatus,
                    baseRatePerKW = route.baseRatePerKW,
                    panelsCost = route.panelsCost,
                    mountingCost = route.mountingCost,
                    labourCost = route.labourCost,
                    foundationCost = route.foundationCost,
                    grossTotal = route.grossTotal,
                    subsidyEligible = route.subsidyEligible,
                    subsidyReason = route.subsidyReason,
                    subsidyAmount = route.subsidyAmount,
                    netPayable = route.netPayable,
                    annualSavings = route.annualSavings,
                    estimatedPaybackYears = route.estimatedPaybackYears
                )
            }
            PaymentOptionRoute(
                estimateResult = estimateResult,
                onNavigateNext = { paymentMethod ->
                    navController.navigate(
                        Screen.OrderSummary(
                            recommendedCapacityKW = route.recommendedCapacityKW,
                            systemType = route.systemType,
                            mountType = route.mountType,
                            panelType = route.panelType,
                            paymentMethod = paymentMethod,
                            grossTotal = route.grossTotal,
                            subsidyAmount = route.subsidyAmount,
                            netPayable = route.netPayable
                        )
                    )
                }
            )
        }

        // Step 11: Order Summary
        composable<Screen.OrderSummary> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.OrderSummary>()
            OrderSummaryRoute(
                recommendedCapacityKW = route.recommendedCapacityKW,
                systemType = route.systemType,
                mountType = route.mountType,
                panelType = route.panelType,
                paymentMethod = route.paymentMethod,
                grossTotal = route.grossTotal,
                subsidyAmount = route.subsidyAmount,
                netPayable = route.netPayable,
                onNavigateNext = {
                    navController.navigate(Screen.InstallationTracking(systemType = route.systemType))
                }
            )
        }

        // Step 12: Installation Tracking
        composable<Screen.InstallationTracking> { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.InstallationTracking>()
            InstallationTrackingRoute(
                systemType = route.systemType,
                onNavigateNext = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Screen.CustomerDashboard> {
            DashboardRoute(
                onNavigateToMySolarSystem = { navController.navigate(Screen.MySolarSystem) },
                onNavigateToPerformance = { navController.navigate(Screen.Performance) },
                onNavigateToFinancial = { navController.navigate(Screen.Financial) },
                onNavigateToEnvironmental = { navController.navigate(Screen.Environmental) }
            )
        }
        
        composable<Screen.MySolarSystem> {
            SystemDetailsRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.Performance> {
            PerformanceRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.Financial> {
            FinancialRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Environmental> {
            EnvironmentalRoute(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Profile> {
            com.example.feature.profile.ui.ProfileRoute(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToHome = {
                    navController.navigateToTopLevel(Screen.Home)
                },
                onNavigateToRecommends = {
                    navController.navigateToTopLevel(Screen.Recommends)
                },
                onSignOut = {
                    navController.navigate(Screen.AuthGraph) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }
    }
}

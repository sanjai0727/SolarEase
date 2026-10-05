package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
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

@Composable
fun SolarEaseNavGraph(navController: NavHostController, startDestination: Any = Screen.AuthGraph) {
    NavHost(
        navController = navController,
        startDestination = startDestination
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
                onNavigateToProfile = { navController.navigate(Screen.Profile) { launchSingleTop = true } },
                onNavigateToRecommends = { navController.navigate(Screen.Recommends) { launchSingleTop = true } }
            )
        }

        composable<Screen.Analytics> {
            com.example.feature.analytics.ui.AnalyticsRoute(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRecommends = {
                    navController.navigate(Screen.Recommends) { launchSingleTop = true }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile) { launchSingleTop = true }
                },
                onNavigateToTnebBillStatus = {
                    navController.navigate(Screen.TnebBillStatus)
                },
                onNavigateToLogMeterReading = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<Screen.Recommends> {
            com.example.feature.recommends.ui.RecommendsRoute(
                onNavigateToHome = {
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile) { launchSingleTop = true }
                },
                onNavigateToSolarLens = {
                    navController.navigate(Screen.SolarEaseLens)
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
                onNavigateToSolarLens = { navController.navigate(Screen.SolarEaseLens) }
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
                    navController.navigate(Screen.Home) {
                        popUpTo(Screen.Home) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToRecommends = {
                    navController.navigate(Screen.Recommends) { launchSingleTop = true }
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

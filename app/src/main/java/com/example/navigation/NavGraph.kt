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
import com.example.feature.home.ui.modules.SolarEaseLensRoute
import com.example.feature.home.ui.modules.TnebBillStatusRoute
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
                onNavigateToSolarPanel = { navController.navigate(Screen.SolarPanel) },
                onNavigateToProfile = { }
            )
        }
        
        composable<Screen.BillCalculator> {
            BillCalculatorRoute(onNavigateBack = { navController.popBackStack() })
        }
        
        composable<Screen.ElectricityChatbot> {
            ElectricityChatbotRoute(onNavigateBack = { navController.popBackStack() })
        }
        
        composable<Screen.SolarEaseLens> {
            SolarEaseLensRoute(onNavigateBack = { navController.popBackStack() })
        }
        
        composable<Screen.TnebBillStatus> {
            TnebBillStatusRoute(onNavigateBack = { navController.popBackStack() })
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
    }
}

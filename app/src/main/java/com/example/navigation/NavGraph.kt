package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.feature.auth.ui.AuthPhoneRoute
import com.example.feature.auth.ui.AuthOtpRoute
import com.example.feature.auth.ui.AuthProfileRoute
import com.example.feature.assessment.ui.CustomerCategoryRoute
import com.example.feature.assessment.ui.SystemTypeRoute
import com.example.feature.assessment.ui.AssessmentInputRoute
import com.example.feature.assessment.ui.RecommendationRoute
import com.example.feature.assessment.ui.PanelSelectionRoute
import com.example.feature.assessment.ui.CostEstimationRoute
import com.example.feature.order.ui.PaymentOptionRoute
import com.example.feature.order.ui.OrderSummaryRoute
import com.example.feature.order.ui.SiteSurveyStatusRoute
import com.example.feature.order.ui.InstallationTrackingRoute

@Composable
fun SolarEaseNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.AuthPhone
    ) {
        composable<Screen.AuthPhone> {
            AuthPhoneRoute(
                onNavigateToOtp = { phone ->
                    navController.navigate(Screen.AuthOtp(phone))
                }
            )
        }
        
        composable<Screen.AuthOtp> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.AuthOtp>()
            AuthOtpRoute(
                phoneNumber = args.phoneNumber,
                onNavigateToProfile = {
                    navController.navigate(Screen.AuthProfile) {
                        popUpTo(Screen.AuthPhone) { inclusive = true }
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.AuthProfile> {
            AuthProfileRoute(
                onRegistrationComplete = {
                    navController.navigate(Screen.CustomerCategory) {
                        popUpTo(0)
                    }
                }
            )
        }
        
        composable<Screen.CustomerCategory> {
            CustomerCategoryRoute(
                onNavigateNext = { navController.navigate(Screen.SystemType) }
            )
        }
        
        composable<Screen.SystemType> {
            SystemTypeRoute(
                onNavigateNext = { navController.navigate(Screen.AssessmentInput) }
            )
        }
        
        composable<Screen.AssessmentInput> {
            AssessmentInputRoute(
                onNavigateNext = { navController.navigate(Screen.Recommendation) }
            )
        }
        
        composable<Screen.Recommendation> {
            RecommendationRoute(
                onNavigateNext = { navController.navigate(Screen.PanelSelection) }
            )
        }
        
        composable<Screen.PanelSelection> {
            PanelSelectionRoute(
                onNavigateNext = { navController.navigate(Screen.CostEstimation) }
            )
        }
        
        composable<Screen.CostEstimation> {
            CostEstimationRoute(
                onNavigateNext = { navController.navigate(Screen.PaymentOption) }
            )
        }
        
        composable<Screen.PaymentOption> {
            PaymentOptionRoute(
                onNavigateNext = { navController.navigate(Screen.OrderSummary) }
            )
        }
        
        composable<Screen.OrderSummary> {
            OrderSummaryRoute(
                onNavigateNext = { navController.navigate(Screen.SiteSurveyStatus) }
            )
        }
        
        composable<Screen.SiteSurveyStatus> {
            SiteSurveyStatusRoute(
                onNavigateNext = { navController.navigate(Screen.InstallationTracking) }
            )
        }
        
        composable<Screen.InstallationTracking> {
            InstallationTrackingRoute()
        }
    }
}

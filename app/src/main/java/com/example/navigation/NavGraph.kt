package com.example.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.example.feature.auth.ui.AuthPhoneRoute
import com.example.feature.auth.ui.AuthOtpRoute
import com.example.feature.auth.ui.AuthProfileRoute
import com.example.feature.ebservice.ui.EbServiceNumberRoute
import com.example.feature.assessment.ui.CustomerCategoryRoute
import com.example.feature.assessment.ui.SystemTypeRoute
import com.example.feature.assessment.ui.AssessmentInputRoute
import com.example.feature.assessment.ui.RecommendationRoute
import com.example.feature.assessment.ui.RecommendationResult
import com.example.feature.assessment.ui.PanelSelectionRoute
import com.example.feature.assessment.ui.CostEstimationRoute
import com.example.feature.order.ui.PaymentOptionRoute
import com.example.feature.order.ui.OrderSummaryRoute
import com.example.feature.order.ui.SiteSurveyStatusRoute
import com.example.feature.order.ui.InstallationTrackingRoute
import com.example.feature.dashboard.ui.DashboardRoute
import com.example.feature.systemdetails.ui.SystemDetailsRoute
import com.example.feature.performance.ui.PerformanceRoute
import com.example.feature.financial.ui.FinancialRoute
import com.example.feature.environmental.ui.EnvironmentalRoute

@Composable
fun SolarEaseNavGraph(navController: NavHostController, startDestination: Any = Screen.AuthPhone) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable<Screen.AuthPhone> {
            AuthPhoneRoute(
                onNavigateToOtp = { fullName, phone ->
                    navController.navigate(Screen.AuthOtp(fullName, phone))
                }
            )
        }
        
        composable<Screen.AuthOtp> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.AuthOtp>()
            AuthOtpRoute(
                fullName = args.fullName,
                phoneNumber = args.phoneNumber,
                onNavigateToProfile = {
                    navController.navigate(Screen.AuthProfile)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable<Screen.AuthProfile> {
            AuthProfileRoute(
                onRegistrationComplete = {
                    navController.navigate(Screen.EbServiceNumber)
                }
            )
        }
        
        composable<Screen.EbServiceNumber> {
            EbServiceNumberRoute(
                onNavigateNext = { 
                    navController.navigate(Screen.CustomerCategory)
                }
            )
        }
        
        composable<Screen.CustomerCategory> {
            CustomerCategoryRoute(
                onNavigateNext = { customerType -> navController.navigate(Screen.SystemType(customerType)) }
            )
        }
        
        composable<Screen.SystemType> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.SystemType>()
            SystemTypeRoute(
                onNavigateNext = { systemType -> navController.navigate(Screen.AssessmentInput(args.customerType, systemType)) },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.AssessmentInput> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.AssessmentInput>()
            AssessmentInputRoute(
                systemType = args.systemType,
                customerType = args.customerType,
                onNavigateNext = { result -> 
                    navController.navigate(Screen.Recommendation(
                        monthlyConsumption = result.monthlyConsumption,
                        availableArea = result.availableArea,
                        mountType = result.mountType,
                        systemType = result.systemType,
                        customerType = result.customerType,
                        recommendedSystemSizeKw = result.recommendedSystemSizeKw,
                        requiredAreaSqM = result.requiredAreaSqM,
                        isFeasible = result.isFeasible
                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.Recommendation> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.Recommendation>()
            val result = RecommendationResult(
                monthlyConsumption = args.monthlyConsumption,
                availableArea = args.availableArea,
                mountType = args.mountType,
                systemType = args.systemType,
                customerType = args.customerType,
                recommendedSystemSizeKw = args.recommendedSystemSizeKw,
                requiredAreaSqM = args.requiredAreaSqM,
                isFeasible = args.isFeasible
            )
            RecommendationRoute(
                result = result,
                onNavigateNext = { 
                    navController.navigate(Screen.PanelSelection(
                        monthlyConsumption = args.monthlyConsumption,
                        availableArea = args.availableArea,
                        mountType = args.mountType,
                        systemType = args.systemType,
                        customerType = args.customerType,
                        recommendedSystemSizeKw = args.recommendedSystemSizeKw,
                        requiredAreaSqM = args.requiredAreaSqM,
                        isFeasible = args.isFeasible
                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.PanelSelection> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PanelSelection>()
            PanelSelectionRoute(
                onNavigateNext = { panelType ->
                    navController.navigate(Screen.CostEstimation(
                        monthlyConsumption = args.monthlyConsumption,
                        availableArea = args.availableArea,
                        mountType = args.mountType,
                        systemType = args.systemType,
                        panelType = panelType,
                        customerType = args.customerType
                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.CostEstimation> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.CostEstimation>()
            CostEstimationRoute(
                monthlyConsumption = args.monthlyConsumption,
                availableArea = args.availableArea,
                mountType = args.mountType,
                systemType = args.systemType,
                panelType = args.panelType,
                customerType = args.customerType,
                onNavigateNext = { result -> 
                    navController.navigate(Screen.PaymentOption(
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
                    ))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable<Screen.PaymentOption> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.PaymentOption>()
            val estimateResult = com.example.feature.assessment.util.SolarEstimateResult(
                recommendedCapacityKW = args.recommendedCapacityKW,
                requiredArea = args.requiredArea,
                availableArea = args.availableArea,
                areaStatus = args.areaStatus,
                systemType = args.systemType,
                mountType = args.mountType,
                panelType = args.panelType,
                customerType = args.customerType,
                baseRatePerKW = args.baseRatePerKW,
                panelsCost = args.panelsCost,
                mountingCost = args.mountingCost,
                labourCost = args.labourCost,
                foundationCost = args.foundationCost,
                grossTotal = args.grossTotal,
                subsidyEligible = args.subsidyEligible,
                subsidyReason = args.subsidyReason,
                subsidyAmount = args.subsidyAmount,
                netPayable = args.netPayable,
                annualSavings = args.annualSavings,
                estimatedPaybackYears = args.estimatedPaybackYears
            )
            PaymentOptionRoute(
                estimateResult = estimateResult,
                onNavigateNext = { paymentMethod -> 
                    navController.navigate(Screen.OrderSummary(
                        recommendedCapacityKW = estimateResult.recommendedCapacityKW,
                        systemType = estimateResult.systemType,
                        mountType = estimateResult.mountType,
                        panelType = estimateResult.panelType,
                        paymentMethod = paymentMethod,
                        grossTotal = estimateResult.grossTotal,
                        subsidyAmount = estimateResult.subsidyAmount,
                        netPayable = estimateResult.netPayable
                    )) 
                }
            )
        }
        
        composable<Screen.OrderSummary> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.OrderSummary>()
            OrderSummaryRoute(
                recommendedCapacityKW = args.recommendedCapacityKW,
                systemType = args.systemType,
                mountType = args.mountType,
                panelType = args.panelType,
                paymentMethod = args.paymentMethod,
                grossTotal = args.grossTotal,
                subsidyAmount = args.subsidyAmount,
                netPayable = args.netPayable,
                onNavigateNext = { navController.navigate(Screen.SiteSurveyStatus(systemType = args.systemType)) }
            )
        }
        
        composable<Screen.SiteSurveyStatus> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.SiteSurveyStatus>()
            SiteSurveyStatusRoute(
                onNavigateNext = { navController.navigate(Screen.InstallationTracking(systemType = args.systemType)) }
            )
        }
        
        composable<Screen.InstallationTracking> { backStackEntry ->
            val args = backStackEntry.toRoute<Screen.InstallationTracking>()
            InstallationTrackingRoute(
                systemType = args.systemType,
                onNavigateNext = {
                    navController.navigate(Screen.CustomerDashboard) {
                        popUpTo<Screen.AuthPhone> { inclusive = true }
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
    }
}

package com.example.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object AuthGraph : Screen()

    @Serializable
    data object AuthMain : Screen()

    @Serializable
    data object AuthPhone : Screen()
    
    @Serializable
    data class AuthOtp(val fullName: String, val phoneNumber: String) : Screen()
    

    
    // New Routes
    @Serializable
    data object Home : Screen()

    @Serializable
    data object Analytics : Screen()

    @Serializable
    data object Recommends : Screen()
    
    @Serializable
    data object BillCalculator : Screen()
    
    @Serializable
    data object ElectricityChatbot : Screen()
    
    @Serializable
    data object SolarEaseLens : Screen()
    
    @Serializable
    data object LensMain : Screen()
    
    @Serializable
    data object LensCamera : Screen()

    @Serializable
    data object LensDetails : Screen()

    @Serializable
    data object LensReview : Screen()

    @Serializable
    data object LensResult : Screen()
    
    @Serializable
    data object TnebBillStatus : Screen()

    @Serializable
    data object MeterScanner : Screen()

    @Serializable
    data object BillHistory : Screen()
    
    @Serializable
    data object SolarPanel : Screen()

    // Solar Vendor Flow (Stitch 12-Step Journey)
    @Serializable
    data object EbServiceNumber : Screen()

    @Serializable
    data object CustomerCategory : Screen()

    @Serializable
    data object SystemType : Screen()

    @Serializable
    data object MonthlyConsumption : Screen()

    @Serializable
    data object MountType : Screen()

    @Serializable
    data object AvailableArea : Screen()

    @Serializable
    data object Recommendation : Screen()

    @Serializable
    data object PanelSelection : Screen()

    @Serializable
    data object NearbyVendors : Screen()

    @Serializable
    data class VendorProfile(val vendorId: String = "abc_solar") : Screen()

    @Serializable
    data object EstimateRequest : Screen()

    @Serializable
    data object RequestConfirmation : Screen()

    @Serializable
    data class AssessmentInput(
        val customerType: String = "Residential",
        val systemType: String = "On-Grid"
    ) : Screen()

    @Serializable
    data class CostEstimation(
        val monthlyConsumption: Double,
        val availableArea: Double,
        val mountType: String,
        val systemType: String,
        val panelType: String,
        val customerType: String
    ) : Screen()

    @Serializable
    data class PaymentOption(
        val recommendedCapacityKW: Double,
        val requiredArea: Double,
        val availableArea: Double,
        val areaStatus: String,
        val systemType: String,
        val mountType: String,
        val panelType: String,
        val customerType: String,
        val baseRatePerKW: Double,
        val panelsCost: Double,
        val mountingCost: Double,
        val labourCost: Double,
        val foundationCost: Double,
        val grossTotal: Double,
        val subsidyEligible: Boolean,
        val subsidyReason: String,
        val subsidyAmount: Double,
        val netPayable: Double,
        val annualSavings: Double,
        val estimatedPaybackYears: Double
    ) : Screen()

    @Serializable
    data class OrderSummary(
        val recommendedCapacityKW: Double,
        val systemType: String,
        val mountType: String,
        val panelType: String,
        val paymentMethod: String,
        val grossTotal: Double,
        val subsidyAmount: Double,
        val netPayable: Double
    ) : Screen()

    @Serializable
    data class SiteSurveyStatus(val systemType: String) : Screen()

    @Serializable
    data class InstallationTracking(val systemType: String) : Screen()

    @Serializable
    data object CustomerDashboard : Screen()
    
    @Serializable
    data object MySolarSystem : Screen()
    
    @Serializable
    data object Performance : Screen()
    
    @Serializable
    data object Financial : Screen()
    
    @Serializable
    data object Environmental : Screen()

    @Serializable
    data object Profile : Screen()
}

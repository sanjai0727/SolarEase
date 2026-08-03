package com.example.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object AuthPhone : Screen()
    
    @Serializable
    data class AuthOtp(val fullName: String, val phoneNumber: String) : Screen()
    
    @Serializable
    data object AuthProfile : Screen()
    
    @Serializable
    data object EbServiceNumber : Screen()
    @Serializable
    data object CustomerCategory : Screen()
    
    @Serializable
    data class SystemType(val customerType: String) : Screen()
    
    @Serializable
    data class AssessmentInput(
        val customerType: String,
        val systemType: String
    ) : Screen()
    
    @Serializable
    data class Recommendation(
        val monthlyConsumption: Double,
        val availableArea: Double,
        val mountType: String,
        val systemType: String,
        val customerType: String,
        val recommendedSystemSizeKw: Double,
        val requiredAreaSqM: Double,
        val isFeasible: Boolean
    ) : Screen()
    
    @Serializable
    data class PanelSelection(
        val monthlyConsumption: Double,
        val availableArea: Double,
        val mountType: String,
        val systemType: String,
        val customerType: String,
        val recommendedSystemSizeKw: Double,
        val requiredAreaSqM: Double,
        val isFeasible: Boolean
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
}

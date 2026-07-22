package com.example.navigation

import kotlinx.serialization.Serializable

sealed class Screen {
    @Serializable
    data object AuthPhone : Screen()
    
    @Serializable
    data class AuthOtp(val phoneNumber: String) : Screen()
    
    @Serializable
    data object AuthProfile : Screen()
    
    @Serializable
    data object CustomerCategory : Screen()
    
    @Serializable
    data object SystemType : Screen()
    
    @Serializable
    data object AssessmentInput : Screen()
    
    @Serializable
    data object Recommendation : Screen()
    
    @Serializable
    data object PanelSelection : Screen()
    
    @Serializable
    data object CostEstimation : Screen()
    
    @Serializable
    data object PaymentOption : Screen()
    
    @Serializable
    data object OrderSummary : Screen()
    
    @Serializable
    data object SiteSurveyStatus : Screen()
    
    @Serializable
    data object InstallationTracking : Screen()
}
